package com.ioristudios.anydoc.model

data class BrowseItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String,
    val size: String,
    val lastModified: String,
    val childCount: Int = 0  // for folders: number of supported items inside
)
