package com.ioristudios.anydoc.util

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * High-performance object pool for Android Bitmaps.
 * Prevents continuous allocations and GC pauses during fast scrolling.
 */
object BitmapPool {
    private val pool = ConcurrentLinkedQueue<Bitmap>()
    private const val MAX_POOL_SIZE = 8

    /**
     * Acquire a mutable bitmap with matching dimensions, or return null if none available.
     */
    fun acquire(width: Int, height: Int): Bitmap? {
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val bmp = iterator.next()
            if (!bmp.isRecycled && bmp.isMutable && bmp.width == width && bmp.height == height) {
                iterator.remove()
                bmp.eraseColor(android.graphics.Color.WHITE)
                return bmp
            }
        }
        return null
    }

    /**
     * Release a bitmap back to the pool for reuse, or recycle it if pool is full.
     */
    fun release(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        if (pool.size < MAX_POOL_SIZE && bitmap.isMutable) {
            pool.offer(bitmap)
        } else {
            bitmap.recycle()
        }
    }

    /**
     * Clear all pooled bitmaps and free memory.
     */
    fun clear() {
        while (pool.isNotEmpty()) {
            pool.poll()?.recycle()
        }
    }
}
