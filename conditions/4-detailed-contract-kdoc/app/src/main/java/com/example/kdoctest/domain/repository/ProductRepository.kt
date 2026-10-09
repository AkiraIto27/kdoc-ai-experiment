package com.example.kdoctest.domain.repository

import com.example.kdoctest.domain.model.ProductPage

/**
 * 商品一覧をカーソルでページ取得するための契約。
 * 呼び出し側は返された[ProductPage.nextCursor]を次の取得に使用する。
 */
interface ProductRepository {
    /**
     * 商品一覧の1ページを取得する。取得処理と例外の扱いは実装に従う。
     *
     * @param cursor 前回のページで返された次カーソル。nullなら先頭ページを取得する。
     * @param limit 取得件数の上限（1..100）。既定値は50で、範囲外の検証方法は実装に従う。
     * @return 商品一覧とページ情報。商品一覧が空の場合もあり、次カーソルのnullは次ページがないことを表す。
     */
    suspend fun getProducts(cursor: String? = null, limit: Int = 50): ProductPage
}

/**
 * 商品一覧の取得失敗を、取得不能・要求条件不正・応答データ不正に分類する例外。
 *
 * @param message 失敗内容を説明するメッセージ。
 * @param cause 元の原因例外。原因例外を持たない場合はnull。
 */
sealed class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * 取得先の異常や、要求・応答本文の取得失敗を表す。
     *
     * @param cause 元の原因例外。HTTPステータスによる失敗など、原因例外がなければnull。
     */
    class Unavailable(cause: Throwable? = null) : CatalogException("商品を取得できませんでした。", cause)
    /**
     * ページ取得の条件が無効であることを表す。
     *
     * @param message 無効な取得条件を説明するメッセージ。
     */
    class InvalidRequest(message: String) : CatalogException(message)
    /**
     * 受信した商品データの解釈・モデル変換・検証の失敗を表す。
     *
     * @param cause 元の原因例外。原因例外を持たない場合はnull。
     */
    class InvalidResponse(cause: Throwable? = null) : CatalogException("商品データを読み取れませんでした。", cause)
}
