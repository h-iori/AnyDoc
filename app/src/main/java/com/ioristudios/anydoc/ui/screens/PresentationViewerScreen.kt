package com.ioristudios.anydoc.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.anydoc.model.*
import com.ioristudios.anydoc.model.Direction
import com.ioristudios.anydoc.model.Orientation
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.NeonOrange
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import com.ioristudios.anydoc.util.SlideRenderCache
import com.ioristudios.anydoc.util.SlideRenderer
import com.ioristudios.anydoc.viewmodel.DocumentViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PresentationFullscreenViewer(
    state: DocumentViewerState.Ready,
    isSearching: Boolean,
    onBack: () -> Unit,
    onTitleDoubleTap: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onGoToSlide: (Int) -> Unit,
    onNextSlide: () -> Unit,
    onPrevSlide: () -> Unit,
    onStartSlideshow: () -> Unit,
    onStopSlideshow: () -> Unit,
    onToggleFullscreen: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val presentation = state.presentationState.parsedContent ?: return
    val currentSlideIndex = state.presentationState.currentSlide
    val isSlideshowActive = state.presentationState.isSlideshowActive
    val isFullscreen = state.presentationState.isFullscreen
    val totalSlides = presentation.slides.size
    
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
    val coroutineScope = rememberCoroutineScope()
    
    var barsVisible by remember { mutableStateOf(true) }
    var showNotes by remember { mutableStateOf(false) }

    // If slideshow is running, default to bars hidden
    LaunchedEffect(isSlideshowActive) {
        if (isSlideshowActive) {
            barsVisible = false
        }
    }

    // Auto-scroll/switch slide to match active search match
    LaunchedEffect(state.activeMatch) {
        val matches = state.searchMatches
        if (matches.isNotEmpty() && state.activeMatch >= 0) {
            val pageIdx = matches[state.activeMatch].pageIndex
            onGoToSlide(pageIdx)
        }
    }

    // Gesture zoom/pan state for current slide
    var scale by remember(currentSlideIndex) { mutableFloatStateOf(1f) }
    var offsetX by remember(currentSlideIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(currentSlideIndex) { mutableFloatStateOf(0f) }

    val currentSlide = presentation.slides.getOrNull(currentSlideIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070B)) // Ultra-dark backdrop
    ) {
        // Main Slideshow Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            barsVisible = !barsVisible
                        },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2f
                            }
                        }
                    )
                }
                .pointerInput(currentSlideIndex) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx().toInt() }

            AnimatedContent(
                targetState = currentSlideIndex,
                transitionSpec = {
                    val targetSlide = presentation.slides.getOrNull(targetState)
                    val transition = targetSlide?.transition ?: SlideTransition.None
                    val isForward = targetState > initialState
                    val durationMs = when (transition) {
                        is SlideTransition.Fade -> transition.durationMs
                        is SlideTransition.Push -> transition.durationMs
                        is SlideTransition.Wipe -> transition.durationMs
                        is SlideTransition.Cover -> transition.durationMs
                        is SlideTransition.Dissolve -> transition.durationMs
                        is SlideTransition.Split -> transition.durationMs
                        is SlideTransition.Reveal -> transition.durationMs
                        is SlideTransition.Wheel -> transition.durationMs
                        else -> 300
                    }.coerceAtLeast(100)

                    when (transition) {
                        is SlideTransition.Fade -> {
                            fadeIn(animationSpec = tween(durationMs)) togetherWith fadeOut(animationSpec = tween(durationMs))
                        }
                        is SlideTransition.Push -> {
                            val direction = transition.direction
                            val slideIn = when (direction) {
                                Direction.LEFT -> slideInHorizontally(animationSpec = tween(durationMs)) { it }
                                Direction.RIGHT -> slideInHorizontally(animationSpec = tween(durationMs)) { -it }
                                Direction.UP -> slideInVertically(animationSpec = tween(durationMs)) { it }
                                Direction.DOWN -> slideInVertically(animationSpec = tween(durationMs)) { -it }
                            }
                            val slideOut = when (direction) {
                                Direction.LEFT -> slideOutHorizontally(animationSpec = tween(durationMs)) { -it }
                                Direction.RIGHT -> slideOutHorizontally(animationSpec = tween(durationMs)) { it }
                                Direction.UP -> slideOutVertically(animationSpec = tween(durationMs)) { -it }
                                Direction.DOWN -> slideOutVertically(animationSpec = tween(durationMs)) { it }
                            }
                            slideIn togetherWith slideOut
                        }
                        is SlideTransition.Cover -> {
                            val slideIn = when (transition.direction) {
                                Direction.LEFT -> slideInHorizontally(animationSpec = tween(durationMs)) { it }
                                Direction.RIGHT -> slideInHorizontally(animationSpec = tween(durationMs)) { -it }
                                Direction.UP -> slideInVertically(animationSpec = tween(durationMs)) { it }
                                Direction.DOWN -> slideInVertically(animationSpec = tween(durationMs)) { -it }
                            }
                            slideIn togetherWith fadeOut(animationSpec = tween(durationMs))
                        }
                        is SlideTransition.Reveal -> {
                            val slideOut = when (transition.direction) {
                                Direction.LEFT -> slideOutHorizontally(animationSpec = tween(durationMs)) { -it }
                                Direction.RIGHT -> slideOutHorizontally(animationSpec = tween(durationMs)) { it }
                                Direction.UP -> slideOutVertically(animationSpec = tween(durationMs)) { -it }
                                Direction.DOWN -> slideOutVertically(animationSpec = tween(durationMs)) { it }
                            }
                            fadeIn(animationSpec = tween(durationMs)) togetherWith slideOut
                        }
                        is SlideTransition.None -> {
                            if (isForward) {
                                slideInHorizontally(animationSpec = tween(300)) { it } togetherWith slideOutHorizontally(animationSpec = tween(300)) { -it }
                            } else {
                                slideInHorizontally(animationSpec = tween(300)) { -it } togetherWith slideOutHorizontally(animationSpec = tween(300)) { it }
                            }
                        }
                        else -> {
                            fadeIn(animationSpec = tween(durationMs)) togetherWith fadeOut(animationSpec = tween(durationMs))
                        }
                    }
                },
                label = "SlideTransition"
            ) { targetIndex ->
                val slide = presentation.slides.getOrNull(targetIndex)
                if (slide != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SlideView(
                            slide = slide,
                            slideWidth = presentation.slideWidth,
                            slideHeight = presentation.slideHeight,
                            containerWidthPx = containerWidthPx
                        )
                    }
                }
            }
        }

        // Top App Bar Overlay
        AnimatedVisibility(
            visible = barsVisible && !isFullscreen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search text in slides", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            onSearchClose()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit search", tint = NeonOrange)
                        }
                    },
                    actions = {
                        if (state.searchQuery.isNotEmpty()) {
                            val label = if (state.searchMatches.isEmpty()) "0/0"
                            else "${state.activeMatch + 1}/${state.searchMatches.size}"
                            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(enabled = state.searchMatches.isNotEmpty(), onClick = { haptics.performHapticFeedback(); onPrevMatch() }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray)
                            }
                            IconButton(enabled = state.searchMatches.isNotEmpty(), onClick = { haptics.performHapticFeedback(); onNextMatch() }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xEC12121A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = NeonOrange,
                        actionIconContentColor = Color.White
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.request.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${state.request.extension.uppercase()} Presentation • ${totalSlides} Slides",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onSearchOpen() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search text")
                        }
                        if (currentSlide?.notes != null) {
                            IconButton(onClick = { haptics.performHapticFeedback(); showNotes = !showNotes }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Notes",
                                    tint = if (showNotes) NeonOrange else Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xEC12121A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = NeonOrange,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }

        // Bottom Controls Overlay (Glassmorphic Navigation Pill)
        AnimatedVisibility(
            visible = barsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(64.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xCC1A1A24),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Previous Slide Button
                    IconButton(
                        onClick = { haptics.performHapticFeedback(); onPrevSlide() },
                        enabled = currentSlideIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous Slide",
                            tint = if (currentSlideIndex > 0) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    // Slide Index Indicator
                    Text(
                        text = "${currentSlideIndex + 1} / $totalSlides",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Next Slide Button
                    IconButton(
                        onClick = { haptics.performHapticFeedback(); onNextSlide() },
                        enabled = currentSlideIndex < totalSlides - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Slide",
                            tint = if (currentSlideIndex < totalSlides - 1) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    VerticalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.height(24.dp).width(1.dp)
                    )

                    // Play/Pause Slideshow
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback()
                            if (isSlideshowActive) onStopSlideshow() else onStartSlideshow()
                        }
                    ) {
                        Icon(
                            imageVector = if (isSlideshowActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isSlideshowActive) "Pause Slideshow" else "Start Slideshow",
                            tint = NeonOrange
                        )
                    }

                    // Fullscreen toggle
                    IconButton(
                        onClick = { haptics.performHapticFeedback(); onToggleFullscreen() }
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Slide Notes Panel (Overlay bottom sheet style)
        AnimatedVisibility(
            visible = showNotes && currentSlide?.notes != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = if (barsVisible) 96.dp else 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xF212121A)),
                shape = RoundedCornerShape(16.dp)
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
                            text = "Slide Notes",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonOrange
                            )
                        )
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = { haptics.performHapticFeedback(); showNotes = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Notes",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = currentSlide?.notes.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SlideView(
    slide: SlideModel,
    slideWidth: Float,
    slideHeight: Float,
    containerWidthPx: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(slide.index, containerWidthPx) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(slide.index, containerWidthPx) {
        if (containerWidthPx <= 0) return@LaunchedEffect
        val cacheKey = "${slide.index}_${containerWidthPx}"
        val cached = SlideRenderCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            bitmap = cached
        } else {
            withContext(Dispatchers.Default) {
                val rendered = SlideRenderer.render(
                    slide = slide,
                    slideWidth = slideWidth,
                    slideHeight = slideHeight,
                    targetWidthPx = containerWidthPx
                )
                SlideRenderCache.put(cacheKey, rendered)
                bitmap = rendered
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Slide ${slide.index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonOrange)
            }
        }
    }
}
