package com.example.kdoctest.domain.model

import java.time.Instant

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val category: ProductCategory,
    val price: ProductPrice,
    val salesUnit: SalesUnit,
    val availability: Availability,
    val specifications: ProductSpecifications,
    val variants: List<ProductVariant>,
    val warehouseStocks: List<WarehouseStock>,
    val tags: List<String>,
    val replacementProductId: String?,
    val updatedAt: Instant,
)

data class ProductCategory(val id: String, val name: String)

data class ProductPrice(val amountYen: Long, val taxIncluded: Boolean)

enum class SalesUnitType { PIECE, PACK, BOX }

data class SalesUnit(
    val type: SalesUnitType,
    val piecesPerUnit: Int,
    val minimumOrderUnits: Int,
)

enum class AvailabilityStatus { AVAILABLE, OUT_OF_STOCK, UNKNOWN, DISCONTINUED }

data class Availability(
    val status: AvailabilityStatus,
    val stockUnits: Int?,
    val restockAt: Instant?,
)

data class DimensionsMm(val width: Int, val depth: Int, val height: Int)

data class ProductAttribute(val name: String, val value: String)

data class ProductSpecifications(
    val weightGrams: Int,
    val dimensionsMm: DimensionsMm,
    val attributes: List<ProductAttribute>,
    val careInstructions: List<String>,
)

data class ProductVariant(
    val id: String,
    val label: String,
    val additionalPriceYen: Long,
    val attributes: List<ProductAttribute>,
)

data class WarehouseStock(
    val warehouseId: String,
    val name: String,
    val stockUnits: Int?,
    val leadTimeDays: Int?,
)

data class ProductPage(
    val items: List<Product>,
    val nextCursor: String?,
    val totalCount: Int,
    val snapshotId: String,
    val responseBytes: Int,
)
