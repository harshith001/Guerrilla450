package com.example.guerrilla450.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guerrilla450.data.DashWallpaperFit
import com.example.guerrilla450.data.DashWallpaperKind
import com.example.guerrilla450.data.DashWallpaperPaths
import com.example.guerrilla450.ui.GuerrillaIcons
import com.example.guerrilla450.ui.components.*
import com.example.guerrilla450.ui.theme.*
import com.example.guerrilla450.viewmodel.AuthViewModel
import com.example.guerrilla450.viewmodel.ConnectionState
import com.example.guerrilla450.viewmodel.DashViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    conn: ConnectionState,
    onConnChange: (ConnectionState) -> Unit,
    authViewModel: AuthViewModel,
    dashViewModel: DashViewModel,
    onSignedOut: () -> Unit,
    onBack: () -> Unit,
) {
    val auth by authViewModel.state.collectAsState()
    val email = auth.email ?: "Not signed in"
    val initials = remember(auth.email, auth.displayName) {
        val src = auth.displayName?.takeIf { it.isNotBlank() } ?: auth.email ?: "?"
        src.split(" ", ".", "@").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    }

    var autoConnect by remember { mutableStateOf(true) }
    var screenOff   by remember { mutableStateOf(true) }
    var keepAwake   by remember { mutableStateOf(true) }
    var units       by remember { mutableStateOf("Kilometres") }

    // Real voice setting, shared with RouteScreen via the VoiceManager singleton.
    val ctx = LocalContext.current

    val dashUi by dashViewModel.ui.collectAsState()
    var pendingWallpaperUri     by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingWallpaperPreview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var cropX    by remember { mutableStateOf(0f) }
    var cropY    by remember { mutableStateOf(0f) }
    var fitMode  by remember { mutableStateOf(DashWallpaperFit.CROP) }

    val wallpaperMultiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(DashWallpaperPaths.MAX_SLOTS)
    ) { uris ->
        if (uris.size == 1) {
            val uri = uris.first()
            pendingWallpaperUri = uri
            cropX = 0f; cropY = 0f
            fitMode = DashWallpaperFit.CROP
            pendingWallpaperPreview = wallpaperPreviewFromUri(ctx, uri)
        } else if (uris.isNotEmpty()) {
            pendingWallpaperUri = null; pendingWallpaperPreview = null
            dashViewModel.addWallpapersFromUris(uris)
        }
    }

    val wallpaperPreview = remember(dashUi.wallpaperPath, dashUi.wallpaperKind) {
        dashUi.wallpaperPath?.let { path ->
            when (dashUi.wallpaperKind) {
                DashWallpaperKind.VIDEO -> wallpaperPreviewFromVideo(path)
                else -> runCatching { android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    val voiceManager = remember { com.example.guerrilla450.dash.nav.VoiceManager.get(ctx) }
    val voiceMode by voiceManager.mode.collectAsState()
    val voice = when (voiceMode) {
        com.example.guerrilla450.dash.nav.VoiceMode.OFF   -> "Off"
        com.example.guerrilla450.dash.nav.VoiceMode.CHIME -> "Chime"
        com.example.guerrilla450.dash.nav.VoiceMode.FULL  -> "Full TTS"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .padding(bottom = 24.dp),
    ) {
        ScreenHeader(title = "Settings", onBack = onBack,
            hint = "Connection, screen-off streaming, voice guidance, units, and media/call permissions.")

        // Account card
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp, onClick = {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldTint),
                ) {
                    Text(initials, color = Gold, fontFamily = GeistMonoFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    auth.displayName?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = TextHi, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily)
                    }
                    Text(email, color = if (auth.displayName.isNullOrBlank()) TextHi else TextMid, fontSize = if (auth.displayName.isNullOrBlank()) 15.5.sp else 12.5.sp, fontWeight = if (auth.displayName.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal, fontFamily = GeistFamily, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        SectionLabel("Connection")
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            SettingRow(GuerrillaIcons.Bt, "Tripper Dash",
                sub = when (conn) { ConnectionState.Connected -> "Connected"; ConnectionState.Searching -> "Connecting…"; ConnectionState.Offline -> "Not connected" },
                control = { GuerrillaChip(if (conn == ConnectionState.Connected) "Linked" else "Off", if (conn == ConnectionState.Connected) ChipTone.Gold else ChipTone.Off, dot = true) })
            GuerrillaDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(GuerrillaIcons.Sync, "Auto-connect on start", "Link when the bike is near",
                control = { GuerrillaToggle(autoConnect) { autoConnect = it } })
            GuerrillaDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(GuerrillaIcons.Zap, "Stream quality", "Balanced · saves battery",
                control = { Icon(GuerrillaIcons.ChevronRight, null, tint = TextLo, modifier = Modifier.size(18.dp)) }, last = true)
        }

        SectionLabel("During a ride")
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            SettingRow(GuerrillaIcons.Power, "Turn phone screen off", "Map keeps streaming to the dash",
                control = { GuerrillaToggle(screenOff) { screenOff = it } })
            GuerrillaDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(GuerrillaIcons.Dash, "Keep dash awake", "Prevent Tripper sleep",
                control = { GuerrillaToggle(keepAwake) { keepAwake = it } }, last = true)
        }

        SectionLabel("Media & calls on dash")
        // Re-check the grant on ON_RESUME so the chip flips to "On" the moment the user comes back
        // from the system notification-access screen (the Settings value isn't observable on its own).
        val lifecycleOwner = LocalLifecycleOwner.current
        fun answerGranted() = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.ANSWER_PHONE_CALLS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        var mediaGranted by remember { mutableStateOf(com.example.guerrilla450.media.MediaInfoProvider.isAccessGranted(ctx)) }
        var callAnswerGranted by remember { mutableStateOf(answerGranted()) }
        DisposableEffect(lifecycleOwner) {
            val obs = LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME) {
                    mediaGranted = com.example.guerrilla450.media.MediaInfoProvider.isAccessGranted(ctx)
                    callAnswerGranted = answerGranted()
                }
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
        }
        val callPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            callAnswerGranted = it
        }
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            SettingRow(
                GuerrillaIcons.Zap,
                "Now playing & calls on dash",
                if (mediaGranted) "Enabled · song info + caller shown while riding"
                else "Tap to allow notification access",
                control = {
                    GuerrillaChip(
                        if (mediaGranted) "On" else "Enable",
                        if (mediaGranted) ChipTone.Gold else ChipTone.Off, dot = true,
                    )
                },
                onClick = if (mediaGranted) null else {
                    { runCatching { ctx.startActivity(com.example.guerrilla450.media.MediaInfoProvider.accessSettingsIntent()) } }
                },
            )
            GuerrillaDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(
                GuerrillaIcons.Bt,
                "Answer calls from joystick",
                if (callAnswerGranted) "Enabled · UP answers, DOWN rejects"
                else "Tap to allow answering/rejecting calls",
                control = {
                    GuerrillaChip(
                        if (callAnswerGranted) "On" else "Enable",
                        if (callAnswerGranted) ChipTone.Gold else ChipTone.Off, dot = true,
                    )
                },
                last = true,
                onClick = if (callAnswerGranted) null else {
                    { callPermLauncher.launch(android.Manifest.permission.ANSWER_PHONE_CALLS) }
                },
            )
        }

        SectionLabel("Dash Wallpaper")
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                        .background(Surf2).border(1.dp, Line, RoundedCornerShape(11.dp)),
                ) { Icon(GuerrillaIcons.Dash, contentDescription = null, tint = TextMid, modifier = Modifier.size(19.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            pendingWallpaperPreview != null -> if (pendingWallpaperUri == null) "Edit selected" else "Adjust crop"
                            dashUi.wallpaperSaving -> "Saving…"
                            dashUi.wallpaperPath == null -> "Default idle screen"
                            else -> "Gallery ${dashUi.wallpaperGalleryIndex + 1} of ${dashUi.wallpaperGalleryCount}"
                        },
                        color = TextHi, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily,
                    )
                    Text(
                        "Up to 5 images, GIFs or videos · joystick L/R to cycle while idle",
                        color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            dashUi.wallpaperError?.let { error ->
                Text(error, color = androidx.compose.ui.graphics.Color(0xFFE05252.toInt()), fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp))
            }
            val previewImg = if (pendingWallpaperPreview != null) pendingWallpaperPreview else wallpaperPreview
            if (previewImg != null) {
                Spacer(Modifier.height(14.dp))
                DashCropPreview(
                    image = previewImg,
                    horizontalBias = if (pendingWallpaperPreview != null) cropX else dashUi.wallpaperCropX,
                    verticalBias   = if (pendingWallpaperPreview != null) cropY else dashUi.wallpaperCropY,
                    fit            = if (pendingWallpaperPreview != null) fitMode else dashUi.wallpaperFit,
                    modifier = Modifier.fillMaxWidth().let {
                        if (pendingWallpaperPreview == null && dashUi.wallpaperGalleryCount > 1) {
                            it.pointerInput(dashUi.wallpaperGalleryCount) {
                                var dragX = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { dragX = 0f },
                                    onHorizontalDrag = { _, amount -> dragX += amount },
                                    onDragEnd = {
                                        if (kotlin.math.abs(dragX) > 60f)
                                            dashViewModel.cycleWallpaperFromSettings(if (dragX < 0) 1 else -1)
                                    },
                                )
                            }
                        } else it
                    },
                    showGuide = pendingWallpaperPreview == null,
                )
            }
            if (pendingWallpaperPreview != null) {
                Spacer(Modifier.height(12.dp))
                GuerrillaSegmented(
                    options = listOf("Crop", "Fit height", "Fit width"),
                    selected = fitMode.label(),
                    onSelect = { fitMode = it.toWallpaperFit() },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (fitMode == DashWallpaperFit.CROP) {
                    Spacer(Modifier.height(8.dp))
                    Text("Horizontal", color = TextLo, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                    Slider(value = cropX, onValueChange = { cropX = it }, valueRange = -1f..1f, modifier = Modifier.fillMaxWidth())
                    Text("Vertical", color = TextLo, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                    Slider(value = cropY, onValueChange = { cropY = it }, valueRange = -1f..1f, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pendingWallpaperPreview != null) {
                    GuerrillaBtn(
                        "Save", onClick = {
                            val uri = pendingWallpaperUri
                            if (uri != null) dashViewModel.setWallpaperFromUri(uri, cropX, cropY, fitMode)
                            else dashViewModel.updateCurrentWallpaperOptions(cropX, cropY, fitMode)
                            pendingWallpaperUri = null; pendingWallpaperPreview = null
                        },
                        icon = GuerrillaIcons.Check, variant = BtnVariant.Primary, size = BtnSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                    GuerrillaBtn(
                        "Cancel", onClick = { pendingWallpaperUri = null; pendingWallpaperPreview = null },
                        icon = GuerrillaIcons.X, variant = BtnVariant.Ghost, size = BtnSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    if (dashUi.wallpaperGalleryCount > 1) {
                        GuerrillaIconBtn(icon = GuerrillaIcons.ChevronLeft, size = 40.dp,
                            onClick = { dashViewModel.cycleWallpaperFromSettings(-1) })
                    }
                    GuerrillaBtn(
                        "Add media", onClick = {
                            wallpaperMultiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        icon = GuerrillaIcons.Plus, variant = BtnVariant.Primary, size = BtnSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                    if (dashUi.wallpaperGalleryCount > 1) {
                        GuerrillaIconBtn(icon = GuerrillaIcons.ChevronRight, size = 40.dp,
                            onClick = { dashViewModel.cycleWallpaperFromSettings(1) })
                    }
                    if (dashUi.wallpaperPath != null) {
                        GuerrillaBtn(
                            "Edit", onClick = {
                                pendingWallpaperUri = null
                                pendingWallpaperPreview = wallpaperPreview
                                cropX = dashUi.wallpaperCropX; cropY = dashUi.wallpaperCropY
                                fitMode = dashUi.wallpaperFit
                            },
                            icon = GuerrillaIcons.Zap, variant = BtnVariant.Ghost, size = BtnSize.Sm,
                            modifier = Modifier.weight(1f),
                        )
                        GuerrillaBtn(
                            "Remove", onClick = { dashViewModel.clearWallpaper() },
                            icon = GuerrillaIcons.Power, variant = BtnVariant.Danger, size = BtnSize.Sm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        SectionLabel("Voice & guidance")
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            GuerrillaSegmented(listOf("Off", "Chime", "Full TTS"), voice, {
                voiceManager.setMode(when (it) {
                    "Off"      -> com.example.guerrilla450.dash.nav.VoiceMode.OFF
                    "Full TTS" -> com.example.guerrilla450.dash.nav.VoiceMode.FULL
                    else       -> com.example.guerrilla450.dash.nav.VoiceMode.CHIME
                })
            }, Modifier.fillMaxWidth())
        }

        SectionLabel("Units")
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            GuerrillaSegmented(listOf("Kilometres", "Miles"), units, { units = it }, Modifier.fillMaxWidth())
        }

        SectionLabel("Sync")
        GuerrillaCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            val (syncTitle, syncSub) = when {
                !auth.syncAvailable -> "Local only" to "Add your own Firebase project to sync across devices"
                auth.isSignedIn     -> "Synced" to (auth.email ?: "Signed in")
                else                -> "Not signed in" to "Sign in to sync across devices · data stays local until then"
            }
            SettingRow(GuerrillaIcons.Sync, syncTitle, syncSub,
                control = {
                    GuerrillaChip(
                        if (auth.isSignedIn) "On" else "Off",
                        if (auth.isSignedIn) ChipTone.Gold else ChipTone.Off, dot = true,
                    )
                }, last = true)
        }

        Spacer(Modifier.height(22.dp))

        if (auth.isSignedIn) {
            GuerrillaBtn(
                "Sign out",
                onClick = { authViewModel.signOut(); onSignedOut() },
                icon = GuerrillaIcons.Power,
                variant = BtnVariant.Danger,
                size = BtnSize.Md,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }

        TestBuildCard()

        val appVersion = remember { com.example.guerrilla450.data.UpdateChecker.currentVersionName(ctx) }
        Text(
            "NORTHSTAR v$appVersion · ${if (!auth.syncAvailable) "local only" else if (auth.isSignedIn) "sync on" else "sync off"}",
            color = TextDis, fontSize = 11.sp, fontFamily = GeistMonoFamily,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp),
        )
        Text(
            "Independent community project — not affiliated with, endorsed by, or supported by " +
                "Royal Enfield. \"Royal Enfield\", \"Guerrilla 450\" and \"Tripper\" are trademarks of " +
                "their respective owners, used here only to describe compatible hardware. The dash " +
                "link is unofficial and exists solely for interoperability with hardware you own — use at your own risk.",
            color = TextDis, fontSize = 10.sp, fontFamily = GeistFamily,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, start = 8.dp, end = 8.dp),
        )
    }
}

/**
 * Test-build channel: installs a freshly pushed APK WITHOUT a version bump. Compares the running
 * APK's CHECKSUM against the published one (Firestore `meta/test_build`) and, if they differ,
 * offers a one-tap download+install (same signing key → installs in place, keeping data). Checksum
 * means it's correct however the build was installed, and the card clears itself once the matching
 * APK is running. Invisible when up to date / no build published / Firebase off.
 */
@Composable
private fun TestBuildCard() {
    // Debug-channel only: the test-build installer must never appear in a public release build.
    if (!com.example.guerrilla450.BuildConfig.DEBUG) return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var build by remember { mutableStateOf<com.example.guerrilla450.data.TestBuildChecker.TestBuild?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(true) }

    suspend fun refresh() {
        checking = true
        build = com.example.guerrilla450.data.TestBuildChecker.fetchLatest(ctx)
        checking = false
    }
    LaunchedEffect(Unit) { refresh() }

    val b = build
    // Nothing published, or the running APK already matches the published checksum → no card.
    if (b == null || !com.example.guerrilla450.data.TestBuildChecker.needsInstall(ctx, b)) return

    fun installNow() {
        busy = true; status = "Downloading…"
        scope.launch {
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.guerrilla450.data.UpdateChecker.download(ctx, b.url) { p ->
                    status = "Downloading… ${(p * 100).toInt()}%"
                }
            }
            if (file == null) { status = "Download failed — try again"; busy = false; return@launch }
            status = "Opening installer…"
            // No bookkeeping needed: once the new APK is running its checksum matches the published
            // one, so needsInstall() returns false and this card disappears on its own.
            val started = com.example.guerrilla450.data.UpdateChecker.install(ctx, file)
            if (!started) status = "Allow “install unknown apps”, then tap again"
            busy = false
        }
    }

    GuerrillaCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("New test build", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append(b.builtAt.ifBlank { b.buildId })
                        if (b.sizeBytes > 0) append(" · ${b.sizeBytes / (1024 * 1024)} MB")
                    },
                    color = TextMid, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp),
                )
                if (b.notes.isNotBlank())
                    Text(b.notes, color = TextLo, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
                if (status.isNotBlank())
                    Text(status, color = TextLo, fontSize = 11.sp, fontFamily = GeistMonoFamily, modifier = Modifier.padding(top = 4.dp))
            }
            GuerrillaBtn(
                if (busy) "…" else "Install",
                onClick = { if (!busy) installNow() },
                icon = GuerrillaIcons.Wifi,
                variant = BtnVariant.Primary,
                size = BtnSize.Sm,
            )
        }
    }
}

private fun wallpaperPreviewFromUri(
    context: android.content.Context,
    uri: android.net.Uri,
): androidx.compose.ui.graphics.ImageBitmap? {
    val mime = context.contentResolver.getType(uri).orEmpty()
    return if (mime.startsWith("video/")) {
        runCatching {
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(context, uri)
                r.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST)?.asImageBitmap()
            } finally { r.release() }
        }.getOrNull()
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        runCatching {
            val src = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(src) { d, _, _ ->
                d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        }.getOrNull()
    } else {
        context.contentResolver.openInputStream(uri).use { android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }
}

private fun wallpaperPreviewFromVideo(path: String): androidx.compose.ui.graphics.ImageBitmap? =
    runCatching {
        val r = android.media.MediaMetadataRetriever()
        try {
            r.setDataSource(path)
            r.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST)?.asImageBitmap()
        } finally { r.release() }
    }.getOrNull()

