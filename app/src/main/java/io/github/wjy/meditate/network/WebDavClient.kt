package io.github.wjy.meditate.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import android.util.Base64

class WebDavClient(
    private val url: String,
    private val user: String,
    private val pass: String
) {
    private val client = HttpClient(OkHttp)

    private val authHeader = "Basic " + Base64.encodeToString(
        "$user:$pass".toByteArray(),
        Base64.NO_WRAP
    )

    suspend fun uploadFile(remotePath: String, file: File): Boolean {
        return try {
            val response: HttpResponse = client.put("$url/$remotePath") {
                header(HttpHeaders.Authorization, authHeader)
                setBody(file.readBytes())
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadFile(remotePath: String, targetFile: File): Boolean {
        return try {
            val response: HttpResponse = client.get("$url/$remotePath") {
                header(HttpHeaders.Authorization, authHeader)
            }
            if (response.status.isSuccess()) {
                targetFile.writeBytes(response.bodyAsBytes())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
