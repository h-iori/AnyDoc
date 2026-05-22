package com.ioristudios.anydoc.ui.screens

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ioristudios.anydoc.model.DocumentContent
import com.ioristudios.anydoc.model.DocumentKind
import com.ioristudios.anydoc.model.DocumentViewerState
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.getAccentForExtension
import com.ioristudios.anydoc.ui.theme.neonGlow
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════════
// FULLSCREEN TEXT / CODE / MARKDOWN VIEWER
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Highlight colours ────────────────────────────────────────────────────────
private val HighlightNormal = Color(0xFFFFE066)
private val HighlightActive = Color(0xFFFF9800)

// ─── Syntax highlighting colours (neon-on-dark) ──────────────────────────────
private object SyntaxColors {
    val Keyword = Color(0xFFC084FC)       // purple-400
    val String = Color(0xFF34D399)        // emerald-400
    val Comment = Color(0xFF6B7280)       // gray-500
    val Number = Color(0xFFFB923C)        // orange-400
    val Operator = Color(0xFF22D3EE)      // cyan-400
    val Type = Color(0xFF818CF8)          // indigo-400
    val Function = Color(0xFF60A5FA)      // blue-400
    val Annotation = Color(0xFFF472B6)    // pink-400
    val Tag = Color(0xFFF43F5E)           // rose-500
    val Attribute = Color(0xFFFBBF24)     // amber-400
    val Property = Color(0xFF38BDF8)      // sky-400
}

// ─── Language-specific keyword sets ──────────────────────────────────────────
private val kotlinJavaKeywords = setOf(
    "abstract", "actual", "annotation", "as", "assert", "break", "by", "catch",
    "class", "companion", "const", "constructor", "continue", "crossinline",
    "data", "delegate", "do", "else", "enum", "expect", "external",
    "false", "field", "final", "finally", "for", "fun", "get", "if", "implements",
    "import", "in", "infix", "init", "inline", "inner", "interface", "internal",
    "is", "it", "lateinit", "lazy", "native", "new", "noinline", "null", "object",
    "open", "operator", "out", "override", "package", "private", "protected",
    "public", "reified", "return", "sealed", "set", "static", "super",
    "suspend", "synchronized", "this", "throw", "throws", "transient", "true",
    "try", "typealias", "typeof", "val", "var", "vararg", "volatile",
    "when", "where", "while", "yield"
)

private val jsKeywords = setOf(
    "async", "await", "break", "case", "catch", "class", "const", "continue",
    "debugger", "default", "delete", "do", "else", "export", "extends",
    "false", "finally", "for", "from", "function", "get", "if", "import",
    "in", "instanceof", "let", "new", "null", "of", "return", "set",
    "static", "super", "switch", "this", "throw", "true", "try", "typeof",
    "undefined", "var", "void", "while", "with", "yield"
)

private val pythonKeywords = setOf(
    "False", "None", "True", "and", "as", "assert", "async", "await", "break",
    "class", "continue", "def", "del", "elif", "else", "except", "finally",
    "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
    "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
)

private val cppKeywords = setOf(
    "alignas", "alignof", "and", "asm", "auto", "bool", "break", "case",
    "catch", "char", "class", "const", "constexpr", "continue", "decltype",
    "default", "delete", "do", "double", "dynamic_cast", "else", "enum",
    "explicit", "export", "extern", "false", "float", "for", "friend", "goto",
    "if", "inline", "int", "long", "mutable", "namespace", "new", "noexcept",
    "nullptr", "operator", "or", "private", "protected", "public", "register",
    "return", "short", "signed", "sizeof", "static", "static_cast", "struct",
    "switch", "template", "this", "throw", "true", "try", "typedef", "typeid",
    "typename", "union", "unsigned", "using", "virtual", "void", "volatile",
    "while", "#include", "#define", "#ifdef", "#ifndef", "#endif", "#pragma"
)

