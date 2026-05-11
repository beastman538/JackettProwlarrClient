package com.aggregatorx.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import com.aggregatorx.app.data.model.*
import com.aggregatorx.app.ui.theme.*
import com.aggregatorx.app.ui.viewmodel.VideoPreviewResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Futuristic Search Bar with glow effect
 */
@Composable
fun FuturisticSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search across all providers...",
    isLoading: Boolean = false
) {
    val glowAlpha by animateFloatAsState(
        targetValue = if (query.isNotEmpty()) 0.6f else 0.3f,
        animationSpec = tween(300)
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                            colors = listOf(CyberCyan, CyberBlue, CyberPurple)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                    alpha = glowAlpha
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextTertiary,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = CyberCyan,
                    strokeWidth = 2.dp
                )
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = TextTertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            GlowButton(
                onClick = onSearch,
                enabled = query.isNotEmpty() && !isLoading
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Search",
                    tint = DarkBackground
                )
            }
        }
    }
}

/**
 * Glowing Button Component
 */
@Composable
fun GlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = CyberCyan,
    content: @Composable RowScope.() -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(200)
    )
    
    Box(
        modifier = modifier
            .drawBehind {
                if (enabled) {
                    drawCircle(
                        color = color,
                        radius = size.minDimension / 2 + 4.dp.toPx(),
                        alpha = 0.3f
                    )
                }
            }
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = color.copy(alpha = animatedAlpha),
                contentColor = DarkBackground,
                disabledContainerColor = color.copy(alpha = 0.3f),
                disabledContentColor = DarkBackground.copy(alpha = 0.5f)
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            content()
        }
    }
}

/**
 * Provider Card Component
 */
@Composable
fun ProviderCard(
    provider: Provider,
    onToggle: (Boolean) -> Unit,
    onReanalyze: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isAnalyzing: Boolean = false
) {
    val categoryColor = when (provider.category) {
        ProviderCategory.STREAMING -> CategoryStreaming
        ProviderCategory.TORRENT -> CategoryTorrent
        ProviderCategory.NEWS -> CategoryNews
        ProviderCategory.MEDIA -> CategoryMedia
        ProviderCategory.API_BASED -> CategoryAPI
        else -> CategoryGeneral
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        categoryColor.copy(alpha = 0.5f),
                        categoryColor.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Provider icon/avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(categoryColor, categoryColor.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = provider.name.take(2).uppercase(),
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = provider.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Switch(
                    checked = provider.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip(
                    label = "Searches",
                    value = provider.totalSearches.toString(),
                    color = CyberCyan
                )
                StatChip(
                    label = "Success",
                    value = "${((1f - provider.failedSearches.toFloat() / 
                        maxOf(provider.totalSearches, 1).toFloat()) * 100).toInt()}%",
                    color = AccentGreen
                )
                StatChip(
                    label = provider.category.name,
                    value = "",
                    color = categoryColor,
                    isCategory = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReanalyze,
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyberCyan
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = !isAnalyzing).copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.5f), CyberBlue.copy(alpha = 0.5f))
                        )
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-analyze")
                    }
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Stat Chip Component
 */
@Composable
fun StatChip(
    label: String,
    value: String,
    color: Color,
    isCategory: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCategory) {
                Text(
                    text = value,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = if (isCategory) color else TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Result Thumbnail Component
 *
 * Gesture mapping:
 *   TAP        → opens a full-screen thumbnail preview dialog with play option
 *   LONG PRESS → triggers video extraction & inline playback
 *
 * Always attempts to display a thumbnail. Shows a styled placeholder when
 * no URL is available or the image fails to load.
 */
@Composable
fun InlineThumbnailPreview(
    thumbnailUrl: String?,
    duration: String? = null,
    modifier: Modifier = Modifier,
    onTapPreview: () -> Unit = {},
    onHoldFullscreen: () -> Unit = {},
    isExtracting: Boolean = false
) {
    val context = LocalContext.current
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showTapPulse by remember { mutableStateOf(false) }
    val pulseAlpha by animateFloatAsState(
        targetValue = if (showTapPulse) 0.45f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "tap_pulse"
    )
    LaunchedEffect(showTapPulse) {
        if (showTapPulse) { delay(320); showTapPulse = false }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap        = { showTapPulse = true; onTapPreview() },
                    onLongPress  = { onHoldFullscreen() }
                )
            }
    ) {
        // ── Thumbnail image (always attempted) ───────────────────────────
        if (!thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError   = { imageLoadFailed = true },
                onSuccess = { imageLoadFailed = false }
            )
        }

        // Placeholder when no URL or load failure
        if (thumbnailUrl.isNullOrEmpty() || imageLoadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(DarkSurfaceVariant, DarkBackground)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = TextTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // ── Overlay ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = when {
                            isExtracting    -> 0.55f
                            pulseAlpha > 0f -> 0.08f + pulseAlpha * 0.25f
                            else            -> 0.18f
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isExtracting -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Loading…", style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan, fontSize = 9.sp)
                }
                else -> Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Tap to preview",
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Duration badge
        duration?.let { dur ->
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Text(dur, style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }

        // Tap hint badge
        if (!isExtracting) {
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = CyberCyan.copy(alpha = 0.78f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = DarkBackground,
                        modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Tap", style = MaterialTheme.typography.labelSmall,
                        color = DarkBackground, fontSize = 8.sp)
                }
            }
        }
    }
}

