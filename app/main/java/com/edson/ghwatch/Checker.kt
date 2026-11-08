package com.edson.ghwatch

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object Checker {

    /** Consulta todos os repositórios. Na 1ª vez só memoriza a versão (não notifica). */
    suspend fun run(c: Context) = withContext(Dispatchers.IO) {
        val list = Store.repos(c)
        val token = Store.token(c)
        val updated = list.map { r ->
            val rel = GitHub.latest(r.slug, token) ?: return@map r
            if (r.lastTag != null && r.lastTag != rel.tag) Notifier.notifyUpdate(c, r, rel)
            r.copy(lastTag = rel.tag, lastUrl = rel.url, lastChecked = System.currentTimeMillis())
        }
        // Mescla com o estado atual para não sobrescrever mudanças feitas na UI durante a checagem
        val current = Store.repos(c)
        Store.save(c, current.map { cur -> updated.firstOrNull { it.slug == cur.slug } ?: cur })
    }

    /** Agenda a checagem periódica (mínimo do Android: 15 min). */
    fun schedule(c: Context, minutes: Int) {
        val req = PeriodicWorkRequestBuilder<UpdateWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(c)
            .enqueueUniquePeriodicWork("gh-check", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Checker.run(applicationContext)
        return Result.success()
    }
}

