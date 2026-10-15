package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREVIEW_HEIGHT_DP = 360
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 4f
private const val ZOOM_STEP = 0.5f

/** Picks the right in-app preview for [file]'s kind. */
@Composable
internal fun FilePreview(file: SharedFile, modifier: Modifier = Modifier) {
    when (file.kind) {
        FileKind.PDF -> PdfPreview(file, modifier)
        FileKind.IMAGE -> ImagePreview(file, modifier)
        FileKind.VIDEO -> VideoPreview(file, modifier)
        FileKind.DOCUMENT -> DocumentPreview(file, modifier)
    }
}

// ---- PDF ----

/** An opened PDF; pages are rendered on demand. */
private class PdfDocument(private val descriptor: ParcelFileDescriptor) {
    private val renderer = PdfRenderer(descriptor)
    val pageCount: Int get() = renderer.pageCount

    @Synchronized
    fun render(index: Int, widthPx: Int): Bitmap = renderer.openPage(index).use { page ->
        val height = (widthPx.toFloat() * page.height / page.width).toInt()
        Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }

    fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

@Composable
private fun PdfPreview(file: SharedFile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var page by rememberSaveable(file.id) { mutableIntStateOf(0) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    val document by produceState<PdfDocument?>(initialValue = null, file.uri) {
        val copy: File? = cachedCopy(context, file)
        value = copy?.let {
            withContext(Dispatchers.IO) {
                runCatching { PdfDocument(ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)) }.getOrNull()
            }
        }
    }
    // Capture the instance: reading the delegated property inside onDispose would close the
    // newly opened document when it replaces the previous (null) value.
    DisposableEffect(document) {
        val opened = document
        onDispose { opened?.close() }
    }

    val pageCount = document?.pageCount ?: file.pageCount ?: 1
    val bitmap by produceState<Bitmap?>(initialValue = null, document, page) {
        val doc = document ?: return@produceState
        value = withContext(Dispatchers.IO) { runCatching { doc.render(page.coerceIn(0, doc.pageCount - 1), 1080) }.getOrNull() }
    }

    PreviewCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT_DP.dp)
                .background(BadgeNeutralBackground),
            contentAlignment = Alignment.Center,
        ) {
            val current = bitmap
            if (current != null) {
                Image(
                    bitmap = current.asImageBitmap(),
                    contentDescription = stringResource(R.string.files_page_of, page + 1, pageCount),
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                PaperPlaceholder(title = file.title)
            }
        }
        HorizontalDivider(color = FieldBorder)
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { page-- }, enabled = page > 0) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.files_prev_page), tint = TextPrimary)
            }
            Text(
                text = stringResource(R.string.files_page_of, page + 1, pageCount),
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { page++ }, enabled = page < pageCount - 1) {
                Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.files_next_page), tint = TextPrimary)
            }
            IconButton(onClick = { fullscreen = true }, enabled = bitmap != null) {
                Icon(painterResource(R.drawable.ic_fullscreen), stringResource(R.string.files_fullscreen), tint = TextPrimary)
            }
        }
    }

    val current = bitmap
    if (fullscreen && current != null) {
        FullscreenViewer(onDismiss = { fullscreen = false }) { ZoomableBitmap(current, pageDescription = file.title) }
    }
}

/** A blank page with the file's title, shown while the PDF renders or when it isn't available. */
@Composable
private fun PaperPlaceholder(title: String) {
    Surface(
        modifier = Modifier
            .padding(vertical = 24.dp)
            .width(220.dp)
            .aspectRatio(0.72f),
        color = White,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.files_college_short), color = TextPrimary, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(title, color = TextSecondary, fontSize = AppTextSize.Caption, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            repeat(3) { i ->
                Box(
                    Modifier
                        .padding(vertical = 3.dp)
                        .fillMaxWidth(1f - i * 0.15f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FieldBorder),
                )
            }
        }
    }
}

// ---- Image ----

@Composable
private fun ImagePreview(file: SharedFile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(initialValue = null, file.uri) {
        val source = file.uri ?: return@produceState
        value = withContext(Dispatchers.IO) { decodeSampled(context, source, maxSide = 1600) }
    }
    var scale by remember(file.id) { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember(file.id) { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset = if (scale == MIN_ZOOM) Offset.Zero else offset + pan
    }

    PreviewCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(PhotoGradient)
                .transformable(transform),
            contentAlignment = Alignment.Center,
        ) {
            val current = bitmap
            if (current != null) {
                Image(
                    bitmap = current.asImageBitmap(),
                    contentDescription = file.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_image_outline),
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(84.dp),
                )
            }
        }
        HorizontalDivider(color = FieldBorder)
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                scale = (scale - ZOOM_STEP).coerceAtLeast(MIN_ZOOM)
                if (scale == MIN_ZOOM) offset = Offset.Zero
            }) {
                Icon(painterResource(R.drawable.ic_zoom_out), stringResource(R.string.files_zoom_out), tint = TextPrimary)
            }
            Text(
                text = stringResource(R.string.files_zoom_level, (scale * 100).toInt()),
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { scale = (scale + ZOOM_STEP).coerceAtMost(MAX_ZOOM) }) {
                Icon(painterResource(R.drawable.ic_zoom_in), stringResource(R.string.files_zoom_in), tint = TextPrimary)
            }
            IconButton(onClick = { fullscreen = true }, enabled = bitmap != null) {
                Icon(painterResource(R.drawable.ic_fullscreen), stringResource(R.string.files_fullscreen), tint = TextPrimary)
            }
        }
    }

    val current = bitmap
    if (fullscreen && current != null) {
        FullscreenViewer(onDismiss = { fullscreen = false }) { ZoomableBitmap(current, pageDescription = file.title) }
    }
}

