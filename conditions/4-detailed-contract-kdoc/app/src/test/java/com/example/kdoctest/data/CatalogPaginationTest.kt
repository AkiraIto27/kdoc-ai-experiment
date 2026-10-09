package com.example.kdoctest.data

import com.example.kdoctest.data.dto.PageInfoDto
import com.example.kdoctest.data.dto.ProductDto
import com.example.kdoctest.data.dto.ProductPageDto
import com.example.kdoctest.data.remote.catalogJson
import com.example.kdoctest.data.remote.createCatalogHttpClient
import com.example.kdoctest.data.repository.HttpProductRepository
import com.example.kdoctest.domain.model.Product
import com.example.kdoctest.domain.repository.CatalogException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPaginationTest {
    @Test
    fun all500ProductsArriveExactlyOnceInStableOrderAndFixtureIsCached() = runTest {
        val fixtureText = readCatalogFixture()
        val expected = catalogJson.decodeFromString<ProductPageDto>(fixtureText).items
            .sortedWith(compareByDescending<ProductDto> { Instant.parse(it.updatedAt) }.thenBy { it.id })
            .map { it.id }
        var reads = 0
        val client = createCatalogHttpClient({ reads++; fixtureText })
        assertEquals(0, reads)
        withTestClient(client) {
            val repository = HttpProductRepository(it)
            val items = mutableListOf<Product>()
            var cursor: String? = null
            repeat(10) { pageIndex ->
                val page = repository.getProducts(cursor, 50)
                assertEquals(50, page.items.size)
                assertEquals(500, page.totalCount)
                assertEquals("catalog-v1", page.snapshotId)
                assertTrue(page.responseBytes > 0)
                if (pageIndex == 9) assertNull(page.nextCursor) else assertNotNull(page.nextCursor)
                items += page.items
                cursor = page.nextCursor
            }
            assertEquals(500, items.size)
            assertEquals(500, items.map { product -> product.id }.distinct().size)
            assertEquals(expected, items.map { product -> product.id })
            assertEquals(1, reads)
        }
    }

    @Test
    fun equalUpdateInstantsUseAscendingIdsAcrossPageBoundaries() = runTest {
        val fixture = ProductPageDto(
            "catalog-v1",
            listOf(
                sampleProduct().copy(id = "P-003", updatedAt = "2026-09-11T09:00:00+09:00"),
                sampleProduct().copy(id = "P-002", updatedAt = "2026-09-11T00:00:00Z"),
                sampleProduct().copy(id = "P-001", updatedAt = "2026-09-12T00:00:00Z"),
            ),
            PageInfoDto(null, 3),
        )
        withTestClient(createCatalogHttpClient({ catalogJson.encodeToString(fixture) })) { client ->
            val repository = HttpProductRepository(client)
            val first = repository.getProducts(limit = 2)
            val second = repository.getProducts(first.nextCursor, 2)
            assertEquals(listOf("P-001", "P-002", "P-003"), (first.items + second.items).map { it.id })
            assertNull(second.nextCursor)
        }
    }

    @Test
    fun minimumMaximumAndPartialFinalPageAreSupported() = runTest {
        withTestClient(createCatalogHttpClient(::readCatalogFixture)) { client ->
            val repository = HttpProductRepository(client)
            assertEquals(1, repository.getProducts(limit = 1).items.size)
            assertEquals(100, repository.getProducts(limit = 100).items.size)
            var cursor: String? = null
            val ids = mutableListOf<String>()
            repeat(7) { pageIndex ->
                val page = repository.getProducts(cursor, 77)
                assertEquals(if (pageIndex == 6) 38 else 77, page.items.size)
                ids += page.items.map { it.id }
                cursor = page.nextCursor
            }
            assertNull(cursor)
            assertEquals(500, ids.size)
            assertEquals(500, ids.distinct().size)
        }
    }

    @Test
    fun invalidLimitsAreRejectedByRepositoryAndHttpEndpoint() = runTest {
        withTestClient(createCatalogHttpClient(::readCatalogFixture)) { client ->
            val repository = HttpProductRepository(client)
            for (limit in listOf(-1, 0, 101, Int.MAX_VALUE)) {
                expectFailure<CatalogException.InvalidRequest> { repository.getProducts(limit = limit) }
            }
            for (limit in listOf("-1", "0", "101", "not-a-number", "2147483648")) {
                val response = client.get("https://catalog.example.test/v1/products") { parameter("limit", limit) }
                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
            val duplicated = client.get("https://catalog.example.test/v1/products?limit=50&limit=50")
            assertEquals(HttpStatusCode.BadRequest, duplicated.status)
        }
    }

    @Test
    fun cursorsRejectTamperingChangedLimitAndAnotherSnapshot() = runTest {
        val fixture = readCatalogFixture()
        withTestClient(createCatalogHttpClient({ fixture })) { client ->
            val repository = HttpProductRepository(client)
            val cursor = repository.getProducts().nextCursor!!
            for (invalid in listOf("", "not-a-cursor", "x$cursor")) {
                expectFailure<CatalogException.InvalidRequest> { repository.getProducts(invalid) }
                val response = client.get("https://catalog.example.test/v1/products") {
                    parameter("limit", 50)
                    parameter("cursor", invalid)
                }
                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
            expectFailure<CatalogException.InvalidRequest> { repository.getProducts(cursor, 100) }
            withTestClient(createCatalogHttpClient({ fixture.replace("catalog-v1", "catalog-v2") })) { otherClient ->
                expectFailure<CatalogException.InvalidRequest> { HttpProductRepository(otherClient).getProducts(cursor) }
            }
        }
    }

    @Test
    fun responseBytesMeasureTheExactUtf8HttpBody() = runTest {
        withTestClient(createCatalogHttpClient({ samplePageJson() })) { client ->
            val bytes = client.get("https://catalog.example.test/v1/products?limit=50").body<ByteArray>()
            val page = HttpProductRepository(client).getProducts()
            assertEquals(bytes.size, page.responseBytes)
            assertTrue(bytes.size > bytes.toString(Charsets.UTF_8).length)
        }
    }

    @Test
    fun concurrentRequestsShareOneFixtureRead() = runTest {
        var reads = 0
        withTestClient(createCatalogHttpClient({ reads++; delay(1); samplePageJson() })) { client ->
            val repository = HttpProductRepository(client)
            val results = List(8) { async { repository.getProducts() } }.awaitAll()
            assertEquals(1, reads)
            assertTrue(results.all { it.items.single().id == "P-001" })
        }
    }
}
