package io.github.wjy.meditate.network

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebDavClient(
    url: String,
    user: String,
    pass: String,
    private val ignoreCert: Boolean = false
) {
    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 60000
        }
        engine {
            if (ignoreCert) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                
                sslManager = { connection ->
                    connection.sslSocketFactory = sslContext.socketFactory
                    connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
            }
        }
    }
    
    private val baseUrl = if (url.endsWith("/")) url else "$url/"

    private val authHeader = "Basic " + Base64.encodeToString(
        "$user:$pass".toByteArray(),
        Base64.NO_WRAP
    )

    suspend fun testConnection(): Result<Unit> {
        return try {
            val response: HttpResponse = client.request(baseUrl) {
                method = HttpMethod.Options
                header(HttpHeaders.Authorization, authHeader)
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

    /**
     * 流式上传：直接从文件读取 Channel 发送到网络，不占用大内存
     */
    suspend fun uploadFile(remotePath: String, file: File): Result<Unit> {
        return try {
            val channel: ByteReadChannel = file.inputStream().toByteReadChannel()
            val response: HttpResponse = client.put("$baseUrl$remotePath") {
                header(HttpHeaders.Authorization, authHeader)
                setBody(channel)
            }
            if (response.status.isSuccess()) Result.success(Unit) 
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 流式下载：将网络流直接写入文件输出流，避免 bodyAsBytes 导致的 OOM
     */
    suspend fun downloadFile(remotePath: String, targetFile: File): Result<Unit> {
        return try {
            client.prepareGet("$baseUrl$remotePath") {
                header(HttpHeaders.Authorization, authHeader)
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel: ByteReadChannel = response.body()
                    targetFile.outputStream().use { output ->
                        channel.copyTo(output)
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.status.value}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