@Composable
private fun DashCropPreview(
    image: androidx.compose.ui.graphics.ImageBitmap,
    horizontalBias: Float,
    verticalBias: Float,
    fit: DashWallpaperFit,
    modifier: Modifier = Modifier,
    showGuide: Boolean = true,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(526f / 300f)
            .clip(RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.Color(0xFF0B0D0E.toInt())),
    ) {
        if (fit == DashWallpaperFit.CROP) {
            val srcRatio = image.width.toFloat() / image.height.toFloat()
            val dstRatio = size.width / size.height
            val (srcOff, srcSz) = if (srcRatio > dstRatio) {
                val cropW = (image.height * dstRatio).toInt().coerceAtLeast(1)
                val extra = (image.width - cropW).coerceAtLeast(0)
                val left = ((extra / 2f) + (extra / 2f) * horizontalBias.coerceIn(-1f, 1f)).toInt()
                androidx.compose.ui.unit.IntOffset(left, 0) to androidx.compose.ui.unit.IntSize(cropW, image.height)
            } else {
                val cropH = (image.width / dstRatio).toInt().coerceAtLeast(1)
                val extra = (image.height - cropH).coerceAtLeast(0)
                val top = ((extra / 2f) + (extra / 2f) * verticalBias.coerceIn(-1f, 1f)).toInt()
                androidx.compose.ui.unit.IntOffset(0, top) to androidx.compose.ui.unit.IntSize(image.width, cropH)
            }
            drawImage(image, srcOffset = srcOff, srcSize = srcSz,
                dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
        } else {
            val scale = if (fit == DashWallpaperFit.FIT_HEIGHT) size.height / image.height.toFloat()
                        else size.width / image.width.toFloat()
            val drawW = (image.width * scale).toInt().coerceAtLeast(1)
            val drawH = (image.height * scale).toInt().coerceAtLeast(1)
            val left = (size.width.toInt() - drawW) / 2
            val top  = (size.height.toInt() - drawH) / 2
            drawImage(image,
                srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                srcSize = androidx.compose.ui.unit.IntSize(image.width, image.height),
                dstOffset = androidx.compose.ui.unit.IntOffset(left, top),
                dstSize = androidx.compose.ui.unit.IntSize(drawW, drawH))
        }
        if (showGuide) {
            // Semi-circle guide showing the Tripper's round visible area.
            val guideRadius = size.height * 0.89f
            val guideLeft = (size.width - guideRadius * 2f) / 2f
            drawArc(
                color = Gold.copy(alpha = 0.85f),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(guideLeft, 0f),
                size = Size(guideRadius * 2f, guideRadius * 2f),
                style = Stroke(width = 2f),
            )
        }
    }
}

private fun DashWallpaperFit.label() = when (this) {
    DashWallpaperFit.CROP       -> "Crop"
    DashWallpaperFit.FIT_HEIGHT -> "Fit height"
    DashWallpaperFit.FIT_WIDTH  -> "Fit width"
}

private fun String.toWallpaperFit() = when (this) {
    "Fit height" -> DashWallpaperFit.FIT_HEIGHT
    "Fit width"  -> DashWallpaperFit.FIT_WIDTH
    else         -> DashWallpaperFit.CROP
}

@Composable
private fun SectionLabel(label: String) {
    Eyebrow(label, Modifier.padding(top = 22.dp, bottom = 9.dp, start = 4.dp))
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    sub: String? = null,
    control: @Composable () -> Unit,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 6.dp, vertical = 13.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Surf2).border(1.dp, Line, RoundedCornerShape(11.dp)),
        ) {
            Icon(icon, contentDescription = null, tint = TextMid, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHi, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily)
            if (sub != null) Text(sub, color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        control()
    }
}
