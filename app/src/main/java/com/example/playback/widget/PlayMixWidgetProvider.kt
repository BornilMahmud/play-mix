package com.example.playback.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.playback.service.PlayMixMediaService

class PlayMixWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidgetInstance(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetId = widgetId,
                title = lastKnownTitle,
                artist = lastKnownArtist,
                isPlaying = lastKnownIsPlaying
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: lastKnownTitle
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: lastKnownArtist
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, lastKnownIsPlaying)
                lastKnownTitle = title
                lastKnownArtist = artist
                lastKnownIsPlaying = isPlaying
                pushUpdateToAllWidgets(context, title, artist, isPlaying)
            }
            ACTION_WIDGET_PLAY_PAUSE -> {
                dispatchMediaKeyToService(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            ACTION_WIDGET_NEXT -> {
                dispatchMediaKeyToService(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            }
            ACTION_WIDGET_PREV -> {
                dispatchMediaKeyToService(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_UPDATE = "com.example.playmix.ACTION_WIDGET_UPDATE"
        const val ACTION_WIDGET_PLAY_PAUSE = "com.example.playmix.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.example.playmix.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.example.playmix.ACTION_WIDGET_PREV"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        private var lastKnownTitle: String = "Play Mix Ready"
        private var lastKnownArtist: String = "Tap to play or control music"
        private var lastKnownIsPlaying: Boolean = false

        fun notifyPlaybackChanged(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean
        ) {
            lastKnownTitle = title
            lastKnownArtist = artist
            lastKnownIsPlaying = isPlaying
            pushUpdateToAllWidgets(context.applicationContext, title, artist, isPlaying)
        }

        private fun pushUpdateToAllWidgets(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean
        ) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, PlayMixWidgetProvider::class.java))
                ids.forEach { id ->
                    updateWidgetInstance(context, manager, id, title, artist, isPlaying)
                }
            }
        }

        private fun updateWidgetInstance(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String,
            artist: String,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_play_mix)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            val immutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPending = PendingIntent.getActivity(context, 10, launchIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, launchPending)
            views.setOnClickPendingIntent(R.id.widget_icon, launchPending)

            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                buildBroadcastPendingIntent(context, ACTION_WIDGET_PREV, 11, flags)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                buildBroadcastPendingIntent(context, ACTION_WIDGET_PLAY_PAUSE, 12, flags)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                buildBroadcastPendingIntent(context, ACTION_WIDGET_NEXT, 13, flags)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildBroadcastPendingIntent(
            context: Context,
            action: String,
            requestCode: Int,
            flags: Int
        ): PendingIntent {
            val intent = Intent(context, PlayMixWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }

        private fun dispatchMediaKeyToService(context: Context, keyCode: Int) {
            runCatching {
                val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    component = ComponentName(context, PlayMixMediaService::class.java)
                    putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(downIntent)
                } else {
                    context.startService(downIntent)
                }
            }
        }
    }
}
