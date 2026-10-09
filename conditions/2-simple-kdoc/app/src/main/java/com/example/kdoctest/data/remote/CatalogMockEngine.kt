package com.example.kdoctest.data.remote

import com.example.kdoctest.data.dto.PageInfoDto
import com.example.kdoctest.data.dto.ProductDto
import com.example.kdoctest.data.dto.ProductPageDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

fun createCatalogHttpClient(
    readFixture: suspend () -> String,
    scenario: CatalogScenario = CatalogScenario.NORMAL,
): HttpClient {
    val source = CachedCatalogFixture(readFixture)
    val headers = headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8")
    return HttpClient(MockEngine) {
        expectSuccess = false
        engine {
            addHandler { request ->
                if (request.method != HttpMethod.Get || request.url.protocol != URLProtocol.HTTPS ||
                    request.url.host != "catalog.example.test" || request.url.encodedPath != "/v1/products"
                ) {
                    return@addHandler respond(
                        catalogJson.encodeToString(ServiceError("not_found")), HttpStatusCode.NotFound, headers,
                    )
                }
                val limits = request.url.parameters.getAll("limit")
                val cursors = request.url.parameters.getAll("cursor")
                val limit = if (limits == null) 50 else limits.singleOrNull()?.toIntOrNull()
                if (limit == null || limit !in 1..100 || (cursors != null && cursors.size != 1)) {
                    return@addHandler respond(
                        catalogJson.encodeToString(ServiceError("invalid_request")), HttpStatusCode.BadRequest, headers,
                    )
                }
                if (scenario == CatalogScenario.ERROR) {
                    return@addHandler respond(
                        catalogJson.encodeToString(ServiceError("unavailable")), HttpStatusCode.ServiceUnavailable, headers,
                    )
                }
                val fixture = if (scenario == CatalogScenario.EMPTY) {
                    ProductPageDto("catalog-v1", emptyList(), PageInfoDto(null, 0))
                } else {
                    source.read()
                }
                val cursor = cursors?.single()
                val offset = if (cursor == null) 0 else {
                    (limit until fixture.items.size step limit).firstOrNull {
                        catalogCursor(fixture.snapshotId, limit, it) == cursor
                    }
                }
                if (offset == null) {
                    return@addHandler respond(
                        catalogJson.encodeToString(ServiceError("invalid_cursor")), HttpStatusCode.BadRequest, headers,
                    )
                }
                if (scenario == CatalogScenario.NEXT_PAGE_ERROR && offset > 0) {
                    return@addHandler respond(
                        catalogJson.encodeToString(ServiceError("unavailable")), HttpStatusCode.ServiceUnavailable, headers,
                    )
                }
                val end = minOf(offset + limit, fixture.items.size)
                val page = ProductPageDto(
                    snapshotId = fixture.snapshotId,
                    items = fixture.items.subList(offset, end),
                    pageInfo = PageInfoDto(
                        nextCursor = if (end < fixture.items.size) catalogCursor(fixture.snapshotId, limit, end) else null,
                        totalCount = fixture.items.size,
                    ),
                )
                val body = catalogJson.encodeToString(page).toByteArray(Charsets.UTF_8)
                respond(ByteReadChannel(body), HttpStatusCode.OK, headers)
            }
        }
    }
}

private class CachedCatalogFixture(private val readFixture: suspend () -> String) {
    private val mutex = Mutex()
    private var cached: ProductPageDto? = null

    suspend fun read(): ProductPageDto = mutex.withLock {
        cached ?: catalogJson.decodeFromString<ProductPageDto>(readFixture()).let { fixture ->
            require(fixture.snapshotId.isNotBlank())
            require(fixture.pageInfo.nextCursor == null)
            require(fixture.pageInfo.totalCount == fixture.items.size)
            require(fixture.items.map(ProductDto::id).distinct().size == fixture.items.size)
            val sorted = fixture.items.sortedWith(
                compareByDescending<ProductDto> { Instant.parse(it.updatedAt) }.thenBy { it.id },
            )
            fixture.copy(items = sorted).also { cached = it }
        }
    }
}

@Serializable
private data class ServiceError(val error: String)
