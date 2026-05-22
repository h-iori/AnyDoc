package com.ioristudios.anydoc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.anydoc.model.*
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.util.SpreadsheetLayoutEngine
import com.ioristudios.anydoc.util.XlsxParser
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

// ─── Excel-themed color palette ─────────────────────────────────────────────

private object ExcelUi {
    val ExcelGreen = Color(0xFF217346)
    val ExcelGreenDark = Color(0xFF185C37)
    val ExcelGreenLight = Color(0xFF33A854)
    val ExcelGreenSubtle = Color(0xFFE2EFDA)

    val GridBackground = Color(0xFFFFFFFF)
    val HeaderBackground = Color(0xFFF3F2F1)
    val HeaderText = Color(0xFF1F1F1F)
    val HeaderBorder = Color(0xFFD6D6D6)
    val GridLine = Color(0xFFE2E2E2)
    val CellText = Color(0xFF1F1F1F)
    val CellTextSecondary = Color(0xFF605E5C)

    val SelectedCellBorder = Color(0xFF217346)
    val SelectedCellFill = Color(0x0C217346)
    val SelectedHeaderBg = Color(0xFFE2EFDA)

    val FormulaBarBg = Color(0xFFFAFAFA)
    val FormulaBarBorder = Color(0xFFD2D0CE)

    val SheetTabBarBg = Color(0xFFEEEDEC)
    val SheetTabActive = Color(0xFFFFFFFF)
    val SheetTabInactive = Color(0xFFD6D6D6)
    val SheetTabText = Color(0xFF3B3A39)
    val SheetTabActiveText = Color(0xFF217346)

    val ToolbarDark = Color.Black.copy(alpha = 0.88f)
}

