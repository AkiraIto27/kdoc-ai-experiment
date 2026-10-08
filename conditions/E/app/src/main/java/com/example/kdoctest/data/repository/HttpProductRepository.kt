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
 * 注入されたHTTPクライアントで商品ページを取得し、JSONをドメインモデルへ変換する。
 *
 * @param client 取得に使用するクライアント。生成と終了は呼び出し側が管理する。
 */
class HttpProductRepository(private val client: HttpClient) : ProductRepository {
    /**
     * `GET https://catalog.example.test/v1/products`で指定されたページを取得する。
     * 応答本文をUTF-8とJSONとして読み、ページ条件の検証と商品モデルへの変換を行う。
     *
     * @param cursor 前回取得した次カーソル。nullならクエリへ含めず、先頭ページを要求する。
     * @param limit 取得件数の上限（1..100）。範囲外ならHTTP要求を行わない。
     * @return 商品、次カーソル、全件数、スナップショットID、応答本文のバイト数を持つページ。
     * 商品が空の場合もあり、次カーソルのnullは次ページがないことを表す。
     * @throws CatalogException.InvalidRequest [limit]が範囲外、または取得した応答がHTTP 400の場合。
     * @throws CatalogException.Unavailable 要求・本文取得の失敗、またはHTTP 400以外の非2xx応答の場合。
     * @throws CatalogException.InvalidResponse 本文の解釈、モデル変換、または変換時の検証に失敗した場合。
     * @throws CancellationException 取得処理がキャンセルされた場合。キャンセルは変換せず再送出する。
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
