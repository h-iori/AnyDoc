package com.ioristudios.anydoc.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.Job
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioristudios.anydoc.model.*
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.NeonPurpleSubtle
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import com.ioristudios.anydoc.viewmodel.DocumentViewerViewModel
import com.ioristudios.anydoc.util.DocxParser
import java.io.File
import java.util.zip.ZipFile
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.shadow
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.TextUnit

private data class FlatParagraph(
    val para: DocxParagraph,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private fun collectFlatParagraphs(
    elements: List<PositionedElement>,
    offsetX: Float,
    offsetY: Float,
    outList: MutableList<FlatParagraph>
) {
    for (element in elements) {
        when (element) {
            is PositionedElement.Paragraph -> {
                outList.add(
                    FlatParagraph(
                        para = element.para,
                        x = offsetX + element.x,
                        y = offsetY + element.y,
                        width = element.width,
                        height = element.height
                    )
                )
            }
            is PositionedElement.Table -> {
                val cellWidths = element.cellWidths
                val rowHeights = element.rowHeights
                val tableAbsX = offsetX + element.x
                val tableAbsY = offsetY + element.y

                var currentY = 0f
                element.table.rows.forEachIndexed { rowIndex, row ->
                    val rowHeight = rowHeights.getOrNull(rowIndex) ?: 20f
                    var currentX = 0f
                    row.cells.forEachIndexed { cellIndex, _ ->
                        val cellWidth = cellWidths.getOrNull(cellIndex) ?: 80f
                        val cellContent = element.cellElements.getOrNull(rowIndex)?.getOrNull(cellIndex) ?: emptyList()
                        collectFlatParagraphs(
                            elements = cellContent,
                            offsetX = tableAbsX + currentX,
                            offsetY = tableAbsY + currentY,
                            outList = outList
                        )
                        currentX += cellWidth
                    }
                    currentY += rowHeight
                }
            }
            is PositionedElement.Image -> {}
        }
    }
}


// ─── Highlight colours ────────────────────────────────────────────────────────
private val HighlightNormal = Color(0xFFFFE066)   // yellow
private val HighlightActive = Color(0xFFFF9800)   // orange

private data class PdfPageFrame(
    val bitmap: Bitmap,
    val widthPts: Float,
    val heightPts: Float
)

// ─── LRU Bitmap Cache (shared across compositions) ────────────────────────────
// Capped at 1/8th of available VM heap to avoid OOM
private val pdfPageBitmapCache: LruCache<String, PdfPageFrame> by lazy {
    val maxMemKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxMemKb / 8
    object : LruCache<String, PdfPageFrame>(cacheSize) {
        override fun sizeOf(key: String, value: PdfPageFrame): Int =
            value.bitmap.byteCount / 1024
    }
}

// ─── Helper: build highlighted AnnotatedString ────────────────────────────────
private fun buildHighlightedString(
    text: String,
    query: String,
    activeIndex: Int,
    matchesBeforeThisPage: Int
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

// ─── Helper: draw accurate highlight rects onto a Bitmap ──────────────────────
private fun Bitmap.withAccurateHighlights(
    matchRects: List<Pair<android.graphics.RectF, Boolean>>,
    renderScale: Float
): Bitmap {
    if (matchRects.isEmpty()) return this
    val result = copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val normalPaint = Paint().apply {
        color = HighlightNormal.copy(alpha = 0.45f).toArgb()
        style = Paint.Style.FILL
    }
    val activePaint = Paint().apply {
        color = HighlightActive.copy(alpha = 0.6f).toArgb()
        style = Paint.Style.FILL
    }

    matchRects.forEach { (rect, isActive) ->
        val paint = if (isActive) activePaint else normalPaint
        val left = rect.left * renderScale
        val top = rect.top * renderScale
        val right = rect.right * renderScale
        val bottom = rect.bottom * renderScale
        canvas.drawRoundRect(android.graphics.RectF(left, top, right, bottom), 4f, 4f, paint)
    }
    return result
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

    // Check if we should use fullscreen PDF or Word mode
    val ready = state as? DocumentViewerState.Ready
    val isPdf = ready?.content is DocumentContent.PdfContent
    val isWord = ready?.content is DocumentContent.WordDocumentContent

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
    } else if (isWord && ready != null) {
        // ─── Fullscreen Word viewer ─────────────────────────────────────────
        WordFullscreenViewer(
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
            onEdit = {
                viewModel.enterEditMode()
            },
            onExitEdit = {
                viewModel.exitEditMode()
            },
            onSave = {
                viewModel.save()
            },
            onParagraphTextChange = { index, text ->
                viewModel.updateWordParagraphText(index, text)
            },
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
                        containerColor = if (ready.request.canEdit) NeonPurpleSubtle else AppColors.SurfaceHighest,
                        contentColor = if (ready.request.canEdit) AppColors.BrandStrong else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape,
                        modifier = if (ready.request.canEdit) {
                            Modifier
                                .neonGlow(color = AppColors.BrandStrong, radius = 12.dp, shape = CircleShape)
                                .border(1.5.dp, AppColors.BrandStrong, CircleShape)
                        } else {
                            Modifier.neonGlow(color = AppColors.Brand.copy(alpha = 0.4f), radius = spacing.itemGap)
                        }
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
// FULLSCREEN PDF VIEWER  — AGENT 1 (Layout) + AGENT 2 (Gestures)
//                        + AGENT 3 (Scrollbar) + AGENT 4 (Search)
//                        + AGENT 5 (Performance)
// ═══════════════════════════════════════════════════════════════════════════════

private val TOP_BAR_HEIGHT: Dp = 64.dp   // standard M3 TopAppBar height

private object WordUi {
    val WordBlue = Color(0xFF185ABD)
    val WordBluePressed = Color(0xFF0F4C9E)
    val WordCanvasGray = Color(0xFFF3F2F1)
    val WordPageWhite = Color.White
    val WordTextBlack = Color(0xFF1F1F1F)
    val WordSecondaryText = Color(0xFF605E5C)
    val WordBorderGray = Color(0xFFD2D0CE)
    val WordToolbarWhite = Color(0xFFFAF9F8)
    val WordShadow = Color.Black.copy(alpha = 0.18f)
    val PageAspectRatio = 8.5f / 11f
    val PageMaxWidth = 720.dp
    val PageOuterPadding = 16.dp
    val PageHorizontalMargin = 42.dp
    val PageVerticalMargin = 48.dp
}

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
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // ── AGENT 2: Shared zoom / pan state ─────────────────────────────────────
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isMultiTouch by remember { mutableStateOf(false) }
    var flingJobX by remember { mutableStateOf<Job?>(null) }
    var flingJobY by remember { mutableStateOf<Job?>(null) }

    // ── AGENT 1: Scroll / visibility state ───────────────────────────────────
    val lazyListState = rememberLazyListState()
    // barsVisible — always true when searching (FIX #12)
    var barsVisible by remember { mutableStateOf(true) }
    var scrollbarVisible by remember { mutableStateOf(false) }

    // Status bar height so first page clears status bar in addition to top bar
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topContentOffset = TOP_BAR_HEIGHT + statusBarPadding

    // FIX #12 — Keep bars visible while search is active
    LaunchedEffect(lazyListState, isSearching) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                if (!isSearching) {
                    val scrolledDown = index > previousIndex || (index == previousIndex && offset > previousOffset)
                    barsVisible = !scrolledDown
                } else {
                    barsVisible = true   // always show bar while searching
                }
                previousIndex = index
                previousOffset = offset
            }
    }

    // Tap anywhere on the page (when not searching) to toggle bars
    // NOTE: handled in the pointerInput below

    // Scrollbar visibility: show while scrolling, hide 1.5 s after stop
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            scrollbarVisible = true
        } else {
            delay(1500)
            scrollbarVisible = false
        }
    }

    // FIX #10 — Auto-scroll to the page containing the active match,
    //           resetting zoom/pan and aligning exactly with the top bar
    LaunchedEffect(state.activeMatch) {
        val matchList = state.searchMatches
        if (matchList.isNotEmpty() && state.activeMatch >= 0) {
            val pageIdx = matchList[state.activeMatch].pageIndex
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            lazyListState.animateScrollToItem(
                index = pageIdx,
                scrollOffset = 0
            )
        }
    }

    val totalPages = content.pageTexts.size.coerceAtLeast(1)

    // FIX #7 — Smooth scroll fraction using both index + pixel offset, mapping exactly 0f to 1f
    val scrollFraction by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 0f

            val firstVisibleItem = visibleItems.first()
            val lastVisibleItem = visibleItems.last()
            val totalItems = layoutInfo.totalItemsCount

            // 1. Check if we are at the very start
            if (firstVisibleItem.index == 0 && firstVisibleItem.offset == 0) {
                return@derivedStateOf 0f
            }

            // 2. Check if we are at the very end
            if (lastVisibleItem.index == totalItems - 1) {
                val lastItemBottom = lastVisibleItem.offset + lastVisibleItem.size
                val viewportBottom = layoutInfo.viewportEndOffset
                if (lastItemBottom <= viewportBottom) {
                    return@derivedStateOf 1f
                }
            }

            // 3. Otherwise, interpolate using exact page height calculation
            val pageHeightPx = visibleItems.firstOrNull { it.index < totalPages }?.size ?: 1
            val bottomSpacerPx = with(density) { 80.dp.toPx() }
            val totalHeight = totalPages * pageHeightPx + bottomSpacerPx
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val maxScroll = (totalHeight - viewportHeight).coerceAtLeast(1f)

            val currentScroll = lazyListState.firstVisibleItemIndex * pageHeightPx + lazyListState.firstVisibleItemScrollOffset
            (currentScroll.toFloat() / maxScroll).coerceIn(0f, 1f)
        }
    }

    // Precise page calculation: find the page item covering the center of the viewport
    val currentPage by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 1

            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2

            // Find the item that contains the viewport center
            val centerItem = visibleItems.firstOrNull { item ->
                val itemStart = item.offset
                val itemEnd = item.offset + item.size
                viewportCenter in itemStart..itemEnd
            } ?: visibleItems.firstOrNull() // fallback to first visible item

            val index = centerItem?.index ?: 0
            val pageIdx = index.coerceIn(0, totalPages - 1)
            pageIdx + 1
        }
    }

    // Trigger haptic feedback when the page changes (crossing boundaries)
    var isFirstPageLoad by remember { mutableStateOf(true) }
    LaunchedEffect(currentPage) {
        if (isFirstPageLoad) {
            isFirstPageLoad = false
        } else {
            haptics.performHapticFeedback()
        }
    }

    // Issue 1 Fix — Animated top padding: shrinks to status-bar-only when bars are hidden
    val animatedTopPadding by animateDpAsState(
        targetValue = if (barsVisible) topContentOffset else statusBarPadding,
        animationSpec = tween(200),
        label = "topPadding"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Issue 4: Unified zoom container — graphicsLayer wraps the entire LazyColumn ─
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = animatedTopPadding)
                .pointerInput(totalPages) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        flingJobX?.cancel()
                        flingJobY?.cancel()
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val activeChanges = changes.filter { it.pressed }

                            if (activeChanges.isEmpty()) {
                                // Gesture ended — fling if zoomed in
                                if (scale > 1.1f) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    val maxPanX = ((size.width * scale) - size.width).coerceAtLeast(0f) / 2f
                                    val maxPanY = ((size.height * scale) - size.height).coerceAtLeast(0f) / 2f

                                    flingJobX = coroutineScope.launch {
                                        Animatable(offsetX).animateDecay(
                                            initialVelocity = velocity.x,
                                            animationSpec = exponentialDecay()
                                        ) {
                                            offsetX = this.value.coerceIn(-maxPanX, maxPanX)
                                        }
                                    }
                                    flingJobY = coroutineScope.launch {
                                        var lastValue = offsetY
                                        Animatable(offsetY).animateDecay(
                                            initialVelocity = velocity.y,
                                            animationSpec = exponentialDecay()
                                        ) {
                                            val delta = this.value - lastValue
                                            lastValue = this.value
                                            
                                            val newOffsetY = offsetY + delta
                                            if (newOffsetY > maxPanY) {
                                                offsetY = maxPanY
                                                val overscroll = newOffsetY - maxPanY
                                                lazyListState.dispatchRawDelta(-overscroll)
                                            } else if (newOffsetY < -maxPanY) {
                                                offsetY = -maxPanY
                                                val overscroll = newOffsetY - (-maxPanY)
                                                lazyListState.dispatchRawDelta(-overscroll)
                                            } else {
                                                offsetY = newOffsetY
                                            }
                                        }
                                    }
                                }
                                isMultiTouch = false
                                break
                            }

                            val isMulti = activeChanges.size >= 2
                            isMultiTouch = isMulti

                            if (isMulti) {
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                val scaleChange = newScale / scale

                                if (newScale > 1f) {
                                    val maxPanX = ((size.width * newScale) - size.width).coerceAtLeast(0f) / 2f
                                    val maxPanY = ((size.height * newScale) - size.height).coerceAtLeast(0f) / 2f
                                    offsetX = (offsetX * scaleChange - (centroid.x - center.x) * (scaleChange - 1) + pan.x).coerceIn(-maxPanX, maxPanX)
                                    offsetY = (offsetY * scaleChange - (centroid.y - center.y) * (scaleChange - 1) + pan.y).coerceIn(-maxPanY, maxPanY)
                                    scale = newScale
                                } else {
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                }
                                changes.forEach { it.consume() }
                            } else {
                                if (scale > 1f) {
                                    val pointer = activeChanges.first()
                                    velocityTracker.addPosition(pointer.uptimeMillis, pointer.position)

                                    val pan = event.calculatePan()
                                    val maxPanX = ((size.width * scale) - size.width).coerceAtLeast(0f) / 2f
                                    val maxPanY = ((size.height * scale) - size.height).coerceAtLeast(0f) / 2f
                                    offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    
                                    val newOffsetY = offsetY + pan.y
                                    if (newOffsetY > maxPanY) {
                                        offsetY = maxPanY
                                        val overscroll = newOffsetY - maxPanY
                                        lazyListState.dispatchRawDelta(-overscroll)
                                    } else if (newOffsetY < -maxPanY) {
                                        offsetY = -maxPanY
                                        val overscroll = newOffsetY - (-maxPanY)
                                        lazyListState.dispatchRawDelta(-overscroll)
                                    } else {
                                        offsetY = newOffsetY
                                    }
                                    changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
                .pointerInput(isSearching) {
                    detectTapGestures(
                        onTap = { if (!isSearching) barsVisible = !barsVisible },
                        onDoubleTap = { tapOffset ->
                            flingJobX?.cancel()
                            flingJobY?.cancel()
                            if (scale > 1.1f) {
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            } else {
                                val targetScale = 2.5f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxPanX = ((size.width * targetScale) - size.width).coerceAtLeast(0f) / 2f
                                val maxPanY = ((size.height * targetScale) - size.height).coerceAtLeast(0f) / 2f
                                offsetX = (-(tapOffset.x - center.x) * targetScale).coerceIn(-maxPanX, maxPanX)
                                offsetY = (-(tapOffset.y - center.y) * targetScale).coerceIn(-maxPanY, maxPanY)
                                scale = targetScale
                            }
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    transformOrigin = TransformOrigin.Center
                }
        ) {
        // ── PDF pages list ─────────────────────────────────────────────────────
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = scale <= 1f && !isMultiTouch,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(content.pageTexts.indices.toList().ifEmpty { listOf(0) }) { pageIndex, _ ->
                val pageSearchQuery = if (isSearching) state.searchQuery else ""

                PdfPageItem(
                    path = content.path,
                    pageIndex = pageIndex,
                    state = state,
                    searchQuery = pageSearchQuery
                )
            }
            // Bottom padding so last page clears the nav bar
            item { Spacer(modifier = Modifier.size(80.dp)) }
        }
        } // end zoom container Box

        // ── AGENT 1: Top bar overlay (always above page content, never behind) ─
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
                            Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                enabled = state.searchMatches.isNotEmpty(),
                                onClick = { haptics.performHapticFeedback(); onPrevMatch() }
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous",
                                    tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray
                                )
                            }
                            IconButton(
                                enabled = state.searchMatches.isNotEmpty(),
                                onClick = { haptics.performHapticFeedback(); onNextMatch() }
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next",
                                    tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.92f),
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
                                text = "${state.request.extension.uppercase()} - $totalPages pages",
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

        // ── Issue 2: Draggable scrollbar — 36dp touch target, sleek glassmorphic UI ──
        var isDragging by remember { mutableStateOf(false) }
        var dragScrollJob by remember { mutableStateOf<Job?>(null) }
        val thumbWidthDp by animateDpAsState(
            targetValue = if (isDragging) 12.dp else 8.dp,
            animationSpec = tween(150),
            label = "thumbWidth"
        )

        AnimatedVisibility(
            visible = scrollbarVisible || lazyListState.isScrollInProgress || isDragging,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(800)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(36.dp)  // wide touch target
                .padding(top = animatedTopPadding, bottom = 8.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerHeightPx = with(density) { maxHeight.toPx() }
                val thumbMinHeightDp = 40.dp
                val thumbHeightPx = with(density) { thumbMinHeightDp.toPx() }
                val thumbOffsetPx = (scrollFraction * (containerHeightPx - thumbHeightPx))
                    .coerceIn(0f, (containerHeightPx - thumbHeightPx).coerceAtLeast(0f))

                // Wide invisible hit area for drag — covers full track width
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalPages) {
                            detectVerticalDragGestures(
                                onDragStart = { 
                                    isDragging = true
                                    scrollbarVisible = true
                                    dragScrollJob?.cancel()
                                },
                                onDragEnd = { 
                                    isDragging = false
                                    dragScrollJob?.cancel()
                                },
                                onDragCancel = { 
                                    isDragging = false
                                    dragScrollJob?.cancel()
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    
                                    val availableHeight = (size.height - thumbHeightPx).coerceAtLeast(1f)
                                    val fraction = ((change.position.y - thumbHeightPx / 2f) / availableHeight).coerceIn(0f, 1f)
                                    
                                    val layoutInfo = lazyListState.layoutInfo
                                    val visibleItems = layoutInfo.visibleItemsInfo
                                    if (visibleItems.isNotEmpty()) {
                                        val totalItems = layoutInfo.totalItemsCount
                                        val pageHeightPx = visibleItems.firstOrNull { it.index < totalPages }?.size ?: visibleItems.first().size
                                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                        
                                        val bottomSpacerPx = with(density) { 80.dp.toPx() }
                                        val totalHeight = totalPages * pageHeightPx + bottomSpacerPx
                                        val maxScroll = (totalHeight - viewportHeight).coerceAtLeast(0f)
                                        
                                        val targetScrollPx = fraction * maxScroll
                                        val targetItem = (targetScrollPx / pageHeightPx).toInt().coerceIn(0, totalItems - 1)
                                        val targetOffset = (targetScrollPx % pageHeightPx).toInt()
                                        
                                        dragScrollJob?.cancel()
                                        dragScrollJob = coroutineScope.launch(Dispatchers.Main.immediate) {
                                            lazyListState.scrollToItem(targetItem, targetOffset)
                                        }
                                    }
                                }
                            )
                        }
                )

                // Visual track — sleek translucent border & fill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .fillMaxHeight()
                        .width(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                // Animated thumb — premium rounded pill with high-contrast accent border
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                        .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                        .width(thumbWidthDp)
                        .heightIn(min = thumbMinHeightDp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.BrandStrong)
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
        }

        // ── Issue 3: Bottom-left page capsule ─────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 20.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$currentPage / $totalPages",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // ── Snackbar ─────────────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PDF PAGE ITEM — AGENT 4 (on-canvas highlights) + AGENT 5 (LRU cache)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PdfPageItem(
    path: String,
    pageIndex: Int,
    state: DocumentViewerState.Ready,
    searchQuery: String
) {
    // AGENT 5 — LRU cache key
    val cacheKey = "$path:$pageIndex"

    var pageFrame by remember(path, pageIndex) { mutableStateOf<PdfPageFrame?>(pdfPageBitmapCache.get(cacheKey)) }
    var error by remember(path, pageIndex) { mutableStateOf<String?>(null) }

    // AGENT 5 — Render bitmap in background; check cache first
    LaunchedEffect(path, pageIndex) {
        if (pageFrame != null) return@LaunchedEffect   // cache hit — skip render

        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        renderer.openPage(pageIndex).use { page ->
                            // AGENT 5 — Adaptive quality: moderate base width, higher when zoomed
                            val targetWidth = (page.width * 2).coerceIn(720, 1440)
                            val renderScale = targetWidth.toFloat() / page.width.toFloat()
                            val bmp = Bitmap.createBitmap(
                                (page.width * renderScale).toInt(),
                                (page.height * renderScale).toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            PdfPageFrame(bmp, page.width.toFloat(), page.height.toFloat())
                        }
                    }
                }
            }
        }
        result.onSuccess { frame ->
            pdfPageBitmapCache.put(cacheKey, frame)
            pageFrame = frame
        }
        result.onFailure { error = it.localizedMessage ?: "Render error" }
    }

    // Recycle when removed from composition only if not in cache
    DisposableEffect(path, pageIndex) {
        onDispose {
            // Don't recycle — let LRU cache manage lifecycle
        }
    }

    // AGENT 4 — Build highlighted bitmap (on-canvas overlay using accurate rects)
    val highlightedBitmap = remember(pageFrame, searchQuery, state.activeMatch) {
        val frame = pageFrame ?: return@remember null
        val matches = state.searchMatches.filter { it.pageIndex == pageIndex }
        
        if (searchQuery.isBlank() || matches.isEmpty()) {
            frame.bitmap
        } else {
            val activeMatchObj = state.searchMatches.getOrNull(state.activeMatch)
            val matchRects = matches.flatMap { match ->
                val isActive = (match === activeMatchObj)
                match.pdfRects.map { rect -> rect to isActive }
            }
            val renderScale = frame.bitmap.width.toFloat() / frame.widthPts
            frame.bitmap.withAccurateHighlights(matchRects, renderScale)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Issue 4 — Simple render: no per-page zoom; zoom is applied by the parent wrapper Box
        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            error ?: "Error",
                            color = Color.Red.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                highlightedBitmap == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.BrandStrong, modifier = Modifier.size(36.dp))
                    }
                }
                else -> {
                    // AGENT 4 — Display bitmap (with on-canvas highlights already baked in)
                    Image(
                        bitmap = highlightedBitmap.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        // Thin separator line between pages
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFF1E2433))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordFullscreenViewer(
    state: DocumentViewerState.Ready,
    isSearching: Boolean,
    onBack: () -> Unit,
    onTitleDoubleTap: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onSave: () -> Unit,
    onParagraphTextChange: (Int, String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Zoom / pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isMultiTouch by remember { mutableStateOf(false) }
    var flingJobX by remember { mutableStateOf<Job?>(null) }
    var flingJobY by remember { mutableStateOf<Job?>(null) }

    // Scroll / visibility state
    val lazyListState = rememberLazyListState()
    var barsVisible by remember { mutableStateOf(true) }
    var scrollbarVisible by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragScrollJob by remember { mutableStateOf<Job?>(null) }
    val thumbWidthDp by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 8.dp,
        animationSpec = tween(150),
        label = "thumbWidth"
    )

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topContentOffset = TOP_BAR_HEIGHT + statusBarPadding

    // Keep bars visible while search or edit is active
    LaunchedEffect(lazyListState, isSearching, state.isEditing) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                if (!isSearching && !state.isEditing) {
                    val scrolledDown = index > previousIndex || (index == previousIndex && offset > previousOffset)
                    barsVisible = !scrolledDown
                } else {
                    barsVisible = true
                }
                previousIndex = index
                previousOffset = offset
            }
    }

    // Scrollbar visibility
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            scrollbarVisible = true
        } else {
            delay(1500)
            scrollbarVisible = false
        }
    }

    // Paginate elements
    val pages = state.wordLayoutPages
    val totalPages = pages.size.coerceAtLeast(1)

    // Auto-scroll to page with active match
    LaunchedEffect(state.activeMatch) {
        val matchList = state.searchMatches
        if (matchList.isNotEmpty() && state.activeMatch >= 0) {
            val pageIdx = matchList[state.activeMatch].pageIndex
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            lazyListState.animateScrollToItem(
                index = pageIdx,
                scrollOffset = 0
            )
        }
    }

    // Scroll fraction
    val scrollFraction by remember(totalPages) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 0f

            val firstVisibleItem = visibleItems.first()
            val lastVisibleItem = visibleItems.last()
            val totalItems = layoutInfo.totalItemsCount

            if (firstVisibleItem.index == 0 && firstVisibleItem.offset == 0) {
                return@derivedStateOf 0f
            }

            if (lastVisibleItem.index == totalItems - 1) {
                val lastItemBottom = lastVisibleItem.offset + lastVisibleItem.size
                val viewportBottom = layoutInfo.viewportEndOffset
                if (lastItemBottom <= viewportBottom) {
                    return@derivedStateOf 1f
                }
            }

            val pageHeightPx = visibleItems.firstOrNull { it.index < totalPages }?.size ?: 1
            val bottomSpacerPx = with(density) { 80.dp.toPx() }
            val totalHeight = totalPages * pageHeightPx + bottomSpacerPx
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val maxScroll = (totalHeight - viewportHeight).coerceAtLeast(1f)

            val currentScroll = lazyListState.firstVisibleItemIndex * pageHeightPx + lazyListState.firstVisibleItemScrollOffset
            (currentScroll.toFloat() / maxScroll).coerceIn(0f, 1f)
        }
    }

    // Current page
    val currentPage by remember(totalPages) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 1

            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centerItem = visibleItems.firstOrNull { item ->
                val itemStart = item.offset
                val itemEnd = item.offset + item.size
                viewportCenter in itemStart..itemEnd
            } ?: visibleItems.firstOrNull()

            val index = centerItem?.index ?: 0
            val pageIdx = index.coerceIn(0, totalPages - 1)
            pageIdx + 1
        }
    }

    var isFirstPageLoad by remember { mutableStateOf(true) }
    LaunchedEffect(currentPage) {
        if (isFirstPageLoad) {
            isFirstPageLoad = false
        } else {
            haptics.performHapticFeedback()
        }
    }

    val animatedTopPadding by animateDpAsState(
        targetValue = if (barsVisible) topContentOffset else statusBarPadding,
        animationSpec = tween(200),
        label = "topPadding"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WordUi.WordCanvasGray)
    ) {
        // Word pages list inside Zoom container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = animatedTopPadding)
                .pointerInput(totalPages) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        flingJobX?.cancel()
                        flingJobY?.cancel()
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val activeChanges = changes.filter { it.pressed }

                            if (activeChanges.isEmpty()) {
                                if (scale > 1.1f) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    val maxPanX = ((size.width * scale) - size.width).coerceAtLeast(0f) / 2f
                                    val maxPanY = ((size.height * scale) - size.height).coerceAtLeast(0f) / 2f

                                    flingJobX = coroutineScope.launch {
                                        Animatable(offsetX).animateDecay(
                                            initialVelocity = velocity.x,
                                            animationSpec = exponentialDecay()
                                        ) {
                                            offsetX = this.value.coerceIn(-maxPanX, maxPanX)
                                        }
                                    }
                                    flingJobY = coroutineScope.launch {
                                        var lastValue = offsetY
                                        Animatable(offsetY).animateDecay(
                                            initialVelocity = velocity.y,
                                            animationSpec = exponentialDecay()
                                        ) {
                                            val delta = this.value - lastValue
                                            lastValue = this.value
                                            val newOffsetY = offsetY + delta
                                            if (newOffsetY > maxPanY) {
                                                offsetY = maxPanY
                                                val overscroll = newOffsetY - maxPanY
                                                lazyListState.dispatchRawDelta(-overscroll)
                                            } else if (newOffsetY < -maxPanY) {
                                                offsetY = -maxPanY
                                                val overscroll = newOffsetY - (-maxPanY)
                                                lazyListState.dispatchRawDelta(-overscroll)
                                            } else {
                                                offsetY = newOffsetY
                                            }
                                        }
                                    }
                                }
                                isMultiTouch = false
                                break
                            }

                            val isMulti = activeChanges.size >= 2
                            isMultiTouch = isMulti

                            if (isMulti) {
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                val scaleChange = newScale / scale

                                if (newScale > 1f) {
                                    val maxPanX = ((size.width * newScale) - size.width).coerceAtLeast(0f) / 2f
                                    val maxPanY = ((size.height * newScale) - size.height).coerceAtLeast(0f) / 2f
                                    offsetX = (offsetX * scaleChange - (centroid.x - center.x) * (scaleChange - 1) + pan.x).coerceIn(-maxPanX, maxPanX)
                                    offsetY = (offsetY * scaleChange - (centroid.y - center.y) * (scaleChange - 1) + pan.y).coerceIn(-maxPanY, maxPanY)
                                    scale = newScale
                                } else {
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                }
                                changes.forEach { it.consume() }
                            } else {
                                if (scale > 1f) {
                                    val pointer = activeChanges.first()
                                    velocityTracker.addPosition(pointer.uptimeMillis, pointer.position)

                                    val pan = event.calculatePan()
                                    val maxPanX = ((size.width * scale) - size.width).coerceAtLeast(0f) / 2f
                                    val maxPanY = ((size.height * scale) - size.height).coerceAtLeast(0f) / 2f
                                    offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)

                                    val newOffsetY = offsetY + pan.y
                                    if (newOffsetY > maxPanY) {
                                        offsetY = maxPanY
                                        val overscroll = newOffsetY - maxPanY
                                        lazyListState.dispatchRawDelta(-overscroll)
                                    } else if (newOffsetY < -maxPanY) {
                                        offsetY = -maxPanY
                                        val overscroll = newOffsetY - (-maxPanY)
                                        lazyListState.dispatchRawDelta(-overscroll)
                                    } else {
                                        offsetY = newOffsetY
                                    }
                                    changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
                .pointerInput(isSearching) {
                    detectTapGestures(
                        onTap = { if (!isSearching && !state.isEditing) barsVisible = !barsVisible },
                        onDoubleTap = { tapOffset ->
                            flingJobX?.cancel()
                            flingJobY?.cancel()
                            if (scale > 1.1f) {
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            } else {
                                val targetScale = 2.5f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxPanX = ((size.width * targetScale) - size.width).coerceAtLeast(0f) / 2f
                                val maxPanY = ((size.height * targetScale) - size.height).coerceAtLeast(0f) / 2f
                                offsetX = (-(tapOffset.x - center.x) * targetScale).coerceIn(-maxPanX, maxPanX)
                                offsetY = (-(tapOffset.y - center.y) * targetScale).coerceIn(-maxPanY, maxPanY)
                                scale = targetScale
                            }
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            LazyColumn(
                state = lazyListState,
                userScrollEnabled = scale <= 1f && !isMultiTouch,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(pages) { index, page ->
                    val matchesBeforePage = remember(pages, state.searchQuery, index) {
                        if (state.searchQuery.isBlank()) 0 else {
                            var count = 0
                            for (i in 0 until index) {
                                count += countMatchesInPage(pages[i], state.searchQuery)
                            }
                            count
                        }
                    }
                    WordPageItem(
                        filePath = state.request.path,
                        page = page,
                        pageNumber = index + 1,
                        totalPages = totalPages,
                        searchQuery = if (isSearching) state.searchQuery else "",
                        activeMatchIndex = state.activeMatch,
                        matchesBeforePage = matchesBeforePage,
                        isEditing = state.isEditing,
                        editedWordParagraphs = state.editedWordParagraphs,
                        onParagraphTextChange = onParagraphTextChange
                    )
                }
                item { Spacer(modifier = Modifier.size(80.dp)) }
            }
        }

        // Top bar overlay
        AnimatedVisibility(
            visible = barsVisible,
            enter = slideInVertically(tween(200)) { -it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            if (state.isEditing) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Edit ${state.request.displayName}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onExitEdit() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppColors.BrandStrong)
                        }
                    },
                    actions = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onSave() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.88f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = AppColors.BrandStrong,
                        actionIconContentColor = Color.White
                    )
                )
            } else if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search in document", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = AppColors.BrandStrong
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
                            Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                enabled = state.searchMatches.isNotEmpty(),
                                onClick = { haptics.performHapticFeedback(); onPrevMatch() }
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous",
                                    tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray
                                )
                            }
                            IconButton(
                                enabled = state.searchMatches.isNotEmpty(),
                                onClick = { haptics.performHapticFeedback(); onNextMatch() }
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next",
                                    tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.Gray
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.92f),
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
                                text = "${state.request.extension.uppercase()} - $totalPages pages",
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

        WordFloatingEditButton(
            visible = state.request.canEdit && !state.isEditing && !isSearching && barsVisible,
            onClick = {
                haptics.performHapticFeedback()
                onEdit()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 28.dp)
        )

        // Draggable scrollbar
        AnimatedVisibility(
            visible = (scrollbarVisible || lazyListState.isScrollInProgress || isDragging) && !state.isEditing,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(800)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(36.dp)
                .padding(top = animatedTopPadding, bottom = 8.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerHeightPx = with(density) { maxHeight.toPx() }
                val thumbMinHeightDp = 40.dp
                val thumbHeightPx = with(density) { thumbMinHeightDp.toPx() }
                val thumbOffsetPx = (scrollFraction * (containerHeightPx - thumbHeightPx))
                    .coerceIn(0f, (containerHeightPx - thumbHeightPx).coerceAtLeast(0f))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalPages) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    scrollbarVisible = true
                                    dragScrollJob?.cancel()
                                },
                                onDragEnd = {
                                    isDragging = false
                                    dragScrollJob?.cancel()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragScrollJob?.cancel()
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    val availableHeight = (size.height - thumbHeightPx).coerceAtLeast(1f)
                                    val fraction = ((change.position.y - thumbHeightPx / 2f) / availableHeight).coerceIn(0f, 1f)
                                    val layoutInfo = lazyListState.layoutInfo
                                    val visibleItems = layoutInfo.visibleItemsInfo
                                    if (visibleItems.isNotEmpty()) {
                                        val totalItems = layoutInfo.totalItemsCount
                                        val pageHeightPx = visibleItems.firstOrNull { it.index < totalPages }?.size ?: visibleItems.first().size
                                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                        val bottomSpacerPx = with(density) { 80.dp.toPx() }
                                        val totalHeight = totalPages * pageHeightPx + bottomSpacerPx
                                        val maxScroll = (totalHeight - viewportHeight).coerceAtLeast(0f)

                                        val targetScrollPx = fraction * maxScroll
                                        val targetItem = (targetScrollPx / pageHeightPx).toInt().coerceIn(0, totalItems - 1)
                                        val targetOffset = (targetScrollPx % pageHeightPx).toInt()

                                        dragScrollJob?.cancel()
                                        dragScrollJob = coroutineScope.launch(Dispatchers.Main.immediate) {
                                            lazyListState.scrollToItem(targetItem, targetOffset)
                                        }
                                    }
                                }
                            )
                        }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .fillMaxHeight()
                        .width(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .border(
                            width = 0.5.dp,
                            color = Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                        .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                        .width(thumbWidthDp)
                        .heightIn(min = thumbMinHeightDp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8A8886))
                        .border(
                            width = 0.5.dp,
                            color = Color.Black.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
        }

        // Bottom-left page capsule
        if (!state.isEditing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 20.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$currentPage / $totalPages",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Centering saving progress overlay
        if (state.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = AppColors.BrandStrong)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Saving...", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordFloatingEditButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 2 },
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = NeonPurpleSubtle,
            contentColor = AppColors.BrandStrong,
            modifier = Modifier
                .size(58.dp)
                .neonGlow(color = AppColors.BrandStrong, radius = 12.dp, shape = CircleShape)
                .border(1.5.dp, AppColors.BrandStrong, CircleShape)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Word document")
        }
    }
}


