package com.example.kdoctest.domain.usecase

import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.ProductRepository

/**
 * 取得件数の上限を検証してから、商品ページの取得をリポジトリへ委譲する。
 *
 * @param repository カーソルの解釈と商品ページの取得を担う実装。
 */
class LoadCatalogPage(private val repository: ProductRepository) {
    /**
     * 指定されたページを取得し、リポジトリの結果を返す。取得中の例外やキャンセルはそのまま伝播する。
     *
     * @param cursor 前回のページで返された次カーソル。nullなら先頭ページを取得する。
     * @param limit 取得件数の上限。1..100の範囲で、既定値は50。
     * @return 商品一覧とページ情報。商品一覧が空の場合もあり、次カーソルのnullは次ページがないことを表す。
     * @throws IllegalArgumentException [limit]が範囲外の場合。この場合はリポジトリを呼び出さない。
     */
    suspend operator fun invoke(cursor: String? = null, limit: Int = 50): ProductPage {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return repository.getProducts(cursor, limit)
    }
}
