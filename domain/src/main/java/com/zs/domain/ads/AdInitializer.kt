package com.zs.domain.ads

import android.content.Context

internal fun interface AdInitializer {
    fun initialize(context: Context, id: String)
}