package com.example.kdoctest.domain.usecase

import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.ProductRepository

/**
 * カタログページ取得。
 *
 * @param repository リポジトリ。
 */
class LoadCatalogPage(private val repository: ProductRepository) {
    /**
     * 呼び出す。
     *
     * @param cursor カーソル。
     * @param limit 件数。
     * @return 商品ページ。
     */
    suspend operator fun invoke(cursor: String? = null, limit: Int = 50): ProductPage {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return repository.getProducts(cursor, limit)
    }
}
