package com.ioristudios.anydoc.model

data class FileItem(
    val id: String,
    val name: String,
    val extension: String,
    val size: String,
    val metadata: String,
    val path: String
)

val DummyRecentFiles = listOf(
    FileItem("1", "Q4_Financial_Report_Final.pdf", "pdf", "2.4 MB", "Viewed 2 hours ago", ""),
    FileItem("2", "User_Data_Export_2023.xlsx", "xlsx", "14 MB", "Viewed yesterday", ""),
    FileItem("3", "main_app_logic.js", "js", "45 KB", "Viewed 3 days ago", "")
)

val DummySearchFiles = listOf(
    FileItem("1", "Project_Nexus_Timeline_v2.xlsx", "xlsx", "845 KB", "Yesterday, 14:30", ""),
    FileItem("2", "Client_Onboarding_Brief.docx", "docx", "1.2 MB", "Oct 12, 2023", ""),
    FileItem("3", "Archive_2022_Backups.folder", "folder", "42 Items", "Folder", ""),
    FileItem("4", "Release_Notes_Q3_2025.md", "md", "18 KB", "2 days ago", "")
)
