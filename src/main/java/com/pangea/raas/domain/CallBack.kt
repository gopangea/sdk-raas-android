package com.pangea.raas.domain


interface CallBack<T>{

    fun  onResponse(result: T)

    fun  onFailure(result: T, throwable: Throwable?)

}