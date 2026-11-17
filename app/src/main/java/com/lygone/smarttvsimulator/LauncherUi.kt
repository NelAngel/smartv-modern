@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.lygone.smarttvsimulator

import android.app.UiModeManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────────── Paleta Hisense VIDAA + Sunset ─────────────────────────────

private val Ink = Color(0xFF070B14)
private val SurfaceGlass = Color(0xCC0D1322)
private val SurfaceCard = Color(0xE6141C2E)
private val TextHi = Color(0xFFFFFFFF)
private val TextLo = Color(0xFFD0D8EA)
private val AngelCyan = Color(0xFF4FA8FF)
private val VidaaTeal = Color(0xFF00D4C5)
private val Gold = Color(0xFFFFD166)

private enum class NavDestination(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Rounded.Home),
    Apps("Apps", Icons.Rounded.Apps),
    Favorites("Favoritos", Icons.Rounded.Star),
    Search("Buscar", Icons.Rounded.Search),
    Settings("Ajustes", Icons.Rounded.Settings)
}

// ───────────────────────────── Pantalla principal ─────────────────────────────

@Composable
fun LauncherApp(homeSignal: Int, packageSignal: Int, resumeSignal: Int) {
    val context = LocalContext.current
    val isTv = remember { isTelevision(context) }
    val dpad = remember { isTv || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) }

    var apps by remember { mutableStateOf<List<TvApp>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var favorites by remember { mutableStateOf(Prefs.favorites(context)) }
    var recents by remember { mutableStateOf(Prefs.recents(context)) }
    var activeSection by remember { mutableStateOf(NavDestination.Home) }
    var search by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val spotlight = remember { mutableStateOf<TvApp?>(null) }

    val now = rememberNow()
    val isDefaultHome = remember(resumeSignal) { checkDefaultHome(context) }
    val scope = rememberCoroutineScope()
    val firstFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    LaunchedEffect(packageSignal, reload) {
        apps = withContext(Dispatchers.Default) { AppRepository.load(context, isTv) }
        loaded = true
    }

    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val recentApps = remember(apps, recents) { recents.mapNotNull { byPackage[it] } }
    val lastUsedApp = recentApps.firstOrNull() ?: apps.firstOrNull()
    val favoriteApps = remember(apps, favorites) { apps.filter { it.packageName in favorites } }

    // Orden estilo VIDAA: Última app usada primero, luego Favoritos, luego el resto
    val homeCarouselApps = remember(apps, recentApps, favoriteApps) {
        val ordered = LinkedHashMap<String, TvApp>()
        recentApps.forEach { ordered[it.packageName] = it }
        favoriteApps.forEach { ordered[it.packageName] = it }
        apps.forEach { ordered[it.packageName] = it }
        ordered.values.toList()
    }

    val query = search.trim()
    val searchResults = remember(apps, query) {
        if (query.isEmpty()) apps else apps.filter { it.name.contains(query, ignoreCase = true) }
    }

    val openApp: (TvApp) -> Unit = { app ->
        if (AppRepository.launch(context, app)) {
            recents = Prefs.pushRecent(context, app.packageName)
        }
    }
    val toggleFavorite: (TvApp) -> Unit = { app ->
        favorites = if (app.packageName in favorites) favorites - app.packageName else favorites + app.packageName
        Prefs.saveFavorites(context, favorites)
    }
    val onFocused: (TvApp) -> Unit = { spotlight.value = it }

    fun goHome() {
        activeSection = NavDestination.Home
        search = ""
        showSettings = false
        scope.launch {
            if (dpad) {
                delay(120)
                runCatching { firstFocus.requestFocus() }
            }
        }
    }

    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    LaunchedEffect(loaded) {
        if (loaded && dpad) {
            delay(220)
            runCatching { firstFocus.requestFocus() }
        }
    }
    LaunchedEffect(activeSection) {
        if (activeSection == NavDestination.Search) {
            delay(120)
            runCatching { searchFocus.requestFocus() }
        }
    }

    BackHandler(enabled = isDefaultHome || activeSection != NavDestination.Home) { goHome() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AngelCyan, background = Ink, surface = SurfaceCard,
            onBackground = TextHi, onSurface = TextHi
        )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
            val wide = maxWidth >= 720.dp
            val pad = if (wide) 52.dp else 20.dp
            val cardW = if (wide) 196.dp else 132.dp
            val cardH = if (wide) 118.dp else 86.dp

            // 1. Fondo panorámico sin personaje + viñeta suave para legibilidad + halo dinámico
            ScenicWallpaperLayer(spotlight)

            Column(
                Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // 2. Barra superior: Marca LYGANE a la izquierda + Perfil ANGEL y RELOJ GRANDE a la derecha
                TopStatusHeader(now = now, wide = wide, pad = pad)

                // 3. Contenido central según pestaña activa
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        !loaded -> Text(
                            "Cargando aplicaciones…",
                            color = TextHi,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        activeSection == NavDestination.Home -> {
                            VidaaHomeStage(
                                wide = wide,
                                pad = pad,
                                cardW = cardW,
                                cardH = cardH,
                                lastUsedApp = lastUsedApp,
                                hasRecents = recentApps.isNotEmpty(),
                                focusedApp = spotlight.value ?: lastUsedApp,
                                carouselApps = homeCarouselApps,
                                favorites = favorites,
                                firstFocus = firstFocus,
                                onFocused = onFocused,
                                onOpen = openApp,
                                onToggleFavorite = toggleFavorite
                            )
                        }

                        activeSection == NavDestination.Apps -> {
                            AppGridOverlay(
                                title = "Todas las aplicaciones (${apps.size})",
                                apps = apps,
                                emptyText = "No se encontraron aplicaciones compatibles.",
                                cardW = cardW,
                                cardH = cardH,
                                wide = wide,
                                pad = pad,
                                favorites = favorites,
                                focusFirst = firstFocus,
                                onFocused = onFocused,
                                onOpen = openApp,
                                onToggleFavorite = toggleFavorite
                            )
                        }

                        activeSection == NavDestination.Favorites -> {
                            AppGridOverlay(
                                title = "Favoritos",
                                apps = favoriteApps,
                                emptyText = "Aún no tienes favoritos.\nMantén pulsado OK sobre una app para fijarla aquí.",
                                cardW = cardW,
                                cardH = cardH,
                                wide = wide,
                                pad = pad,
                                favorites = favorites,
                                focusFirst = firstFocus,
                                onFocused = onFocused,
                                onOpen = openApp,
                                onToggleFavorite = toggleFavorite
                            )
                        }

                        activeSection == NavDestination.Search -> {
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.padding(horizontal = pad, vertical = 8.dp)) {
                                    SearchField(
                                        value = search,
                                        onChange = { search = it },
                                        onClose = { goHome() },
                                        focusRequester = searchFocus,
                                        modifier = Modifier.fillMaxWidth(if (wide) 0.55f else 1f)
                                    )
                                }
                                AppGridOverlay(
                                    title = if (query.isEmpty()) "Explorar aplicaciones" else "Resultados para “$query”",
                                    apps = searchResults,
                                    emptyText = "No se encontró ninguna aplicación.",
                                    cardW = cardW,
                                    cardH = cardH,
                                    wide = wide,
                                    pad = pad,
                                    favorites = favorites,
                                    focusFirst = null,
                                    onFocused = onFocused,
                                    onOpen = openApp,
                                    onToggleFavorite = toggleFavorite
                                )
                            }
                        }

                        else -> Unit
                    }
                }

                // 4. Barra de navegación inferior tipo píldora (estilo captura + Hisense VIDAA)
                BottomVidaaDock(
                    selected = activeSection,
                    wide = wide,
                    pad = pad,
                    onSelect = { dest ->
                        if (dest == NavDestination.Settings) {
                            showSettings = true
                        } else {
                            activeSection = dest
                            if (dest != NavDestination.Search) search = ""
                        }
                    }
                )
            }
        }

        if (showSettings) {
            SettingsDialog(
                isDefaultHome = isDefaultHome,
                appCount = apps.size,
                onSetHome = {
                    showSettings = false
                    requestDefaultHome(context) { intent -> roleLauncher.launch(intent) }
                },
                onRefresh = { showSettings = false; reload++ },
                onClose = { showSettings = false }
            )
        }
    }
}

