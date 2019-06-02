package com.ederigo.android.glideplayground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.bumptech.glide.intoProgressTarget
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.Target
import com.ederigo.android.glideplayground.util.glide.GlideApp
import com.ederigo.android.glideplayground.util.glide.ProgressTarget
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        private const val GOOD_PREVIEW_URL = "https://stmed.net/sites/default/files/styles/480x272/public/lijiang-wallpapers-28687-3064464.jpg"
        private const val BAD_PREVIEW_URL = "https://stmed.net/sites/default/files/styles/480x272/public/lijiang-wallpapers-28687-3064465.jpg"
        private const val GOOD_THUMBNAIL_URL = "https://stmed.net/sites/default/files/lijiang-wallpapers-28687-3064464.jpg"
        private const val BAD_THUMBNAIL_URL = "https://stmed.net/sites/default/files/lijiang-wallpapers-28687-3064465.jpg"

        private val TAG = MainActivity::class.java.simpleName
    }

    private var previewUrl = GOOD_PREVIEW_URL
    private var thumbnailUrl = GOOD_THUMBNAIL_URL

    private var isSkippingThumbnailMemoryCache = false
    private var isSkippingThumbnailDiskCache = false
    private var isSkippingPreviewMemoryCache = false
    private var isSkippingPreviewDiskCache = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isBreakPreview.setOnCheckedChangeListener { _, isChecked ->
            previewUrl = if (isChecked) BAD_PREVIEW_URL else GOOD_PREVIEW_URL
        }

        isBreakThumbnail.setOnCheckedChangeListener { _, isChecked ->
            thumbnailUrl = if (isChecked) BAD_THUMBNAIL_URL else GOOD_THUMBNAIL_URL
        }

        isIgnoreMemoryPreview.setOnCheckedChangeListener { _, isChecked ->
            isSkippingPreviewMemoryCache = isChecked
        }

        isIgnoreMemoryThumbnail.setOnCheckedChangeListener { _, isChecked ->
            isSkippingThumbnailMemoryCache = isChecked
        }

        isIgnoreDiskPreview.setOnCheckedChangeListener { _, isChecked ->
            isSkippingPreviewDiskCache = isChecked
        }

        isIgnoreDiskThumbnail.setOnCheckedChangeListener { _, isChecked ->
            isSkippingThumbnailDiskCache = isChecked
        }

        val target = MyProgressTarget(DrawableImageViewTarget(imageView), thumbnailUrl)

        reloadButton.setOnClickListener {
            Log.d(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

            target.model = thumbnailUrl
            GlideApp.with(this)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .thumbnail(
                    GlideApp.with(this)
                        .load(previewUrl)
                        .diskCacheStrategy(if (isSkippingPreviewDiskCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
                        .skipMemoryCache(isSkippingPreviewMemoryCache)
                )
                .diskCacheStrategy(if (isSkippingThumbnailDiskCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
                .skipMemoryCache(isSkippingThumbnailMemoryCache)
                .transition(DrawableTransitionOptions.withCrossFade())
                .intoProgressTarget(target)
        }
    }

    private inner class MyProgressTarget<T, Z>(target: Target<Z>, model: T) : ProgressTarget<T, Z>(target, model) {

        override val granularityPercentage: Float
            get() = 1f

        override fun onConnecting() {
            Log.d(TAG, "onConnecting")
            progress.visibility = View.VISIBLE
            progress.isIndeterminate = true
            status.visibility = View.VISIBLE
            status.text = "Connecting"
        }

        override fun onDownloading(bytesRead: Long, expectedLength: Long) {
            Log.d(TAG, "onDownloading: bytesRead = $bytesRead; expectedLength = $expectedLength")
            val percent = 100f * bytesRead / expectedLength
            progress.isIndeterminate = false
            progress.progress = percent.toInt()
            status.text = String.format(
                "Downloading %.2f/%.2f MB %.1f%%",
                bytesRead / (1024f * 1024f), expectedLength / (1024f * 1024f), percent
            )
        }

        override fun onDownloaded() {
            Log.d(TAG, "onDownloaded")
            progress.isIndeterminate = true
            status.text = "Decoding and transforming"
        }

        override fun onComplete() {
            Log.d(TAG, "onComplete")
            progress.visibility = View.INVISIBLE
            status.visibility = View.INVISIBLE
        }

        override fun onError(t: Throwable?) {
            Log.d(TAG, "onError")
            progress.visibility = View.INVISIBLE
            status.visibility = View.VISIBLE
            status.text = "Failed"
        }

    }
}
