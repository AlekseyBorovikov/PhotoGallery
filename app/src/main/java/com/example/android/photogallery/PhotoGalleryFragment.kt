package com.example.android.photogallery

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.example.android.photogallery.api.FlickrApi
import com.example.android.photogallery.databinding.FragmentPhotoGalleryBinding
import com.example.android.photogallery.models.GalleryItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

private const val TAG = "PhotoGalleryFragment"

/**
 * A fragment representing a list of Items.
 */
class PhotoGalleryFragment : Fragment() {

    private var columnCount = 2
    private var _binding: FragmentPhotoGalleryBinding? = null
    private val photoGalleryViewModel: PhotoGalleryViewModel by lazy {
        ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)
    }
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoGalleryBinding.inflate(inflater)
        // Set the adapter
        val recyclerView = binding.photoRecyclerView
        with(recyclerView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGalleryViewModel.galleryItemLivaData.observe(viewLifecycleOwner) { galleryItems ->
            Log.d(TAG, "Have gallery items from ViewModel $galleryItems")
            binding.photoRecyclerView.adapter = PhotoGalleryAdapter(galleryItems)
        }
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            PhotoGalleryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}