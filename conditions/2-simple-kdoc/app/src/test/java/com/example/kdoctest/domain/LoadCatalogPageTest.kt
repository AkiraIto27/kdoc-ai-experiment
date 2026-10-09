package com.example.kdoctest.domain

import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.ProductRepository
import com.example.kdoctest.domain.usecase.LoadCatalogPage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadCatalogPageTest {
    @Test
    fun invalidPageSizeDoesNotReachRepository() = runTest {
        var calls = 0
        val repository = object : ProductRepository {
            override suspend fun getProducts(cursor: String?, limit: Int): ProductPage {
                calls++
                return ProductPage(emptyList(), null, 0, "catalog-v1", 0)
            }
        }
        val loadPage = LoadCatalogPage(repository)
        for (limit in listOf(0, -1, 101, Int.MAX_VALUE)) {
            try {
                loadPage(limit = limit)
                throw AssertionError("Expected invalid limit to be rejected")
            } catch (_: IllegalArgumentException) {
                // Expected input validation.
            }
        }
        assertEquals(0, calls)
    }
}
