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
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ───────────────────────────── Paleta ─────────────────────────────

private val Ink = Color(0xFF05070D)
private val Surface1 = Color(0xFF0F1524)
private val Surface2 = Color(0xFF182036)
private val TextHi = Color(0xFFF5F7FC)
private val TextLo = Color(0xFF8F9BB5)
private val Brand = Color(0xFF7AA2FF)
private val Gold = Color(0xFFFFD166)

private enum class HomeTab(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Rounded.Home),
    Apps("Apps", Icons.Rounded.Apps),
    Favorites("Favoritos", Icons.Rounded.Star)
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
    var tab by remember { mutableStateOf(HomeTab.Home) }
    var search by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val spotlight = remember { mutableStateOf<TvApp?>(null) }
    val now = rememberNow()
    val isDefaultHome = remember(resumeSignal) { checkDefaultHome(context) }
    val listState = rememberLazyListState()
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
    val favoriteApps = remember(apps, favorites) { apps.filter { it.packageName in favorites } }
    val query = search.trim()
    val results = remember(apps, query) {
        if (query.isEmpty()) apps else apps.filter { it.name.contains(query, ignoreCase = true) }
    }

    val openApp: (TvApp) -> Unit = { app ->
        if (AppRepository.launch(context, app)) recents = Prefs.pushRecent(context, app.packageName)
    }
    val toggleFavorite: (TvApp) -> Unit = { app ->
        favorites = if (app.packageName in favorites) favorites - app.packageName else favorites + app.packageName
        Prefs.saveFavorites(context, favorites)
    }
    val onFocused: (TvApp) -> Unit = { spotlight.value = it }

    fun goHome() {
        tab = HomeTab.Home
        search = ""
        searchOpen = false
        showSettings = false
        scope.launch {
            listState.scrollToItem(0)
            if (dpad) {
                delay(150)
                runCatching { firstFocus.requestFocus() }
            }
        }
    }

    // Pulsar Inicio con el launcher abierto lo devuelve a la pantalla de inicio.
    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    // En TV, el foco arranca en la primera app para poder navegar con el mando de inmediato.
    LaunchedEffect(loaded) {
        if (loaded && dpad) {
            delay(250)
            runCatching { firstFocus.requestFocus() }
        }
    }
    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            delay(120)
            runCatching { searchFocus.requestFocus() }
        }
    }
    // Como launcher predeterminado, Atrás no debe cerrar la pantalla de inicio.
    BackHandler(enabled = isDefaultHome || searchOpen || tab != HomeTab.Home) { goHome() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Brand, background = Ink, surface = Surface1,
            onBackground = TextHi, onSurface = TextHi
        )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
            val wide = maxWidth >= 720.dp
            val pad = if (wide) 48.dp else 20.dp
            val tile = if (wide) (maxHeight * 0.19f).coerceIn(96.dp, 136.dp) else 78.dp
            val heroHeight = if (wide) (maxHeight * 0.36f).coerceIn(170.dp, 280.dp) else 150.dp

            AmbientGlow(spotlight)

            Column(
                Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .then(if (wide) Modifier.navigationBarsPadding() else Modifier)
                    .imePadding()
            ) {
                if (wide) {
                    TvTopBar(
                        tab = tab, searchOpen = searchOpen, search = search, dpad = dpad, now = now,
                        searchFocus = searchFocus,
                        onTab = { t -> tab = t; searchOpen = false; search = "" },
                        onSearchChange = { search = it },
                        onOpenSearch = { searchOpen = true },
                        onCloseSearch = { searchOpen = false; search = "" },
                        onSettings = { showSettings = true }
                    )
                } else {
                    PhoneTopBar(
                        searchOpen = searchOpen, search = search, now = now, searchFocus = searchFocus,
                        onSearchChange = { search = it },
                        onOpenSearch = { searchOpen = true },
                        onCloseSearch = { searchOpen = false; search = "" },
                        onSettings = { showSettings = true }
                    )
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        !loaded -> Text(
                            "Cargando aplicaciones…", color = TextLo, fontSize = 15.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        searchOpen -> AppGrid(
                            title = if (query.isEmpty()) "Todas las aplicaciones" else "Resultados para “$query”",
                            apps = results, emptyText = "No se encontró ninguna aplicación.",
                            tile = tile, wide = wide, pad = pad, favorites = favorites, focusFirst = null,
                            onFocused = onFocused, onOpen = openApp, onToggleFavorite = toggleFavorite
                        )
                        tab == HomeTab.Home -> HomeContent(
                            listState = listState, wide = wide, pad = pad, tile = tile, heroHeight = heroHeight,
                            dpad = dpad, greeting = greetingFor(currentHour()), spotlight = spotlight,
                            fallback = recentApps.firstOrNull() ?: apps.firstOrNull(),
                            fallbackFromRecents = recentApps.isNotEmpty(),
                            recentApps = recentApps, favoriteApps = favoriteApps, allApps = apps,
                            favorites = favorites, firstFocus = firstFocus,
                            onFocused = onFocused, onOpen = openApp, onToggleFavorite = toggleFavorite,
                            onSeeAll = { tab = HomeTab.Apps }
                        )
                        tab == HomeTab.Apps -> AppGrid(
                            title = "Todas las aplicaciones", apps = apps,
                            emptyText = "No se encontraron aplicaciones compatibles.",
                            tile = tile, wide = wide, pad = pad, favorites = favorites, focusFirst = firstFocus,
                            onFocused = onFocused, onOpen = openApp, onToggleFavorite = toggleFavorite
                        )
                        else -> AppGrid(
                            title = "Favoritos", apps = favoriteApps,
                            emptyText = if (dpad) {
                                "Aún no tienes favoritos.\nMantén pulsado OK sobre una app para fijarla aquí."
                            } else {
                                "Aún no tienes favoritos.\nMantén pulsada una app para fijarla aquí."
                            },
                            tile = tile, wide = wide, pad = pad, favorites = favorites, focusFirst = firstFocus,
                            onFocused = onFocused, onOpen = openApp, onToggleFavorite = toggleFavorite
                        )
                    }
                }

                if (!wide) {
                    FloatingNav(selected = tab) { t -> tab = t; searchOpen = false; search = "" }
                }
            }
        }

        if (showSettings) {
            SettingsDialog(
                isDefaultHome = isDefaultHome, appCount = apps.size,
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

// ───────────────────────────── Fondo ambiental ─────────────────────────────

/** Resplandor de fondo que toma el color de la app enfocada, como en Google TV / Apple TV. */
@Composable
private fun AmbientGlow(spotlight: State<TvApp?>) {
    val target = spotlight.value?.accent ?: Brand
    val color by animateColorAsState(target, tween(700), label = "ambient")
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(
                Brush.radialGradient(
                    0f to color.copy(alpha = 0.40f), 1f to Color.Transparent,
                    center = Offset(size.width * 0.85f, size.height * 0.05f), radius = size.width * 0.8f
                )
            )
            drawRect(
                Brush.radialGradient(
                    0f to Brand.copy(alpha = 0.13f), 1f to Color.Transparent,
                    center = Offset(0f, size.height), radius = size.width * 0.6f
                )
            )
        }
    )
}

