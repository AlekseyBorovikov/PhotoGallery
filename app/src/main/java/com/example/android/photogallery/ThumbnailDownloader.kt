package com.example.android.photogallery

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ThumbnailDownloader"
private const val MESSAGE_DOWNLOAD = 0

class ThumbnailDownloader<in T>(
    private val responseHandler: Handler,
    private val onThumbnailDownload: (T, Bitmap) -> Unit,
) : HandlerThread(TAG) {

    private var hasQuit = false
    private lateinit var requestHandler: Handler
    // безопасная для потоков хэш таблица
    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrFetchr = FlickrFetchr()

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    val fragmentLifecycleObserver: LifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                Log.i(TAG, "Starting background thread")
                start()
                // Получение looper - способ убедиться в готовности потока
                // помогает избежать состояния гонки
                looper
            }

            override fun onDestroy(owner: LifecycleOwner) {
                Log.i(TAG, "Destroying background thread")
                quit()
            }
        }

    val viewLifecycleObserver: DefaultLifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                Log.i(TAG, "Clearing all requests from queue")
                requestHandler.removeMessages(MESSAGE_DOWNLOAD)
                requestMap.clear()
            }
        }

    fun queueThumbnail(target: T, url: String) {
        Log.i(TAG, "Got a URL: $url")
        // В данном случае [requestMap] используется для сопосотавления ViewHolder и url картинки
        // такой подход обеспечит заполнение каждого холдера нужной картинкой,
        // при этом не занимая много места, т.к. холдер не создаются каждый раз а переиспользуются
        requestMap[target] = url
        // Записывает message в handler и присвает этот handler как обработчик
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()
    }

    // Вызывается при первом создании Looper
    // Поэтому подходит для инициализации Handler
    @Suppress("UNCHECKED_CAST", "NULLABLE_TYPE_PARAMETER_AGAINST_NOT_NULL_TYPE_PARAMETER")
    override fun onLooperPrepared() {
        requestHandler = object : Handler(looper) {
            // Вызывается, когда извлекается message, готовый к обработке
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    val target = msg.obj as T
                    Log.i(TAG, "Got a request for URL: ${requestMap[target]}")
                    handleRequest(target)
                }
            }
        }
        super.onLooperPrepared()
    }

    @Suppress("NULLABLE_TYPE_PARAMETER_AGAINST_NOT_NULL_TYPE_PARAMETER")
    private fun handleRequest(target: T) {
        val url = requestMap[target] ?: return
        val bitmap = flickrFetchr.fetchPhoto(url) ?: return

        // Связывает конкретное сообщение (здесь, это пустое сообщение с callback)
        // и выполняет его callback
        responseHandler.post {
            // Проверка нужна, т.к. пока будет грузится картинка viewholder может быть переработан
            // для другой url
            if (requestMap[target] != url || hasQuit) return@post
            requestMap.remove(target)
            onThumbnailDownload(target, bitmap)
        }
    }
}