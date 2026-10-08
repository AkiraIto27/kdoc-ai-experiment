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
 * 注入HTTPクライアントで商品ページを取得し、JSONをドメインモデルへ変換する。
 *
 * @param client 取得用クライアント。生成・終了は呼び出し側が管理する。
 *
 * 背景: 比較の再現性と外部依存の少なさを優先し、共通アプリでは固定JSONを返すKtor MockEngineを注入する。JSON解析・変換まで通し、ソケット・TLS・DNSは評価対象外とする。
 */
class HttpProductRepository(private val client: HttpClient) : ProductRepository {
    /**
     * `GET https://catalog.example.test/v1/products`で指定ページを取得する。
     * 本文をUTF-8・JSONとして読み、ページ条件を検証してドメインモデルへ変換する。
     *
     * @param cursor 前回の次カーソル。nullはクエリに含めず先頭を要求する。
     * @param limit 件数上限（1..100）。範囲外ではHTTP要求しない。
     * @return 商品・次カーソル・全件数・スナップショットID・本文バイト数。空一覧もあり、次カーソルnullで終端。
     * @throws CatalogException.InvalidRequest [limit]範囲外、またはHTTP 400。
     * @throws CatalogException.Unavailable 要求・本文取得失敗、またはHTTP 400以外の非2xx。
     * @throws CatalogException.InvalidResponse 本文解釈・モデル変換・変換時検証の失敗。
     * @throws CancellationException 取得のキャンセル。変換せず再送出する。
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
