package com.pangea.raas.domain

import android.content.Context
import android.util.Log
import com.pangea.raas.data.models.CardInformation
import com.pangea.raas.data.models.TokenRequest
import com.pangea.raas.data.models.TokenResponse
import com.pangea.raas.remote.ErrorDescription
import com.pangea.raas.remote.RaaSApi
import com.pangea.raas.remote.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

open class Pangea private constructor() : RxBeaconOperations {
    private var pangeaSessionId = ""
    private lateinit var rxBeaconWrapper: RxBeaconOperations
    private var debugInfo: Boolean = false //by default the class has the debug info turned off
    private lateinit var environment:Environment

    companion object {
        private const val TAG = "Pangea"

        fun createSession(
            context: Context,
            debugInfo: Boolean,
            pangeaSessionID: String,
            environment: Environment
        ): Pangea {
            return Pangea().apply {
                //if(pangeaSessionID == ""){ pangeaSessionId = createUUID()}
                this.pangeaSessionId = pangeaSessionID
                this.debugInfo = debugInfo
                this.environment = environment
                rxBeaconWrapper = RxBeaconWrapper(
                    context = context,
                    sessionId = pangeaSessionId,
                    debugInfo = debugInfo
                )
            }
        }

        private const val ERROR_ENCRYPTION_MESSAGE =
            "Encryption fails, invalid public key, the format for your public key should be like this: \"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmyq5BMXtv1VWu2XSfzWPsKvyxIzFQoyz59DsGUpaePjIznI+0ioCUeuEBCrcr8i/tFcmp0af88ga9y5vezk94yZKmN1+oEa51wlFu+jtX9X6bEDF8UZAq3u4xOoI1VGVW1b0oh9NEMSZXemL974ALoQv6Lc5LZ6KfgXTm8Hu71G5gAgsvPizVGbyLtsG1Rd1Nye8q59ai14ovV/ISlKIQgPs+RCWgwSd6NjrvOv9gWPnwkjc59Z46kiTgHv3KNszr7sBY1Cdnd4gGYrDMyU04m9v1+UW24syxcFKyyivPf3Wjxp73tSerSRFWQvUPBSvs+cbjf866hCqeFKh+XUCAwIDAQAB\""
    }

    enum class Environment {
        PRODUCTION,
        DEV,
        INTEGRATION,
    }

    fun getSessionId(): String {
        return pangeaSessionId
    }


    fun createToken(card: CardInformation, callBack: CallBack<TokenResponse>) {
        thread(start = true) {
            if (!isCardNumberValid(card.cardNumber)) {
                if (debugInfo) {
                    Log.e(
                        TAG,
                        "createToken: cardNumber is not a valid card number : ${card.cardNumber} "
                    )
                }
                callBack.onFailure(TokenResponse("cardNumber is not a valid card number"), null)
                return@thread
            }
            getToken(card, getRequestId(), callBack)
        }
    }

    private fun getRequestId(): String{
        var requestId = "0"
        while (requestId.toLong(11) == 0L) {//if requestID is Zero
            //On the way that this random number is produced there is a minimum probability to get zero, that's the reason of this loop
            requestId = Math.random().toString().replace(".", "").replace("E-", "").toLong().toString(11)
        }
        return requestId
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



    private fun addSessionIDToClientInfo(jsonObject: JSONObject): JSONObject {
        jsonObject.put("clientSessionId", getSessionId())
        return jsonObject
    }

    private fun getEncoded64BaseString(jsonObject: JSONObject):String{
        if(debugInfo){
            println("object to decode: $jsonObject")
        }
        return CryptoUtil.get64BaseStringFromString(jsonObject.toString())
    }

    fun getClientData(callBack: CallBack<String>){
        thread(start = true) {
            fetchClientInfo(callBack)
        }
    }

    private fun fetchClientInfo(callBack: CallBack<String>){
        val tokenApi = RetrofitClient.initRetrofitInstance(environment, debugInfo).buildService(
            RaaSApi::class.java
        )
        tokenApi?.getClientSession()?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let {responseBody ->
                        val clientInfo = JSONObject(responseBody.string())
                        val clientInfoWithSessionID = addSessionIDToClientInfo(clientInfo)
                        val clientInfoWithSessionIDEncoded = getEncoded64BaseString(clientInfoWithSessionID)
                        callBack.onResponse(clientInfoWithSessionIDEncoded)
                    }
                } else {
                    if (debugInfo) {
                        Log.w(TAG, "onResponse: response is not ok, code: ${response.code()} \n  responseBody: ${response.errorBody().toString()}")
                    }
                    try {
                        val errorDescription = getErrorDescription(response)
                        callBack.onFailure("", Throwable(errorDescription.toString()))
                    } catch (e: java.lang.Exception) {
                        callBack.onFailure("", Throwable(e))
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
                if (debugInfo) {
                    Log.e(TAG, "onFailure, body: ${call.request().body}")
                }
                callBack.onFailure("", throwable)
            }
        })
    }

    private fun getToken(card: CardInformation, requestId: String, callBack: CallBack<TokenResponse>) {
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

        val tokenApi = RetrofitClient.initRetrofitInstance(environment, debugInfo).buildService(
            RaaSApi::class.java
        )
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
                    try {
                        val errorDescription = getErrorDescription(response)
                        callBack.onFailure(TokenResponse(""), Throwable(errorDescription.toString()))
                    } catch (e: java.lang.Exception) {
                        callBack.onFailure(TokenResponse(""), e)
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

    private fun <T> getErrorDescription(response: Response<T>): ErrorDescription {
        val errorMessage = StringBuilder()
        val bufferedReader: BufferedReader?
        if (response.errorBody() != null) {
            bufferedReader = BufferedReader(InputStreamReader(response.errorBody()!!.byteStream()))
            var eLine: String?
            while (bufferedReader.readLine().also { eLine = it } != null) {
                errorMessage.append(eLine)
            }
            bufferedReader.close()
        }
        return ErrorDescription(
            rawMessage = response.raw().toString(),
            body = response.body().toString(),
            bodyError = errorMessage.toString()
        )
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

    //override fun rCookie(): String = rxBeaconWrapper.rCookie()


    override fun removeLocationUpdates() {
        rxBeaconWrapper.removeLocationUpdates()
    }
    //RiskField ends

}

