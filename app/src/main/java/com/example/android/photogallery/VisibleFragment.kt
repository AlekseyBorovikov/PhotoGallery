package com.example.android.photogallery

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment

private const val TAG = "VisibleFragment"

// Переопределение фрагмента для запуска рсивера, который будет перехватывать уведомления
// если приложение на переднем плане
abstract class VisibleFragment : Fragment() {

    // Receiver будет перехватывать сигнал об отправке уведомления и если приложение не на переднем
    // плане, то уведомление будет отправлено в NotificationReceiver.
    // Чтоб данный ресивер первый обработал сигнал в NotificationReceiver установлен приоритет -999
    // приоритет -1000 и ниже зарезервированны системой
    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Если мы получаем это, то видимы, поэтому отмените оповещения
            Log.i(TAG, "canceling notification")
            resultCode = Activity.RESULT_CANCELED
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(CheckPhotoWorker.ACTION_SHOW_NOTIFICATION)
        requireActivity().registerReceiver(
            onShowNotification, filter, CheckPhotoWorker.PERM_PRIVATE, null
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(onShowNotification)
    }
}