package com.lygone.smarttvsimulator

import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class WeatherInfo(
    val tempCelsius: Int,
    val conditionText: String,
    val isDay: Boolean,
    val code: Int
)

object LiveInfoRepository {

    // Coordenadas de Arequipa, Perú (actualiza automáticamente cada 30 min)
    private const val WEATHER_URL =
        "https://api.open-meteo.com/v1/forecast?latitude=-16.4090&longitude=-71.5375&current=temperature_2m,is_day,weather_code&timezone=America%2FLima"

    // Feed RSS oficial de noticias de Perú (RPP)
    private const val NEWS_RSS_URL = "https://rpp.pe/feed"

    fun fetchWeather(): WeatherInfo? {
        return try {
            val jsonStr = httpGet(WEATHER_URL) ?: return null
            val current = JSONObject(jsonStr).getJSONObject("current")
            val temp = current.getDouble("temperature_2m").roundToInt()
            val isDay = current.optInt("is_day", 1) == 1
            val code = current.optInt("weather_code", 0)
            WeatherInfo(
                tempCelsius = temp,
                conditionText = weatherDescription(code),
                isDay = isDay,
                code = code
            )
        } catch (_: Exception) {
            null
        }
    }

    fun fetchPeruNews(limit: Int = 10): List<String> {
        return try {
            val xml = httpGet(NEWS_RSS_URL) ?: return emptyList()
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            val headlines = ArrayList<String>()
            var insideItem = false
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT && headlines.size < limit) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> insideItem = true
                        "title" -> if (insideItem) {
                            val title = parser.nextText().trim()
                            if (title.isNotBlank()) headlines.add(title)
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && parser.name == "item") {
                    insideItem = false
                }
                eventType = parser.next()
            }
            headlines
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "LYGANE-SmartTV/2.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun weatherDescription(code: Int): String = when (code) {
        0 -> "Despejado"
        1, 2 -> "Parc. nublado"
        3 -> "Nublado"
        45, 48 -> "Neblina"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Lluvia"
        71, 73, 75 -> "Nieve"
        95, 96, 99 -> "Tormenta"
        else -> "Arequipa"
    }
}
