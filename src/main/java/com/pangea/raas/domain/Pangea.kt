package com.pangea.raas.domain

import android.content.Context
import android.util.Log
import com.pangea.raas.data.models.CardInformation
import com.pangea.raas.data.models.TokenRequest
import com.pangea.raas.data.models.TokenResponse
import com.pangea.raas.remote.RetrofitClient
import com.pangea.raas.remote.TokenApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

open class Pangea private constructor() : RxBeaconOperations {
    private var pangeaSessionId = ""
    private lateinit var rxBeaconWrapper: RxBeaconOperations
    private var debugInfo: Boolean = false //by default the class has the debug info turned off

    companion object {
        private const val TAG = "Pangea"

        fun createSession(context: Context, debugInfo: Boolean): Pangea {
            return Pangea().apply {
                pangeaSessionId = createUUID()
                this.debugInfo = debugInfo
                rxBeaconWrapper = RxBeaconWrapper(
                    context = context,
                    sessionId = pangeaSessionId,
                    debugInfo = debugInfo
                )
            }
        }

        private const val ERROR_ENCRYPTION_MESSAGE =
            "Encryption fails, invalid public key, the format for your public key should be: \n -----BEGIN PUBLIC KEY-----\n" + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmyq5BMXtv1VWu2XSfzWP\n" + "sKvyxIzFQoyz59DsGUpaePjIznI+0ioCUeuEBCrcr8i/tFcmp0af88ga9y5vezk9\n" + "4yZKmN1+oEa51wlFu+jtX9X6bEDF8UZAq3u4xOoI1VGVW1b0oh9NEMSZXemL974A\n" + "LoQv6Lc5LZ6KfgXTm8Hu71G5gAgsvPizVGbyLtsG1Rd1Nye8q59ai14ovV/ISlKI\n" + "QgPs+RCWgwSd6NjrvOv9gWPnwkjc59Z46kiTgHv3KNszr7sBY1Cdnd4gGYrDMyU0\n" + "4m9v1+UW24syxcFKyyivPf3Wjxp73tSerSRFWQvUPBSvs+cbjf866hCqeFKh+XUC\n" + "AwIDAQAB\n" + "-----END PUBLIC KEY-----\n or this one: \n" + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmyq5BMXtv1VWu2XSfzWP" + "sKvyxIzFQoyz59DsGUpaePjIznI+0ioCUeuEBCrcr8i/tFcmp0af88ga9y5vezk9" + "4yZKmN1+oEa51wlFu+jtX9X6bEDF8UZAq3u4xOoI1VGVW1b0oh9NEMSZXemL974A" + "LoQv6Lc5LZ6KfgXTm8Hu71G5gAgsvPizVGbyLtsG1Rd1Nye8q59ai14ovV/ISlKI" + "QgPs+RCWgwSd6NjrvOv9gWPnwkjc59Z46kiTgHv3KNszr7sBY1Cdnd4gGYrDMyU0" + "4m9v1+UW24syxcFKyyivPf3Wjxp73tSerSRFWQvUPBSvs+cbjf866hCqeFKh+XUC" + "AwIDAQAB"
    }

    fun getSessionId(): String {
        return pangeaSessionId
    }


    fun createToken(card: CardInformation, callBack: CallBack) {
        thread(start = true) {
            if (!isCardNumberValid(card.cardNumber)) {
                if (debugInfo) {
                    Log.e(TAG, "createToken: cardNumber is not a valid card number : ${card.cardNumber} "
                    )
                }
                callBack.onFailure(TokenResponse("cardNumber is not a valid card number"), null)
                return@thread
            }
            var requestId = "0"
            while (requestId.toLong(11) == 0L) {//if requestID is Zero
                //On the way that this random number is produced there is a minimum probability to get zero, that's the reason of this loop
                requestId = Math.random().toString().replace(".", "").toLong().toString(11)
            }
            getToken(card, requestId, callBack)
        }
    }

