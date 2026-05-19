package com.ioristudios.anydoc.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recent_files")

object RecentFilesManager {
    private val RECENT_FILES_KEY = stringPreferencesKey("recent_files_json")
    private const val MAX_RECENT_FILES = 20

    fun getRecentFiles(context: Context): Flow<List<String>> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[RECENT_FILES_KEY] ?: "[]"
            parseJsonArray(jsonString)
        }
    }

    suspend fun addRecentFile(context: Context, path: String) {
        context.dataStore.edit { preferences ->
            val jsonString = preferences[RECENT_FILES_KEY] ?: "[]"
            val currentList = parseJsonArray(jsonString).toMutableList()
            
            // Remove if already exists to move it to the top
            currentList.remove(path)
            
            // Add to the top
            currentList.add(0, path)
            
            // Keep only max items
            val trimmedList = currentList.take(MAX_RECENT_FILES)
            
            preferences[RECENT_FILES_KEY] = toJsonArray(trimmedList)
        }
    }

    suspend fun removeRecentFile(context: Context, path: String) {
        context.dataStore.edit { preferences ->
            val jsonString = preferences[RECENT_FILES_KEY] ?: "[]"
            val currentList = parseJsonArray(jsonString).toMutableList()
            
            if (currentList.remove(path)) {
                preferences[RECENT_FILES_KEY] = toJsonArray(currentList)
            }
        }
    }

    private fun parseJsonArray(jsonString: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun toJsonArray(list: List<String>): String {
        val jsonArray = JSONArray()
        for (item in list) {
            jsonArray.put(item)
        }
        return jsonArray.toString()
    }
}
