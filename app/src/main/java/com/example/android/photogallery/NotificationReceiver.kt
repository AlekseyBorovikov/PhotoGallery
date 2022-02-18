package com.example.android.photogallery

import android.app.Activity
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

private const val TAG = "NotificationReceiver"

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "received result: $resultCode")
        if (resultCode != Activity.RESULT_OK) {
            // Активность переднего плана отменила возврат трансляции.
            Log.i(TAG, "received result: $resultCode")
        }
        else {
            val requestCode = intent.getIntExtra(CheckPhotoWorker.REQUEST_CODE, 0)
            val notification: Notification? = intent.getParcelableExtra(CheckPhotoWorker.NOTIFICATION)
            val notificationManager = NotificationManagerCompat.from(context)
            if (notification != null) {
                notificationManager.notify(requestCode, notification)
            }
        }
    }

}