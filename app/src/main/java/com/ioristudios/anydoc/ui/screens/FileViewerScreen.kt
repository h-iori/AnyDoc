package com.ioristudios.anydoc.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioristudios.anydoc.model.DocumentContent
import com.ioristudios.anydoc.model.DocumentViewerState
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import com.ioristudios.anydoc.viewmodel.DocumentViewerViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    onBack: () -> Unit,
    viewModel: DocumentViewerViewModel = viewModel()
) {
    val spacing = rememberAppSpacing()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.uiState.collectAsState()
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val ready = state as? DocumentViewerState.Ready
            LargeTopAppBar(
                title = {
                    Column {
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
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (ready?.isEditing == true) {
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            viewModel.exitEditMode()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel editing")
                        }
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            viewModel.save()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback()
                        coroutineScope.launch { snackbarHostState.showSnackbar("Use the search field below to find text in this document.") }
                    }) {
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
        },
        floatingActionButton = {
            val ready = state as? DocumentViewerState.Ready
            if (ready != null && !ready.isEditing) {
                FloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback()
                        viewModel.enterEditMode()
                    },
                    containerColor = if (ready.request.canEdit) AppColors.BrandStrong else AppColors.SurfaceHighest,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.neonGlow(
                        color = AppColors.Brand.copy(alpha = 0.4f),
                        radius = spacing.itemGap
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = if (ready.request.canEdit) "Edit" else "Read only")
                }
            }
        }
    ) { paddingValues ->
        when (val current = state) {
            is DocumentViewerState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.BrandStrong)
                }
            }
            is DocumentViewerState.Error -> {
                ErrorContent(current, Modifier.padding(paddingValues))
            }
            is DocumentViewerState.Ready -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
                ) {
                    item {
                        SearchPanel(current, viewModel::updateSearch, viewModel::nextMatch)
                    }
                    item {
                        when (val content = current.content) {
                            is DocumentContent.PdfContent -> PdfDocumentView(content.path)
                            is DocumentContent.TextContent -> TextDocumentView(current, viewModel::updateEditedText)
                            is DocumentContent.CsvContent -> GridDocumentView(
                                state = current,
                                onCellChange = viewModel::updateCell,
                                onAddRow = viewModel::addRow,
                                onAddColumn = viewModel::addColumn
                            )
                            is DocumentContent.OfficeTextContent -> OfficeTextDocumentView(current, viewModel::updateEditedText)
                            is DocumentContent.UnsupportedContent -> UnsupportedDocumentView(content.message)
                        }
                    }
                    if (current.isSaving) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

@Composable
private fun SearchPanel(
    state: DocumentViewerState.Ready,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search inside document") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            IconButton(
                enabled = state.searchMatches.isNotEmpty(),
                onClick = onNext
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next match")
            }
        }
        if (state.searchQuery.isNotBlank()) {
            val label = if (state.searchMatches.isEmpty()) {
                "No matches"
            } else {
                "${state.activeMatch + 1} of ${state.searchMatches.size}: ${state.searchMatches[state.activeMatch].preview}"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextDocumentView(
    state: DocumentViewerState.Ready,
    onTextChange: (String) -> Unit
) {
    if (state.isEditing) {
        OutlinedTextField(
            value = state.editedText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 520.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if ((state.content as? DocumentContent.TextContent)?.isCodeLike == true) {
                    FontFamily.Monospace
                } else {
                    FontFamily.Default
                },
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    } else {
        val content = state.content as DocumentContent.TextContent
        ReadOnlyTextCard(
            text = content.text,
            monospace = content.isCodeLike
        )
    }
}

@Composable
private fun OfficeTextDocumentView(
    state: DocumentViewerState.Ready,
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
                Text(
                    text = "Read-only slide text preview",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.BrandStrong
                )
            }
            sections.ifEmpty { listOf("No extractable text found.") }.forEachIndexed { index, section ->
                ReadOnlyTextCard(
                    text = if (state.request.extension in listOf("ppt", "pptx")) "Slide ${index + 1}\n\n$section" else section,
                    monospace = false
                )
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
        Column(
            modifier = Modifier.horizontalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                                modifier = Modifier
                                    .width(width)
                                    .background(AppColors.Surface, RoundedCornerShape(6.dp))
                                    .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(10.dp),
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
private fun PdfDocumentView(path: String) {
    var bitmaps by remember(path) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var error by remember(path) { mutableStateOf<String?>(null) }

    LaunchedEffect(path) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        val rendered = mutableListOf<Bitmap>()
                        for (index in 0 until renderer.pageCount) {
                            renderer.openPage(index).use { page ->
                                val maxWidth = 1600
                                val scale = (maxWidth.toFloat() / page.width.toFloat()).coerceIn(1f, 2f)
                                val bitmap = Bitmap.createBitmap(
                                    (page.width * scale).toInt(),
                                    (page.height * scale).toInt(),
                                    Bitmap.Config.ARGB_8888
                                )
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                rendered += bitmap
                            }
                        }
                        rendered
                    }
                }
            }
        }
        result.onSuccess { pages ->
            bitmaps = pages
        }.onFailure {
            error = it.localizedMessage ?: "Could not render PDF."
        }
    }

    when {
        error != null -> UnsupportedDocumentView(error ?: "Could not render PDF.")
        bitmaps.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.BrandStrong)
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                bitmaps.forEach { bitmap ->
                    ZoomableImage(bitmap = bitmap)
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(bitmap: Bitmap) {
    var scale by remember { mutableFloatStateOf(1f) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "PDF page",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                }
            },
        contentScale = ContentScale.FillWidth
    )
}

@Composable
private fun ReadOnlyTextCard(text: String, monospace: Boolean) {
    SelectionContainer {
        Text(
            text = text.ifBlank { "No previewable text found." },
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
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Preview unavailable", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorContent(
    state: DocumentViewerState.Error,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.displayName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