/**
 * In-app WebView browser — opens a URL without leaving the results screen.
 */
@Composable
fun InAppBrowserDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary,
                        modifier = Modifier.size(20.dp))
                }
                Text(
                    text = title.ifEmpty { url },
                    color = TextSecondary, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = TextTertiary,
                        modifier = Modifier.size(18.dp))
                }
            }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Full-screen thumbnail preview dialog shown when user taps a thumbnail.
 * Displays the image large with a play button to launch video extraction.
 */
@Composable
fun ThumbnailPreviewDialog(
    thumbnailUrl: String?,
    title: String,
    duration: String? = null,
    onDismiss: () -> Unit,
    onWatch: () -> Unit,
    onBrowser: () -> Unit
) {
    val context = LocalContext.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* consume click so it doesn't dismiss */ },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbnailUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Videocam, null, tint = TextTertiary,
                            modifier = Modifier.size(64.dp))
                    }
                    // Play button overlay
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onWatch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play",
                            tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                    // Duration badge
                    duration?.let {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Text(it, color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                // Title + actions
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, color = TextPrimary, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onWatch,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan, contentColor = DarkBackground)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Watch", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onBrowser,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Browser")
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Checks whether a URL plausibly points to an actual media stream (not an
 * HTML page).  Used as a gate before handing URLs to ExoPlayer — this
 * prevents the "no source / trying alternative" errors that happen when
 * ExoPlayer tries to parse HTML as video.
 *
 * Returns true for common video extensions, stream keywords, CDN patterns,
 * and known video hosting domains.
 */
private fun isLikelyStreamUrl(url: String): Boolean {
    val lowerUrl = url.lowercase()

    // Obvious video file extensions
    val videoExtensions = listOf(
        ".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".avi", ".mov",
        ".flv", ".wmv", ".ts", ".m4v", ".3gp", ".f4v", ".ogv"
    )
    if (videoExtensions.any { lowerUrl.contains(it) }) return true

    // Stream path keywords
    val streamKeywords = listOf(
        "/video/", "/stream/", "/hls/", "/dash/", "/manifest",
        "videoplayback", "/get_video", "/dl/", "/embed/",
        "/media/", "/cdn-cgi/", "googlevideo.com",
        "akamaized.net", "cloudfront.net", "/file/",
        "cdn.streamtape", "dood.", "filemoon.", "streamwish.",
        "mixdrop.", "voe.sx"
    )
    if (streamKeywords.any { lowerUrl.contains(it) }) return true

    // Reject URLs that look like normal web pages (HTML content)
    val htmlPageIndicators = listOf(
        "text/html", "/search?", "/category/", "/tag/",
        "/login", "/register", "/user/", "/forum/"
    )
    if (htmlPageIndicators.any { lowerUrl.contains(it) }) return false

    // If it has query-heavy structure with no video indicators, likely HTML
    val hasVideoQueryParam = lowerUrl.contains("video_id=") ||
        lowerUrl.contains("stream=") || lowerUrl.contains("file=") ||
        lowerUrl.contains("source=")

    return hasVideoQueryParam
}

/**
 * Search Result Card Component - Enhanced with Inline Video Preview & Download
 */
