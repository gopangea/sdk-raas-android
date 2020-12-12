package com.pangea.raas.data.models

import com.google.gson.annotations.Expose


data class TokenResponse(
    @Expose
    @JvmField val token: String
)