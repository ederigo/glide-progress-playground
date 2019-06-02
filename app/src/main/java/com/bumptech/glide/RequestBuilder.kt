package com.bumptech.glide

import com.bumptech.glide.util.Executors
import com.ederigo.android.glideplayground.util.glide.ProgressTarget

fun <T> RequestBuilder<T>.intoProgressTarget(target: ProgressTarget<*, T>) =
    into(target, target, Executors.mainThreadExecutor())