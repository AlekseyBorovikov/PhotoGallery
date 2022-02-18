package com.example.android.photogallery

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.example.android.photogallery.databinding.FragmentPhotoGalleryBinding
import com.example.android.photogallery.databinding.FragmentPhotoGalleryItemBinding
import com.example.android.photogallery.models.GalleryItem
import java.util.concurrent.TimeUnit

private const val TAG = "PhotoGalleryFragment"
private const val POLL_WORK = "POLL_WORK"

/**
 * A fragment representing a list of Items.
 */
class PhotoGalleryFragment : VisibleFragment() {

    private var columnCount = 2
    private var _binding: FragmentPhotoGalleryBinding? = null
    private val photoGalleryViewModel: PhotoGalleryViewModel by lazy {
        ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)
    }
    private val binding get() = _binding!!
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        // Сохраняет фрагмент. Так лучше никогда не делать
        retainInstance = true
        setHasOptionsMenu(true)

        val responseHandler = Handler(Looper.getMainLooper())
        thumbnailDownloader = ThumbnailDownloader(responseHandler) { photoHolder, bitmap ->
            val drawable = BitmapDrawable(resources, bitmap)
            photoHolder.bindDrawable(drawable)
        }
        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView

        searchView.apply {

            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        Log.d(TAG, "QueryTextSubmit:$query")
                        query?.let { photoGalleryViewModel.fetchPhotos(it) }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        Log.d(TAG, "QueryTextChange: $query")
                        return false
                    }
                }
            )

            setOnSearchClickListener {
                searchView.setQuery(photoGalleryViewModel.searchTerm, false)
            }

        }

        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        val toggleItemTitle = if (isPolling) R.string.stop_polling else R.string.start_polling
        toggleItem.setTitle(toggleItemTitle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_item_clear -> {
                photoGalleryViewModel.fetchPhotos("")
                true
            }
            R.id.menu_item_toggle_polling -> {
                val isPolling = QueryPreferences.isPolling(requireContext())
                if (isPolling) {
                    WorkManager.getInstance(requireContext()).cancelUniqueWork(POLL_WORK)
                    QueryPreferences.setPolling(requireContext(), false)
                } else {
                    // Условие на то, что устройство должено быть подключено к сети без ограничения трафика
                    val constants = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()
                    // минимальный период 15 минут
                    val workerRequest = PeriodicWorkRequest
                        .Builder(CheckPhotoWorker::class.java, 15, TimeUnit.MINUTES)
                        .setConstraints(constants)
                        .build()
                    WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                        POLL_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        workerRequest
                    )
                    QueryPreferences.setPolling(requireContext(), true)
                }
                activity?.invalidateOptionsMenu()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoGalleryBinding.inflate(inflater)
        viewLifecycleOwner.lifecycle.addObserver(thumbnailDownloader.viewLifecycleObserver)
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

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(
            thumbnailDownloader.viewLifecycleObserver
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        lifecycle.removeObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    inner class PhotoGalleryAdapter(
        private val values: List<GalleryItem>
    ) : RecyclerView.Adapter<PhotoHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {

            return PhotoHolder(
                FragmentPhotoGalleryItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val item = values[position]

            thumbnailDownloader.queueThumbnail(holder, item.url)

            val placeHolder: Drawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bill_up_close
            ) ?: ColorDrawable()
            holder.bindDrawable(placeHolder)
        }

        override fun getItemCount(): Int = values.size

    }

    inner class PhotoHolder(binding: FragmentPhotoGalleryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val bindDrawable: (Drawable) -> Unit = binding.root::setImageDrawable
    }
}
