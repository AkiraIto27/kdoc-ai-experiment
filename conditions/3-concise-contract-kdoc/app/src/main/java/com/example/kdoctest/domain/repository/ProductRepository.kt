package com.example.kdoctest.domain.repository

import com.example.kdoctest.domain.model.ProductPage

/**
 * 商品一覧をカーソルでページ取得する契約。次の取得には[ProductPage.nextCursor]を使う。
 */
interface ProductRepository {
    /**
     * 商品一覧の1ページを取得する。取得・例外・範囲外検証は実装に従う。
     *
     * @param cursor 前回の次カーソル。nullで先頭。
     * @param limit 件数上限（1..100、既定50）。
     * @return 商品とページ情報。空一覧もあり、次カーソルnullで終端。
     */
    suspend fun getProducts(cursor: String? = null, limit: Int = 50): ProductPage
}

/**
 * 商品取得の失敗を取得不能・要求不正・応答不正に分類する例外。
 *
 * @param message 失敗の説明。
 * @param cause 原因例外。なければnull。
 */
sealed class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * 取得先の異常や、要求・本文取得の失敗。
     *
     * @param cause 原因例外。HTTPステータスによる失敗など、原因がなければnull。
     */
    class Unavailable(cause: Throwable? = null) : CatalogException("商品を取得できませんでした。", cause)
    /**
     * 無効なページ取得条件を表す。
     *
     * @param message 無効な取得条件の説明。
     */
    class InvalidRequest(message: String) : CatalogException(message)
    /**
     * 受信商品データの解釈・モデル変換・検証の失敗。
     *
     * @param cause 原因例外。なければnull。
     */
    class InvalidResponse(cause: Throwable? = null) : CatalogException("商品データを読み取れませんでした。", cause)
}
