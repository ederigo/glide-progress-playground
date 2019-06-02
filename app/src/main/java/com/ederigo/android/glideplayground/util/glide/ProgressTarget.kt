package com.ederigo.android.glideplayground.util.glide

import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

open class WrappingTarget<Z>(target: Target<Z>) : Target<Z> by target

abstract class ProgressTarget<T, Z>(
    target: Target<Z>,
    var model: T? = null
) : WrappingTarget<Z>(target),
    RequestListener<Z>,
    ProgressGlideModule.ProgressListener {

    companion object {
        private val TAG = ProgressTarget::class.java.simpleName
    }

    private var isLoading = false

    override val granularityPercentage: Float
        get() = 1.0f

    /**
     * Convert a model into an Url string that is used to match up the OkHttp requests. For explicit
     * [GlideUrl][com.bumptech.glide.load.model.GlideUrl] loads this needs to return
     * [toStringUrl][com.bumptech.glide.load.model.GlideUrl.toStringUrl]. For custom models do the same as your
     * [BaseGlideUrlLoader][com.bumptech.glide.load.model.stream.BaseGlideUrlLoader] does.
     * @param model return the representation of the given model, DO NOT use [.getModel] inside this method.
     * @return a stable Url representation of the model, otherwise the progress reporting won't work
     */
    protected fun toUrlString(model: T?) = model.toString()

    override fun onProgress(bytesRead: Long, expectedLength: Long) {
        if (!isLoading) return
        if (bytesRead >= expectedLength) onDownloaded()
        else onDownloading(bytesRead, expectedLength)
    }

    /**
     * Called when the Glide load has started.
     * At this time it is not known if the Glide will even go and use the network to fetch the image.
     */
    protected abstract fun onConnecting()

    /**
     * Called when there's any progress on the download; not called when loading from cache.
     * At this time we know how many bytes have been transferred through the wire.
     */
    protected abstract fun onDownloading(bytesRead: Long, expectedLength: Long)

    /**
     * Called when the bytes downloaded reach the length reported by the server; not called when loading from cache.
     * At this time it is fairly certain, that Glide either finished reading the stream.
     * This means that the image was either already decoded or saved the network stream to cache.
     * In the latter case there's more work to do: decode the image from cache and transform.
     * These cannot be listened to for progress so it's unsure how fast they'll be, best to show indeterminate progress.
     */
    protected abstract fun onDownloaded()

    protected abstract fun onError(t: Throwable?)

    protected abstract fun onComplete()

    override fun onLoadStarted(placeholder: Drawable?) {
        Log.d(TAG, "onLoadStarted")
        super.onLoadStarted(placeholder)
        start()
        onConnecting()
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        Log.d(TAG, "onLoadCleared")
        cleanup()
        super.onLoadCleared(placeholder)
    }

    override fun onResourceReady(
        resource: Z,
        model: Any,
        target: Target<Z>,
        dataSourcedataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        val isMainModel = model == this.model
        Log.d(TAG, "onResourceReady: isMainModel = $isMainModel; dataSourcedataSource = $dataSourcedataSource; isFirstResource = $isFirstResource")
        if (isMainModel) {
            cleanup()
            onComplete()
        } else if (!isLoading) {
            start()
            onConnecting()
        }
        return false
    }

    override fun onLoadFailed(
        e: GlideException?,
        model: Any,
        target: Target<Z>,
        isFirstResource: Boolean
    ): Boolean {
        val isMainModel = model == this.model
        Log.d(TAG, "onLoadFailed: isMainModel = $isMainModel; isFirstResource = $isFirstResource")
        if (isMainModel) {
            cleanup()
            onError(e)
        }
        return false
    }

    private fun start() {
        if (isLoading) return
        isLoading = true
        ProgressGlideModule.expect(toUrlString(model), this)
    }

    private fun cleanup() {
        if (!isLoading) return
        isLoading = false
        ProgressGlideModule.forget(toUrlString(model))
    }
}