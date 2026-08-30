package com.tibobutton.app.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tibobutton.app.data.ResetApi
import com.tibobutton.app.data.ResetClassifier
import com.tibobutton.app.data.WidgetPrefs
import com.tibobutton.app.notify.NotificationHelper
import com.tibobutton.app.widget.WidgetRenderer
import java.time.Instant

class RefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val api = ResetApi()
        val old = WidgetPrefs.load(applicationContext)
        return try {
            val forecast = api.fetchForecast()
            val history = api.fetchHistory()
            val state = ResetClassifier.build(forecast, history)
            WidgetPrefs.save(applicationContext, state)
            WidgetRenderer.updateAll(applicationContext, refreshing = false)
            NotificationHelper.maybeNotify(applicationContext, old, state)
            Result.success()
        } catch (t: Throwable) {
            val failed = old.copy(
                error = t.message?.take(100) ?: "刷新失败",
                updatedAt = old.updatedAt ?: Instant.now()
            )
            WidgetPrefs.save(applicationContext, failed)
            WidgetRenderer.updateAll(applicationContext, refreshing = false)
            Result.retry()
        }
    }
}
