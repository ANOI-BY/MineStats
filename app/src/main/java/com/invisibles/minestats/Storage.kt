package com.invisibles.minestats

import android.content.Context

class Storage(private val context: Context) {

    companion object{
        const val emptyValue = "null"
    }

    private val preferences = context.getSharedPreferences("API_DATA", Context.MODE_PRIVATE)

    fun getValue(key: String, default: String = "null"): String {
        val el = preferences.getString(key, default)
        return el ?: default
    }

    fun writeValue(key: String, value: String){
        if (key.isNotEmpty() && value.isNotEmpty()){
            with(preferences.edit()){
                putString(key, value)
                apply()
            }
        }
    }
}