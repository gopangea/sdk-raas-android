package com.pangea.raas.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


internal object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "https://api.pangea-raas-integration.com/raas/v1/"

    private var debugInfo:Boolean = false
    var retrofit: Retrofit? = null
        private set

    fun initRetrofitInstance(debugInfo:Boolean):RetrofitClient {
        this.debugInfo = debugInfo
        val clientHttp = OkHttpClient.Builder()
        if (debugInfo){
            val httpLoggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            clientHttp.addInterceptor(httpLoggingInterceptor)
        }
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(clientHttp.build())
            .build()
        return this
    }

    fun<T> buildService(service: Class<T>): T?{
        retrofit?.let {
            return it.create(service)
        }
        if (debugInfo){
            Log.w(TAG, "buildService: the retrofit client is null, use RetrofitClient.initRetrofitInstance(debugInfo:Boolean) to get it first", )
        }
        return null
    }
}