// ───────────────────────────── Barras superiores ─────────────────────────────

@Composable
private fun TvTopBar(
    tab: HomeTab, searchOpen: Boolean, search: String, dpad: Boolean, now: State<Long>,
    searchFocus: FocusRequester,
    onTab: (HomeTab) -> Unit, onSearchChange: (String) -> Unit,
    onOpenSearch: () -> Unit, onCloseSearch: () -> Unit, onSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandMark()
        Spacer(Modifier.width(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HomeTab.values().forEach { t ->
                TopTab(
                    tab = t, selected = !searchOpen && t == tab,
                    selectOnFocus = dpad && !searchOpen, onSelect = { onTab(t) }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (searchOpen) {
            SearchField(search, onSearchChange, onCloseSearch, searchFocus, Modifier.width(320.dp))
        } else {
            TvIconButton(Icons.Rounded.Search, "Buscar", onOpenSearch)
        }
        Spacer(Modifier.width(20.dp))
        ClockBlock(now)
        Spacer(Modifier.width(16.dp))
        TvIconButton(Icons.Rounded.Settings, "Ajustes", onSettings)
    }
}

@Composable
private fun PhoneTopBar(
    searchOpen: Boolean, search: String, now: State<Long>, searchFocus: FocusRequester,
    onSearchChange: (String) -> Unit, onOpenSearch: () -> Unit, onCloseSearch: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searchOpen) {
            SearchField(search, onSearchChange, onCloseSearch, searchFocus, Modifier.weight(1f))
        } else {
            Box(Modifier.weight(1f)) { GreetingBlock(now) }
            TvIconButton(Icons.Rounded.Search, "Buscar", onOpenSearch)
            Spacer(Modifier.width(8.dp))
            TvIconButton(Icons.Rounded.Settings, "Ajustes", onSettings)
        }
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(Brand, Color(0xFFB794FF)))),
            contentAlignment = Alignment.Center
        ) { Text("L", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(12.dp))
        Text("LYGANE", color = TextHi, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
    }
}

