package com.pangea.raas.data.models

data class CardInformation(
        @JvmField var publicKey: String,
        @JvmField var partnerIdentifier: String,
        @JvmField var cardNumber: String,
        @JvmField var cvv: String,
)