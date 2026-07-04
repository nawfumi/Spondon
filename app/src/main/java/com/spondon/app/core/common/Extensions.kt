package com.spondon.app.core.common

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
fun Date.daysSince(): Long {
    val diff = System.currentTimeMillis() - this.time
    return TimeUnit.MILLISECONDS.toDays(diff)
}

fun Date.formatDisplay(): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(this)
}

