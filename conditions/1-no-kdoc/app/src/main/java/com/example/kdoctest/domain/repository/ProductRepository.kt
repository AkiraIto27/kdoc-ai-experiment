package com.example.kdoctest.domain.repository

import com.example.kdoctest.domain.model.ProductPage

interface ProductRepository {
    suspend fun getProducts(cursor: String? = null, limit: Int = 50): ProductPage
}

sealed class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Unavailable(cause: Throwable? = null) : CatalogException("商品を取得できませんでした。", cause)
    class InvalidRequest(message: String) : CatalogException(message)
    class InvalidResponse(cause: Throwable? = null) : CatalogException("商品データを読み取れませんでした。", cause)
}
