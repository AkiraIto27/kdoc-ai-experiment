package com.example.kdoctest.data.repository

import com.example.kdoctest.data.dto.ProductPageDto
import com.example.kdoctest.data.dto.toDomain
import com.example.kdoctest.data.remote.catalogJson
import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.CatalogException
import com.example.kdoctest.domain.repository.ProductRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

/**
 * 商品ページをHTTPで取得してドメインモデルへ変換する。
 *
 * ```kotlin
 * val repository = HttpProductRepository(client)
 * val page = repository.getProducts(cursor = null, limit = 50)
 * ```
 *
 * - 注入された[client]で商品ページを取得し、JSONを[ProductPage]へ変換する
 *
 * @param client 取得用の[HttpClient]。生成・終了は呼び出し側が管理する
 */
class HttpProductRepository(private val client: HttpClient) : ProductRepository {
    /**
     * 指定した商品ページをHTTPで取得する。
     *
     * ```kotlin
     * val first = repository.getProducts(cursor = null, limit = 50)
     * val next = first.nextCursor?.let { repository.getProducts(cursor = it, limit = 50) }
     * ```
     *
     * - `GET https://catalog.example.test/v1/products`へ要求する
     * - 本文をUTF-8・JSONとして読み、ページ条件を検証して[ProductPage]へ変換する
     *
     * @param cursor 前回返された[ProductPage.nextCursor]。`null`ならクエリに含めず、先頭ページを要求する
     * @param limit 取得件数の上限（`1..100`件）。範囲外ではHTTP要求を行わない
     * @return 商品一覧・次カーソル・全件数・スナップショットID・本文サイズ（バイト）を持つ[ProductPage]。一覧が空の場合もあり、[ProductPage.nextCursor]が`null`なら次ページなし
     * @throws CatalogException.InvalidRequest [limit]が範囲外、またはHTTP 400の場合
     * @throws CatalogException.Unavailable 要求処理・応答本文の取得に失敗した場合、またはHTTP 400以外の非2xxの場合
     * @throws CatalogException.InvalidResponse 本文解釈・モデル変換・変換時の検証に失敗した場合
     * @throws CancellationException 取得をキャンセルした場合。例外を変換せず再送出する
     */
    override suspend fun getProducts(cursor: String?, limit: Int): ProductPage {
        if (limit !in 1..100) throw CatalogException.InvalidRequest("limit must be between 1 and 100")
        val response = try {
            client.get("https://catalog.example.test/v1/products") {
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw CatalogException.Unavailable(failure)
        }
        if (response.status == HttpStatusCode.BadRequest) {
            throw CatalogException.InvalidRequest("ページの取得条件が無効です。")
        }
        if (response.status.value !in 200..299) throw CatalogException.Unavailable()
        val bytes = try {
            response.body<ByteArray>()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw CatalogException.Unavailable(failure)
        }
        return try {
            val payload = bytes.decodeToString(throwOnInvalidSequence = true)
            catalogJson.decodeFromString<ProductPageDto>(payload).toDomain(bytes.size, limit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw CatalogException.InvalidResponse(failure)
        }
    }
}
