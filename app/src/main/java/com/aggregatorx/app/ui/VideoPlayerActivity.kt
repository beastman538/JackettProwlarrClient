package com.aggregatorx.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.aggregatorx.app.engine.util.EngineUtils
import com.aggregatorx.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Full-screen video player activity launched by the "Open in App" button.
 *
 * Extras:
 *   VIDEO_URL  — playable stream URL (required)
 *   TITLE      — display title (optional)
 *   HEADERS_*  — any number of "HEADERS_<key>=<value>" extras for CDN auth
 */
class VideoPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_URL = "VIDEO_URL"
        const val EXTRA_TITLE     = "TITLE"
        const val EXTRA_HEADERS_PREFIX = "HEADERS_"

        fun buildIntent(
            context: Context,
            videoUrl: String,
            title: String = "",
            headers: Map<String, String> = emptyMap()
        ): Intent = Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra(EXTRA_VIDEO_URL, videoUrl)
            putExtra(EXTRA_TITLE, title)
            headers.forEach { (k, v) -> putExtra("$EXTRA_HEADERS_PREFIX$k", v) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and go edge-to-edge while video plays
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: run { finish(); return }
        val title    = intent.getStringExtra(EXTRA_TITLE) ?: ""

        // Reconstruct headers map from intent extras
        val headers = buildMap<String, String> {
            intent.extras?.keySet()
                ?.filter { it.startsWith(EXTRA_HEADERS_PREFIX) }
                ?.forEach { key ->
                    val headerKey = key.removePrefix(EXTRA_HEADERS_PREFIX)
                    intent.getStringExtra(key)?.let { put(headerKey, it) }
                }
        }

        setContent {
            AggregatorXPlayerTheme {
                FullScreenPlayer(
                    videoUrl  = videoUrl,
                    title     = title,
                    headers   = headers,
                    onClose   = { finish() }
                )
            }
        }
    }
}

// ── Minimal theme wrapper so the player looks correct standalone ──────────────
@Composable
private fun AggregatorXPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface    = DarkCard,
            primary    = CyberCyan
        ),
        content = content
    )
}

// ── Media type detection ──────────────────────────────────────────────────────
private enum class MediaType { HLS, DASH, PROGRESSIVE, UNKNOWN }

private fun detectMediaType(url: String): MediaType {
    val lower = url.lowercase()
    return when {
        lower.contains(".m3u8") || lower.contains("/hls/") ||
        lower.contains("master.m3u8") || lower.contains("index.m3u8") -> MediaType.HLS
        lower.contains(".mpd") || lower.contains("/dash/") -> MediaType.DASH
        lower.contains(".mp4") || lower.contains(".webm") ||
        lower.contains(".mkv") || lower.contains(".m4v") ||
        lower.contains(".mov") || lower.contains(".avi") -> MediaType.PROGRESSIVE
        else -> MediaType.UNKNOWN
    }
}

// ── Full-screen player composable ─────────────────────────────────────────────
@Composable
private fun FullScreenPlayer(
    videoUrl: String,
    title: String,
    headers: Map<String, String>,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    var isPlaying      by remember { mutableStateOf(true) }
    var currentPos     by remember { mutableStateOf(0L) }
    var duration       by remember { mutableStateOf(0L) }
    var isBuffering    by remember { mutableStateOf(true) }
    var hasError       by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf("") }
    var showControls   by remember { mutableStateOf(true) }
    var retryCount     by remember { mutableStateOf(0) }
    var formatOverride by remember { mutableStateOf<MediaType?>(null) }

    val detectedType = remember(videoUrl) { detectMediaType(videoUrl) }
    val activeType   = formatOverride ?: detectedType

    val httpFactory = remember(videoUrl, headers) {
        val ua = headers["User-Agent"] ?: EngineUtils.DEFAULT_USER_AGENT
        DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .apply { if (headers.isNotEmpty()) setDefaultRequestProperties(headers) }
    }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(1_500, 60_000, 600, 1_500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(8 * 1024 * 1024)
            .build()
    }

    val exoPlayer = remember(videoUrl, retryCount, activeType) {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
                val uri    = Uri.parse(videoUrl)
                val source = when (activeType) {
                    MediaType.HLS        -> HlsMediaSource.Factory(httpFactory)
                                               .setAllowChunklessPreparation(true)
                                               .createMediaSource(MediaItem.fromUri(uri))
                    MediaType.DASH       -> DashMediaSource.Factory(httpFactory)
                                               .createMediaSource(MediaItem.fromUri(uri))
                    MediaType.PROGRESSIVE,
                    MediaType.UNKNOWN    -> ProgressiveMediaSource.Factory(httpFactory)
                                               .createMediaSource(MediaItem.fromUri(uri))
                }
                setMediaSource(source)
                prepare()
                playWhenReady = true
            }
    }

    // Auto-hide controls after 3 s of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3_000)
            showControls = false
        }
    }

    // Position polling
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPos = exoPlayer.currentPosition
            duration   = exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            delay(500)
        }
    }

    // Player listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                    hasError = false
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) {
                hasError     = true
                errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                        "Network error — check your connection"
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "Stream not found (404)"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                        "Unsupported format"
                    else -> "Playback error (${error.errorCode})"
                }
                // Auto-switch format on first failure
                if (formatOverride == null && activeType != MediaType.PROGRESSIVE) {
                    formatOverride = MediaType.PROGRESSIVE
                    hasError = false
                    retryCount++
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        // ── Video surface ─────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player       = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Buffering spinner ─────────────────────────────────────────────
        if (isBuffering && !hasError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(64.dp))
            }
        }

        // ── Error overlay ─────────────────────────────────────────────────
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(56.dp))
                    Text(errorMessage, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                hasError       = false
                                formatOverride = null
                                retryCount++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                        OutlinedButton(
                            onClick = onClose,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }

        // ── Controls overlay ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls && !hasError,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    // Open in browser fallback
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                    }) {
                        Icon(Icons.Default.OpenInBrowser, "Open in browser", tint = CyberCyan, modifier = Modifier.size(24.dp))
                    }
                }

                // Center transport controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip back 10 s
                    TransportButton(size = 52.dp, onClick = {
                        exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10_000L))
                    }) {
                        Icon(Icons.Default.Replay10, "−10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    // Play / Pause
                    TransportButton(size = 72.dp, onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    // Skip forward 10 s
                    TransportButton(size = 52.dp, onClick = {
                        val dur = exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
                        exoPlayer.seekTo(minOf(dur, exoPlayer.currentPosition + 10_000L))
                    }) {
                        Icon(Icons.Default.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                // Bottom: seek bar + time
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Slider(
                        value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { frac ->
                            exoPlayer.seekTo((frac * duration).toLong())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor        = CyberCyan,
                            activeTrackColor  = CyberCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(currentPos), color = Color.White, fontSize = 12.sp)
                        Text(formatDuration(duration),   color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportButton(
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
