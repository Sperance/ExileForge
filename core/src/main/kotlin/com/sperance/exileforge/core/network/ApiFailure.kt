package com.sperance.exileforge.core.network

import java.io.IOException
import kotlinx.serialization.json.*
import okhttp3.*

class ApiFailure(val status: Int?, val code: String?, message: String) : IOException(message)