@Composable
private fun WordPageItem(
    filePath: String,
    page: LayoutPage,
    pageNumber: Int,
    totalPages: Int,
    searchQuery: String,
    activeMatchIndex: Int,
    matchesBeforePage: Int,
    isEditing: Boolean,
    editedWordParagraphs: Map<Int, String>,
    onParagraphTextChange: (Int, String) -> Unit
) {
    val cacheKey = "$filePath:$pageNumber:$searchQuery:$activeMatchIndex:isEditing:$isEditing"
    val renderTriggerKey = remember(page) { page.hashCode() }
    var bitmap by remember(cacheKey, renderTriggerKey) {
        mutableStateOf<Bitmap?>(com.ioristudios.anydoc.util.RenderCache.get("$cacheKey:$renderTriggerKey"))
    }
    var error by remember(cacheKey, renderTriggerKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(cacheKey, renderTriggerKey) {
        if (bitmap != null) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            runCatching {
                com.ioristudios.anydoc.util.PageRenderer.render(
                    page = page,
                    filePath = filePath,
                    searchQuery = searchQuery,
                    activeMatchIndex = activeMatchIndex,
                    matchesBeforePage = matchesBeforePage,
                    hideParagraphText = isEditing
                )
            }
        }
        result.onSuccess { bmp ->
            com.ioristudios.anydoc.util.RenderCache.put("$cacheKey:$renderTriggerKey", bmp)
            bitmap = bmp
        }
        result.onFailure { err ->
            error = err.localizedMessage ?: "Render error"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WordUi.PageOuterPadding)
            .widthIn(max = WordUi.PageMaxWidth)
            .wrapContentWidth(Alignment.CenterHorizontally)
            .aspectRatio(WordUi.PageAspectRatio)
            .shadow(12.dp, RoundedCornerShape(2.dp), ambientColor = WordUi.WordShadow, spotColor = WordUi.WordShadow)
            .border(0.5.dp, WordUi.WordBorderGray.copy(alpha = 0.8f), RoundedCornerShape(2.dp)),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = WordUi.WordPageWhite)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val scalePointsToDp = maxWidth / page.config.pageWidthPoints

            when {
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: "Unknown render error",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                bitmap == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.BrandStrong, modifier = Modifier.size(36.dp))
                    }
                }
                else -> {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Page $pageNumber",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillWidth
                    )

                    if (isEditing) {
                        val flatParas = remember(page.elements) {
                            val list = mutableListOf<FlatParagraph>()
                            collectFlatParagraphs(
                                page.elements,
                                page.config.marginLeftPoints,
                                page.config.marginTopPoints,
                                list
                            )
                            list
                        }

                        flatParas.forEach { flatPara ->
                            val pWidth = (flatPara.width * scalePointsToDp.value).dp
                            val pHeight = (flatPara.height * scalePointsToDp.value).dp
                            val pX = (flatPara.x * scalePointsToDp.value).dp
                            val pY = (flatPara.y * scalePointsToDp.value).dp

                            val textColor = remember(flatPara.para) {
                                val hex = flatPara.para.spans.firstOrNull()?.color
                                if (hex != null) {
                                    try {
                                        val cleanHex = hex.replace("#", "")
                                        val colorInt = android.graphics.Color.parseColor(
                                            if (cleanHex.length == 6) "#$cleanHex" else if (cleanHex.length == 8) "#$cleanHex" else "#000000"
                                        )
                                        Color(colorInt)
                                    } catch (e: Exception) {
                                        Color.Black
                                    }
                                } else {
                                    Color.Black
                                }
                            }

                            var isFocused by remember { mutableStateOf(false) }
                            val textValue = remember(flatPara.para.originalIndex) {
                                mutableStateOf(
                                    editedWordParagraphs[flatPara.para.originalIndex]
                                        ?: flatPara.para.spans.joinToString("") { it.text }
                                )
                            }

                            LaunchedEffect(editedWordParagraphs[flatPara.para.originalIndex]) {
                                val latest = editedWordParagraphs[flatPara.para.originalIndex]
                                    ?: flatPara.para.spans.joinToString("") { it.text }
                                if (latest != textValue.value) {
                                    textValue.value = latest
                                }
                            }

                            val firstSpan = flatPara.para.spans.firstOrNull()
                            val fontSizeSp = ((firstSpan?.fontSize ?: 12f) * scalePointsToDp.value).sp
                            val fontWeight = if (firstSpan?.bold == true) FontWeight.Bold else FontWeight.Normal
                            val fontStyle = if (firstSpan?.italic == true) FontStyle.Italic else FontStyle.Normal
                            val textDecoration = if (firstSpan?.underline == true) TextDecoration.Underline else TextDecoration.None
                            val align = when (flatPara.para.alignment) {
                                DocxTextAlignment.Start -> TextAlign.Start
                                DocxTextAlignment.Center -> TextAlign.Center
                                DocxTextAlignment.End -> TextAlign.End
                                DocxTextAlignment.Justify -> TextAlign.Justify
                            }
                            val lineSpacingSp = flatPara.para.lineSpacingTwips?.let {
                                ((it / 20f) * scalePointsToDp.value).sp
                            }

                            BasicTextField(
                                value = textValue.value,
                                onValueChange = { newText ->
                                    textValue.value = newText
                                    onParagraphTextChange(flatPara.para.originalIndex, newText)
                                },
                                modifier = Modifier
                                    .offset(x = pX, y = pY)
                                    .width(pWidth)
                                    .heightIn(min = pHeight)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .background(
                                        color = if (isFocused) AppColors.BrandStrong.copy(alpha = 0.05f) else Color.Transparent,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (isFocused) AppColors.BrandStrong.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .padding(horizontal = 2.dp),
                                textStyle = TextStyle(
                                    fontSize = fontSizeSp,
                                    fontWeight = fontWeight,
                                    fontStyle = fontStyle,
                                    textDecoration = textDecoration,
                                    textAlign = align,
                                    color = textColor,
                                    lineHeight = lineSpacingSp ?: TextUnit.Unspecified
                                ),
                                cursorBrush = SolidColor(AppColors.BrandStrong),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )
                        }
                    }
                }
            }
            Text(
                text = "$pageNumber / $totalPages",
                color = WordUi.WordSecondaryText.copy(alpha = 0.7f),
                style = TextStyle(fontSize = 10.sp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
        }
    }
}

