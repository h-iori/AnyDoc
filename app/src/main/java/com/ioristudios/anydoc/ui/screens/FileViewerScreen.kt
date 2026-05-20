package com.ioristudios.anydoc.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioristudios.anydoc.model.DocumentContent
import com.ioristudios.anydoc.model.DocumentViewerState
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import com.ioristudios.anydoc.viewmodel.DocumentViewerViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

// ─── Highlight colours ────────────────────────────────────────────────────────
private val HighlightNormal = Color(0xFFFFE066)   // yellow
private val HighlightActive = Color(0xFFFF9800)   // orange

// ─── Helper: build highlighted AnnotatedString ────────────────────────────────
private fun buildHighlightedString(
    text: String,
    query: String,
    activeIndex: Int,            // index of the active match within this text's occurrences
    matchesBeforeThisPage: Int   // how many global matches live before this page
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        var localMatchIdx = 0
        while (cursor <= text.length) {
            val found = text.indexOf(query, cursor, ignoreCase = true)
            if (found == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, found))
            val globalMatchIdx = matchesBeforeThisPage + localMatchIdx
            val bg = if (globalMatchIdx == activeIndex) HighlightActive else HighlightNormal
            withStyle(SpanStyle(background = bg, color = Color.Black)) {
                append(text.substring(found, found + query.length))
            }
            cursor = found + query.length
            localMatchIdx++
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    isExternal: Boolean = false,
    onBack: () -> Unit,
    viewModel: DocumentViewerViewModel = viewModel()
) {
    val spacing = rememberAppSpacing()
    val state by viewModel.uiState.collectAsState()
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }

    // Rename dialog state
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    if (isExternal) {
        BackHandler {
            if (backPressedOnce) {
                (context as? Activity)?.finishAndRemoveTask()
            } else {
                backPressedOnce = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Double tap on back to exit",
                        duration = SnackbarDuration.Short
                    )
                    delay(2000)
                    backPressedOnce = false
                }
            }
        }
    }

    LaunchedEffect(filePath) {
        viewModel.open(filePath)
    }

    LaunchedEffect((state as? DocumentViewerState.Ready)?.message) {
        val message = (state as? DocumentViewerState.Ready)?.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename document", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("New name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.BrandStrong,
                        unfocusedBorderColor = AppColors.BorderSubtle
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameFile(renameInput)
                    showRenameDialog = false
                }) { Text("Rename", color = AppColors.BrandStrong) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
            containerColor = AppColors.SurfaceElevated
        )
    }

    // Check if we should use fullscreen PDF mode
    val ready = state as? DocumentViewerState.Ready
    val isPdf = ready?.content is DocumentContent.PdfContent

    if (isPdf && ready != null) {
        // ─── Fullscreen PDF viewer ──────────────────────────────────────────
        PdfFullscreenViewer(
            state = ready,
            isSearching = isSearching,
            onBack = {
                if (isExternal) {
                    if (backPressedOnce) {
                        (context as? Activity)?.finishAndRemoveTask()
                    } else {
                        backPressedOnce = true
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Double tap on back to exit", duration = SnackbarDuration.Short)
                            delay(2000)
                            backPressedOnce = false
                        }
                    }
                } else {
                    onBack()
                }
            },
            onTitleDoubleTap = {
                renameInput = ready.request.displayName.substringBeforeLast(".")
                showRenameDialog = true
            },
            onSearchOpen = { isSearching = true },
            onSearchClose = {
                viewModel.updateSearch("")
                isSearching = false
            },
            onSearchQueryChange = viewModel::updateSearch,
            onNextMatch = viewModel::nextMatch,
            onPrevMatch = viewModel::prevMatch,
            snackbarHostState = snackbarHostState
        )
    } else {
        // ─── Standard scaffold viewer (non-PDF or loading/error) ───────────
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isSearching && ready != null) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = ready.searchQuery,
                                onValueChange = { viewModel.updateSearch(it) },
                                placeholder = { Text("Search inside document", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.performHapticFeedback()
                                viewModel.updateSearch("")
                                isSearching = false
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit search", tint = AppColors.BrandStrong)
                            }
                        },
                        actions = {
                            if (ready.searchQuery.isNotEmpty()) {
                                val label = if (ready.searchMatches.isEmpty()) "0/0"
                                else "${ready.activeMatch + 1}/${ready.searchMatches.size}"
                                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(enabled = ready.searchMatches.isNotEmpty(), onClick = { haptics.performHapticFeedback(); viewModel.prevMatch() }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", tint = if (ready.searchMatches.isNotEmpty()) Color.White else Color.Gray)
                                }
                                IconButton(enabled = ready.searchMatches.isNotEmpty(), onClick = { haptics.performHapticFeedback(); viewModel.nextMatch() }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", tint = if (ready.searchMatches.isNotEmpty()) Color.White else Color.Gray)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = AppColors.SurfaceElevated.copy(alpha = 0.98f),
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = AppColors.BrandStrong,
                            actionIconContentColor = Color.White
                        )
                    )
                } else {
                    LargeTopAppBar(
                        title = {
                            Column(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            val r = state as? DocumentViewerState.Ready ?: return@detectTapGestures
                                            renameInput = r.request.displayName.substringBeforeLast(".")
                                            showRenameDialog = true
                                        }
                                    )
                                }
                            ) {
                                Text(
                                    text = ready?.request?.displayName
                                        ?: (state as? DocumentViewerState.Error)?.displayName
                                        ?: "Document",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                ready?.let {
                                    Text(
                                        text = "${it.request.extension.uppercase()} - ${it.request.mimeType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.performHapticFeedback()
                                if (isExternal) {
                                    if (backPressedOnce) {
                                        (context as? Activity)?.finishAndRemoveTask()
                                    } else {
                                        backPressedOnce = true
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Double tap on back to exit", duration = SnackbarDuration.Short)
                                            delay(2000)
                                            backPressedOnce = false
                                        }
                                    }
                                } else {
                                    onBack()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (ready?.isEditing == true) {
                                IconButton(onClick = { haptics.performHapticFeedback(); viewModel.exitEditMode() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel editing")
                                }
                                IconButton(onClick = { haptics.performHapticFeedback(); viewModel.save() }) {
                                    Icon(Icons.Default.Save, contentDescription = "Save")
                                }
                            }
                            IconButton(onClick = { haptics.performHapticFeedback(); isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search in document")
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = AppColors.SurfaceBase.copy(alpha = 0.98f),
                            scrolledContainerColor = AppColors.SurfaceElevated.copy(alpha = 0.98f),
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = AppColors.BrandStrong,
                            actionIconContentColor = Color.White
                        )
                    )
                }
            },
            floatingActionButton = {
                if (ready != null && !ready.isEditing) {
                    FloatingActionButton(
                        onClick = { haptics.performHapticFeedback(); viewModel.enterEditMode() },
                        containerColor = if (ready.request.canEdit) AppColors.BrandStrong else AppColors.SurfaceHighest,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.neonGlow(color = AppColors.Brand.copy(alpha = 0.4f), radius = spacing.itemGap)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = if (ready.request.canEdit) "Edit" else "Read only")
                    }
                }
            }
        ) { paddingValues ->
            when (val current = state) {
                is DocumentViewerState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.BrandStrong)
                    }
                }
                is DocumentViewerState.Error -> {
                    ErrorContent(current, Modifier.padding(paddingValues))
                }
                is DocumentViewerState.Ready -> {
                    val searchQuery = if (isSearching) current.searchQuery else ""
                    val activeMatch = current.activeMatch
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
                    ) {
                        item {
                            when (val content = current.content) {
                                is DocumentContent.TextContent -> TextDocumentView(
                                    state = current,
                                    searchQuery = searchQuery,
                                    activeMatchIndex = activeMatch,
                                    onTextChange = viewModel::updateEditedText
                                )
                                is DocumentContent.CsvContent -> GridDocumentView(
                                    state = current,
                                    onCellChange = viewModel::updateCell,
                                    onAddRow = viewModel::addRow,
                                    onAddColumn = viewModel::addColumn
                                )
                                is DocumentContent.OfficeTextContent -> OfficeTextDocumentView(
                                    state = current,
                                    searchQuery = searchQuery,
                                    activeMatchIndex = activeMatch,
                                    onTextChange = viewModel::updateEditedText
                                )
                                is DocumentContent.UnsupportedContent -> UnsupportedDocumentView(content.message)
                                else -> {}
                            }
                        }
                        if (current.isSaving) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FULLSCREEN PDF VIEWER
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfFullscreenViewer(
    state: DocumentViewerState.Ready,
    isSearching: Boolean,
    onBack: () -> Unit,
    onTitleDoubleTap: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val content = state.content as DocumentContent.PdfContent
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    // ── Shared zoom / pan state ──────────────────────────────────────────────
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    // ── Scroll / visibility state ────────────────────────────────────────────
    val lazyListState = rememberLazyListState()
    var barsVisible by remember { mutableStateOf(true) }
    var scrollbarVisible by remember { mutableStateOf(false) }

    // Track scroll direction to show/hide bars
    LaunchedEffect(lazyListState) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                val scrolledDown = index > previousIndex || (index == previousIndex && offset > previousOffset)
                barsVisible = !scrolledDown
                previousIndex = index
                previousOffset = offset
            }
    }

    // Scrollbar visibility: show while scrolling, hide 1.5 s after stop
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            scrollbarVisible = true
        } else {
            delay(1500)
            scrollbarVisible = false
        }
    }

    // Auto-scroll to the page containing the active match
    LaunchedEffect(state.activeMatch) {
        val matchList = state.searchMatches
        if (matchList.isNotEmpty() && state.activeMatch >= 0) {
            val pageIdx = matchList[state.activeMatch].pageIndex
            // +1 because item 0 is a header spacer; items 1..N are page items
            lazyListState.animateScrollToItem(pageIdx)
        }
    }

    // Current visible page info
    val currentPage by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex + 1 }
    }
    val totalPages = content.pageTexts.size.coerceAtLeast(1)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ── PDF pages list ───────────────────────────────────────────────────
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var zoom = 1f
                        var panX = 0f
                        do {
                            val event = awaitPointerEvent()
                            val fingersDown = event.changes.count { it.pressed }
                            if (fingersDown >= 2) {
                                zoom *= event.calculateZoom()
                                panX += event.calculatePan().x
                            }
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })

                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX = (offsetX + panX * scale).coerceIn(-2000f, 2000f)
                        } else {
                            offsetX = 0f
                        }
                    }
                },
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(content.pageTexts.indices.toList().ifEmpty { listOf(0) }) { pageIndex, _ ->
                val pageSearchQuery = if (isSearching) state.searchQuery else ""
                val matchesBeforeThisPage = if (isSearching) {
                    state.searchMatches.count { it.pageIndex < pageIndex }
                } else 0

                PdfPageItem(
                    path = content.path,
                    pageIndex = pageIndex,
                    scale = scale,
                    offsetX = offsetX,
                    pageText = content.pageTexts.getOrElse(pageIndex) { "" },
                    searchQuery = pageSearchQuery,
                    matchesBeforeThisPage = matchesBeforeThisPage,
                    globalActiveMatch = state.activeMatch
                )
            }
            // Bottom padding so last page clears the nav bar
            item { Spacer(modifier = Modifier.size(80.dp)) }
        }

        // ── Top bar overlay ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = barsVisible,
            enter = slideInVertically(tween(200)) { -it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search in PDF", color = Color.White.copy(alpha = 0.5f)) },
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
                        IconButton(onClick = { haptics.performHapticFeedback(); onSearchClose() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = AppColors.BrandStrong)
                        }
                    },
                    actions = {
                        if (state.searchQuery.isNotEmpty()) {
                            val label = if (state.searchMatches.isEmpty()) "0/0"
                            else "${state.activeMatch + 1}/${state.searchMatches.size}"
                            Text(text = label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(enabled = state.searchMatches.isNotEmpty(), onClick = { haptics.performHapticFeedback(); onPrevMatch() }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous", tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray)
                            }
                            IconButton(enabled = state.searchMatches.isNotEmpty(), onClick = { haptics.performHapticFeedback(); onNextMatch() }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.88f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = AppColors.BrandStrong,
                        actionIconContentColor = Color.White
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = { onTitleDoubleTap() })
                            }
                        ) {
                            Text(
                                text = state.request.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${state.request.extension.uppercase()} · $totalPages pages",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.BrandStrong)
                        }
                    },
                    actions = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onSearchOpen() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.88f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = AppColors.BrandStrong,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }

        // ── Enterprise page indicator scrollbar ──────────────────────────────
        AnimatedVisibility(
            visible = scrollbarVisible || lazyListState.isScrollInProgress,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(600)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        ) {
            val fraction = (currentPage - 1).toFloat() / (totalPages - 1).coerceAtLeast(1).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(fraction.coerceIn(0f, 1f))
                        .align(Alignment.TopEnd)
                ) {}
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (fraction * 1800f).roundToInt().coerceIn(0, 1800)) }
                        .background(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, AppColors.BrandStrong.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentPage\n/ $totalPages",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color.White,
                            lineHeight = 13.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // ── Snackbar ─────────────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PDF PAGE ITEM — lazy render + optional highlighted text panel
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PdfPageItem(
    path: String,
    pageIndex: Int,
    scale: Float,
    offsetX: Float,
    pageText: String,
    searchQuery: String,
    matchesBeforeThisPage: Int,
    globalActiveMatch: Int
) {
    var bitmap by remember(path, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var error by remember(path, pageIndex) { mutableStateOf<String?>(null) }

    // Render bitmap in background
    LaunchedEffect(path, pageIndex) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        renderer.openPage(pageIndex).use { page ->
                            val targetWidth = 1440
                            val renderScale = targetWidth.toFloat() / page.width.toFloat()
                            val bmp = Bitmap.createBitmap(
                                (page.width * renderScale).toInt(),
                                (page.height * renderScale).toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            // White background so transparent PDFs look correct
                            android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                }
            }
        }
        result.onSuccess { bitmap = it }
        result.onFailure { error = it.localizedMessage ?: "Render error" }
    }

    // Recycle when removed from composition
    DisposableEffect(path, pageIndex) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = if (scale > 1f) offsetX else 0f
                    // Prevent content clipping during zoom
                    clip = false
                }
        ) {
            when {
                error != null -> {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), contentAlignment = Alignment.Center) {
                        Text(error ?: "Error", color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                bitmap == null -> {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.BrandStrong, modifier = Modifier.size(36.dp))
                    }
                }
                else -> {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        // ── Highlighted text panel shown when search is active ───────────────
        if (searchQuery.isNotBlank() && pageText.isNotBlank()) {
            val hasMatches = pageText.contains(searchQuery, ignoreCase = true)
            if (hasMatches) {
                val annotated = remember(pageText, searchQuery, globalActiveMatch) {
                    buildHighlightedString(pageText, searchQuery, globalActiveMatch, matchesBeforeThisPage)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }

        // Thin separator line between pages
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(height = 2.dp, width = 0.dp)
                .background(Color(0xFF222222))
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// TEXT & OTHER DOCUMENT VIEWS (with search highlights)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TextDocumentView(
    state: DocumentViewerState.Ready,
    searchQuery: String,
    activeMatchIndex: Int,
    onTextChange: (String) -> Unit
) {
    if (state.isEditing) {
        OutlinedTextField(
            value = state.editedText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 520.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if ((state.content as? DocumentContent.TextContent)?.isCodeLike == true) FontFamily.Monospace else FontFamily.Default,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    } else {
        val content = state.content as DocumentContent.TextContent
        val annotated = remember(content.text, searchQuery, activeMatchIndex) {
            buildHighlightedString(content.text, searchQuery, activeMatchIndex, 0)
        }
        ReadOnlyTextCard(annotatedText = annotated, monospace = content.isCodeLike)
    }
}

@Composable
private fun OfficeTextDocumentView(
    state: DocumentViewerState.Ready,
    searchQuery: String,
    activeMatchIndex: Int,
    onTextChange: (String) -> Unit
) {
    if (state.isEditing && state.request.canEdit) {
        OutlinedTextField(
            value = state.editedText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 520.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
    } else {
        val sections = (state.content as DocumentContent.OfficeTextContent).sections
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.request.extension in listOf("ppt", "pptx")) {
                Text(text = "Read-only slide text preview", style = MaterialTheme.typography.labelLarge, color = AppColors.BrandStrong)
            }
            var offset = 0
            sections.ifEmpty { listOf("No extractable text found.") }.forEachIndexed { index, section ->
                val matchesBeforeSection = if (searchQuery.isNotBlank()) {
                    sections.take(index).sumOf { sec ->
                        var cnt = 0; var s = 0
                        while (true) { val i = sec.indexOf(searchQuery, s, ignoreCase = true); if (i < 0) break; cnt++; s = i + searchQuery.length }
                        cnt
                    }
                } else 0
                val label = if (state.request.extension in listOf("ppt", "pptx")) "Slide ${index + 1}\n\n$section" else section
                val annotated = remember(label, searchQuery, activeMatchIndex) {
                    buildHighlightedString(label, searchQuery, activeMatchIndex, matchesBeforeSection)
                }
                ReadOnlyTextCard(annotatedText = annotated, monospace = false)
                offset += matchesBeforeSection
            }
        }
    }
}

@Composable
private fun GridDocumentView(
    state: DocumentViewerState.Ready,
    onCellChange: (Int, Int, String) -> Unit,
    onAddRow: () -> Unit,
    onAddColumn: () -> Unit
) {
    val rows = if (state.isEditing) state.editedRows else (state.content as DocumentContent.CsvContent).rows
    val scrollState = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.isEditing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAddRow) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Row")
                }
                TextButton(onClick = onAddColumn) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Column")
                }
            }
        }
        Column(modifier = Modifier.horizontalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.ifEmpty { listOf(listOf("")) }.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val width = 160.dp
                    row.ifEmpty { listOf("") }.forEachIndexed { columnIndex, value ->
                        if (state.isEditing) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { onCellChange(rowIndex, columnIndex, it) },
                                modifier = Modifier.width(width),
                                singleLine = true
                            )
                        } else {
                            Text(
                                text = value,
                                modifier = Modifier.width(width).background(AppColors.Surface, RoundedCornerShape(6.dp)).border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(6.dp)).padding(10.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyTextCard(annotatedText: AnnotatedString, monospace: Boolean) {
    SelectionContainer {
        Text(
            text = if (annotatedText.text.isBlank()) AnnotatedString("No previewable text found.") else annotatedText,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(12.dp))
                .padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
            )
        )
    }
}

@Composable
private fun UnsupportedDocumentView(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(12.dp)).border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(12.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Preview unavailable", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorContent(state: DocumentViewerState.Error, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.displayName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
