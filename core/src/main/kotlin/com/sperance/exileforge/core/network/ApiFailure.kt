package com.sperance.exileforge.core.network

import java.io.IOException

class ApiFailure(
    val status: Int?,
    val code: String?,
    message: String,
    /**
     * What the server interpolated into its own sentence.
     *
     * Since 0.17.0 the envelope carries these, so the client can fill its own template rather than
     * printing the server's English. Empty when the server is older, and then the sentence stands.
     */
    val args: List<String> = emptyList(),
    /** The answer was not the game's envelope (3.79.0): an HTML page, a cut body — shown as an outage, not as its text. */
    val malformed: Boolean = false,
) : IOException(message)
