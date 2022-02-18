package com.example.android.photogallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.photogallery.api.FlickrApi
import com.example.android.photogallery.api.FlickrResponse
import com.example.android.photogallery.api.PhotoInterceptor
import com.example.android.photogallery.models.GalleryItem
import com.example.android.photogallery.models.PhotoResponse
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://api.flickr.com"
private const val TAG = "FlickrFetch"

class FlickrFetchr {

    private val flickrApi: FlickrApi

    init {
        val client = OkHttpClient.Builder().addInterceptor(PhotoInterceptor()).build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        flickrApi = retrofit.create(FlickrApi::class.java)
    }

    fun fetchPhotos(): LiveData<List<GalleryItem>> = fetchPhotosMetadata(fetchPhotoRequest())

    fun searchPhotos(query: String): LiveData<List<GalleryItem>> =
        fetchPhotosMetadata(searchPhotoRequest(query))

    private fun fetchPhotosMetadata(flickrRequest: Call<FlickrResponse>): LiveData<List<GalleryItem>> {
        val responseLiveData: MutableLiveData<List<GalleryItem>> = MutableLiveData()

        flickrRequest.enqueue(object : Callback<FlickrResponse> {
            override fun onFailure(call: Call<FlickrResponse>, t: Throwable) {
                val request = call.request()
                Log.e(TAG, "Failed to fetch photos", t)
            }

            override fun onResponse(call: Call<FlickrResponse>, response: Response<FlickrResponse>) {
                Log.d(TAG, "Response received: ${response.body()}")
                val flickrResponse: FlickrResponse? = response?.body()
                val photoResponse: PhotoResponse? = flickrResponse?.photos
                var galleryItems: List<GalleryItem> = photoResponse?.galleryItems ?: mutableListOf()
                galleryItems = galleryItems.filterNot { it.url.isBlank() }
                responseLiveData.value = galleryItems
            }
        })

        return responseLiveData
    }

    @WorkerThread
    fun fetchPhoto(url: String): Bitmap? {
        val response: Response<ResponseBody> = flickrApi.fetchUrlBytes(url).execute()
        val bitmap = response.body()?.byteStream()?.use(BitmapFactory::decodeStream)
        return bitmap
    }

    fun fetchPhotoRequest(): Call<FlickrResponse> = flickrApi.fetchPhotos()

    fun searchPhotoRequest(query: String): Call<FlickrResponse> = flickrApi.searchPhotos(query)

}