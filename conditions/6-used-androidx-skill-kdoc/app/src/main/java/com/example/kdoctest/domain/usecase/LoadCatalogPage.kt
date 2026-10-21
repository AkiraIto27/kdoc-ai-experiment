package com.example.kdoctest.domain.usecase

import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.ProductRepository

/**
 * 商品ページの取得を[repository]へ委ねる。
 *
 * ```kotlin
 * val loadPage = LoadCatalogPage(repository)
 * val page = loadPage()
 * ```
 *
 * - [invoke]で取得件数の上限を検証してから、[ProductRepository.getProducts]へ取得を委ねる
 *
 * @param repository カーソルの解釈と商品ページ取得を担う[ProductRepository]
 */
class LoadCatalogPage(private val repository: ProductRepository) {
    /**
     * 指定ページの取得結果を返す。
     *
     * ```kotlin
     * val loadPage = LoadCatalogPage(repository)
     * val page = loadPage(cursor = null, limit = 50)
     * ```
     *
     * - [repository]の[ProductRepository.getProducts]が返す[ProductPage]をそのまま返す
     * - 取得中の例外とキャンセルはそのまま呼び出し元へ伝える
     *
     * @param cursor 前回返された[ProductPage.nextCursor]。`null`で先頭ページ
     * @param limit 取得件数の上限（`1..100`件、既定50件）
     * @return 商品一覧とページ情報を持つ[ProductPage]。一覧が空の場合もあり、[ProductPage.nextCursor]が`null`なら次ページなし
     * @throws IllegalArgumentException [limit]が`1..100`の範囲外の場合。この場合は[ProductRepository.getProducts]を呼ばない
     */
    suspend operator fun invoke(cursor: String? = null, limit: Int = 50): ProductPage {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return repository.getProducts(cursor, limit)
    }
}