    private fun isCardNumberValid(cardNumber: String): Boolean {
        val numbers = cardNumber.replace(Regex("\\D+"), "")
            .map { Character.getNumericValue(it) }
            .reversed()
        if (numbers.size < 12) {
            return false
        }
        var total = 0
        numbers.forEachIndexed { index, _ ->
            total += if (index % 2 != 0) {
                2 * numbers[index] - if (numbers[index] > 4) 9 else 0
            } else {
                numbers[index]
            }
        }
        return total % 10 == 0
    }


    private fun createUUID(): String {
        val s = CharArray(36)
        val hexDigits = "0123456789abcdef"
        s.forEachIndexed { index, _ ->
            s[index] = hexDigits[(0 until 16).random()]
        }
        s[14] = '4'
        val temporalNumber = if (s[19].toString().matches(Regex("\\D+"))) {
            0x0
        } else {
            s[19].toString().toInt(16)
        }
        s[19] = ((temporalNumber and 3) or 8).toString(16).first() //s[19] can be only 8,9,a or b
        s[8] = '-'; s[13] = '-';s[18] = '-'; s[23] = '-'
        return s.concatToString()

    }

    //this method uses a third party library named: org.bouncycastle, this libraries size are up 4 mb:
    //libs/bcpkix-jdk15to18-166.jar and libs/bcprov-ext-jdk15to18-166.jar, that's why it was replaced for cryptoUtils
//    private fun encrypt(publicKey: String, text: String): String {
//        var encrypted = ""
//        try {
//            //converting the string public key to byte array
//            val reader = PemReader(StringReader(publicKey))
//            val pemObject = reader.readPemObject()
//            val keyBytes: ByteArray = pemObject.content
//            val keySpec: EncodedKeySpec = X509EncodedKeySpec(keyBytes)
//            val keyFactory = KeyFactory.getInstance("RSA")
//            val key = keyFactory.generatePublic(keySpec)
//            //encrypt
//            val cipher = Cipher.getInstance(KEY_ALGORITHM)
//            cipher.init(Cipher.ENCRYPT_MODE, key)
//            val cipherData: ByteArray = cipher.doFinal(text.toByteArray())
//            encrypted = Base64.encodeToString(cipherData, Base64.NO_WRAP)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return encrypted
//    }

    private fun getToken(card: CardInformation, requestId: String, callBack: CallBack) {
        val encryptedCardNumber: String
        val encryptedCvv: String
        try {
            encryptedCardNumber = CryptoUtil.encrypt(card.publicKey, card.cardNumber)
            encryptedCvv = CryptoUtil.encrypt(card.publicKey, card.cvv)
        } catch (e: Exception) {
            callBack.onFailure(TokenResponse(ERROR_ENCRYPTION_MESSAGE), e)
            return
        }
        val tokenRequest = TokenRequest(
            encryptedCardNumber = encryptedCardNumber,
            encryptedCvv = encryptedCvv,
            partnerIdentifier = card.partnerIdentifier,
            requestId = requestId
        )

        val tokenApi = RetrofitClient.initRetrofitInstance(debugInfo).buildService(TokenApi::class.java)
        tokenApi?.postTemporaryToken(tokenRequest)?.enqueue(object : Callback<TokenResponse> {
            override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { tokenResponse ->
                        callBack.onResponse(tokenResponse)
                    }
                } else {
                    if (debugInfo) {
                        Log.w(TAG, "onResponse: response is not ok, code: ${response.code()} \n  responseBody: ${response.body()}")
                    }
                }
            }

            override fun onFailure(call: Call<TokenResponse>, throwable: Throwable) {
                if (debugInfo) {
                    Log.e(TAG, "onFailure, body: ${call.request().body}")
                }
                callBack.onFailure(TokenResponse(""), throwable)
            }
        })
    }

    //RiskField begins
    override fun updateSessionToken(sessionId: String) {
        pangeaSessionId = sessionId
        rxBeaconWrapper.updateSessionToken(sessionId)
    }

    override fun logRequest(url: String) {
        rxBeaconWrapper.logRequest(url)
    }

    override fun logSensitiveDeviceInfo() {
        rxBeaconWrapper.logSensitiveDeviceInfo()
    }

    override fun rCookie(): String = rxBeaconWrapper.rCookie()


    override fun removeLocationUpdates() {
        rxBeaconWrapper.removeLocationUpdates()
    }
    //RiskField ends

}