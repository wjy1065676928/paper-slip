package io.github.wjy.meditate.network

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.io.File

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
