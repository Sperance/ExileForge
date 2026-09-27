package com.sperance.exileforge.core.network

/** The last `static/index.json` one server served, kept on the device: what a start without the network reads instead. */
interface ManifestCache {
    suspend fun read(): String?
    suspend fun write(text: String)
}