// ───────────────────────────── Fondo Escénico Limpio ─────────────────────────────

@Composable
private fun ScenicWallpaperLayer(spotlight: State<TvApp?>) {
    val accent = spotlight.value?.accent ?: AngelCyan
    val animatedAccent by animateColorAsState(accent, tween(650), label = "accent-glow")

    Box(Modifier.fillMaxSize()) {
        // Imagen de fondo sin personaje (guarda la imagen generada como res/drawable-nodpi/launcher_bg.jpg)
        Image(
            painter = painterResource(id = R.drawable.launcher_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Degradado sutil inferior y lateral para contraste impecable de textos y tarjetas
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color(0x4D060A14),
                    0.35f to Color(0x1A060A14),
                    0.68f to Color(0x88060A14),
                    1.0f to Color(0xE6050811)
                )
            )
        )

        // Resplandor ambiental dinámico según la app enfocada
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to animatedAccent.copy(alpha = 0.24f),
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.22f, size.height * 0.78f),
                        radius = size.width * 0.55f
                    )
                )
            }
        )
    }
}

// ───────────────────────────── Header Superior con Reloj Grande ─────────────────────────────

@Composable
private fun TopStatusHeader(now: State<Long>, wide: Boolean, pad: Dp) {
    val context = LocalContext.current
    val ms = now.value

    Row(
        Modifier.fillMaxWidth().padding(horizontal = pad, vertical = if (wide) 18.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Identidad discreta LYGANE Smart TV
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(SurfaceGlass)
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(10.dp).clip(CircleShape).background(VidaaTeal)
            )
            Text(
                "LYGANE TV",
                color = TextHi,
                fontSize = if (wide) 13.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.weight(1f))

        // Bloque derecho: Perfil ANGEL + Wi-Fi + RELOJ GRANDE
        Row(
            Modifier.clip(RoundedCornerShape(22.dp))
                .background(SurfaceGlass)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
                .padding(horizontal = if (wide) 20.dp else 14.dp, vertical = if (wide) 10.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 10.dp)
        ) {
            // Avatar minimalista de perfil
            Box(
                Modifier.size(if (wide) 40.dp else 32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AngelCyan, VidaaTeal))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(if (wide) 24.dp else 19.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "ANGEL",
                        color = TextHi,
                        fontSize = if (wide) 15.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        Icons.Rounded.Wifi,
                        contentDescription = "Wi-Fi conectado",
                        tint = VidaaTeal,
                        modifier = Modifier.size(if (wide) 17.dp else 14.dp)
                    )
                }
                Text(
                    formatDateShort(ms),
                    color = TextLo,
                    fontSize = if (wide) 12.sp else 10.sp
                )
            }

            // Divisor vertical
            Box(
                Modifier.width(1.dp).height(if (wide) 36.dp else 28.dp)
                    .background(Color.White.copy(alpha = 0.22f))
            )

            // HORA EN TAMAÑO GRANDE
            Text(
                text = formatTime(context, ms),
                color = TextHi,
                fontSize = if (wide) 40.sp else 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x80000000),
                        offset = Offset(0f, 3f),
                        blurRadius = 8f
                    )
                )
            )
        }
    }
}

