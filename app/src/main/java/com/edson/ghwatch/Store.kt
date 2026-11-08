package com.edson.ghwatch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Repo(
    val slug: String,
    val lastTag: String? = null,
    val lastUrl: String? = null,
    val lastChecked: Long = 0L
)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("ghwatch", Context.MODE_PRIVATE)

    @Synchronized
    fun repos(c: Context): List<Repo> {
        val arr = JSONArray(prefs(c).getString("repos", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Repo(
                o.getString("slug"),
                o.optString("tag").ifEmpty { null },
                o.optString("url").ifEmpty { null },
                o.optLong("checked")
            )
        }
    }

    @Synchronized
    fun save(c: Context, list: List<Repo>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("slug", it.slug)
                put("tag", it.lastTag ?: "")
                put("url", it.lastUrl ?: "")
                put("checked", it.lastChecked)
            })
        }
        prefs(c).edit().putString("repos", arr.toString()).apply()
    }

    fun intervalMinutes(c: Context): Int = prefs(c).getInt("interval", 60)
    fun setInterval(c: Context, m: Int) = prefs(c).edit().putInt("interval", m).apply()

    fun token(c: Context): String = prefs(c).getString("token", "") ?: ""
    fun setToken(c: Context, t: String) = prefs(c).edit().putString("token", t.trim()).apply()
}
