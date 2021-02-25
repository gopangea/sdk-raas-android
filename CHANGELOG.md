# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- No changes comming

## [1.0.2] - 2020-02-25
### Added
- A new method that will return a base64 encoded string which the RaaS partner will store in their database instead of the clientSessionId
```
getClientData()
```
### Changed

- CallBack interface: This interface has chaged to a generic type, in this way can it be use to wrap any type of reponse from the pagea sdk that could be added in the future, the refactored interface is this one:

```
    fun  onResponse(result: T)
    fun  onFailure(result: T, throwable: Throwable?)
```
You can use it as the same as the previous one, just adding the type of the respose you are expecting back:
```
mPangea.createToken(cardInformation, object : CallBack<TokenResponse> {
            override fun onResponse(tokenResponse: TokenResponse) {
                Log.e(TAG, "onResponse: The token is; ${tokenResponse.token}" )
                //do something with your token
            }
            override fun onFailure(tokenResponse: TokenResponse, throwable: Throwable?) {
                Log.e(TAG, "the error is ${throwable?.localizedMessage}" )
                 //failed request
            }
        })
        
pangea.getClientData(object :CallBack<String>{
            override fun onResponse(clientInfo: String) {
                println(clientInfo)
                //do something with your clientInfo

            }
            override fun onFailure(result: String, throwable: Throwable?) {
                println("failed: ${throwable?.localizedMessage}")
                 //failed request
            }
        })
```
## [1.0.1] - 2020-12-12
### Added
- Support for Java projects
### Changed
- The import for Pangea.Companion.Environment.{type} changed to Pangea.Environment.{type}

## [1.0.0] - 2020-12-08
### Added
- A custom header x-pangea-user-agent that describes the version number, build number and OS 

## [0.1.3] - 2020-09-29
### Changed
- Initial version of the SDK