@Composable
fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit,
    onDownload: () -> Unit = {},
    onOpenExternal: () -> Unit = {},
    onLike: () -> Unit = {},
    isLiked: Boolean = false,
    showControls: Boolean = true,
    onExtractVideoUrl: (suspend (String) -> String?)? = null,
    onExtractVideoForPreview: (suspend (String) -> VideoPreviewResult?)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scoreColor = getScoreColor(result.relevanceScore)
    val scope = rememberCoroutineScope()
    
    var showFullscreenPlayer by remember { mutableStateOf(false) }
    var fullscreenVideoUrl by remember { mutableStateOf<String?>(null) }
    var fullscreenVideoHeaders by remember { mutableStateOf<Map<String, String>?>(null) }
    var isExtractingForFullscreen by remember { mutableStateOf(false) }
    var showExtractionError by remember { mutableStateOf(false) }
    var extractionErrorMessage by remember { mutableStateOf<String?>(null) }

    /**
     * Launches full video extraction then opens the fullscreen player.
     *
     * KEY DESIGN RULE: we NEVER pass a raw HTML page URL to ExoPlayer.
     * Only URLs that look like actual media streams (contain common
     * video extensions, stream keywords, or known CDN patterns) are
     * sent to the player.  If extraction completely fails we show an
     * error snackbar and offer "Open in Browser" instead of letting
     * ExoPlayer choke on HTML.
     */
    val openFullscreenPlayer: () -> Unit = {
        if (!result.url.isNullOrEmpty() && !isExtractingForFullscreen) {
            isExtractingForFullscreen = true
            scope.launch {
                try {
                    var resolvedUrl: String? = null
                    var resolvedHeaders: Map<String, String>? = null

                    // Attempt 1: full extraction chain (7-step) with headers
                    if (resolvedUrl == null && onExtractVideoForPreview != null) {
                        val previewResult = onExtractVideoForPreview(result.url)
                        if (previewResult != null && previewResult.videoUrl.isNotEmpty()
                            && isLikelyStreamUrl(previewResult.videoUrl)
                        ) {
                            resolvedUrl = previewResult.videoUrl
                            resolvedHeaders = previewResult.headers
                        }
                    }

                    // Attempt 2: simple URL extraction
                    if (resolvedUrl == null && onExtractVideoUrl != null) {
                        val extractedUrl = onExtractVideoUrl(result.url)
                        if (!extractedUrl.isNullOrEmpty() && isLikelyStreamUrl(extractedUrl)) {
                            resolvedUrl = extractedUrl
                            resolvedHeaders = null
                        }
                    }

                    // Attempt 3: raw URL ONLY if it itself looks like a media stream
                    if (resolvedUrl == null && isLikelyStreamUrl(result.url)) {
                        resolvedUrl = result.url
                        resolvedHeaders = null
                    }

                    if (resolvedUrl != null) {
                        fullscreenVideoUrl = resolvedUrl
                        fullscreenVideoHeaders = resolvedHeaders
                        showFullscreenPlayer = true
                        showExtractionError = false
                        extractionErrorMessage = null
                    } else {
                        // Extraction failed — do NOT hand garbage to ExoPlayer
                        showExtractionError = true
                        extractionErrorMessage = "Could not find a playable video stream. Try \"Browser\" to open the page directly."
                    }
                } catch (e: Exception) {
                    showExtractionError = true
                    extractionErrorMessage = "Video extraction failed: ${e.message?.take(80) ?: "unknown error"}"
                } finally {
                    isExtractingForFullscreen = false
                }
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenExternal)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        scoreColor.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Thumbnail with gesture: TAP = visual preview pulse, HOLD = fullscreen video
                InlineThumbnailPreview(
                    thumbnailUrl = result.thumbnailUrl,
                    duration = result.duration,
                    isExtracting = isExtractingForFullscreen,
                    modifier = Modifier.size(140.dp),
                    onHoldFullscreen = openFullscreenPlayer
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Description
                    result.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Metadata row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        // Score
                        ScoreBadge(score = result.relevanceScore)
                        
                        // Seeders
                        result.seeders?.let { seeders ->
                            MetadataBadge(
                                icon = Icons.Default.ArrowUpward,
                                value = seeders.toString(),
                                color = AccentGreen
                            )
                        }
                        
                        // Size
                        result.size?.let { size ->
                            MetadataBadge(
                                icon = Icons.Default.Storage,
                                value = size,
                                color = TextTertiary
                            )
                        }
                        // Quality
                        result.quality?.let { quality ->
                            QualityBadge(quality = quality)
                        }
                        
                        // Rating
                        result.rating?.let { rating ->
                            MetadataBadge(
                                icon = Icons.Default.Star,
                                value = String.format("%.1f", rating),
                                color = AccentYellow
                            )
                        }
                    }
                }
            }
            
            // Fullscreen player dialog — HOLD thumbnail or Watch button
            if (showFullscreenPlayer && !fullscreenVideoUrl.isNullOrEmpty()) {
                VideoPlayerDialog(
                    videoUrl = fullscreenVideoUrl!!,
                    title = result.title,
                    headers = fullscreenVideoHeaders,
                    onDismiss = {
                        showFullscreenPlayer = false
                        fullscreenVideoUrl = null
                        fullscreenVideoHeaders = null
                    },
                    onOpenExternal = {
                        showFullscreenPlayer = false
                        fullscreenVideoUrl = null
                        fullscreenVideoHeaders = null
                        onOpenExternal()
                    }
                )
            }

            // Extraction error dialog — shown when we couldn't find a playable stream
            if (showExtractionError) {
                AlertDialog(
                    onDismissRequest = {
                        showExtractionError = false
                        extractionErrorMessage = null
                    },
                    containerColor = DarkCard,
                    titleContentColor = TextPrimary,
                    textContentColor = TextSecondary,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video Unavailable", style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    text = {
                        Text(
                            extractionErrorMessage ?: "Could not extract a playable video stream.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExtractionError = false
                                extractionErrorMessage = null
                                onOpenExternal()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkBackground
                            )
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Browser")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showExtractionError = false
                            extractionErrorMessage = null
                        }) {
                            Text("Close", color = TextSecondary)
                        }
                    }
                )
            }

            // Action buttons row
            if (showControls) {
                HorizontalDivider(
                    color = DarkSurfaceVariant,
                    thickness = 1.dp
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Watch button - Opens fullscreen video player
                    Button(
                        onClick = openFullscreenPlayer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Watch",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Download button - Auto downloads highest quality
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen.copy(alpha = 0.9f),
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Open in browser button
                    OutlinedButton(
                        onClick = onOpenExternal,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberCyan
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.horizontalGradient(
                                colors = listOf(CyberCyan.copy(alpha = 0.6f), CyberBlue.copy(alpha = 0.6f))
                            )
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Browser",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Like / thumbs-up button
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) Color(0xFFFF4081) else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Download Progress Card
 */
@Composable
fun DownloadProgressCard(
    title: String,
    progress: Int,
    status: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CyberCyan,
                trackColor = DarkSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Score Badge
 */
@Composable
fun ScoreBadge(score: Float) {
    val color = getScoreColor(score)
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = "${score.toInt()}",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Metadata Badge
 */
@Composable
fun MetadataBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 10.sp
        )
    }
}

/**
 * Quality Badge
 */
@Composable
fun QualityBadge(quality: String) {
    val color = when {
        quality.contains("4k", ignoreCase = true) || quality.contains("2160") -> AccentGreen
        quality.contains("1080") || quality.contains("full hd", ignoreCase = true) -> CyberCyan
        quality.contains("720") -> CyberBlue
        else -> TextTertiary
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = quality.uppercase(),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

/**
 * Provider Results Section Header
 */
@Composable
fun ProviderResultsHeader(
    providerName: String,
    resultCount: Int,
    searchTime: Long,
    success: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = CyberCyan
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (success) categoryColor.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (success) AccentGreen else AccentRed)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (!success && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentRed
                    )
                }
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (success) {
                Text(
                    text = "$resultCount results",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "${searchTime}ms",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

/**
 * Animated Loading Indicator
 */
@Composable
fun FuturisticLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(CyberCyan, CyberBlue, CyberPurple, CyberCyan)
                        ),
                        radius = this.size.minDimension / 2,
                        style = Stroke(width = 3.dp.toPx()),
                        alpha = glowAlpha
                    )
                }
                .graphicsLayer { rotationZ = rotation }
        )
        
        // Inner circle
        Box(
            modifier = Modifier
                .size(size * 0.6f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Empty State Component
 */
@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextTertiary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * Security Score Indicator
 */
@Composable
fun SecurityScoreIndicator(
    score: Float,
    modifier: Modifier = Modifier
) {
    val color = getSecurityColor(score)
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(1000)
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .drawBehind {
                    // Background circle
                    drawCircle(
                        color = DarkSurfaceVariant,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 8.dp.toPx())
                    )
                    // Progress arc
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = (animatedScore / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${score.toInt()}",
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Security Score",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}