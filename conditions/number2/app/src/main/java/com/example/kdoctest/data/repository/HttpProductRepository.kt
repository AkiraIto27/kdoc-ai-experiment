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
 * HTTPの商品リポジトリ。
 *
 * @param client HTTPクライアント。
 */
class HttpProductRepository(private val client: HttpClient) : ProductRepository {
    /**
     * 商品を取得する。
     *
     * @param cursor カーソル。
     * @param limit 件数。
     * @return 商品ページ。
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
