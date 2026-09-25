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
) : IOException(message)