@Composable
private fun ClockBlock(now: State<Long>) {
    val context = LocalContext.current
    val ms = now.value
    Column(horizontalAlignment = Alignment.End) {
        Text(formatTime(context, ms), color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.Light)
        Text(formatDate(ms), color = TextLo, fontSize = 11.sp)
    }
}

@Composable
private fun GreetingBlock(now: State<Long>) {
    val ms = now.value
    Column {
        Text(formatDate(ms), color = TextLo, fontSize = 12.sp)
        Text(greetingFor(hourOf(ms)), color = TextHi, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopTab(tab: HomeTab, selected: Boolean, selectOnFocus: Boolean, onSelect: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val currentSelect by rememberUpdatedState(onSelect)
    // En TV, mover el foco sobre una pestaña cambia el contenido al instante.
    LaunchedEffect(focused) { if (focused && selectOnFocus) currentSelect() }
    val bg by animateColorAsState(
        when {
            focused -> Color.White
            selected -> Color.White.copy(alpha = 0.14f)
            else -> Color.Transparent
        },
        tween(160), label = "tab-bg"
    )
    val fg = when {
        focused -> Ink
        selected -> TextHi
        else -> TextLo
    }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg)
            .clickable(interactionSource = source, indication = null, role = Role.Tab, onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(tab.icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(tab.label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SearchField(
    value: String, onChange: (String) -> Unit, onClose: () -> Unit,
    focusRequester: FocusRequester, modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(Surface2).padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = TextLo, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Buscar aplicaciones", color = TextLo, fontSize = 15.sp)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = TextHi, fontSize = 15.sp),
                cursorBrush = SolidColor(Brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
        TvIconButton(Icons.Rounded.Close, "Cerrar búsqueda", onClose, size = 36.dp)
    }
}

// ───────────────────────────── Contenido: Inicio ─────────────────────────────

@Composable
private fun HomeContent(
    listState: LazyListState, wide: Boolean, pad: Dp, tile: Dp, heroHeight: Dp, dpad: Boolean,
    greeting: String, spotlight: State<TvApp?>, fallback: TvApp?, fallbackFromRecents: Boolean,
    recentApps: List<TvApp>, favoriteApps: List<TvApp>, allApps: List<TvApp>,
    favorites: Set<String>, firstFocus: FocusRequester,
    onFocused: (TvApp) -> Unit, onOpen: (TvApp) -> Unit, onToggleFavorite: (TvApp) -> Unit,
    onSeeAll: () -> Unit
) {
    // La primera fila no vacía recibe el foco inicial del mando.
    val firstRow = when {
        recentApps.isNotEmpty() -> 0
        favoriteApps.isNotEmpty() -> 1
        else -> 2
    }
    val previewLimit = 16
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(if (wide) 10.dp else 6.dp)
    ) {
        item(key = "hero") {
            HeroSection(
                spotlight = spotlight, fallback = fallback, fallbackFromRecents = fallbackFromRecents,
                greeting = greeting, dpad = dpad, wide = wide, pad = pad, height = heroHeight,
                favorites = favorites, onOpen = onOpen, onToggleFavorite = onToggleFavorite
            )
        }
        if (recentApps.isNotEmpty()) {
            item(key = "recents") {
                AppRow(
                    title = "Recientes", apps = recentApps, tile = tile, wide = wide, pad = pad,
                    favorites = favorites, focusFirst = if (firstRow == 0) firstFocus else null,
                    showSeeAll = false, onFocused = onFocused, onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite, onSeeAll = onSeeAll
                )
            }
        }
        if (favoriteApps.isNotEmpty()) {
            item(key = "favorites") {
                AppRow(
                    title = "Favoritos", apps = favoriteApps, tile = tile, wide = wide, pad = pad,
                    favorites = favorites, focusFirst = if (firstRow == 1) firstFocus else null,
                    showSeeAll = false, onFocused = onFocused, onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite, onSeeAll = onSeeAll
                )
            }
        }
        item(key = "all") {
            AppRow(
                title = "Todas las aplicaciones", apps = allApps.take(previewLimit), tile = tile, wide = wide,
                pad = pad, favorites = favorites, focusFirst = if (firstRow == 2) firstFocus else null,
                showSeeAll = allApps.size > previewLimit, onFocused = onFocused, onOpen = onOpen,
                onToggleFavorite = onToggleFavorite, onSeeAll = onSeeAll
            )
        }
    }
}

@Composable
private fun HeroSection(
    spotlight: State<TvApp?>, fallback: TvApp?, fallbackFromRecents: Boolean, greeting: String,
    dpad: Boolean, wide: Boolean, pad: Dp, height: Dp, favorites: Set<String>,
    onOpen: (TvApp) -> Unit, onToggleFavorite: (TvApp) -> Unit
) {
    val focusedApp = spotlight.value
    val app = focusedApp ?: fallback
    val isFavorite = app != null && app.packageName in favorites
    val eyebrow = when {
        app == null -> "BIENVENIDO"
        dpad && focusedApp != null -> "EN FOCO"
        fallbackFromRecents -> "CONTINÚA CON"
        else -> "DESTACADA"
    }
    val hint = if (dpad) {
        "Pulsa OK para abrir · Mantén OK para fijar en favoritos"
    } else {
        "Toca Abrir · Mantén pulsada cualquier app para fijarla"
    }

    Row(
        Modifier.fillMaxWidth().height(height).padding(horizontal = pad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Crossfade(targetState = app, animationSpec = tween(300), label = "hero-text") { a ->
                Column {
                    Text(eyebrow, color = Brand, fontSize = if (wide) 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        a?.name ?: greeting, color = TextHi,
                        fontSize = if (wide) 46.sp else 28.sp, lineHeight = if (wide) 50.sp else 32.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (a != null) hint else "Tus aplicaciones aparecerán aquí.",
                        color = TextLo, fontSize = if (wide) 14.sp else 12.sp, maxLines = 2
                    )
                }
            }
            if (app != null) {
                Spacer(Modifier.height(if (wide) 20.dp else 14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("Abrir", Icons.Rounded.PlayArrow, primary = true) { onOpen(app) }
                    PillButton(
                        if (isFavorite) "En favoritos" else "Favorito",
                        if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        primary = false
                    ) { onToggleFavorite(app) }
                }
            }
        }
        Crossfade(targetState = app, animationSpec = tween(300), label = "hero-icon") { a ->
            if (a != null) HeroIcon(a, if (wide) 150.dp else 84.dp)
        }
    }
}

@Composable
private fun HeroIcon(app: TvApp, size: Dp) {
    Box(
        Modifier.size(size)
            .glow(app.accent, 0.5f)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(Surface2)
            .background(Brush.verticalGradient(listOf(app.accent.copy(alpha = 0.6f), app.accent.copy(alpha = 0.2f)))),
        contentAlignment = Alignment.Center
    ) {
        Image(app.icon, null, Modifier.size(size * 0.62f))
    }
}

// ───────────────────────────── Filas y cuadrícula ─────────────────────────────

@Composable
private fun AppRow(
    title: String, apps: List<TvApp>, tile: Dp, wide: Boolean, pad: Dp, favorites: Set<String>,
    focusFirst: FocusRequester?, showSeeAll: Boolean,
    onFocused: (TvApp) -> Unit, onOpen: (TvApp) -> Unit, onToggleFavorite: (TvApp) -> Unit,
    onSeeAll: () -> Unit
) {
    Column {
        SectionTitle(title, wide, pad)
        // El padding vertical deja sitio al zoom y al resplandor del elemento enfocado.
        LazyRow(
            contentPadding = PaddingValues(horizontal = pad, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(if (wide) 22.dp else 14.dp)
        ) {
            itemsIndexed(apps, key = { _, a -> a.packageName }) { index, app ->
                AppTile(
                    app = app, size = tile, wide = wide, favorite = app.packageName in favorites,
                    focusRequester = if (index == 0) focusFirst else null,
                    onFocused = onFocused, onOpen = onOpen, onToggleFavorite = onToggleFavorite
                )
            }
            if (showSeeAll) {
                item(key = "see-all") { SeeAllTile(tile, wide, onSeeAll) }
            }
        }
    }
}

@Composable
private fun AppGrid(
    title: String, apps: List<TvApp>, emptyText: String, tile: Dp, wide: Boolean, pad: Dp,
    favorites: Set<String>, focusFirst: FocusRequester?,
    onFocused: (TvApp) -> Unit, onOpen: (TvApp) -> Unit, onToggleFavorite: (TvApp) -> Unit
) {
    if (apps.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Apps, null, tint = TextLo.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(14.dp))
            Text(emptyText, color = TextLo, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = tile + 28.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = pad, end = pad, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                SectionTitle(title, wide, 0.dp)
                Spacer(Modifier.height(12.dp))
            }
        }
        gridItemsIndexed(apps, key = { _, a -> a.packageName }) { index, app ->
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.TopCenter) {
                AppTile(
                    app = app, size = tile, wide = wide, favorite = app.packageName in favorites,
                    focusRequester = if (index == 0) focusFirst else null,
                    onFocused = onFocused, onOpen = onOpen, onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, wide: Boolean, pad: Dp) {
    Text(
        text, color = TextHi, fontSize = if (wide) 20.sp else 17.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = pad)
    )
}

// ───────────────────────────── Tarjetas ─────────────────────────────

@Composable
private fun AppTile(
    app: TvApp, size: Dp, wide: Boolean, favorite: Boolean, focusRequester: FocusRequester?,
    onFocused: (TvApp) -> Unit, onOpen: (TvApp) -> Unit, onToggleFavorite: (TvApp) -> Unit
) {
    TileFrame(
        size = size, accent = app.accent, label = app.name, wide = wide, focusRequester = focusRequester,
        onClick = { onOpen(app) }, onLongClick = { onToggleFavorite(app) }, onFocused = { onFocused(app) },
        badge = { if (favorite) FavBadge(Modifier.align(Alignment.TopEnd)) }
    ) {
        Image(app.icon, app.name, Modifier.size(size * 0.62f))
    }
}

@Composable
private fun SeeAllTile(size: Dp, wide: Boolean, onClick: () -> Unit) {
    TileFrame(
        size = size, accent = Brand, label = "Ver todas", wide = wide, focusRequester = null,
        onClick = onClick, onLongClick = null, onFocused = {}
    ) {
        Icon(Icons.Rounded.Apps, null, tint = TextHi, modifier = Modifier.size(size * 0.4f))
    }
}

/**
 * Marco común de las tarjetas: zoom con muelle, resplandor del color de la app, borde blanco
 * al enfocar, pulsación corta = abrir, pulsación larga (o tecla Menú) = favorito.
 */
@Composable
private fun TileFrame(
    size: Dp, accent: Color, label: String, wide: Boolean, focusRequester: FocusRequester?,
    onClick: () -> Unit, onLongClick: (() -> Unit)?, onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) 1.1f else 1f, spring(dampingRatio = 0.75f, stiffness = 420f), label = "tile-scale"
    )
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(size * 0.22f)
    val currentOnFocused by rememberUpdatedState(onFocused)
    LaunchedEffect(focused) { if (focused) currentOnFocused() }

    Column(
        modifier.width(size).onKeyEvent { e ->
            if (onLongClick != null && e.type == KeyEventType.KeyUp && e.key == Key.Menu) {
                onLongClick()
                true
            } else {
                false
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(if (focused) Modifier.glow(accent, 0.55f) else Modifier)
                .clip(shape)
                .background(Surface2)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.38f), accent.copy(alpha = 0.10f))))
                .then(if (focused) Modifier.border(3.dp, Color.White, shape) else Modifier)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .combinedClickable(
                    interactionSource = source, indication = null, role = Role.Button,
                    onLongClick = if (onLongClick != null) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    } else {
                        null
                    },
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
            badge()
        }
        Text(
            label, color = if (focused) TextHi else TextLo,
            fontSize = if (wide) 14.sp else 12.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        )
    }
}

@Composable
private fun FavBadge(modifier: Modifier = Modifier) {
    Box(
        modifier.padding(6.dp).size(20.dp).clip(CircleShape).background(Color(0xB3000000)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(13.dp))
    }
}

/** Halo suave alrededor del elemento; se dibuja detrás y no recorta el contenido. */
private fun Modifier.glow(color: Color, strength: Float): Modifier = this.drawBehind {
    val radius = this.size.minDimension * 0.5f + 14.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            0.6f to color.copy(alpha = strength), 1f to Color.Transparent,
            center = center, radius = radius
        ),
        radius = radius
    )
}