/** Blue-to-amber wash used behind photos (and as the placeholder while they load). */
internal val PhotoGradient = Brush.linearGradient(listOf(Color(0xFF2443A8), Color(0xFF9AAEE6), Color(0xFFE8C988)))

private fun decodeSampled(context: Context, uri: String, maxSide: Int): Bitmap? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri.toUri())?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
    resolver.openInputStream(uri.toUri())?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}.getOrNull()

/** Reads an image's pixel size without decoding it, e.g. "4032 × 3024". */
internal fun imageDimensions(context: Context, uri: String): Pair<Int, Int>? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri.toUri())?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth > 0) bounds.outWidth to bounds.outHeight else null
}.getOrNull()

// ---- Video ----

@Composable
private fun VideoPreview(file: SharedFile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val source = file.uri
    var started by rememberSaveable(file.id) { mutableStateOf(false) }
    var prepared by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(file.durationMs ?: 0L) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(playing) {
        while (playing) {
            videoView?.let { position = it.currentPosition.toLong() }
            delay(250)
        }
    }

    val onPlayPause = {
        when {
            source == null -> Toast.makeText(context, R.string.files_video_not_bundled, Toast.LENGTH_SHORT).show()
            !started -> started = true
            playing -> {
                videoView?.pause()
                playing = false
            }
            else -> {
                videoView?.start()
                playing = true
            }
        }
    }
    val loading = started && !prepared

    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = if (loading) BadgeNeutralBackground else FilesDark) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp), contentAlignment = Alignment.Center) {
                if (started && source != null) {
                    VideoSurface(
                        source = source,
                        onReady = { videoView = it },
                        onPrepared = { player ->
                            duration = player.duration.toLong()
                            prepared = true
                            playing = true
                        },
                        onCompleted = {
                            playing = false
                            position = duration
                        },
                    )
                }
                if (loading) {
                    // "Loading video…" while the player buffers.
                    LoadingVideo(file)
                } else {
                    Surface(
                        onClick = onPlayPause,
                        modifier = Modifier.size(84.dp),
                        shape = CircleShape,
                        color = White.copy(alpha = if (playing) 0.6f else 0.92f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                                contentDescription = stringResource(if (playing) R.string.files_pause_video else R.string.files_play_video),
                                tint = FilesDark,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }
                }
            }
            if (!loading) {
                HorizontalDivider(color = White.copy(alpha = 0.12f))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    LinearProgressIndicator(
                        progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = White,
                        trackColor = White.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(
                            text = "${durationLabel(position)} / ${durationLabel(duration)}",
                            modifier = Modifier.weight(1f),
                            color = White,
                            fontSize = AppTextSize.Body,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(if (playing) R.string.files_streaming_quality else R.string.files_quality),
                            color = White,
                            fontSize = AppTextSize.Body,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoSurface(
    source: String,
    onReady: (VideoView) -> Unit,
    onPrepared: (MediaPlayer) -> Unit,
    onCompleted: () -> Unit,
) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            VideoView(it).apply {
                setVideoURI(source.toUri())
                setOnPreparedListener { player ->
                    onPrepared(player)
                    start()
                }
                setOnCompletionListener { onCompleted() }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(context, R.string.files_video_error, Toast.LENGTH_SHORT).show()
                    true
                }
                onReady(this)
            }
        },
        onRelease = { it.stopPlayback() },
    )
}

@Composable
private fun LoadingVideo(file: SharedFile) {
    Column(
        modifier = Modifier.fillMaxSize().background(BadgeNeutralBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = BrandBlue, trackColor = White, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.files_loading_video), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.files_loading_video_detail, fileSizeLabel(file.sizeBytes)),
            color = TextSecondary,
            fontSize = AppTextSize.Label,
        )
    }
}

// ---- Documents (Word / Excel / PowerPoint) ----

@Composable
private fun DocumentPreview(file: SharedFile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    PreviewCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FileTile(format = file.format, size = 92.dp)
            Spacer(Modifier.height(14.dp))
            Text(file.fileName, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                text = listOfNotNull(
                    file.format.label,
                    fileSizeLabel(file.sizeBytes),
                    file.pageCount?.let { stringResource(R.string.files_slides, it) },
                ).joinToString(" · "),
                color = TextSecondary,
                fontSize = AppTextSize.Label,
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.files_opens_online), color = TextSecondary, fontSize = AppTextSize.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { scope.launch { openInViewer(context, file) } },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Icon(painterResource(R.drawable.ic_visibility_outline), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.files_open), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---- Offline ----

/** "Connect to the internet to view files" — files only open online. */
@Composable
internal fun OfflinePreview(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    PreviewCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(BadgeNeutralBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_wifi_off), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.files_offline_view_title),
                color = TextPrimary,
                fontSize = AppTextSize.H6,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.files_offline_view_body), color = TextSecondary, fontSize = AppTextSize.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.files_try_again), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---- Shared pieces ----

@Composable
private fun PreviewCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column { content() }
    }
}

@Composable
private fun FullscreenViewer(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            content()
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.files_close), tint = White)
            }
        }
    }
}

@Composable
private fun ZoomableBitmap(bitmap: Bitmap, pageDescription: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset = if (scale == MIN_ZOOM) Offset.Zero else offset + pan
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = pageDescription,
        modifier = Modifier
            .fillMaxSize()
            .transformable(state)
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
        contentScale = ContentScale.Fit,
    )
}

