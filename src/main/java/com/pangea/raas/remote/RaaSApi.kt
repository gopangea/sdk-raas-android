package com.pangea.raas.remote



import com.pangea.raas.data.models.TokenRequest
import com.pangea.raas.data.models.TokenResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST


internal interface RaaSApi {
    @Headers("Accept: application/json")
    @POST("v1/tokenization/card")
    fun postTemporaryToken(@Body tokenRequest: TokenRequest):Call<TokenResponse>

    @GET("v1/clientSession/data")
    fun getClientSession(): Call<ResponseBody>


}