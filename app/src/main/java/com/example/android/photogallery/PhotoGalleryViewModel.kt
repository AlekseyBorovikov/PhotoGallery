package com.example.android.photogallery

import android.app.Application
import androidx.lifecycle.*
import com.example.android.photogallery.models.GalleryItem

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {

    val galleryItemLivaData: LiveData<List<GalleryItem>>

    private val flickrFetchr = FlickrFetchr()
    private val mutableSearchTerm = MutableLiveData<String>()
    val searchTerm: String
        get() = mutableSearchTerm.value ?: ""

    init {
        mutableSearchTerm.value = QueryPreferences.getStoredQuery(app)

        galleryItemLivaData = Transformations.switchMap(mutableSearchTerm) { searchTerm ->
            if (searchTerm.isBlank()) flickrFetchr.fetchPhotos()
            else flickrFetchr.searchPhotos(searchTerm)
        }
    }

    fun fetchPhotos(query: String = "") {
        QueryPreferences.setStoredQuery(app, query)
        mutableSearchTerm.value = query
    }

}