package com.edson.ghwatch

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Release(val tag: String, val url: String)

object GitHub {

    /** Aceita "owner/repo" ou qualquer URL do github.com. */
    fun parseSlug(input: String): String? {
        var s = input.trim()
            .removePrefix("https://").removePrefix("http://")
            .removePrefix("www.").removePrefix("github.com/")
        val parts = s.split("/").filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val owner = parts[0]
        val repo = parts[1].removeSuffix(".git")
        val ok = Regex("^[A-Za-z0-9_.-]+$")
        if (!ok.matches(owner) || !ok.matches(repo)) return null
        return "$owner/$repo"
    }

    /** Última release; se não houver, usa a tag mais recente. Null em caso de erro. */
    fun latest(slug: String, token: String = ""): Release? {
        val (code, body) = get("https://api.github.com/repos/$slug/releases/latest", token)
        if (code == 200 && body != null) {
            val j = JSONObject(body)
            val tag = j.optString("tag_name")
            if (tag.isNotEmpty()) return Release(tag, j.optString("html_url"))
        }
        if (code == 404) {
            val (c2, b2) = get("https://api.github.com/repos/$slug/tags?per_page=1", token)
            if (c2 == 200 && b2 != null) {
                val arr = JSONArray(b2)
                if (arr.length() > 0) {
                    val tag = arr.getJSONObject(0).getString("name")
                    return Release(tag, "https://github.com/$slug/releases/tag/$tag")
                }
            }
        }
        return null
    }

    private fun get(url: String, token: String): Pair<Int, String?> = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "GitHubWatch-Android")
        if (token.isNotBlank()) c.setRequestProperty("Authorization", "Bearer $token")
        val code = c.responseCode
        val body = if (code == 200) c.inputStream.bufferedReader().use { it.readText() } else null
        c.disconnect()
        code to body
    } catch (e: Exception) {
        -1 to null
    }
}