// ───────────────────────────── Escenario Principal (Saludo + Última App + Carrusel) ─────────────────────────────

@Composable
private fun VidaaHomeStage(
    wide: Boolean,
    pad: Dp,
    cardW: Dp,
    cardH: Dp,
    lastUsedApp: TvApp?,
    hasRecents: Boolean,
    focusedApp: TvApp?,
    carouselApps: List<TvApp>,
    favorites: Set<String>,
    firstFocus: FocusRequester,
    onFocused: (TvApp) -> Unit,
    onOpen: (TvApp) -> Unit,
    onToggleFavorite: (TvApp) -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Fila superior al carrusel: Saludo "ANGEL Estamos listos para usar" a la izquierda
        // y tarjeta destacada de "Última app usada" a la derecha
        Row(
            Modifier.fillMaxWidth().padding(horizontal = pad),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Bloque de bienvenida estilo captura
            Column(Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextHi, fontWeight = FontWeight.ExtraBold)) {
                            append("¡Hola, ")
                        }
                        withStyle(SpanStyle(color = AngelCyan, fontWeight = FontWeight.ExtraBold)) {
                            append("ANGEL")
                        }
                        withStyle(SpanStyle(color = TextHi, fontWeight = FontWeight.ExtraBold)) {
                            append("!")
                        }
                    },
                    fontSize = if (wide) 52.sp else 32.sp,
                    lineHeight = if (wide) 56.sp else 36.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x99000000),
                            offset = Offset(0f, 4f),
                            blurRadius = 12f
                        )
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Estamos listos para usar",
                    color = TextHi.copy(alpha = 0.94f),
                    fontSize = if (wide) 24.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x88000000),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
                Spacer(Modifier.height(10.dp))
                // Línea decorativa azul igual que en el diseño de referencia
                Box(
                    Modifier.width(if (wide) 78.dp else 54.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(AngelCyan, VidaaTeal)))
                )
            }

            // Tarjeta rápida de la Última App Usada (estilo VIDAA Quick Resume)
            if (lastUsedApp != null) {
                LastUsedAppBadge(
                    app = lastUsedApp,
                    isRecent = hasRecents,
                    wide = wide,
                    onOpen = { onOpen(lastUsedApp) }
                )
            }
        }

        Spacer(Modifier.height(if (wide) 22.dp else 14.dp))

        // Etiqueta dinámica de la app enfocada actualmente
        if (focusedApp != null) {
            Row(
                Modifier.padding(horizontal = pad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = focusedApp.name,
                    color = TextHi,
                    fontSize = if (wide) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "•  OK para abrir  •  Mantén OK para favorito",
                    color = TextLo.copy(alpha = 0.85f),
                    fontSize = if (wide) 12.sp else 11.sp
                )
            }
        }

        // Carrusel horizontal estilo Hisense VIDAA / captura de referencia
        LazyRow(
            contentPadding = PaddingValues(horizontal = pad, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(if (wide) 18.dp else 12.dp)
        ) {
            itemsIndexed(carouselApps, key = { _, a -> a.packageName }) { index, app ->
                val isLastUsed = index == 0 && hasRecents
                VidaaAppCard(
                    app = app,
                    width = cardW,
                    height = cardH,
                    wide = wide,
                    isFavorite = app.packageName in favorites,
                    isLastUsed = isLastUsed,
                    focusRequester = if (index == 0) firstFocus else null,
                    onFocused = onFocused,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun LastUsedAppBadge(
    app: TvApp,
    isRecent: Boolean,
    wide: Boolean,
    onOpen: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "resume-scale")
    val shape = RoundedCornerShape(18.dp)

    Row(
        Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(SurfaceGlass)
            .border(
                width = if (focused) 2.5.dp else 1.dp,
                color = if (focused) Color.White else app.accent.copy(alpha = 0.65f),
                shape = shape
            )
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onOpen)
            .padding(horizontal = if (wide) 16.dp else 12.dp, vertical = if (wide) 12.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(if (wide) 46.dp else 36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(app.accent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Image(app.icon, contentDescription = app.name, modifier = Modifier.size(if (wide) 34.dp else 26.dp))
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = VidaaTeal, modifier = Modifier.size(13.dp))
                Text(
                    text = if (isRecent) "ÚLTIMA APP USADA" else "LISTA PARA ABRIR",
                    color = VidaaTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = app.name,
                color = TextHi,
                fontSize = if (wide) 16.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = if (wide) 180.dp else 110.dp)
            )
        }

        if (wide) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(if (focused) Color.White else AngelCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Abrir", tint = Ink, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ───────────────────────────── Tarjetas Panorámicas Estilo VIDAA ─────────────────────────────

@Composable
private fun VidaaAppCard(
    app: TvApp,
    width: Dp,
    height: Dp,
    wide: Boolean,
    isFavorite: Boolean,
    isLastUsed: Boolean,
    focusRequester: FocusRequester?,
    onFocused: (TvApp) -> Unit,
    onOpen: (TvApp) -> Unit,
    onToggleFavorite: (TvApp) -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) 1.09f else 1f,
        spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "card-scale"
    )
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    val currentOnFocused by rememberUpdatedState(onFocused)

    LaunchedEffect(focused) { if (focused) currentOnFocused(app) }

    Column(
        Modifier.width(width).onKeyEvent { e ->
            if (e.type == KeyEventType.KeyUp && e.key == Key.Menu) {
                onToggleFavorite(app)
                true
            } else {
                false
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(width, height)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(if (focused) Modifier.glow(app.accent, 0.65f) else Modifier)
                .clip(shape)
                .background(SurfaceCard)
                .background(
                    Brush.linearGradient(
                        listOf(
                            app.accent.copy(alpha = if (focused) 0.55f else 0.30f),
                            Color(0xCC0D1322)
                        )
                    )
                )
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) Color.White else Color.White.copy(alpha = 0.16f),
                    shape = shape
                )
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    role = Role.Button,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite(app)
                    },
                    onClick = { onOpen(app) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                app.icon,
                contentDescription = app.name,
                modifier = Modifier.size(height * 0.56f)
            )

            // Etiqueta superior izquierda si es la última app usada
            if (isLastUsed) {
                Box(
                    Modifier.align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(VidaaTeal)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("RECIENTE", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Estrella de favorito
            if (isFavorite) {
                Box(
                    Modifier.align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xB3000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(14.dp))
                }
            }

            // Barra inferior con nombre al enfocar (igual que la tarjeta seleccionada en la referencia)
            if (focused) {
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xCC060913))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name,
                        color = TextHi,
                        fontSize = if (wide) 12.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Cuadrícula para Apps / Favoritos / Búsqueda ─────────────────────────────

@Composable
private fun AppGridOverlay(
    title: String,
    apps: List<TvApp>,
    emptyText: String,
    cardW: Dp,
    cardH: Dp,
    wide: Boolean,
    pad: Dp,
    favorites: Set<String>,
    focusFirst: FocusRequester?,
    onFocused: (TvApp) -> Unit,
    onOpen: (TvApp) -> Unit,
    onToggleFavorite: (TvApp) -> Unit
) {
    if (apps.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Apps, null, tint = TextLo.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(emptyText, color = TextHi, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = cardW + 16.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = pad, end = pad, top = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                title,
                color = TextHi,
                fontSize = if (wide) 22.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        gridItemsIndexed(apps, key = { _, a -> a.packageName }) { index, app ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VidaaAppCard(
                    app = app,
                    width = cardW,
                    height = cardH,
                    wide = wide,
                    isFavorite = app.packageName in favorites,
                    isLastUsed = false,
                    focusRequester = if (index == 0) focusFirst else null,
                    onFocused = onFocused,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite
                )
                Text(
                    text = app.name,
                    color = TextHi,
                    fontSize = if (wide) 13.sp else 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp).width(cardW),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ───────────────────────────── Dock Inferior Tipo Píldora (VIDAA + Referencia) ─────────────────────────────

@Composable
private fun BottomVidaaDock(
    selected: NavDestination,
    wide: Boolean,
    pad: Dp,
    onSelect: (NavDestination) -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = pad, vertical = if (wide) 14.dp else 10.dp)
            .clip(shape)
            .background(SurfaceGlass)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(horizontal = if (wide) 16.dp else 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavDestination.values().forEach { item ->
            val isSelected = item == selected
            val source = remember { MutableInteractionSource() }
            val focused by source.collectIsFocusedAsState()

            val bg by animateColorAsState(
                when {
                    focused -> Color.White
                    isSelected -> Color.White.copy(alpha = 0.18f)
                    else -> Color.Transparent
                },
                tween(150),
                label = "dock-bg"
            )
            val fg = when {
                focused -> Ink
                isSelected -> TextHi
                else -> TextLo
            }

            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable(interactionSource = source, indication = null, role = Role.Tab) { onSelect(item) }
                    .padding(horizontal = if (wide) 22.dp else 12.dp, vertical = if (wide) 10.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(if (wide) 20.dp else 18.dp))
                if (wide || isSelected) {
                    Text(
                        text = item.label,
                        color = fg,
                        fontSize = if (wide) 15.sp else 12.sp,
                        fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Buscador, Ajustes y Utilidades ─────────────────────────────

@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier.height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCard)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = AngelCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Buscar aplicación…", color = TextLo, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = TextHi, fontSize = 15.sp),
                cursorBrush = SolidColor(AngelCyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, "Cerrar", tint = TextHi, modifier = Modifier.size(18.dp))
        }
    }
}

private fun Modifier.glow(color: Color, strength: Float): Modifier = this.drawBehind {
    val radius = this.size.maxDimension * 0.58f + 16.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            0.55f to color.copy(alpha = strength),
            1f to Color.Transparent,
            center = center,
            radius = radius
        ),
        radius = radius
    )
}

@Composable
private fun SettingsDialog(
    isDefaultHome: Boolean,
    appCount: Int,
    onSetHome: () -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    val firstButton = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstButton.requestFocus() }
    }
    val shape = RoundedCornerShape(26.dp)
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.widthIn(max = 460.dp)
                .clip(shape)
                .background(Color(0xF50F1626))
                .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
                .padding(26.dp)
        ) {
            Text("LYGANE Smart TV · Perfil ANGEL", color = TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("$appCount aplicaciones instaladas", color = VidaaTeal, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (isDefaultHome) {
                    "✓ LYGANE está configurado como launcher principal."
                } else {
                    "Activa LYGANE como pantalla de inicio para volver aquí automáticamente al presionar Home."
                },
                color = TextLo,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isDefaultHome) {
                    DialogPill("Usar como pantalla de inicio", true, Modifier.focusRequester(firstButton), onSetHome)
                    DialogPill("Actualizar lista de apps", false, Modifier, onRefresh)
                } else {
                    DialogPill("Actualizar lista de apps", true, Modifier.focusRequester(firstButton), onRefresh)
                }
                DialogPill("Cerrar", false, Modifier, onClose)
            }
        }
    }
}

@Composable
private fun DialogPill(text: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val bg = when {
        focused -> Color.White
        primary -> AngelCyan
        else -> Color.White.copy(alpha = 0.12f)
    }
    val fg = if (focused || primary) Ink else TextHi
    Box(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun rememberNow(): State<Long> {
    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = System.currentTimeMillis()
            delay(10_000)
        }
    }
    return now
}

private fun isTelevision(context: Context): Boolean {
    val mode = (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
    return mode == Configuration.UI_MODE_TYPE_TELEVISION ||
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

private fun checkDefaultHome(context: Context): Boolean = try {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    resolved?.activityInfo?.packageName == context.packageName
} catch (_: Exception) {
    false
}

private fun requestDefaultHome(context: Context, launchRole: (Intent) -> Unit) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                launchRole(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private fun formatTime(context: Context, millis: Long): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date(millis))

private fun formatDateShort(millis: Long): String {
    val spanish = Locale("es", "PE")
    return SimpleDateFormat("EEE, d MMM", spanish).format(Date(millis))
        .replaceFirstChar { it.titlecase(spanish) }
}
