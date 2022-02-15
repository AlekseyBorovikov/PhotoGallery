package com.example.android.photogallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.android.photogallery.models.GalleryItem

class PhotoGalleryViewModel : ViewModel() {

    val galleryItemLivaData: LiveData<List<GalleryItem>> = FlickrFetchr().fetchPhotos()

}