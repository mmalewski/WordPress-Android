package org.wordpress.android.imageeditor.preview

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.CENTER_CROP
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.preview_image_fragment.*
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.ImageEditor.RequestListener
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.R.layout
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageStartLoadingToFileState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import java.io.File

class PreviewImageFragment : Fragment() {
    private lateinit var viewModel: PreviewImageViewModel

    companion object {
        const val ARG_LOW_RES_IMAGE_URL = "arg_low_res_image_url"
        const val ARG_HIGH_RES_IMAGE_URL = "arg_high_res_image_url"
        const val ARG_OUTPUT_FILE_EXTENSION = "arg_output_file_extension"
        const val PREVIEW_IMAGE_REDUCED_SIZE_FACTOR = 0.1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.preview_image_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullIntent = checkNotNull(requireActivity().intent)
        initializeViewModels(nonNullIntent)
        initializeViews()
    }

    private fun initializeViews() {
        initializeViewPager()
    }

    private fun initializeViewPager() {
        viewPager.adapter = PreviewImageAdapter(
            loadIntoImageViewWithResultListener = { imageData, imageView ->
                loadIntoImageViewWithResultListener(imageData, imageView)
            }
        )
    }

    private fun initializeViewModels(nonNullIntent: Intent) {
        val lowResImageUrl = nonNullIntent.getStringExtra(ARG_LOW_RES_IMAGE_URL)
        val highResImageUrl = nonNullIntent.getStringExtra(ARG_HIGH_RES_IMAGE_URL)
        val outputFileExtension = nonNullIntent.getStringExtra(ARG_OUTPUT_FILE_EXTENSION)

        viewModel = ViewModelProvider(this).get(PreviewImageViewModel::class.java)
        setupObservers()
        viewModel.onCreateView(lowResImageUrl, highResImageUrl, outputFileExtension)
    }

    private fun setupObservers() {
//        viewModel.uiState.observe(this, Observer { uiState ->
//            if (uiState is ImageDataStartLoadingUiState) {
//                loadIntoImageView(uiState.imageData)
//            }
//            UiHelpers.updateVisibility(progressBar, uiState.progressBarVisible)
//            UiHelpers.updateVisibility(errorLayout, uiState.retryLayoutVisible)
//        })

        viewModel.uiState.observe(this, Observer { state ->
            val tabLayoutMediator = TabLayoutMediator(
                tabLayout,
                    viewPager,
                    true,
                    TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                        val customView = LayoutInflater.from(context).inflate(layout.preview_image_thumbnail, null)
                        val imageView = customView.findViewById<ImageView>(R.id.thumbnailImageView)
                        val imageUiState = state.viewPagerItemsUiState.items[position] as ImageDataStartLoadingUiState
                        loadIntoImageView(imageUiState.imageData.lowResImageUrl, imageView)
                        tab.customView = customView
                    }
            )
            tabLayoutMediator.attach()
            (viewPager.adapter as PreviewImageAdapter).submitList(state.viewPagerItemsUiState.items)
        })

        viewModel.loadIntoFile.observe(this, Observer { fileState ->
            if (fileState is ImageStartLoadingToFileState) {
                loadIntoFile(fileState.imageUrl)
            }
        })

        viewModel.navigateToCropScreenWithFileInfo.observe(this, Observer { filePath ->
            navigateToCropScreenWithInputFilePath(filePath)
        })
    }

    private fun loadIntoImageView(url: String, imageView: ImageView) {
        ImageEditor.instance.loadIntoImageView(url, imageView, CENTER_CROP)
    }

    private fun loadIntoImageViewWithResultListener(imageData: ImageData, imageView: ImageView) {
        ImageEditor.instance.loadIntoImageViewWithResultListener(
            imageData.highResImageUrl,
            imageView,
            CENTER,
            imageData.lowResImageUrl,
            object : RequestListener<Drawable> {
                override fun onResourceReady(resource: Drawable, url: String) {
                    viewModel.onLoadIntoImageViewSuccess(url, imageData)
                }

                override fun onLoadFailed(e: Exception?, url: String) {
                    viewModel.onLoadIntoImageViewFailed(url)
                }
            }
        )
    }

    private fun loadIntoFile(url: String) {
        ImageEditor.instance.loadIntoFileWithResultListener(
            url,
            object : RequestListener<File> {
                override fun onResourceReady(resource: File, url: String) {
                    viewModel.onLoadIntoFileSuccess(resource.path)
                }

                override fun onLoadFailed(e: Exception?, url: String) {
                    viewModel.onLoadIntoFileFailed()
                }
            }
        )
    }

    private fun navigateToCropScreenWithInputFilePath(fileInfo: Pair<String, String?>) {
        // TODO: temporarily stop navigation to next screen
//        val inputFilePath = fileInfo.first
//        val outputFileExtension = fileInfo.second
//        findNavController().navigate(
//            PreviewImageFragmentDirections.actionPreviewFragmentToCropFragment(inputFilePath, outputFileExtension)
//        )
    }
}