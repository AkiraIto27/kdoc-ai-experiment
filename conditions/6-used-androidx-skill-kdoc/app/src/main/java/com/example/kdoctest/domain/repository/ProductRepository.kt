package com.example.kdoctest.domain.repository

import com.example.kdoctest.domain.model.ProductPage

/**
 * 商品一覧をカーソルでページ取得する契約を定める。
 *
 * ```kotlin
 * val first = repository.getProducts()
 * val next = first.nextCursor?.let { repository.getProducts(cursor = it) }
 * ```
 *
 * - 次の取得には、返された[ProductPage.nextCursor]を[getProducts]へ渡す
 */
interface ProductRepository {
    /**
     * 商品一覧を1ページ取得する。
     *
     * ```kotlin
     * val first = repository.getProducts()
     * val next = first.nextCursor?.let { repository.getProducts(cursor = it) }
     * ```
     *
     * - 取得処理・例外の扱い・[limit]が範囲外の場合の検証方法は実装に従う
     *
     * @param cursor 前回返された[ProductPage.nextCursor]。`null`で先頭ページ
     * @param limit 取得件数の上限（`1..100`件、既定50件）
     * @return 商品一覧とページ情報を持つ[ProductPage]。一覧が空の場合もあり、[ProductPage.nextCursor]が`null`なら次ページなし
     */
    suspend fun getProducts(cursor: String? = null, limit: Int = 50): ProductPage
}

/**
 * 商品一覧の取得失敗を分類する。
 *
 * ```kotlin
 * val failure: CatalogException = CatalogException.Unavailable()
 * throw failure
 * ```
 *
 * - [CatalogException.Unavailable]: 取得不能
 * - [CatalogException.InvalidRequest]: 要求条件の不正
 * - [CatalogException.InvalidResponse]: 応答データの不正
 *
 * @param message 失敗内容の説明
 * @param cause 元の原因例外。なければ`null`（既定）
 */
sealed class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * 取得先の異常や取得失敗を示す。
     *
     * ```kotlin
     * val failure = CatalogException.Unavailable(cause = null)
     * throw failure
     * ```
     *
     * - 要求処理や応答本文の取得の失敗を含む
     *
     * @param cause 元の原因例外。HTTPステータスによる失敗など、原因例外がなければ`null`（既定）
     */
    class Unavailable(cause: Throwable? = null) : CatalogException("商品を取得できませんでした。", cause)
    /**
     * ページ取得条件の不正を示す。
     *
     * ```kotlin
     * val failure = CatalogException.InvalidRequest("取得件数が無効です")
     * throw failure
     * ```
     *
     * @param message 無効な取得条件の説明
     */
    class InvalidRequest(message: String) : CatalogException(message)
    /**
     * 受信商品データの読み取り失敗を示す。
     *
     * ```kotlin
     * val failure = CatalogException.InvalidResponse(cause = null)
     * throw failure
     * ```
     *
     * - 受信商品データの解釈・モデル変換・検証の失敗を含む
     *
     * @param cause 元の原因例外。なければ`null`（既定）
     */
    class InvalidResponse(cause: Throwable? = null) : CatalogException("商品データを読み取れませんでした。", cause)
}
