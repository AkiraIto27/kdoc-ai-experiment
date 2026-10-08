package com.example.kdoctest.domain.repository

import com.example.kdoctest.domain.model.ProductPage

/**
 * 商品リポジトリ。
 */
interface ProductRepository {
    /**
     * 商品を取得する。
     *
     * @param cursor カーソル。
     * @param limit 件数。
     * @return 商品ページ。
     */
    suspend fun getProducts(cursor: String? = null, limit: Int = 50): ProductPage
}

/**
 * カタログ例外。
 *
 * @param message メッセージ。
 * @param cause 原因。
 */
sealed class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * 利用不可例外。
     *
     * @param cause 原因。
     */
    class Unavailable(cause: Throwable? = null) : CatalogException("商品を取得できませんでした。", cause)
    /**
     * 不正なリクエスト例外。
     *
     * @param message メッセージ。
     */
    class InvalidRequest(message: String) : CatalogException(message)
    /**
     * 不正なレスポンス例外。
     *
     * @param cause 原因。
     */
    class InvalidResponse(cause: Throwable? = null) : CatalogException("商品データを読み取れませんでした。", cause)
}
