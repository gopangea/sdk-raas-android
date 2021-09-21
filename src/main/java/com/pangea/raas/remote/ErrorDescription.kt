package com.pangea.raas.remote

data class ErrorDescription(
    val rawMessage: String? = null,
    val body: String? = null,
    val bodyError: String? = null
)
