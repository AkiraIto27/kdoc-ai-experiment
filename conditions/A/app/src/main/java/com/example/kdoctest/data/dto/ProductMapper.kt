package com.example.kdoctest.data.dto

import com.example.kdoctest.domain.model.Availability
import com.example.kdoctest.domain.model.AvailabilityStatus
import com.example.kdoctest.domain.model.DimensionsMm
import com.example.kdoctest.domain.model.Product
import com.example.kdoctest.domain.model.ProductAttribute
import com.example.kdoctest.domain.model.ProductCategory
import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.model.ProductPrice
import com.example.kdoctest.domain.model.ProductSpecifications
import com.example.kdoctest.domain.model.ProductVariant
import com.example.kdoctest.domain.model.SalesUnit
import com.example.kdoctest.domain.model.SalesUnitType
import com.example.kdoctest.domain.model.WarehouseStock
import java.time.Instant

internal fun ProductPageDto.toDomain(responseBytes: Int, requestedLimit: Int): ProductPage {
    require(snapshotId.isNotBlank())
    require(pageInfo.totalCount >= items.size)
    require(items.size <= requestedLimit)
    require(items.map { it.id }.distinct().size == items.size)
    require(pageInfo.nextCursor == null || (pageInfo.nextCursor.isNotBlank() && items.isNotEmpty()))
    return ProductPage(
        items = items.map(ProductDto::toDomain),
        nextCursor = pageInfo.nextCursor,
        totalCount = pageInfo.totalCount,
        snapshotId = snapshotId,
        responseBytes = responseBytes,
    )
}

internal fun ProductDto.toDomain(): Product = Product(
    id = id,
    name = name,
    description = description,
    category = ProductCategory(category.id, category.name),
    price = ProductPrice(price.amountYen, price.taxIncluded),
    salesUnit = SalesUnit(
        type = SalesUnitType.valueOf(salesUnit.type),
        piecesPerUnit = salesUnit.piecesPerUnit,
        minimumOrderUnits = salesUnit.minimumOrderUnits,
    ),
    availability = Availability(
        status = AvailabilityStatus.valueOf(availability.status),
        stockUnits = availability.stockUnits,
        restockAt = availability.restockAt?.let(Instant::parse),
    ),
    specifications = ProductSpecifications(
        weightGrams = specifications.weightGrams,
        dimensionsMm = specifications.dimensionsMm.let { DimensionsMm(it.width, it.depth, it.height) },
        attributes = specifications.attributes.map(ProductAttributeDto::toDomain),
        careInstructions = specifications.careInstructions,
    ),
    variants = variants.map {
        ProductVariant(it.id, it.label, it.additionalPriceYen, it.attributes.map(ProductAttributeDto::toDomain))
    },
    warehouseStocks = warehouseStocks.map {
        WarehouseStock(it.warehouseId, it.name, it.stockUnits, it.leadTimeDays)
    },
    tags = tags,
    replacementProductId = replacementProductId,
    updatedAt = Instant.parse(updatedAt),
)

private fun ProductAttributeDto.toDomain(): ProductAttribute = ProductAttribute(name, value)