private val shellKeywords = setOf(
    "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "do",
    "done", "in", "function", "select", "until", "return", "exit", "export",
    "local", "readonly", "declare", "typeset", "unset", "shift", "source",
    "echo", "printf", "read", "set", "eval", "exec", "trap"
)

private val sqlKeywords = setOf(
    "select", "from", "where", "insert", "into", "update", "delete", "create",
    "alter", "drop", "table", "index", "view", "join", "inner", "outer",
    "left", "right", "on", "and", "or", "not", "in", "exists", "between",
    "like", "order", "by", "group", "having", "union", "all", "distinct",
    "as", "null", "is", "set", "values", "primary", "key", "foreign",
    "references", "constraint", "default", "check", "unique", "limit", "offset"
)

private fun getKeywordsForExtension(ext: String): Set<String> = when (ext) {
    "kt", "java", "scala", "gradle" -> kotlinJavaKeywords
    "js", "ts", "jsx", "tsx", "vue", "svelte" -> jsKeywords
    "py", "r" -> pythonKeywords
    "c", "cpp", "h", "cs", "rs", "swift", "go", "dart" -> cppKeywords
    "sh", "bat", "ps1" -> shellKeywords
    "sql" -> sqlKeywords
    else -> emptySet()
}

// ─── Tokenize and highlight a line of code ───────────────────────────────────
private fun highlightLine(
    line: String,
    extension: String,
    keywords: Set<String>
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = line.length

        while (i < len) {
            val ch = line[i]

            // Single-line comments
            if (ch == '/' && i + 1 < len && line[i + 1] == '/') {
                withStyle(SpanStyle(color = SyntaxColors.Comment)) {
                    append(line.substring(i))
                }
                return@buildAnnotatedString
            }
            // Shell / Python comments
            if (ch == '#' && extension in setOf("py", "sh", "bat", "r", "yaml", "yml", "toml", "ini", "cfg", "conf", "properties", "makefile", "env")) {
                withStyle(SpanStyle(color = SyntaxColors.Comment)) {
                    append(line.substring(i))
                }
                return@buildAnnotatedString
            }
            // XML/HTML comments start
            if (ch == '<' && i + 3 < len && line.substring(i, i + 4) == "<!--") {
                val end = line.indexOf("-->", i + 4)
                val commentEnd = if (end >= 0) end + 3 else len
                withStyle(SpanStyle(color = SyntaxColors.Comment)) {
                    append(line.substring(i, commentEnd))
                }
                i = commentEnd
                continue
            }
            // Annotations (e.g., @Override)
            if (ch == '@' && i + 1 < len && line[i + 1].isLetter()) {
                val start = i
                i++
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                withStyle(SpanStyle(color = SyntaxColors.Annotation)) {
                    append(line.substring(start, i))
                }
                continue
            }
            // XML/HTML tags
            if (ch == '<' && extension in setOf("xml", "html", "htm", "svg", "vue", "svelte", "jsx", "tsx")) {
                val start = i
                val end = line.indexOf('>', i)
                val tagEnd = if (end >= 0) end + 1 else len
                val tagStr = line.substring(start, tagEnd)
                // Colorize tag name and attributes
                withStyle(SpanStyle(color = SyntaxColors.Tag)) {
                    append(tagStr)
                }
                i = tagEnd
                continue
            }
            // Strings (double or single quotes)
            if (ch == '"' || ch == '\'' || (ch == '`' && extension in setOf("js", "ts", "jsx", "tsx"))) {
                val quote = ch
                val start = i
                i++
                while (i < len && line[i] != quote) {
                    if (line[i] == '\\') i++ // skip escaped char
                    i++
                }
                if (i < len) i++ // consume closing quote
                withStyle(SpanStyle(color = SyntaxColors.String)) {
                    append(line.substring(start, i))
                }
                continue
            }
            // Numbers
            if (ch.isDigit() || (ch == '.' && i + 1 < len && line[i + 1].isDigit())) {
                val start = i
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '.' || line[i] == '_' || line[i] == 'x' || line[i] == 'X')) i++
                withStyle(SpanStyle(color = SyntaxColors.Number)) {
                    append(line.substring(start, i))
                }
                continue
            }
            // Operators
            if (ch in setOf('+', '-', '*', '/', '=', '!', '&', '|', '^', '~', '%', '?', ':')) {
                withStyle(SpanStyle(color = SyntaxColors.Operator)) {
                    append(ch.toString())
                }
                i++
                continue
            }
            // Keywords and identifiers
            if (ch.isLetter() || ch == '_') {
                val start = i
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                val word = line.substring(start, i)
                when {
                    word in keywords -> withStyle(SpanStyle(color = SyntaxColors.Keyword, fontWeight = FontWeight.SemiBold)) {
                        append(word)
                    }
                    word.firstOrNull()?.isUpperCase() == true && extension in setOf("kt", "java", "ts", "cs", "swift", "scala", "dart") ->
                        withStyle(SpanStyle(color = SyntaxColors.Type)) { append(word) }
                    else -> append(word)
                }
                continue
            }
            // JSON keys
            if (ch == '{' || ch == '}' || ch == '[' || ch == ']') {
                withStyle(SpanStyle(color = SyntaxColors.Operator)) {
                    append(ch.toString())
                }
                i++
                continue
            }
            // Default
            append(ch.toString())
            i++
        }
    }
}

