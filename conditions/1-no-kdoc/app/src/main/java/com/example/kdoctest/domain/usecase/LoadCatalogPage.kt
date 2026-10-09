package com.example.kdoctest.domain.usecase

import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.ProductRepository

class LoadCatalogPage(private val repository: ProductRepository) {
    suspend operator fun invoke(cursor: String? = null, limit: Int = 50): ProductPage {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return repository.getProducts(cursor, limit)
    }
}
