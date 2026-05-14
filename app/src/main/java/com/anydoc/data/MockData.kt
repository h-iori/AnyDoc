package com.anydoc.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.InsertChartOutlined
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.anydoc.ui.theme.ErrorNeon
import com.anydoc.ui.theme.NeonPurple
import com.anydoc.ui.theme.NeonPurpleLight
import com.anydoc.ui.theme.SuccessNeon

enum class FileType(val displayName: String, val iconRef: String) {
    ALL("All Files", "description"),
    PDF("PDF", "picture_as_pdf"),
    WORD("Word", "subject"),
    EXCEL("Excel", "insert_chart_outlined"),
    PPT("PPT", "slideshow"),
    TXT("TXT", "text_snippet"),
    CODE("Code", "code")
}

data class FileItem(
    val name: String,
    val extension: String,
    val size: String,
    val fileType: FileType
)

data class FileIconMeta(
    val icon: ImageVector,
    val accent: Color
)

data class FileTypeChip(
    val fileType: FileType,
    val label: String,
    val count: Int,
    val accent: Color
)

object MockData {
    val iconMetadata: Map<FileType, FileIconMeta> = mapOf(
        FileType.ALL to FileIconMeta(Icons.Outlined.Description, NeonPurple),
        FileType.PDF to FileIconMeta(Icons.Outlined.PictureAsPdf, ErrorNeon),
        FileType.WORD to FileIconMeta(Icons.AutoMirrored.Outlined.Subject, NeonPurpleLight),
        FileType.EXCEL to FileIconMeta(Icons.Outlined.InsertChartOutlined, SuccessNeon),
        FileType.PPT to FileIconMeta(Icons.Outlined.Slideshow, NeonPurple),
        FileType.TXT to FileIconMeta(Icons.AutoMirrored.Outlined.TextSnippet, NeonPurpleLight),
        FileType.CODE to FileIconMeta(Icons.Outlined.Code, SuccessNeon)
    )

    val recentFiles: List<FileItem> = listOf(
        FileItem("Q2_Audit_Report.pdf", "pdf", "2.4 MB", FileType.PDF),
        FileItem("Client_Onboarding.docx", "docx", "844 KB", FileType.WORD),
        FileItem("Revenue_Forecast.xlsx", "xlsx", "1.1 MB", FileType.EXCEL),
        FileItem("Roadmap_Review.pptx", "pptx", "6.8 MB", FileType.PPT),
        FileItem("meeting_notes.txt", "txt", "48 KB", FileType.TXT),
        FileItem("sync_worker.kt", "kt", "32 KB", FileType.CODE),
        FileItem("Vendor_Contract.pdf", "pdf", "3.0 MB", FileType.PDF)
    )

    val filesByType: Map<FileType, List<FileItem>> = mapOf(
        FileType.ALL to recentFiles,
        FileType.PDF to listOf(
            FileItem("Q2_Audit_Report.pdf", "pdf", "2.4 MB", FileType.PDF),
            FileItem("Vendor_Contract.pdf", "pdf", "3.0 MB", FileType.PDF),
            FileItem("Expense_Policy.pdf", "pdf", "1.7 MB", FileType.PDF),
            FileItem("Claims_Packet.pdf", "pdf", "4.1 MB", FileType.PDF),
            FileItem("Travel_Summary.pdf", "pdf", "956 KB", FileType.PDF)
        ),
        FileType.WORD to listOf(
            FileItem("Client_Onboarding.docx", "docx", "844 KB", FileType.WORD),
            FileItem("Project_Brief.docx", "docx", "522 KB", FileType.WORD),
            FileItem("Retention_Playbook.docx", "docx", "1.3 MB", FileType.WORD),
            FileItem("Release_Notes.docx", "docx", "398 KB", FileType.WORD),
            FileItem("Interview_Summary.docx", "docx", "677 KB", FileType.WORD)
        ),
        FileType.EXCEL to listOf(
            FileItem("Revenue_Forecast.xlsx", "xlsx", "1.1 MB", FileType.EXCEL),
            FileItem("Ops_Capacity.xlsx", "xlsx", "762 KB", FileType.EXCEL),
            FileItem("Inventory_Count.xlsx", "xlsx", "2.2 MB", FileType.EXCEL),
            FileItem("Hiring_Plan.xlsx", "xlsx", "604 KB", FileType.EXCEL),
            FileItem("Q3_Targets.xlsx", "xlsx", "912 KB", FileType.EXCEL)
        ),
        FileType.PPT to listOf(
            FileItem("Roadmap_Review.pptx", "pptx", "6.8 MB", FileType.PPT),
            FileItem("Board_Update.pptx", "pptx", "5.7 MB", FileType.PPT),
            FileItem("Launch_Briefing.pptx", "pptx", "4.3 MB", FileType.PPT),
            FileItem("Training_Deck.pptx", "pptx", "8.2 MB", FileType.PPT),
            FileItem("Field_Demo.pptx", "pptx", "3.9 MB", FileType.PPT)
        ),
        FileType.TXT to listOf(
            FileItem("meeting_notes.txt", "txt", "48 KB", FileType.TXT),
            FileItem("incident_log.txt", "txt", "92 KB", FileType.TXT),
            FileItem("migration_plan.txt", "txt", "66 KB", FileType.TXT),
            FileItem("readme.txt", "txt", "12 KB", FileType.TXT),
            FileItem("handoff_notes.txt", "txt", "28 KB", FileType.TXT)
        ),
        FileType.CODE to listOf(
            FileItem("sync_worker.kt", "kt", "32 KB", FileType.CODE),
            FileItem("parser.py", "py", "18 KB", FileType.CODE),
            FileItem("DocumentCard.tsx", "tsx", "14 KB", FileType.CODE),
            FileItem("search_indexer.go", "go", "27 KB", FileType.CODE),
            FileItem("viewer_state.swift", "swift", "21 KB", FileType.CODE)
        )
    )

    val typeChips: List<FileTypeChip> = FileType.entries.map { type ->
        FileTypeChip(
            fileType = type,
            label = type.displayName,
            count = filesByType[type]?.size ?: 0,
            accent = iconMetadata[type]?.accent ?: NeonPurple
        )
    }

    val allFiles: List<FileItem> = filesByType.values.flatten().distinctBy { it.name }
}