// ─── Build highlighted + search-matched annotated string for code ────────────
private fun buildCodeHighlighted(
    text: String,
    extension: String,
    searchQuery: String,
    activeMatchIndex: Int,
    matchesBeforeThis: Int
): AnnotatedString {
    val keywords = getKeywordsForExtension(extension)
    val lines = text.split('\n')

    return buildAnnotatedString {
        lines.forEachIndexed { lineIdx, line ->
            if (lineIdx > 0) append('\n')

            // Line numbers
            val lineNum = "${lineIdx + 1}"
            withStyle(SpanStyle(color = Color(0xFF4A4A5E))) {
                append(lineNum.padStart(5))
                append("  ")
            }

            if (searchQuery.isBlank()) {
                // No search — just syntax highlight
                append(highlightLine(line, extension, keywords))
            } else {
                // Interleave syntax highlighting with search highlights
                val highlighted = highlightLine(line, extension, keywords)
                // Apply the base highlighted string
                append(highlighted)
            }
        }

        // If we have a search query, apply search highlights as an overlay
        if (searchQuery.isNotBlank()) {
            val fullText = this.toString()
            var cursor = 0
            var localMatchIdx = 0
            while (cursor < fullText.length) {
                val found = fullText.indexOf(searchQuery, cursor, ignoreCase = true)
                if (found == -1) break
                val globalMatchIdx = matchesBeforeThis + localMatchIdx
                val bg = if (globalMatchIdx == activeMatchIndex) HighlightActive else HighlightNormal
                addStyle(
                    SpanStyle(background = bg, color = Color.Black),
                    found,
                    found + searchQuery.length
                )
                cursor = found + searchQuery.length
                localMatchIdx++
            }
        }
    }
}

