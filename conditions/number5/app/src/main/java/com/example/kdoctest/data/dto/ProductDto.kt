package com.example.kdoctest.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductPageDto(
    val snapshotId: String,
    val items: List<ProductDto>,
    val pageInfo: PageInfoDto,
)

@Serializable
data class PageInfoDto(val nextCursor: String?, val totalCount: Int)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val category: ProductCategoryDto,
    val price: ProductPriceDto,
    val salesUnit: SalesUnitDto,
    val availability: AvailabilityDto,
    val specifications: ProductSpecificationsDto,
    val variants: List<ProductVariantDto>,
    val warehouseStocks: List<WarehouseStockDto>,
    val tags: List<String>,
    val replacementProductId: String?,
    val updatedAt: String,
)

@Serializable
data class ProductCategoryDto(val id: String, val name: String)

@Serializable
data class ProductPriceDto(val amountYen: Long, val taxIncluded: Boolean)

@Serializable
data class SalesUnitDto(
    val type: String,
    val piecesPerUnit: Int,
    val minimumOrderUnits: Int,
)

@Serializable
data class AvailabilityDto(
    val status: String,
    val stockUnits: Int?,
    val restockAt: String?,
)

@Serializable
data class DimensionsMmDto(val width: Int, val depth: Int, val height: Int)

@Serializable
data class ProductAttributeDto(val name: String, val value: String)

@Serializable
data class ProductSpecificationsDto(
    val weightGrams: Int,
    val dimensionsMm: DimensionsMmDto,
    val attributes: List<ProductAttributeDto>,
    val careInstructions: List<String>,
)

@Serializable
data class ProductVariantDto(
    val id: String,
    val label: String,
    val additionalPriceYen: Long,
    val attributes: List<ProductAttributeDto>,
)

@Serializable
data class WarehouseStockDto(
    val warehouseId: String,
    val name: String,
    val stockUnits: Int?,
    val leadTimeDays: Int?,
)