// ───────────────────────────── Botones ─────────────────────────────

@Composable
private fun PillButton(
    text: String, icon: ImageVector?, primary: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "pill-scale")
    val bg = when {
        focused -> Color.White
        primary -> Brand
        else -> Color.White.copy(alpha = 0.12f)
    }
    val fg = when {
        focused || primary -> Ink
        else -> TextHi
    }
    Row(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50)).background(bg)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TvIconButton(
    icon: ImageVector, description: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = 46.dp
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val bg by animateColorAsState(
        if (focused) Color.White else Color.White.copy(alpha = 0.08f), tween(150), label = "icon-bg"
    )
    Box(
        modifier.size(size).clip(CircleShape).background(bg)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = if (focused) Ink else TextHi, modifier = Modifier.size(size * 0.48f))
    }
}

// ───────────────────────────── Navegación móvil ─────────────────────────────

@Composable
private fun FloatingNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp)
            .clip(shape).background(Color(0xF0111827))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeTab.values().forEach { t ->
            val on = t == selected
            val w by animateFloatAsState(if (on) 1.7f else 1f, label = "nav-weight")
            val bg by animateColorAsState(
                if (on) Brand.copy(alpha = 0.22f) else Color.Transparent, label = "nav-bg"
            )
            Row(
                Modifier.weight(w).height(46.dp).clip(RoundedCornerShape(24.dp)).background(bg)
                    .clickable(role = Role.Tab) { onSelect(t) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(t.icon, t.label, tint = if (on) Brand else TextLo, modifier = Modifier.size(22.dp))
                if (on) {
                    Spacer(Modifier.width(8.dp))
                    Text(t.label, color = Brand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

// ───────────────────────────── Ajustes ─────────────────────────────

@Composable
private fun SettingsDialog(
    isDefaultHome: Boolean, appCount: Int,
    onSetHome: () -> Unit, onRefresh: () -> Unit, onClose: () -> Unit
) {
    val firstButton = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstButton.requestFocus() }
    }
    val shape = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.widthIn(max = 460.dp).clip(shape).background(Surface1)
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape).padding(26.dp)
        ) {
            Text("LYGANE Smart TV", color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Versión 2.0 · $appCount aplicaciones", color = TextLo, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Text(
                if (isDefaultHome) {
                    "✓ LYGANE es tu pantalla de inicio. Al salir de una aplicación, el botón Inicio te trae de vuelta aquí."
                } else {
                    "LYGANE aún no es tu pantalla de inicio. Si la activas, el botón Inicio siempre te devolverá aquí al salir de una aplicación."
                },
                color = TextHi, fontSize = 14.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Consejo: mantén pulsada una aplicación (o mantén OK en el mando) para fijarla en Favoritos.",
                color = TextLo, fontSize = 13.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isDefaultHome) {
                    PillButton(
                        "Usar como pantalla de inicio", Icons.Rounded.Home, primary = true,
                        modifier = Modifier.focusRequester(firstButton), onClick = onSetHome
                    )
                    PillButton("Actualizar aplicaciones", null, primary = false, onClick = onRefresh)
                } else {
                    PillButton(
                        "Actualizar aplicaciones", null, primary = false,
                        modifier = Modifier.focusRequester(firstButton), onClick = onRefresh
                    )
                }
                PillButton("Cerrar", null, primary = false, onClick = onClose)
            }
        }
    }
}

// ───────────────────────────── Utilidades ─────────────────────────────

@Composable
private fun rememberNow(): State<Long> {
    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = System.currentTimeMillis()
            delay(15_000)
        }
    }
    return now
}

private fun isTelevision(context: Context): Boolean {
    val mode = (context.getSystemService(Context.UI_SERVICE) as? UiModeManager)?.currentModeType
    return mode == Configuration.UI_MODE_TYPE_TELEVISION ||
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

/** True si LYGANE es ahora mismo el launcher (pantalla de inicio) predeterminado del sistema. */
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
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                    launchRole(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                }
                return
            }
        }
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            // Algunos televisores no exponen ningún ajuste de launcher.
        }
    }
}

private fun hourOf(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

private fun currentHour(): Int = hourOf(System.currentTimeMillis())

private fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

private fun formatTime(context: Context, millis: Long): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date(millis))

private fun formatDate(millis: Long): String {
    val spanish = Locale("es", "PE")
    return SimpleDateFormat("EEEE, d 'de' MMMM", spanish).format(Date(millis))
        .replaceFirstChar { it.titlecase(spanish) }
}
