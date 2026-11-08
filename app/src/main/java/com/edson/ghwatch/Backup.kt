package com.edson.ghwatch

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object Backup {

    /** Grava a lista de repositórios e o intervalo no arquivo escolhido (o token não é incluído). */
    fun export(c: Context, uri: Uri) {
        val arr = JSONArray()
        Store.repos(c).forEach {
            arr.put(JSONObject().apply {
                put("slug", it.slug)
                put("tag", it.lastTag ?: "")
                put("url", it.lastUrl ?: "")
            })
        }
        val root = JSONObject().apply {
            put("version", 1)
            put("interval", Store.intervalMinutes(c))
            put("repos", arr)
        }
        val out = c.contentResolver.openOutputStream(uri, "wt") ?: error("sem acesso ao arquivo")
        out.use { it.write(root.toString(2).toByteArray()) }
    }

    /** Lê o arquivo de backup e adiciona os repositórios que ainda não existem. Retorna quantos foram adicionados. */
    fun import(c: Context, uri: Uri): Int {
        val input = c.contentResolver.openInputStream(uri) ?: error("sem acesso ao arquivo")
        val text = input.bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val arr = root.getJSONArray("repos")

        val current = Store.repos(c).toMutableList()
        var added = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val slug = o.getString("slug")
            if (current.none { it.slug.equals(slug, ignoreCase = true) }) {
                current.add(
                    Repo(
                        slug,
                        o.optString("tag").ifEmpty { null },
                        o.optString("url").ifEmpty { null }
                    )
                )
                added++
            }
        }
        Store.save(c, current)

        val interval = root.optInt("interval", 0)
        if (interval in listOf(15, 30, 60, 120, 180, 360)) {
            Store.setInterval(c, interval)
            Checker.schedule(c, interval)
        }
        return added
    }
}
