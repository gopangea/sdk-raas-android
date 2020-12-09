package com.pangea.raas.remote

import com.pangea.raas.data.models.Version

object ApiClientHelper {

    fun getUserAgent():String{
        val versionNumber = Version.versionNumber
        val buildNumber = Version.buildNumber
        return "RaasSdk|Android|Version=$versionNumber|Build=$buildNumber"
    }
}
