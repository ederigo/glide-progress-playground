package com.ederigo.android.glideplayground.util.glide

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.LibraryGlideModule
import okhttp3.*
import okio.*
import java.io.InputStream

@GlideModule
class ProgressGlideModule : LibraryGlideModule() {

    companion object {
        private fun createInterceptor(listener: ResponseProgressListener) = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response.newBuilder()
                .body(
                    OkHttpProgressResponseBody(
                        request.url(),
                        response.body()!!,
                        listener
                    )
                )
                .build()
        }

        fun forget(url: String) {
            DispatchingProgressListener.forget(url)
        }

        fun expect(url: String, listener: ProgressListener) {
            DispatchingProgressListener.expect(url, listener)
        }
    }

    private val defaultClient = OkHttpClient.Builder()
        .addNetworkInterceptor(
            createInterceptor(DispatchingProgressListener())
        )
        .build()

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(defaultClient)
        )
    }

    interface ProgressListener {
        /**
         * Control how often the listener needs an update. 0% and 100% will always be dispatched.
         * @return in percentage (0.2 = call [.onProgress] around every 0.2 percent of progress)
         */
        val granularityPercentage: Float

        fun onProgress(bytesRead: Long, expectedLength: Long)
    }

    private interface ResponseProgressListener {
        fun update(url: HttpUrl, bytesRead: Long, contentLength: Long)
    }

    private class DispatchingProgressListener : ResponseProgressListener {

        companion object {
            private val listeners = mutableMapOf<String, ProgressListener>()
            private val progresses = mutableMapOf<String, Long>()

            fun forget(url: String) {
                listeners.remove(url)
                progresses.remove(url)
            }

            fun expect(url: String, listener: ProgressListener) {
                listeners[url] = listener
            }
        }

        private val handler = Handler(Looper.getMainLooper())

        override fun update(url: HttpUrl, bytesRead: Long, contentLength: Long) {
            val key = url.toString()
            val listener = listeners[key] ?: return
            if (contentLength <= bytesRead) {
                forget(key)
            }
            if (needsDispatch(key, bytesRead, contentLength, listener.granularityPercentage)) {
                handler.post { listener.onProgress(bytesRead, contentLength) }
            }
        }

        private fun needsDispatch(
            key: String,
            current: Long,
            total: Long,
            granularity: Float
        ): Boolean {
            if (granularity == 0f || current == 0L || total == current) return true
            val percent = 100f * current / total
            val currentProgress = (percent / granularity).toLong()
            return progresses.put(key, currentProgress) != currentProgress
        }
    }

    private class OkHttpProgressResponseBody(
        private val url: HttpUrl,
        private val responseBody: ResponseBody,
        private val progressListener: ResponseProgressListener
    ) : ResponseBody() {

        private val bufferedSource: BufferedSource by lazy {
            Okio.buffer(source(responseBody.source()))
        }

        override fun contentType() = responseBody.contentType()

        override fun contentLength() = responseBody.contentLength()

        override fun source() = bufferedSource

        private fun source(source: Source) = object : ForwardingSource(source) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long) = super.read(sink, byteCount).also(::update)

            private fun update(bytesRead: Long) {
                val fullLength = contentLength()
                if (bytesRead == -1L) { // this source is exhausted
                    totalBytesRead = fullLength
                } else {
                    totalBytesRead += bytesRead
                }
                progressListener.update(url, totalBytesRead, fullLength)
            }
        }
    }
}