package com.pangea.raas.data.models

import com.google.gson.annotations.Expose

internal data class TokenRequest(
    @Expose
    @JvmField var encryptedCardNumber: String = "",
    @Expose
    @JvmField var encryptedCvv: String = "",
    @Expose
    @JvmField var partnerIdentifier: String = "",
    @Expose
    @JvmField var requestId: String = ""
)



