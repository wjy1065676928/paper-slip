package io.github.wjy.meditate.network

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebDavClient(
    url: String,
    user: String,
    pass: String,
    private val ignoreCert: Boolean = false
) {
    private val client = HttpClient(OkHttp) {
        if (ignoreCert) {
            engine {
                config {
                    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })

                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, SecureRandom())
                    sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    hostnameVerifier { _, _ -> true }
                }
            }
        }
    }
    
    // 确保 URL 以 / 结尾，避免拼接错误
    private val baseUrl = if (url.endsWith("/")) url else "$url/"

    private val authHeader = "Basic " + Base64.encodeToString(
        "$user:$pass".toByteArray(),
        Base64.NO_WRAP
    )

    suspend fun testConnection(): Result<Unit> {
        return try {
            val response: HttpResponse = client.request(baseUrl) {
                method = HttpMethod("PROPFIND")
                header(HttpHeaders.Authorization, authHeader)
                header("Depth", "0")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(remotePath: String, file: File): Result<Unit> {
        return try {
            val response: HttpResponse = client.put("$baseUrl$remotePath") {
                header(HttpHeaders.Authorization, authHeader)
                setBody(file.readBytes())
            }
            if (response.status.isSuccess()) Result.success(Unit) 
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(remotePath: String, targetFile: File): Result<Unit> {
        return try {
            val response: HttpResponse = client.get("$baseUrl$remotePath") {
                header(HttpHeaders.Authorization, authHeader)
            }
            if (response.status.isSuccess()) {
                targetFile.writeBytes(response.bodyAsBytes())
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