private fun countMatchesInElement(element: PositionedElement, query: String): Int {
    if (query.isBlank()) return 0
    return when (element) {
        is PositionedElement.Paragraph -> {
            var count = 0
            var start = 0
            val text = element.para.spans.joinToString("") { it.text }
            while (true) {
                val i = text.indexOf(query, start, ignoreCase = true)
                if (i < 0) break
                count++
                start = i + query.length
            }
            count
        }
        is PositionedElement.Table -> {
            var sum = 0
            element.cellElements.forEach { row ->
                row.forEach { cell ->
                    cell.forEach { subEl ->
                        sum += countMatchesInElement(subEl, query)
                    }
                }
            }
            sum
        }
        is PositionedElement.Image -> 0
    }
}

private fun countMatchesInPage(page: LayoutPage, query: String): Int {
    return page.elements.sumOf { countMatchesInElement(it, query) }
}

private fun twipsToDp(twips: Int): Dp = (twips / 20f).dp

private fun getMatchesBeforeElement(elements: List<DocxElement>, targetElement: DocxElement, query: String): Int {
    if (query.isBlank()) return 0
    var count = 0
    for (el in elements) {
        if (el === targetElement) break
        count += countMatchesInElement(el, query)
    }
    return count
}

private fun countMatchesInElement(element: DocxElement, query: String): Int {
    if (query.isBlank()) return 0
    return when (element) {
        is DocxElement.Paragraph -> countOccurrences(element.para.spans.joinToString("") { it.text }, query)
        is DocxElement.Table -> {
            var sum = 0
            element.table.rows.forEach { r ->
                r.cells.forEach { c ->
                    c.elements.forEach { e ->
                        sum += countMatchesInElement(e, query)
                    }
                }
            }
            sum
        }
        else -> 0
    }
}

private fun countOccurrences(text: String, query: String): Int {
    var count = 0
    var start = 0
    while (true) {
        val i = text.indexOf(query, start, ignoreCase = true)
        if (i < 0) break
        count++
        start = i + query.length
    }
    return count
}