private val TOP_BAR_HEIGHT: Dp = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetFullscreenViewer(
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
    onCellEdit: (Int, Int, String) -> Unit,
    onSelectCell: (Int, Int) -> Unit,
    onSwitchSheet: (Int) -> Unit,
    onAddRow: () -> Unit,
    onAddColumn: () -> Unit,
    onFormulaBarChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val content = state.content as? DocumentContent.SpreadsheetContent ?: return
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    val activeSheet = content.sheets.getOrNull(state.activeSheetIndex)
        ?: content.sheets.firstOrNull()
        ?: return

    val layout = remember(activeSheet) {
        SpreadsheetLayoutEngine.computeLayout(activeSheet)
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topContentOffset = TOP_BAR_HEIGHT + statusBarPadding

    var barsVisible by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var scrollX by rememberSaveable { mutableFloatStateOf(0f) }

    // Keep bars visible while search or edit is active
    LaunchedEffect(listState, isSearching, state.isEditing) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
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

    // Zoom / pan state
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var gestureScale by remember { mutableFloatStateOf(1f) }
    var gesturePanX by remember { mutableFloatStateOf(0f) }
    var gesturePanY by remember { mutableFloatStateOf(0f) }
    
    var isMultiTouch by remember { mutableStateOf(false) }
    var flingJobX by remember { mutableStateOf<Job?>(null) }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Track currently editing cell
    var editingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingText by remember { mutableStateOf("") }

    val animatedTopPadding by animateDpAsState(
        targetValue = if (barsVisible) topContentOffset else statusBarPadding,
        animationSpec = tween(200),
        label = "topPadding"
    )

    val columnOffsets = remember(layout.columnWidths) {
        val offsets = FloatArray(layout.columnWidths.size + 1)
        var current = 0f
        offsets[0] = 0f
        for (i in layout.columnWidths.indices) {
            current += layout.columnWidths[i]
            offsets[i + 1] = current
        }
        offsets
    }
    val totalColumnWidthDp = columnOffsets.last()

    val effectiveScale = scale * gestureScale
    val scaledRowHeaderWidthDp = SpreadsheetLayoutEngine.ROW_HEADER_WIDTH_DP * effectiveScale
    val scaledColHeaderHeightDp = SpreadsheetLayoutEngine.COLUMN_HEADER_HEIGHT_DP * effectiveScale
    val totalColumnWidthPx = with(density) { (totalColumnWidthDp * scale).dp.toPx() } // Unscaled for layout bounds
    val rowHeaderWidthPx = with(density) { scaledRowHeaderWidthDp.dp.toPx() }
    
    // Calculate visible column indices based on scrollX
    val cellViewportWidthDp = with(density) { (containerWidthPx - rowHeaderWidthPx).toDp().value }.coerceAtLeast(0f)
    val scrollXDp = with(density) { scrollX.toDp().value }
    
    val startVisibleDp = scrollXDp / scale
    val endVisibleDp = (scrollXDp + cellViewportWidthDp / gestureScale) / scale

    val firstVisibleColIndex = remember(columnOffsets, startVisibleDp) {
        val idx = columnOffsets.binarySearch(startVisibleDp)
        if (idx >= 0) idx else -idx - 2
    }.coerceAtLeast(0)

    val lastVisibleColIndex = remember(columnOffsets, endVisibleDp) {
        val idx = columnOffsets.binarySearch(endVisibleDp)
        if (idx >= 0) idx else -idx - 1
    }.coerceAtMost(layout.columnWidths.size - 1).coerceAtLeast(0)

    // Auto-navigate to search match
    LaunchedEffect(state.activeMatch, state.activeSheetIndex, state.searchMatches, containerWidthPx) {
        if (containerWidthPx <= 0f) return@LaunchedEffect
        val matches = state.searchMatches
        val activeIdx = state.activeMatch
        if (matches.isNotEmpty() && activeIdx in matches.indices) {
            val match = matches[activeIdx]
            
            // 1. Switch sheet if not on the correct one
            if (match.sheetIndex >= 0 && match.sheetIndex != state.activeSheetIndex) {
                onSwitchSheet(match.sheetIndex)
                return@LaunchedEffect
            }
            
            // 2. Select the cell
            if (match.rowIndex >= 0 && match.colIndex >= 0) {
                onSelectCell(match.rowIndex, match.colIndex)
                
                // 3. Scroll vertically to the rowIndex
                coroutineScope.launch {
                    listState.animateScrollToItem(
                        index = match.rowIndex,
                        scrollOffset = -150
                    )
                }
                
                // 4. Scroll horizontally to the colIndex
                flingJobX?.cancel()
                coroutineScope.launch {
                    val targetLeftDp = columnOffsets.getOrNull(match.colIndex) ?: 0f
                    val targetLeftPx = with(density) { (targetLeftDp * scale).dp.toPx() }
                    
                    val cellViewportWidth = (containerWidthPx - rowHeaderWidthPx).coerceAtLeast(0f)
                    val colWidth = layout.columnWidths.getOrNull(match.colIndex) ?: SpreadsheetLayoutEngine.DEFAULT_COLUMN_WIDTH_DP
                    val colWidthPx = with(density) { (colWidth * scale).dp.toPx() }
                    
                    val targetScrollX = (targetLeftPx - cellViewportWidth / 2f + colWidthPx / 2f)
                        .coerceIn(0f, (totalColumnWidthPx - cellViewportWidth).coerceAtLeast(0f))
                    
                    val distance = kotlin.math.abs(targetScrollX - scrollX)
                    val maxDistanceToAnimate = cellViewportWidth * 1.5f
                    
                    if (distance > maxDistanceToAnimate) {
                        // Snap closer first to avoid instantiating thousands of cells during animation
                        val snapTo = if (targetScrollX > scrollX) {
                            targetScrollX - maxDistanceToAnimate
                        } else {
                            targetScrollX + maxDistanceToAnimate
                        }
                        scrollX = snapTo
                    }
                    
                    Animatable(scrollX).animateTo(
                        targetValue = targetScrollX,
                        animationSpec = androidx.compose.animation.core.spring(
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        )
                    ) {
                        scrollX = this.value
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ExcelUi.GridBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = animatedTopPadding)
        ) {
            // ── Formula bar ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.isEditing,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                FormulaBar(
                    cellRef = state.selectedCell?.let { (row, col) ->
                        "${XlsxParser.columnName(col)}${row + 1}"
                    } ?: "",
                    text = state.formulaBarText,
                    onTextChange = { newText ->
                        onFormulaBarChange(newText)
                        // Also update the editing cell if one is selected
                        state.selectedCell?.let { _ ->
                            editingText = newText
                        }
                    },
                    onCommit = {
                        state.selectedCell?.let { (row, col) ->
                            onCellEdit(row, col, state.formulaBarText)
                            editingCell = null
                        }
                    }
                )
            }

            // ── Zoomable Grid Container ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onGloballyPositioned { coordinates ->
                        containerWidthPx = coordinates.size.width.toFloat()
                        containerHeightPx = coordinates.size.height.toFloat()
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            flingJobX?.cancel()
                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)

                            var isHorizontalDrag = false
                            var isVerticalDrag = false
                            var hasLockedDirection = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val activeChanges = changes.filter { it.pressed }

                                if (activeChanges.isEmpty()) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    
                                    // Commit zoom phase
                                    if (gestureScale != 1f) {
                                        val newScale = (scale * gestureScale).coerceIn(0.25f, 5f)
                                        val newScrollX = scrollX * gestureScale - gesturePanX
                                        
                                        // Calculate max scroll for new scale
                                        val newTotalPx = with(density) { (totalColumnWidthDp * newScale).dp.toPx() }
                                        val viewport = (containerWidthPx - with(density) { (SpreadsheetLayoutEngine.ROW_HEADER_WIDTH_DP * newScale).dp.toPx() }).coerceAtLeast(0f)
                                        val maxScrollX = (newTotalPx - viewport).coerceAtLeast(0f)
                                        
                                        scrollX = newScrollX.coerceIn(0f, maxScrollX)
                                        scale = newScale
                                        
                                        val finalPanY = gesturePanY
                                        gestureScale = 1f
                                        gesturePanX = 0f
                                        gesturePanY = 0f
                                        
                                        if (finalPanY != 0f) {
                                            coroutineScope.launch {
                                                listState.animateScrollBy(-finalPanY)
                                            }
                                        }
                                        
                                        isMultiTouch = false
                                        break
                                    }
                                    
                                    if (isHorizontalDrag) {
                                        val cellViewportWidth = (containerWidthPx - rowHeaderWidthPx).coerceAtLeast(0f)
                                        val maxScrollX = (totalColumnWidthPx - cellViewportWidth).coerceAtLeast(0f)

                                        flingJobX = coroutineScope.launch {
                                            Animatable(scrollX).animateDecay(
                                                initialVelocity = -velocity.x,
                                                animationSpec = exponentialDecay()
                                            ) {
                                                scrollX = this.value.coerceIn(0f, maxScrollX)
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
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    
                                    val newGestureScale = gestureScale * zoom
                                    val projectedScale = (scale * newGestureScale).coerceIn(0.25f, 5f)
                                    val actualNewGestureScale = projectedScale / scale
                                    
                                    val scaleFactor = actualNewGestureScale / gestureScale
                                    
                                    gesturePanX = gesturePanX * scaleFactor + centroid.x * (1 - scaleFactor) + pan.x
                                    gesturePanY = gesturePanY * scaleFactor + centroid.y * (1 - scaleFactor) + pan.y
                                    
                                    gestureScale = actualNewGestureScale
                                    changes.forEach { it.consume() }
                                } else {
                                    val pointer = activeChanges.first()
                                    velocityTracker.addPosition(pointer.uptimeMillis, pointer.position)

                                    val pan = event.calculatePan()
                                    
                                    if (!hasLockedDirection && gestureScale == 1f) {
                                        if (kotlin.math.abs(pan.x) > 1f || kotlin.math.abs(pan.y) > 1f) {
                                            hasLockedDirection = true
                                            if (kotlin.math.abs(pan.x) > kotlin.math.abs(pan.y)) {
                                                isHorizontalDrag = true
                                            } else {
                                                isVerticalDrag = true
                                            }
                                        }
                                    }

                                    if (gestureScale != 1f) {
                                        gesturePanX += pan.x
                                        gesturePanY += pan.y
                                        changes.forEach { it.consume() }
                                    } else if (isHorizontalDrag) {
                                        val cellViewportWidth = (containerWidthPx - rowHeaderWidthPx).coerceAtLeast(0f)
                                        val maxScrollX = (totalColumnWidthPx - cellViewportWidth).coerceAtLeast(0f)
                                        scrollX = (scrollX - pan.x).coerceIn(0f, maxScrollX)
                                        changes.forEach { it.consume() }
                                    } else if (isVerticalDrag) {
                                        // let LazyColumn handle it natively
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
                                if (scale > 1.1f) {
                                    scale = 1f
                                } else {
                                    val targetScale = 2.5f
                                    val scaleFactor = targetScale / scale
                                    val focalX = scrollX + (tapOffset.x - rowHeaderWidthPx)
                                    val newScrollX = focalX * scaleFactor - (tapOffset.x - rowHeaderWidthPx)
                                    
                                    val cellViewportWidth = (containerWidthPx - rowHeaderWidthPx).coerceAtLeast(0f)
                                    val newTotalColumnWidthPx = totalColumnWidthDp * targetScale * density.density
                                    val maxScrollX = (newTotalColumnWidthPx - cellViewportWidth).coerceAtLeast(0f)
                                    
                                    scrollX = newScrollX.coerceIn(0f, maxScrollX)
                                    scale = targetScale
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = gestureScale
                            scaleY = gestureScale
                            translationX = gesturePanX
                            translationY = gesturePanY
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    // ── Column headers (frozen vertically, scrolls horizontally via scrollX offset) ──
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Corner cell
                        Box(
                            modifier = Modifier
                                .width(scaledRowHeaderWidthDp.dp)
                                .height(scaledColHeaderHeightDp.dp)
                                .background(ExcelUi.HeaderBackground)
                                .border(0.5.dp, ExcelUi.HeaderBorder)
                        )

                        // Column header strip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(scaledColHeaderHeightDp.dp)
                                .clipToBounds()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                            ) {
                                for (colIdx in firstVisibleColIndex..lastVisibleColIndex) {
                                    val colWidth = layout.columnWidths[colIdx]
                                    val startOffset = columnOffsets[colIdx]
                                    val startOffsetPx = with(density) { (startOffset * scale).dp.toPx() }
                                    val isSelected = state.selectedCell?.second == colIdx
                                    
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset((startOffsetPx - scrollX).roundToInt(), 0) }
                                            .width((colWidth * scale).dp)
                                            .fillMaxHeight()
                                            .background(if (isSelected) ExcelUi.SelectedHeaderBg else ExcelUi.HeaderBackground)
                                            .border(0.5.dp, ExcelUi.HeaderBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = XlsxParser.columnName(colIdx),
                                            style = TextStyle(
                                                fontSize = (11f * scale).sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) ExcelUi.ExcelGreen else ExcelUi.HeaderText,
                                                textAlign = TextAlign.Center
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Data grid cells and row headers ──
                    LazyColumn(
                        state = listState,
                        userScrollEnabled = !isMultiTouch,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(activeSheet.rowCount.coerceAtLeast(1)) { rowIdx ->
                            val row = activeSheet.rows.find { it.rowIndex == rowIdx }
                            val scaledRowHeightDp = layout.rowHeights.getOrElse(rowIdx) { SpreadsheetLayoutEngine.DEFAULT_ROW_HEIGHT_DP } * scale

                            Row(
                                modifier = Modifier
                                    .height(scaledRowHeightDp.dp)
                                    .fillMaxWidth()
                            ) {
                                // Row number (frozen)
                                val isRowSelected = state.selectedCell?.first == rowIdx
                                Box(
                                    modifier = Modifier
                                        .width(scaledRowHeaderWidthDp.dp)
                                        .fillMaxHeight()
                                        .background(if (isRowSelected) ExcelUi.SelectedHeaderBg else ExcelUi.HeaderBackground)
                                        .border(0.5.dp, ExcelUi.HeaderBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${rowIdx + 1}",
                                        style = TextStyle(
                                            fontSize = (11f * scale).sp,
                                            fontWeight = if (isRowSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isRowSelected) ExcelUi.ExcelGreen else ExcelUi.CellTextSecondary,
                                            textAlign = TextAlign.Center
                                        ),
                                        maxLines = 1
                                    )
                                }

                                // Cells (offsets horizontally)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clipToBounds()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                    ) {
                                        for (colIdx in firstVisibleColIndex..lastVisibleColIndex) {
                                            // Check if this cell is hidden by a merge
                                            if (SpreadsheetLayoutEngine.isCellHiddenByMerge(rowIdx, colIdx, activeSheet.mergedRegions)) {
                                                continue
                                            }

                                             key(colIdx) {
                                                val mergedRegion = SpreadsheetLayoutEngine.getMergedRegionAt(rowIdx, colIdx, activeSheet.mergedRegions)
                                                val cellWidth = (if (mergedRegion != null) {
                                                    SpreadsheetLayoutEngine.mergedWidth(mergedRegion, layout.columnWidths)
                                                } else {
                                                    layout.columnWidths.getOrElse(colIdx) { SpreadsheetLayoutEngine.DEFAULT_COLUMN_WIDTH_DP }
                                                }) * scale
                                                val cellHeight = if (mergedRegion != null) {
                                                    SpreadsheetLayoutEngine.mergedHeight(mergedRegion, layout.rowHeights) * scale
                                                } else {
                                                    scaledRowHeightDp
                                                }
                                                
                                                val startOffset = columnOffsets[colIdx]
                                                val startOffsetPx = with(density) { (startOffset * scale).dp.toPx() }

                                                val cell = row?.cells?.get(colIdx)
                                                val isSelected = state.selectedCell?.first == rowIdx && state.selectedCell.second == colIdx
                                                val isEditingThis = editingCell?.first == rowIdx && editingCell?.second == colIdx

                                                // Get edited value if available
                                                val editKey = "${state.activeSheetIndex}:$rowIdx:$colIdx"
                                                val displayValue = state.editedCells[editKey] ?: cell?.value ?: ""

                                                // Get style
                                                val style = if (cell != null && cell.styleIndex >= 0) {
                                                    content.styles.getOrNull(cell.styleIndex)
                                                } else null

                                                // Search matches highlighting
                                                val matchesInCell = remember(state.searchMatches, state.activeSheetIndex, rowIdx, colIdx) {
                                                    state.searchMatches.filter {
                                                        it.sheetIndex == state.activeSheetIndex &&
                                                        it.rowIndex == rowIdx &&
                                                        it.colIndex == colIdx
                                                    }
                                                }
                                                val isActiveMatchInCell = remember(state.activeMatch, matchesInCell) {
                                                    state.activeMatch >= 0 &&
                                                    state.activeMatch < state.searchMatches.size &&
                                                    matchesInCell.contains(state.searchMatches[state.activeMatch])
                                                }
                                                val isSearchMatched = matchesInCell.isNotEmpty()

                                                // Determine cell background
                                                val bgColor = when {
                                                    isActiveMatchInCell -> Color(0xFFFFC04C) // Active match (orange/dark yellow)
                                                    isSearchMatched -> Color(0xFFFFF2B2) // Other matches (yellow)
                                                    isSelected -> ExcelUi.SelectedCellFill
                                                    style?.backgroundColor != null -> {
                                                        try {
                                                            val hex = style.backgroundColor.let {
                                                                if (it.length == 8 && it.startsWith("FF", ignoreCase = true)) {
                                                                    "#${it.substring(2)}"
                                                                } else if (it.startsWith("#")) it
                                                                else "#$it"
                                                            }
                                                            Color(android.graphics.Color.parseColor(hex))
                                                        } catch (e: Exception) {
                                                            ExcelUi.GridBackground
                                                        }
                                                    }
                                                    else -> ExcelUi.GridBackground
                                                }

                                                // Determine text color
                                                val textColor = if (style?.fontColor != null) {
                                                    try {
                                                        val hex = style.fontColor.let {
                                                            if (it.startsWith("theme:")) return@let null
                                                            if (it.length == 8 && it.startsWith("FF", ignoreCase = true)) {
                                                                "#${it.substring(2)}"
                                                            } else if (it.startsWith("#")) it
                                                            else "#$it"
                                                        }
                                                        if (hex != null) Color(android.graphics.Color.parseColor(hex))
                                                        else ExcelUi.CellText
                                                    } catch (e: Exception) {
                                                        ExcelUi.CellText
                                                    }
                                                } else {
                                                    ExcelUi.CellText
                                                }

                                                // Determine text alignment
                                                val textAlign = when {
                                                    style?.horizontalAlignment == SpreadsheetAlignment.CENTER -> TextAlign.Center
                                                    style?.horizontalAlignment == SpreadsheetAlignment.RIGHT -> TextAlign.End
                                                    style?.horizontalAlignment == SpreadsheetAlignment.LEFT -> TextAlign.Start
                                                    cell?.type == CellType.NUMBER || cell?.type == CellType.FORMULA -> TextAlign.End
                                                    else -> TextAlign.Start
                                                }

                                                val borderColor = if (isSelected) ExcelUi.SelectedCellBorder else ExcelUi.GridLine
                                                val borderWidth = if (isSelected) 2.dp else 0.5.dp

                                                // Determine if cell has custom borders from style
                                                val hasBorders = style?.borders != null && (
                                                    style.borders.top || style.borders.bottom ||
                                                    style.borders.left || style.borders.right
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .offset { IntOffset((startOffsetPx - scrollX).roundToInt(), 0) }
                                                        .width(cellWidth.dp)
                                                        .height(cellHeight.dp)
                                                        .background(bgColor)
                                                        .then(
                                                            if (isSelected) {
                                                                Modifier.border(borderWidth, borderColor)
                                                            } else if (hasBorders && style != null) {
                                                                Modifier.drawBehind {
                                                                    val strokeWidth = 1.dp.toPx()
                                                                    val borderColorVal = Color(0xFF000000)
                                                                    if (style.borders.top) {
                                                                        drawLine(borderColorVal, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
                                                                    }
                                                                    if (style.borders.bottom) {
                                                                        drawLine(borderColorVal, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
                                                                    }
                                                                    if (style.borders.left) {
                                                                        drawLine(borderColorVal, Offset(0f, 0f), Offset(0f, size.height), strokeWidth)
                                                                    }
                                                                    if (style.borders.right) {
                                                                        drawLine(borderColorVal, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth)
                                                                    }
                                                                }
                                                            } else {
                                                                Modifier.border(0.5.dp, ExcelUi.GridLine)
                                                            }
                                                        )
                                                        .pointerInput(listOf(rowIdx, colIdx, state.isEditing)) {
                                                            detectTapGestures(
                                                                onTap = {
                                                                    onSelectCell(rowIdx, colIdx)
                                                                    if (state.isEditing) {
                                                                        // Commit previous edit if any
                                                                        editingCell?.let { (eRow, eCol) ->
                                                                            if (eRow != rowIdx || eCol != colIdx) {
                                                                                onCellEdit(eRow, eCol, editingText)
                                                                            }
                                                                        }
                                                                        editingCell = null
                                                                        val raw = cell?.rawValue ?: displayValue
                                                                        onFormulaBarChange(raw)
                                                                    }
                                                                },
                                                                onDoubleTap = {
                                                                    if (state.isEditing) {
                                                                        editingCell = Pair(rowIdx, colIdx)
                                                                        editingText = displayValue
                                                                        onFormulaBarChange(displayValue)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    contentAlignment = when (textAlign) {
                                                        TextAlign.Center -> Alignment.Center
                                                        TextAlign.End -> Alignment.CenterEnd
                                                        else -> Alignment.CenterStart
                                                    }
                                                ) {
                                                    if (isEditingThis && state.isEditing) {
                                                        val focusRequester = remember { FocusRequester() }
                                                        var textFieldValue by remember {
                                                            mutableStateOf(TextFieldValue(editingText))
                                                        }
                                                        LaunchedEffect(editingText) {
                                                            if (textFieldValue.text != editingText) {
                                                                textFieldValue = textFieldValue.copy(
                                                                    text = editingText,
                                                                    selection = TextRange(editingText.length)
                                                                )
                                                            }
                                                        }
                                                        BasicTextField(
                                                            value = textFieldValue,
                                                            onValueChange = { newValue ->
                                                                textFieldValue = newValue
                                                                editingText = newValue.text
                                                                onFormulaBarChange(newValue.text)
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .focusRequester(focusRequester),
                                                            textStyle = TextStyle(
                                                                fontSize = ((style?.fontSize ?: 11f) * scale).sp,
                                                                fontWeight = if (style?.fontBold == true) FontWeight.Bold else FontWeight.Normal,
                                                                fontStyle = if (style?.fontItalic == true) FontStyle.Italic else FontStyle.Normal,
                                                                color = textColor,
                                                                textAlign = textAlign
                                                            ),
                                                            singleLine = true,
                                                            cursorBrush = SolidColor(ExcelUi.ExcelGreen),
                                                            keyboardOptions = KeyboardOptions(
                                                                imeAction = ImeAction.Done,
                                                                capitalization = KeyboardCapitalization.None
                                                            ),
                                                            keyboardActions = KeyboardActions(
                                                                onDone = {
                                                                    onCellEdit(rowIdx, colIdx, editingText)
                                                                    editingCell = null
                                                                }
                                                            )
                                                        )
                                                        LaunchedEffect(Unit) {
                                                            focusRequester.requestFocus()
                                                        }
                                                    } else {
                                                        Text(
                                                            text = displayValue,
                                                            style = TextStyle(
                                                                fontSize = ((style?.fontSize ?: 11f) * scale).sp,
                                                                fontWeight = if (style?.fontBold == true) FontWeight.Bold else FontWeight.Normal,
                                                                fontStyle = if (style?.fontItalic == true) FontStyle.Italic else FontStyle.Normal,
                                                                color = textColor,
                                                                textAlign = textAlign,
                                                                fontFamily = if (style?.fontFamily?.contains("Courier", true) == true ||
                                                                    style?.fontFamily?.contains("Mono", true) == true) {
                                                                    FontFamily.Monospace
                                                                } else {
                                                                    FontFamily.Default
                                                                }
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Clip
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Sheet tabs (bottom) ─────────────────────────────────────────
            if (content.sheets.size > 1) {
                SheetTabBar(
                    sheets = content.sheets,
                    activeIndex = state.activeSheetIndex,
                    onTabClick = { index ->
                        haptics.performHapticFeedback()
                        // Commit any editing
                        editingCell?.let { (eRow, eCol) ->
                            onCellEdit(eRow, eCol, editingText)
                            editingCell = null
                        }
                        onSwitchSheet(index)
                    }
                )
            }
        }

        // ── Top bar overlay ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = barsVisible,
            enter = slideInVertically(tween(200)) { -it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            if (state.isEditing) {
                // Edit mode toolbar
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
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            // Commit any pending edit
                            editingCell?.let { (eRow, eCol) ->
                                onCellEdit(eRow, eCol, editingText)
                                editingCell = null
                            }
                            onExitEdit()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ExcelUi.ExcelGreenLight)
                        }
                    },
                    actions = {
                        // Add Row
                        TextButton(onClick = {
                            haptics.performHapticFeedback()
                            onAddRow()
                        }) {
                            Text("+ Row", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        // Add Column
                        TextButton(onClick = {
                            haptics.performHapticFeedback()
                            onAddColumn()
                        }) {
                            Text("+ Col", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        // Save
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            editingCell?.let { (eRow, eCol) ->
                                onCellEdit(eRow, eCol, editingText)
                                editingCell = null
                            }
                            onSave()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ExcelUi.ExcelGreenDark.copy(alpha = 0.95f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            } else if (isSearching) {
                // Search toolbar
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search in spreadsheet", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = ExcelUi.ExcelGreenLight
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onSearchClose() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = ExcelUi.ExcelGreenLight)
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
                        containerColor = ExcelUi.ExcelGreenDark.copy(alpha = 0.95f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            } else {
                // Default toolbar
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
                                text = buildString {
                                    append(state.request.extension.uppercase())
                                    if (content.sheets.size > 1) {
                                        append(" · ${content.sheets.size} sheets")
                                    }
                                    append(" · ${activeSheet.rowCount} rows × ${activeSheet.columnCount} cols")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ExcelUi.ExcelGreenLight)
                        }
                    },
                    actions = {
                        IconButton(onClick = { haptics.performHapticFeedback(); onSearchOpen() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ExcelUi.ExcelGreen.copy(alpha = 0.95f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }

        // ── Edit FAB ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.request.canEdit && !state.isEditing && !isSearching && barsVisible,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = if (content.sheets.size > 1) 68.dp else 28.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback()
                    onEdit()
                },
                shape = CircleShape,
                containerColor = ExcelUi.ExcelGreenSubtle,
                contentColor = ExcelUi.ExcelGreen,
                modifier = Modifier
                    .size(58.dp)
                    .neonGlow(color = ExcelUi.ExcelGreen, radius = 12.dp, shape = CircleShape)
                    .border(1.5.dp, ExcelUi.ExcelGreen, CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit spreadsheet")
            }
        }

        // ── Snackbar ────────────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (content.sheets.size > 1) 52.dp else 16.dp)
        )

        // ── Saving overlay ──────────────────────────────────────────────────
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = ExcelUi.ExcelGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Saving...", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ─── Formula Bar ────────────────────────────────────────────────────────────

@Composable
private fun FormulaBar(
    cellRef: String,
    text: String,
    onTextChange: (String) -> Unit,
    onCommit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ExcelUi.FormulaBarBg)
            .border(0.5.dp, ExcelUi.FormulaBarBorder),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cell reference chip
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(ExcelUi.HeaderBackground)
                .border(
                    width = 0.5.dp,
                    color = ExcelUi.FormulaBarBorder,
                    shape = RoundedCornerShape(0.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cellRef.ifEmpty { "-" },
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ExcelUi.HeaderText,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 1
            )
        }

        // fx label
        Text(
            text = "fx",
            style = TextStyle(
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = ExcelUi.CellTextSecondary
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Formula text input
        var textFieldValue by remember {
            mutableStateOf(TextFieldValue(text))
        }
        LaunchedEffect(text) {
            if (textFieldValue.text != text) {
                textFieldValue = textFieldValue.copy(
                    text = text,
                    selection = TextRange(text.length)
                )
            }
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onTextChange(newValue.text)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            textStyle = TextStyle(
                fontSize = 12.sp,
                color = ExcelUi.CellText
            ),
            singleLine = true,
            cursorBrush = SolidColor(ExcelUi.ExcelGreen),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.None
            ),
            keyboardActions = KeyboardActions(onDone = { onCommit() })
        )
    }
}

// ─── Sheet Tab Bar ──────────────────────────────────────────────────────────

@Composable
private fun SheetTabBar(
    sheets: List<SpreadsheetSheet>,
    activeIndex: Int,
    onTabClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ExcelUi.SheetTabBarBg)
            .border(0.5.dp, ExcelUi.HeaderBorder),
        verticalAlignment = Alignment.Bottom
    ) {
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
                .padding(start = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            sheets.forEachIndexed { index, sheet ->
                val isActive = index == activeIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (isActive) ExcelUi.SheetTabActive
                            else Color.Transparent
                        )
                        .then(
                            if (isActive) {
                                Modifier
                                    .border(
                                        width = 0.5.dp,
                                        color = ExcelUi.HeaderBorder,
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                                    .drawBehind {
                                        // Draw green accent line at top of active tab
                                        drawRect(
                                            color = ExcelUi.ExcelGreen,
                                            topLeft = Offset.Zero,
                                            size = Size(size.width, 3.dp.toPx())
                                        )
                                    }
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTabClick(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sheet.name,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) ExcelUi.SheetTabActiveText else ExcelUi.SheetTabText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
