package com.example.kdoctest.data.pages

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

data class CatalogHttpResponse(val statusCode: Int, val body: ByteArray)

fun interface CatalogHttpTransport {
    suspend fun get(url: String): CatalogHttpResponse
}

/** GET transport using the platform connection API, with cancellable suspension. */
class UrlConnectionCatalogTransport(
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 10_000,
    private val maximumBodyBytes: Int = 4 * 1024 * 1024,
) : CatalogHttpTransport {
    init {
        require(connectTimeoutMillis > 0)
        require(readTimeoutMillis > 0)
        require(maximumBodyBytes > 0)
    }

    override suspend fun get(url: String): CatalogHttpResponse = suspendCancellableCoroutine { continuation ->
        val activeConnection = AtomicReference<HttpURLConnection?>(null)
        val worker = thread(start = false, isDaemon = true, name = "catalog-pages-get") {
            val outcome = runCatching {
                if (!continuation.isActive) throw CancellationException("Catalog GET cancelled")
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                activeConnection.set(connection)
                try {
                    if (!continuation.isActive) throw CancellationException("Catalog GET cancelled")
                    connection.requestMethod = "GET"
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = connectTimeoutMillis
                    connection.readTimeout = readTimeoutMillis
                    connection.useCaches = false
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Accept-Encoding", "identity")
                    val status = connection.responseCode
                    if (status !in 200..299) {
                        CatalogHttpResponse(status, byteArrayOf())
                    } else {
                        if (connection.contentLengthLong > maximumBodyBytes) {
                            throw IOException("Catalog response exceeds body limit")
                        }
                        val body = connection.inputStream.use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                if (!continuation.isActive || Thread.currentThread().isInterrupted) {
                                    throw CancellationException("Catalog GET cancelled")
                                }
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (output.size().toLong() + count > maximumBodyBytes) {
                                    throw IOException("Catalog response exceeds body limit")
                                }
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        CatalogHttpResponse(status, body)
                    }
                } finally {
                    activeConnection.compareAndSet(connection, null)
                    connection.disconnect()
                }
            }
            if (continuation.isActive) continuation.resumeWith(outcome)
        }
        continuation.invokeOnCancellation {
            worker.interrupt()
            activeConnection.getAndSet(null)?.let { connection ->
                // Some platform disconnect implementations block; never hold up cancellation.
                thread(isDaemon = true, name = "catalog-pages-disconnect") {
                    runCatching { connection.disconnect() }
                }
            }
        }
        if (continuation.isActive) worker.start()
    }
}
