package com.example.kdoctest.domain.usecase

import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.ProductRepository

/**
 * 件数上限を検証後、商品ページ取得をリポジトリへ委譲する。
 *
 * 背景: domain層はdata・presentation層を参照しない構成で、具象の取得実装をRepository境界の外側に置く。
 * Why: 取得条件を検証するドメインの処理を、HTTPやJSONを扱う具体的な取得方法から分離するため。
 * How: 件数上限を検証し、カーソルと件数を[ProductRepository]へ渡す。HTTP実装やDTOを直接参照せず、取得結果をそのまま返す。
 *
 * @param repository カーソル解釈と商品ページ取得の実装。
 */
class LoadCatalogPage(private val repository: ProductRepository) {
    /**
     * 指定ページのリポジトリ結果を返す。取得中の例外・キャンセルはそのまま伝播する。
     *
     * @param cursor 前回の次カーソル。nullで先頭。
     * @param limit 件数上限（1..100、既定50）。
     * @return 商品とページ情報。空一覧もあり、次カーソルnullで終端。
     * @throws IllegalArgumentException [limit]が範囲外。リポジトリは呼ばない。
     */
    suspend operator fun invoke(cursor: String? = null, limit: Int = 50): ProductPage {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return repository.getProducts(cursor, limit)
    }
}
