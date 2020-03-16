package com.example.gorillagroove.client

import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.utils.logger
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.reflect.KClass


@Suppress("unused")
class HttpClient {
    private val objectMapper = createMapper()
    private val client = OkHttpClient()
    private val logger = logger()

    fun<T: Any> get(
        url: String,
        kClass: KClass<T>,
        callback: ((response: HttpResponse<T>) -> Unit)? = null
    ) {
        sendRequest("GET", url, null, kClass, callback)
    }

    fun<T: Any> post(
        url: String,
        body: Any? = null,
        kClass: KClass<T>,
        callback: ((response: HttpResponse<T>) -> Unit)? = null
    ) {
        sendRequest("POST", url, body, kClass, callback)
    }

    fun post(
        url: String,
        body: Any? = null,
        callback: ((response: HttpResponse<Nothing>) -> Unit)? = null
    ) {
        sendRequestWithoutResponse("POST", url, body, callback)
    }

    fun<T: Any> put(
        url: String,
        body: Any? = null,
        kClass: KClass<T>,
        callback: ((response: HttpResponse<T>) -> Unit)? = null
    ) {
        sendRequest("PUT", url, body, kClass, callback)
    }

    fun put(
        url: String,
        body: Any? = null,
        callback: ((response: HttpResponse<Nothing>) -> Unit)? = null
    ) {
        sendRequestWithoutResponse("PUT", url, body, callback)
    }

    fun<T: Any> delete(
        url: String,
        kClass: KClass<T>,
        callback: ((response: HttpResponse<T>) -> Unit)? = null
    ) {
        sendRequest("DELETE", url, null, kClass, callback)
    }

    fun delete(
        url: String,
        callback: ((response: HttpResponse<Nothing>) -> Unit)? = null
    ) {
        sendRequestWithoutResponse("DELETE", url, null, callback)
    }

    private fun<T: Any> sendRequest(
        protocol: String,
        url: String,
        body: Any?,
        kClass: KClass<T>,
        callback: ((response: HttpResponse<T>) -> Unit)?
    ) {
        GlobalScope.launch {
            client.send(protocol, body, url).use { response ->
                val success = response.isSuccessful

                if (!success) {
                    logger.warn("Error ${response.code} sending $protocol to '$url'. Response: ${response.body?.string()}")
                }

                val responseData = HttpResponse(
                    statusCode = response.code,
                    success = success,
                    nullableData = if (success) {
                        objectMapper.readValue(response.body!!.string(), kClass.java)
                    } else {
                        null
                    },
                    error = if (!success && response.body != null && response.body!!.contentLength() > 0) {
                        response.body!!.string()
                    } else {
                        null
                    }
                )

                callback?.invoke(responseData)
            }
        }
    }

    // There is a lot of duplicate code in here and I hates it. But to make the API for this class work the way I want,
    // I see no other way. Dang Java type erasure making generics funky. This code path allows methods to not have to
    // supply a class for deserialization if they are not interested in a response.
    private fun sendRequestWithoutResponse(
        protocol: String,
        url: String,
        body: Any?,
        callback: ((response: HttpResponse<Nothing>) -> Unit)?
    ) {
        GlobalScope.launch {
            client.send(protocol, body, url).use { response ->
                val success = response.isSuccessful

                if (!success) {
                    logger.warn("Error ${response.code} sending $protocol to '$url'. Response: ${response.body?.string()}")
                }

                val responseData = HttpResponse(
                    statusCode = response.code,
                    success = success,
                    nullableData = null,
                    error = if (!success) response.body?.string() else null
                )
                callback?.invoke(responseData)
            }
        }
    }

    private fun OkHttpClient.send(httpMethod: String, body: Any?, url: String): Response {
        val userRepository = UserRepository(GroovinDB.getDatabase().userRepository())

        val user = userRepository.lastLoggedInUser()
        val requestBody = body?.let {
            objectMapper.writeValueAsString(it).toRequestBody("application/json".toMediaType())
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .method(httpMethod, requestBody)

        user?.token?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return this.newCall(requestBuilder.build()).execute()
    }

    private fun createMapper(): ObjectMapper {
        val mapper = jacksonObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        return mapper
    }
}

@Suppress("unused")
class HttpResponse<T>(
    val statusCode: Int,
    val success: Boolean,
    val error: String?,
    private val nullableData: T?
) {
    // Data should be present unless there was an error or if no response was returned.
    // In either case the callers should know that the data is here if they are accessing it.
    // Don't make them !! all the time.
    val data: T get() = nullableData!!
}
