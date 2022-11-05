package org.lsong.launcher

import android.graphics.drawable.Drawable

data class AppBlock(
    val appName: String,
    val icon: Drawable,
    val packageName: String
)