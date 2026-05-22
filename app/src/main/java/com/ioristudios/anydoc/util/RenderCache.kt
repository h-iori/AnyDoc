package com.ioristudios.anydoc.util

import android.graphics.Bitmap
import android.util.LruCache

object RenderCache {
    private val maxMemKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemKb / 8 // Capped at 1/8th of available heap

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.byteCount / 1024
    }

    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.evictAll()
    }
}