// ─── Build highlighted search-matched annotated string for plain text ────────
private fun buildPlainHighlighted(
    text: String,
    searchQuery: String,
    activeMatchIndex: Int,
    matchesBeforeThis: Int
): AnnotatedString {
    if (searchQuery.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        var localMatchIdx = 0
        while (cursor <= text.length) {
            val found = text.indexOf(searchQuery, cursor, ignoreCase = true)
            if (found == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, found))
            val globalMatchIdx = matchesBeforeThis + localMatchIdx
            val bg = if (globalMatchIdx == activeMatchIndex) HighlightActive else HighlightNormal
            withStyle(SpanStyle(background = bg, color = Color.Black)) {
                append(text.substring(found, found + searchQuery.length))
            }
            cursor = found + searchQuery.length
            localMatchIdx++
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDocumentFullscreenViewer(
    state: DocumentViewerState.Ready,
    isSearching: Boolean,
    onBack: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onSave: () -> Unit,
    onTextChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
    val content = state.content as? DocumentContent.TextContent ?: return
    val isMarkdown = state.request.kind == DocumentKind.Markdown

    // Zoom state: font scale (1.0 = 100%)
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    val minScale = 0.5f
    val maxScale = 3.0f
    val zoomPercent = (fontScale * 100).roundToInt()

    // Markdown: toggle between preview and source editing
    var markdownPreviewMode by remember { mutableStateOf(true) }

    // Cursor / selection tracking for status line
    var cursorPosition by remember { mutableIntStateOf(0) }
    var selectionLength by remember { mutableIntStateOf(0) }

    // Determine if content has been modified
    val isModified = state.isEditing && state.editedText != content.text

    // Compute line count and char count
    val displayText = if (state.isEditing) state.editedText else content.text
    val lineCount = remember(displayText) { displayText.count { it == '\n' } + 1 }
    val charCount = displayText.length

    // Accent colour for the extension
    val accentColor = remember(state.request.extension) {
        com.ioristudios.anydoc.ui.theme.getAccentForExtension(state.request.extension)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (state.request.canEdit && !state.isEditing && !isSearching) {
                FloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback()
                        if (isMarkdown) {
                            markdownPreviewMode = false
                        }
                        onEdit()
                    },
                    shape = CircleShape,
                    containerColor = accentColor.copy(alpha = 0.15f),
                    contentColor = accentColor,
                    modifier = Modifier
                        .size(58.dp)
                        .neonGlow(color = accentColor, radius = 12.dp, shape = CircleShape)
                        .border(1.5.dp, accentColor, CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit file")
                }
            }
        },
        topBar = {
            if (isSearching) {
                // ─── Search TopBar ────────────────────────────────────────
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    "Search in document…",
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color.White
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            onSearchClose()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit search",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (state.searchQuery.isNotEmpty()) {
                            val label = if (state.searchMatches.isEmpty()) "0/0"
                            else "${state.activeMatch + 1}/${state.searchMatches.size}"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                enabled = state.searchMatches.isNotEmpty(),
                                onClick = { haptics.performHapticFeedback(); onPrevMatch() }
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous match",
                                    tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                enabled = state.searchMatches.isNotEmpty(),
                                onClick = { haptics.performHapticFeedback(); onNextMatch() }
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next match",
                                    tint = if (state.searchMatches.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = accentColor.copy(alpha = 0.95f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            } else {
                // ─── Main TopBar ─────────────────────────────────────────
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.request.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = state.request.extension.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Zoom controls
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback()
                                fontScale = (fontScale - 0.1f).coerceAtLeast(minScale)
                            }
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
                        }
                        Text(
                            text = "${zoomPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback()
                                fontScale = (fontScale + 0.1f).coerceAtMost(maxScale)
                            }
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                        }

                        // Search
                        IconButton(onClick = {
                            haptics.performHapticFeedback()
                            onSearchOpen()
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }

                        // Markdown: toggle preview (eye button)
                        if (isMarkdown) {
                            IconButton(onClick = {
                                haptics.performHapticFeedback()
                                markdownPreviewMode = !markdownPreviewMode
                            }) {
                                Icon(
                                    imageVector = if (markdownPreviewMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (markdownPreviewMode) "View Markdown Source" else "View Rendered Markdown",
                                    tint = Color.White
                                )
                            }
                        }

                        // Save and Cancel buttons in editing mode
                        if (state.isEditing) {
                            IconButton(onClick = { haptics.performHapticFeedback(); onExitEdit() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel editing", tint = Color.White)
                            }
                            IconButton(onClick = { haptics.performHapticFeedback(); onSave() }) {
                                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = accentColor.copy(alpha = 0.95f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ─── Editor / Preview Surface ────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val zoom = event.calculateZoom()
                                if (zoom != 1.0f) {
                                    fontScale = (fontScale * zoom).coerceIn(minScale, maxScale)
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                val searchQuery = if (isSearching) state.searchQuery else ""
                val activeMatchIndex = state.activeMatch

                when {
                    // Markdown preview mode
                    isMarkdown && markdownPreviewMode && !state.isEditing -> {
                        MarkdownPreviewSurface(
                            markdownText = content.text,
                            fontScale = fontScale,
                            searchQuery = searchQuery,
                            activeMatchIndex = activeMatchIndex
                        )
                    }
                    // Editing mode (plain text editor for any file type)
                    state.isEditing -> {
                        CodeEditorSurface(
                            text = state.editedText,
                            isEditing = true,
                            isCodeLike = content.isCodeLike || (isMarkdown && !markdownPreviewMode),
                            extension = if (isMarkdown) "md" else state.request.extension,
                            fontScale = fontScale,
                            searchQuery = searchQuery,
                            activeMatchIndex = activeMatchIndex,
                            onTextChange = onTextChange,
                            onCursorChange = { pos, sel ->
                                cursorPosition = pos
                                selectionLength = sel
                            }
                        )
                    }
                    // Read-only code/text view
                    else -> {
                        CodeEditorSurface(
                            text = content.text,
                            isEditing = false,
                            isCodeLike = content.isCodeLike,
                            extension = state.request.extension,
                            fontScale = fontScale,
                            searchQuery = searchQuery,
                            activeMatchIndex = activeMatchIndex,
                            onTextChange = {},
                            onCursorChange = { pos, sel ->
                                cursorPosition = pos
                                selectionLength = sel
                            }
                        )
                    }
                }
            }

            // ─── Status Line ─────────────────────────────────────────────────
            StatusLine(
                encoding = "UTF-8",
                lineCount = lineCount,
                charCount = charCount,
                cursorPosition = cursorPosition,
                selectionLength = selectionLength,
                zoomPercent = zoomPercent,
                isModified = isModified,
                extension = state.request.extension,
                isMarkdownPreview = isMarkdown && markdownPreviewMode && !state.isEditing,
                accentColor = accentColor
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CODE / TEXT EDITOR SURFACE (Read-only + Editing)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CodeEditorSurface(
    text: String,
    isEditing: Boolean,
    isCodeLike: Boolean,
    extension: String,
    fontScale: Float,
    searchQuery: String,
    activeMatchIndex: Int,
    onTextChange: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit
) {
    val baseFontSize = if (isCodeLike) 13.sp else 15.sp
    val scaledFontSize = baseFontSize * fontScale
    val fontFamily = if (isCodeLike) FontFamily.Monospace else FontFamily.Default

    val scrollStateV = rememberScrollState()
    val scrollStateH = rememberScrollState()

    if (isEditing) {
        // Editable BasicTextField
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
                onCursorChange(newValue.selection.start, newValue.selection.length)
            },
            textStyle = TextStyle(
                fontFamily = fontFamily,
                fontSize = scaledFontSize,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = scaledFontSize * 1.6f
            ),
            cursorBrush = SolidColor(AppColors.BrandStrong),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D18))
                .verticalScroll(scrollStateV)
                .horizontalScroll(scrollStateH)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )
    } else {
        // Read-only highlighted view
        val annotated = remember(text, extension, searchQuery, activeMatchIndex) {
            if (isCodeLike) {
                buildCodeHighlighted(text, extension, searchQuery, activeMatchIndex, 0)
            } else {
                buildPlainHighlighted(text, searchQuery, activeMatchIndex, 0)
            }
        }

        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0D18))
                    .verticalScroll(scrollStateV)
                    .horizontalScroll(scrollStateH)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = annotated,
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontSize = scaledFontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = scaledFontSize * 1.6f
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARKDOWN PREVIEW SURFACE (using Markwon)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MarkdownPreviewSurface(
    markdownText: String,
    fontScale: Float,
    searchQuery: String,
    activeMatchIndex: Int
) {
    val density = LocalDensity.current
    val baseFontSizeSp = 16f * fontScale
    val baseFontSizePx = with(density) { (16.sp * fontScale).toPx() }

    val bgColor = Color(0xFF0D0D18)
    val textColor = Color(0xFFEEEEFF)
    val linkColor = Color(0xFFC084FC)

    AndroidView(
        factory = { ctx ->
            val markwon = buildMarkwon(ctx)
            ScrollView(ctx).apply {
                setBackgroundColor(bgColor.toArgb())
                clipToPadding = false
                isVerticalScrollBarEnabled = true
                isSmoothScrollingEnabled = true
                overScrollMode = ScrollView.OVER_SCROLL_ALWAYS

                val tv = TextView(ctx).apply {
                    setTextColor(textColor.toArgb())
                    textSize = baseFontSizeSp
                    setLineSpacing(baseFontSizePx * 0.45f, 1.0f)
                    setPadding(
                        dpToPx(ctx, 20f), dpToPx(ctx, 16f),
                        dpToPx(ctx, 20f), dpToPx(ctx, 80f)
                    )
                    setLinkTextColor(linkColor.toArgb())
                    highlightColor = Color(0x44C084FC).toArgb()
                    setTextIsSelectable(true)
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                }
                markwon.setMarkdown(tv, markdownText)

                // Apply search highlights
                if (searchQuery.isNotBlank()) {
                    applySearchHighlightsToTextView(tv, searchQuery, activeMatchIndex)
                }

                addView(tv, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                tag = tv
            }
        },
        update = { scrollView ->
            val tv = scrollView.tag as? TextView ?: return@AndroidView
            val markwon = buildMarkwon(scrollView.context)
            tv.textSize = baseFontSizeSp
            tv.setLineSpacing(baseFontSizePx * 0.45f, 1.0f)
            markwon.setMarkdown(tv, markdownText)

            // Re-apply search highlights
            if (searchQuery.isNotBlank()) {
                applySearchHighlightsToTextView(tv, searchQuery, activeMatchIndex)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun buildMarkwon(context: Context): Markwon {
    return Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TaskListPlugin.create(context))
        .build()
}

private fun applySearchHighlightsToTextView(
    tv: TextView,
    searchQuery: String,
    activeMatchIndex: Int
) {
    val spannable = tv.text as? Spannable ?: return
    val text = spannable.toString()
    var cursor = 0
    var localMatchIdx = 0

    while (cursor < text.length) {
        val found = text.indexOf(searchQuery, cursor, ignoreCase = true)
        if (found == -1) break
        val bg = if (localMatchIdx == activeMatchIndex)
            HighlightActive.toArgb()
        else
            HighlightNormal.toArgb()
        spannable.setSpan(
            BackgroundColorSpan(bg),
            found,
            found + searchQuery.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (localMatchIdx == activeMatchIndex) {
            spannable.setSpan(
                ForegroundColorSpan(android.graphics.Color.BLACK),
                found,
                found + searchQuery.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        cursor = found + searchQuery.length
        localMatchIdx++
    }
}

private fun dpToPx(context: Context, dp: Float): Int {
    return (dp * context.resources.displayMetrics.density).roundToInt()
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS LINE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatusLine(
    encoding: String,
    lineCount: Int,
    charCount: Int,
    cursorPosition: Int,
    selectionLength: Int,
    zoomPercent: Int,
    isModified: Boolean,
    extension: String,
    isMarkdownPreview: Boolean,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F1A))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Encoding
            StatusChip(encoding)

            // Line / Char count
            StatusChip("$lineCount L · $charCount C")

            // Mode indicator
            if (isMarkdownPreview) {
                StatusChip("Preview", color = accentColor)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cursor / selection
            if (selectionLength > 0) {
                StatusChip("Sel: $selectionLength")
            }

            // Zoom
            StatusChip("$zoomPercent%")

            // Modified state
            if (isModified) {
                Text(
                    text = "• Modified",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFB923C),
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF34D399)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color = Color(0xFF7A7A8E)) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
