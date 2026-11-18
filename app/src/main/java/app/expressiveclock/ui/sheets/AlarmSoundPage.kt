package app.expressiveclock.ui.sheets

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import app.expressiveclock.ui.theme.ClockFace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlin.math.sin
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.expressiveclock.alarm.AlarmAudio
import app.expressiveclock.data.SILENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberAlarmSoundSelector(sound: String?, onSelected: (String?) -> Unit): () -> Unit {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var visible by rememberSaveable { mutableStateOf(false) }
    if (visible) AlarmSoundPage(sound, onDismiss = { visible = false }, onSelected = {
        onSelected(it)
        visible = false
    })
    return { focus.clearFocus(); keyboard?.hide(); visible = true }
}

private data class SystemAlarmSound(val title: String, val uri: String)

@Composable
private fun AlarmSoundPage(initial: String?, onDismiss: () -> Unit, onSelected: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    var previewing by remember { mutableStateOf(false) }
    var previewPaused by remember { mutableStateOf(false) }
    var previewSource by remember { mutableStateOf<String?>(null) }
    var previewGeneration by remember { mutableIntStateOf(0) }
    val preview = remember(context) { AlarmAudio(context, looping = false) }
    preview.onFinished = { previewing = false; previewPaused = false }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    fun stop() { preview.stop(); previewing = false; previewPaused = false }
    DisposableEffect(owner, preview) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); preview.stop() }
    }
    LaunchedEffect(draft) { stop(); error = null }
    LaunchedEffect(previewGeneration) {
        if (previewGeneration > 0) { delay(30_000); stop() }
    }
    val sounds by produceState<List<SystemAlarmSound>?>(null, context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
                manager.cursor.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(SystemAlarmSound(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX),
                                manager.getRingtoneUri(cursor.position).toString()))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
    val name by produceState("Default", draft) {
        value = withContext(Dispatchers.IO) { alarmSoundTitle(context, draft) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            stop()
            val kept = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { }
                        ?: error("Cannot open selected audio")
                }.isSuccess
            }
            if (kept) { draft = uri.toString(); error = null }
            else error = "This file cannot be kept for alarms. Choose another audio file."
        }
    }
    fun previewSound(source: String?) {
        if (previewSource == source && previewing) {
            preview.pause(); previewing = false; previewPaused = true; return
        }
        if (previewSource == source && previewPaused) {
            preview.resume(); previewing = true; previewPaused = false; previewGeneration++; return
        }
        stop()
        error = null
        previewSource = source
        preview.onFallback = { error = "Selected audio unavailable. Previewing the default alarm sound." }
        preview.play(source)
        previewing = true
        previewGeneration++
    }
    val pageProgress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    fun close(apply: Boolean = false) {
        if (closing) return
        closing = true
        stop()
        scope.launch {
            pageProgress.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            if (apply) onSelected(draft) else onDismiss()
        }
    }
    val selectedName = when (draft) {
        null -> "Default alarm sound"
        SILENT -> "Silent"
        else -> sounds?.firstOrNull { it.uri == draft }?.title ?: name
    }
    val selectedSource = when {
        draft == null -> "Device default"
        draft == SILENT -> "No ringtone · vibration follows your alarm setting"
        sounds?.any { it.uri == draft } == true -> "Device sound"
        else -> "Your audio"
    }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
        dismissOnBackPress = false, dismissOnClickOutside = false)) {
        LaunchedEffect(Unit) { pageProgress.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        PredictiveBackHandler(enabled = !closing) { events ->
            try {
                events.collect { event -> pageProgress.snapTo(1f - event.progress.coerceIn(0f, 1f)) }
                close()
            } catch (_: CancellationException) {
                withContext(NonCancellable) {
                    if (!closing) pageProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                }
            }
        }
        val view = LocalView.current
        val lightBars = MaterialTheme.colorScheme.surface.luminance() > .5f
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setDimAmount(0f)
                androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black.copy(alpha = .06f * pageProgress.value)) })
        Surface(Modifier.fillMaxSize().graphicsLayer {
            val p = pageProgress.value
            translationX = size.width * (1f - p)
            scaleX = .97f + .03f * p; scaleY = scaleX
            alpha = .96f + .04f * p
            shape = RoundedCornerShape((24f * (1f - p)).dp)
            clip = p < .999f
            shadowElevation = (12f * (1f - p)).dp.toPx()
        }, color = MaterialTheme.colorScheme.surface) {
            Scaffold(containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    TopAppBar(title = { Text("Alarm Sound", fontFamily = ClockFace.family(22f, 98f, 560),
                        fontWeight = FontWeight(560), style = MaterialTheme.typography.titleLarge.copy(fontSynthesis = FontSynthesis.None)) }, navigationIcon = {
                        IconButton(onClick = { close() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }, actions = {
                        FilledTonalButton(enabled = !closing, onClick = { close(apply = true) },
                            modifier = Modifier.padding(end = 12.dp)) { Text("Apply") }
                    })
                }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        item(key = "featured") {
                            FeaturedSound(selectedName, selectedSource, draft != SILENT,
                                previewing && previewSource == draft, previewPaused && previewSource == draft,
                                onPreview = { previewSound(draft) })
                        }
                        item { SoundGroupHeading("Your Audio") }
                        item {
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow).clickable {
                                    stop(); picker.launch(arrayOf("audio/*"))
                                }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Choose audio file", style = MaterialTheme.typography.titleMedium)
                                    Text("From your device", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (draft != null && draft != SILENT && sounds?.none { it.uri == draft } == true) item {
                            SoundChoice(selectedName, true, "Audio file", previewing && previewSource == draft,
                                onPreview = { previewSound(draft) }, onClick = { })
                        }
                        item { SoundGroupHeading("Default & Silent") }
                        item {
                            SoundChoice("Default alarm sound", draft == null, "Follows your phone’s alarm ringtone",
                                previewing && previewSource == null,
                                onPreview = { previewSound(null) }, onClick = { draft = null })
                        }
                        item {
                            SoundChoice("Silent", draft == SILENT, "No alarm ringtone", onClick = { draft = SILENT })
                        }
                        item { SoundGroupHeading("Device Sounds") }
                        if (sounds == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(12.dp)) }
                        items(sounds.orEmpty(), key = { it.uri }) { sound ->
                            SoundChoice(sound.title, draft == sound.uri, playing = previewing && previewSource == sound.uri,
                                onPreview = { previewSound(sound.uri) }, onClick = { draft = sound.uri })
                        }
                        error?.let { message -> item {
                            Text(message, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                        } }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun FeaturedSound(title: String, source: String, playable: Boolean,
    playing: Boolean, paused: Boolean, onPreview: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(scheme.primary.copy(alpha = .13f), scheme.primary.copy(alpha = .045f))))
        .border(1.dp, scheme.primary.copy(alpha = .10f), RoundedCornerShape(26.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 14.dp)) {
                Text(title, fontFamily = ClockFace.family(21f, 96f, 550), fontWeight = FontWeight(550),
                    style = MaterialTheme.typography.titleLarge.copy(fontSynthesis = FontSynthesis.None), fontSize = 21.sp, lineHeight = 25.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(source, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
            FilledIconButton(onClick = onPreview, enabled = playable, modifier = Modifier.size(54.dp)) {
                Crossfade(playing, animationSpec = tween(130), label = "featuredPreview") { active ->
                    Icon(if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (active) "Pause selected preview" else "Play selected preview")
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SoundWaveform(playing, Modifier.width(92.dp).height(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (playing) "Playing preview" else if (paused) "Paused" else if (playable) "Preview selected sound" else "Ringtone disabled",
                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

/** Decorative envelope only; no microphone capture or audio sampling. Runs only while playing. */
@Composable
private fun SoundWaveform(playing: Boolean, modifier: Modifier) {
    val phase = if (playing) {
        val cycle = rememberInfiniteTransition(label = "previewWave")
        val p by cycle.animateFloat(0f, 6.28318f,
            infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "wavePhase")
        p
    } else 0f
    val amplitude by animateFloatAsState(if (playing) 1f else .15f, tween(180), label = "waveAmplitude")
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        repeat(11) { index ->
            val h = size.height * (.15f + amplitude * .8f * abs(sin(phase + index * .7f)))
            val x = (index + .5f) * size.width / 11
            drawLine(color.copy(alpha = .7f), androidx.compose.ui.geometry.Offset(x, (size.height - h) / 2),
                androidx.compose.ui.geometry.Offset(x, (size.height + h) / 2),
                strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun SoundGroupHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, start = 4.dp, bottom = 6.dp).semantics { heading() })
}

@Composable
private fun SoundChoice(title: String, chosen: Boolean, subtitle: String? = null, playing: Boolean = false,
    onPreview: (() -> Unit)? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(if (chosen) scheme.primary.copy(alpha = .10f) else scheme.surface,
        tween(180), label = "soundSelectionTint")
    val radius by animateDpAsState(if (chosen) 18.dp else 12.dp, tween(180), label = "soundSelectionShape")
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).clip(RoundedCornerShape(radius))
        .background(background).selectable(chosen, role = Role.RadioButton, onClick = onClick)
        .padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = chosen, onClick = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        }
        if (onPreview != null) IconButton(onClick = onPreview) {
            Crossfade(playing, animationSpec = tween(120), label = "rowPreview") { active ->
                Icon(if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (active) "Pause preview" else "Preview $title", tint = scheme.primary)
            }
        }
    }
}
/** Documents can expose opaque IDs; show their display name rather than raw storage paths. */
internal fun alarmSoundTitle(context: android.content.Context, source: String?): String {
    if (source == null) return "Default"
    if (source == app.expressiveclock.data.SILENT) return "Silent"
    val uri=Uri.parse(source)
    val name=runCatching {
        context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()
    return name?.takeIf { it.isNotBlank() }
        ?: runCatching { RingtoneManager.getRingtone(context,uri)?.let { ringtone ->
            try { ringtone.getTitle(context) } finally { ringtone.stop() }
        } }.getOrNull()
        ?: "Default"
}
