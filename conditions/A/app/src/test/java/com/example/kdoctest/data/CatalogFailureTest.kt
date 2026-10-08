package com.example.kdoctest.data

import com.example.kdoctest.data.remote.CatalogScenario
import com.example.kdoctest.data.remote.createCatalogHttpClient
import com.example.kdoctest.data.repository.HttpProductRepository
import com.example.kdoctest.domain.repository.CatalogException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFailureTest {
    @Test
    fun emptyScenarioReturnsASuccessfulEmptySnapshotWithoutLoadingFixture() = runTest {
        withTestClient(createCatalogHttpClient({ error("must remain lazy") }, CatalogScenario.EMPTY)) { client ->
            val page = HttpProductRepository(client).getProducts()
            assertTrue(page.items.isEmpty())
            assertEquals(0, page.totalCount)
            assertEquals("catalog-v1", page.snapshotId)
            assertNull(page.nextCursor)
            assertTrue(page.responseBytes > 0)
        }
    }

    @Test
    fun errorScenarioAlwaysReportsUnavailableWithoutLoadingFixture() = runTest {
        withTestClient(createCatalogHttpClient({ error("must remain lazy") }, CatalogScenario.ERROR)) { client ->
            val repository = HttpProductRepository(client)
            repeat(2) { expectFailure<CatalogException.Unavailable> { repository.getProducts() } }
        }
    }

    @Test
    fun nextPageErrorKeepsFirstPageSuccessfulAndFailsEachFollowingAttempt() = runTest {
        withTestClient(createCatalogHttpClient(::readCatalogFixture, CatalogScenario.NEXT_PAGE_ERROR)) { client ->
            val repository = HttpProductRepository(client)
            val first = repository.getProducts()
            assertEquals(50, first.items.size)
            repeat(2) {
                expectFailure<CatalogException.Unavailable> { repository.getProducts(first.nextCursor!!) }
            }
            assertEquals(first.items, repository.getProducts().items)
        }
    }

    @Test
    fun transportFailureBecomesUnavailable() = runTest {
        val failingClient = HttpClient(MockEngine) {
            engine { addHandler { throw IOException("simulated transport failure") } }
        }
        withTestClient(failingClient) { client ->
            expectFailure<CatalogException.Unavailable> { HttpProductRepository(client).getProducts() }
        }
    }

    @Test
    fun cancellingAnInFlightFixtureReadPropagatesCancellation() = runTest {
        val started = CompletableDeferred<Unit>()
        val observed = CompletableDeferred<Throwable>()
        withTestClient(createCatalogHttpClient({ started.complete(Unit); awaitCancellation() })) { client ->
            val request = launch {
                try {
                    HttpProductRepository(client).getProducts()
                } catch (failure: Throwable) {
                    observed.complete(failure)
                    throw failure
                }
            }
            started.await()
            request.cancelAndJoin()
            assertTrue(observed.await() is CancellationException)
        }
    }
}
