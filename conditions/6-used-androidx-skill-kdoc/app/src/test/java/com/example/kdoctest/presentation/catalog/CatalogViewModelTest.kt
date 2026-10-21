package com.example.kdoctest.presentation.catalog

import androidx.lifecycle.ViewModelStore
import com.example.kdoctest.domain.model.Availability
import com.example.kdoctest.domain.model.AvailabilityStatus
import com.example.kdoctest.domain.model.DimensionsMm
import com.example.kdoctest.domain.model.Product
import com.example.kdoctest.domain.model.ProductCategory
import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.model.ProductPrice
import com.example.kdoctest.domain.model.ProductSpecifications
import com.example.kdoctest.domain.model.SalesUnit
import com.example.kdoctest.domain.model.SalesUnitType
import com.example.kdoctest.domain.repository.CatalogException
import com.example.kdoctest.domain.repository.ProductRepository
import com.example.kdoctest.domain.usecase.LoadCatalogPage
import java.time.Instant
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load requests fifty items and publishes page state`() = runTest {
        val pending = CompletableDeferred<ProductPage>()
        val repository = FakeRepository { _, _ -> pending.await() }
        val viewModel = createViewModel(repository)

        assertTrue(viewModel.uiState.value.isInitialLoading)
        runCurrent()
        assertEquals(listOf(null to 50), repository.requests)

        pending.complete(page())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(50, state.items.size)
        assertEquals(120, state.totalCount)
        assertTrue(state.hasMore)
        assertFalse(state.isLoading)
        assertNull(state.initialError)
    }

    @Test
    fun `initial failure retries the first page`() = runTest {
        val repository = FakeRepository { _, _ -> throw CatalogException.Unavailable() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.initialError)
        assertNull(viewModel.uiState.value.loadMoreError)

        repository.response = { _, _ -> page() }
        viewModel.retry()
        assertTrue(viewModel.uiState.value.isInitialLoading)
        advanceUntilIdle()

        assertEquals(listOf(null to 50, null to 50), repository.requests)
        assertEquals(50, viewModel.uiState.value.items.size)
        assertNull(viewModel.uiState.value.initialError)
    }

    @Test
    fun `next page appends items in order and stops at total count`() = runTest {
        val repository = FakeRepository { cursor, _ ->
            if (cursor == null) page(total = 100) else page(51..100, total = 100, next = null)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        assertTrue(viewModel.uiState.value.isLoadingMore)
        assertEquals(50, viewModel.uiState.value.items.size)
        advanceUntilIdle()

        assertEquals((1..100).map { "product-$it" }, viewModel.uiState.value.items.map { it.id })
        assertEquals(100, viewModel.uiState.value.totalCount)
        assertFalse(viewModel.uiState.value.hasMore)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(listOf(null to 50, "page-2" to 50), repository.requests)
    }

    @Test
    fun `next page failure keeps current items and retries the same cursor`() = runTest {
        var nextAttempts = 0
        val repository = FakeRepository { cursor, _ ->
            if (cursor == null) {
                page()
            } else {
                nextAttempts++
                if (nextAttempts == 1) throw CatalogException.Unavailable()
                page(51..100, next = "page-3")
            }
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        val originalItems = viewModel.uiState.value.items

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(originalItems, viewModel.uiState.value.items)
        assertEquals(120, viewModel.uiState.value.totalCount)
        assertTrue(viewModel.uiState.value.hasMore)
        assertNotNull(viewModel.uiState.value.loadMoreError)
        assertNull(viewModel.uiState.value.initialError)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(null to 50, "page-2" to 50, "page-2" to 50), repository.requests)
        assertEquals(100, viewModel.uiState.value.items.size)
        assertNull(viewModel.uiState.value.loadMoreError)
    }

    @Test
    fun `repeated next page actions do not create duplicate requests`() = runTest {
        val pending = CompletableDeferred<ProductPage>()
        val repository = FakeRepository { cursor, _ ->
            if (cursor == null) page() else pending.await()
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        viewModel.loadNextPage()
        viewModel.retry()
        runCurrent()
        viewModel.loadNextPage()
        assertEquals(2, repository.requests.size)

        pending.complete(page(51..100, next = "page-3"))
        advanceUntilIdle()
        assertEquals(100, viewModel.uiState.value.items.size)
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `refresh cancels append keeps visible items and replaces the catalog`() = runTest {
        val appendResponse = CompletableDeferred<ProductPage>()
        val refreshResponse = CompletableDeferred<ProductPage>()
        var firstPageRequests = 0
        var appendCancelled = false
        val repository = FakeRepository { cursor, _ ->
            if (cursor == null) {
                firstPageRequests++
                if (firstPageRequests == 1) page() else refreshResponse.await()
            } else {
                try {
                    appendResponse.await()
                } catch (error: CancellationException) {
                    appendCancelled = true
                    throw error
                }
            }
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        val originalItems = viewModel.uiState.value.items
        viewModel.loadNextPage()
        runCurrent()

        viewModel.refresh()
        runCurrent()

        assertTrue(appendCancelled)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertEquals(originalItems, viewModel.uiState.value.items)
        assertNull(viewModel.uiState.value.loadMoreError)

        refreshResponse.complete(page(201..250, total = 50, next = null))
        advanceUntilIdle()

        assertEquals((201..250).map { "product-$it" }, viewModel.uiState.value.items.map { it.id })
        assertEquals(50, viewModel.uiState.value.totalCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasMore)
        assertEquals(listOf(null to 50, "page-2" to 50, null to 50), repository.requests)
    }

    @Test
    fun `response from a cancellation ignoring old request cannot overwrite refresh`() = runTest {
        lateinit var oldResponse: Continuation<ProductPage>
        var firstPageRequests = 0
        val repository = FakeRepository { cursor, _ ->
            if (cursor == null) {
                firstPageRequests++
                if (firstPageRequests == 1) page() else page(201..250, total = 50, next = null)
            } else {
                suspendCoroutine { oldResponse = it }
            }
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        viewModel.loadNextPage()
        runCurrent()
        viewModel.refresh()
        advanceUntilIdle()
        val refreshedState = viewModel.uiState.value

        oldResponse.resume(page(51..100, next = "page-3"))
        advanceUntilIdle()

        assertEquals(refreshedState, viewModel.uiState.value)
        assertEquals("product-201", viewModel.uiState.value.items.first().id)
    }

    @Test
    fun `failed refresh preserves products and retry refreshes instead of appending`() = runTest {
        val repository = FakeRepository { _, _ -> page() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        val originalItems = viewModel.uiState.value.items

        repository.response = { _, _ -> throw CatalogException.InvalidResponse() }
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(originalItems, viewModel.uiState.value.items)
        assertNotNull(viewModel.uiState.value.refreshError)
        assertNull(viewModel.uiState.value.initialError)
        assertNull(viewModel.uiState.value.loadMoreError)

        repository.response = { _, _ -> page(201..250, total = 50, next = null) }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(null to 50, null to 50, null to 50), repository.requests)
        assertEquals("product-201", viewModel.uiState.value.items.first().id)
        assertNull(viewModel.uiState.value.refreshError)
    }

    @Test
    fun `empty catalog finishes loading without pagination or error`() = runTest {
        val repository = FakeRepository { _, _ -> page(IntRange.EMPTY, total = 0, next = null) }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(CatalogUiState(), viewModel.uiState.value)
        viewModel.loadNextPage()
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `reaching reported total suppresses more even when a cursor is present`() = runTest {
        val repository = FakeRepository { _, _ -> page(total = 50, next = "unused") }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasMore)
        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(1, repository.requests.size)
    }

    private fun createViewModel(repository: ProductRepository): CatalogViewModel =
        CatalogViewModel(LoadCatalogPage(repository)).also { viewModelStore.put("catalog", it) }

    private class FakeRepository(
        var response: suspend (String?, Int) -> ProductPage,
    ) : ProductRepository {
        val requests = mutableListOf<Pair<String?, Int>>()

        override suspend fun getProducts(cursor: String?, limit: Int): ProductPage {
            requests += cursor to limit
            return response(cursor, limit)
        }
    }

    private fun page(
        ids: IntRange = 1..50,
        total: Int = 120,
        next: String? = "page-2",
    ) = ProductPage(
        items = ids.map(::product),
        nextCursor = next,
        totalCount = total,
        snapshotId = "catalog-v1",
        responseBytes = 1024,
    )

    private fun product(id: Int) = Product(
        id = "product-$id",
        name = "備品 $id",
        description = "毎日の仕事に使う備品です。",
        category = ProductCategory("office", "事務用品"),
        price = ProductPrice(amountYen = 1280, taxIncluded = false),
        salesUnit = SalesUnit(SalesUnitType.BOX, piecesPerUnit = 10, minimumOrderUnits = 1),
        availability = Availability(AvailabilityStatus.AVAILABLE, stockUnits = 24, restockAt = null),
        specifications = ProductSpecifications(
            weightGrams = 100,
            dimensionsMm = DimensionsMm(100, 100, 100),
            attributes = emptyList(),
            careInstructions = emptyList(),
        ),
        variants = emptyList(),
        warehouseStocks = emptyList(),
        tags = listOf("定番"),
        replacementProductId = null,
        updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
    )
}
