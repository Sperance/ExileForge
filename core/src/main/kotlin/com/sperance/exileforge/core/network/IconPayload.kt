package com.sperance.exileforge.core.network

/** An answer from an icon route: [svg] is empty on 304, and [etag] is what the next request sends. */
data class IconPayload(val status: Int, val etag: String, val svg: String) {
    val unchanged: Boolean get() = status == 304
}
