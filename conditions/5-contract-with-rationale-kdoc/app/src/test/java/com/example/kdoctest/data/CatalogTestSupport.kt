package com.example.kdoctest.data

import com.example.kdoctest.data.dto.AvailabilityDto
import com.example.kdoctest.data.dto.DimensionsMmDto
import com.example.kdoctest.data.dto.PageInfoDto
import com.example.kdoctest.data.dto.ProductAttributeDto
import com.example.kdoctest.data.dto.ProductCategoryDto
import com.example.kdoctest.data.dto.ProductDto
import com.example.kdoctest.data.dto.ProductPageDto
import com.example.kdoctest.data.dto.ProductPriceDto
import com.example.kdoctest.data.dto.ProductSpecificationsDto
import com.example.kdoctest.data.dto.ProductVariantDto
import com.example.kdoctest.data.dto.SalesUnitDto
import com.example.kdoctest.data.dto.WarehouseStockDto
import com.example.kdoctest.data.remote.catalogJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.File
import kotlinx.serialization.encodeToString

internal fun readCatalogFixture(): String {
    val file = listOf(
        File("src/main/assets/catalog/products.json"),
        File("app/src/main/assets/catalog/products.json"),
    ).firstOrNull(File::isFile) ?: error("catalog/products.json fixture was not found")
    return file.readText(Charsets.UTF_8)
}

internal fun sampleProduct(): ProductDto = ProductDto(
    id = "P-001",
    name = "保存容器・大",
    description = "日本語の説明と🧰",
    category = ProductCategoryDto("storage", "収納"),
    price = ProductPriceDto(4_000_000_000L, true),
    salesUnit = SalesUnitDto("PACK", 6, 2),
    availability = AvailabilityDto("UNKNOWN", null, "2026-10-01T00:00:00Z"),
    specifications = ProductSpecificationsDto(
        weightGrams = 1234,
        dimensionsMm = DimensionsMmDto(101, 202, 303),
        attributes = listOf(ProductAttributeDto("素材", "樹脂")),
        careInstructions = listOf("手洗い", "乾燥"),
    ),
    variants = listOf(ProductVariantDto("P-001-B", "青", 3_000_000_000L, listOf(ProductAttributeDto("色", "青")))),
    warehouseStocks = listOf(
        WarehouseStockDto("east", "東倉庫", null, null),
        WarehouseStockDto("west", "西倉庫", 0, 3),
    ),
    tags = listOf("業務用", "セット"),
    replacementProductId = null,
    updatedAt = "2026-09-12T01:02:03Z",
)

internal fun samplePageJson(product: ProductDto = sampleProduct()): String = catalogJson.encodeToString(
    ProductPageDto("catalog-v1", listOf(product), PageInfoDto(null, 1)),
)

internal fun responseClient(body: String): HttpClient = HttpClient(MockEngine) {
    expectSuccess = false
    engine {
        addHandler {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8"))
        }
    }
}

internal suspend fun <T> withTestClient(client: HttpClient, block: suspend (HttpClient) -> T): T = try {
    block(client)
} finally {
    client.close()
}

internal suspend inline fun <reified T : Throwable> expectFailure(block: suspend () -> Unit): T {
    try {
        block()
    } catch (failure: Throwable) {
        if (failure is T) return failure
        throw AssertionError("Expected ${T::class.java.simpleName}, got ${failure::class.java.simpleName}", failure)
    }
    throw AssertionError("Expected ${T::class.java.simpleName}, but operation succeeded")
}
