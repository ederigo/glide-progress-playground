package com.ederigo.android.glideplayground.util.glide

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyGlideModule : AppGlideModule() {

    override fun isManifestParsingEnabled() = false

}