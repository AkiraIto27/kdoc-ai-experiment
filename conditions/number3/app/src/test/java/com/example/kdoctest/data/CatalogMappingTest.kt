package com.example.kdoctest.data

import com.example.kdoctest.data.repository.HttpProductRepository
import com.example.kdoctest.domain.model.AvailabilityStatus
import com.example.kdoctest.domain.model.SalesUnitType
import com.example.kdoctest.domain.repository.CatalogException
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.test.runTest

class CatalogMappingTest {
    @Test
    fun jsonMapsNestedTypesLongPricesInstantsAndNullableStocksWithoutLosingMeaning() = runTest {
        withTestClient(responseClient(samplePageJson())) { client ->
            val product = HttpProductRepository(client).getProducts().items.single()
            assertEquals("保存容器・大", product.name)
            assertEquals(4_000_000_000L, product.price.amountYen)
            assertEquals(true, product.price.taxIncluded)
            assertEquals(SalesUnitType.PACK, product.salesUnit.type)
            assertEquals(6, product.salesUnit.piecesPerUnit)
            assertEquals(2, product.salesUnit.minimumOrderUnits)
            assertEquals(AvailabilityStatus.UNKNOWN, product.availability.status)
            assertNull(product.availability.stockUnits)
            assertEquals(Instant.parse("2026-10-01T00:00:00Z"), product.availability.restockAt)
            assertEquals(Instant.parse("2026-09-12T01:02:03Z"), product.updatedAt)
            assertEquals(101, product.specifications.dimensionsMm.width)
            assertEquals(202, product.specifications.dimensionsMm.depth)
            assertEquals(303, product.specifications.dimensionsMm.height)
            assertEquals("樹脂", product.specifications.attributes.single().value)
            assertEquals(listOf("手洗い", "乾燥"), product.specifications.careInstructions)
            assertEquals(3_000_000_000L, product.variants.single().additionalPriceYen)
            assertEquals("青", product.variants.single().attributes.single().value)
            assertNull(product.warehouseStocks[0].stockUnits)
            assertNull(product.warehouseStocks[0].leadTimeDays)
            assertEquals(0, product.warehouseStocks[1].stockUnits)
            assertEquals(3, product.warehouseStocks[1].leadTimeDays)
            assertNull(product.replacementProductId)
        }
    }

    @Test
    fun unknownEnumsInvalidDatesAndWrongJsonTypesBecomeInvalidResponse() = runTest {
        val valid = samplePageJson()
        val invalidResponses = listOf(
            valid.replace("\"PACK\"", "\"PALLET\""),
            valid.replace("\"UNKNOWN\"", "\"PENDING\""),
            valid.replace("2026-09-12T01:02:03Z", "yesterday"),
            valid.replace("2026-10-01T00:00:00Z", "2026-13-01T00:00:00Z"),
            valid.replace("4000000000", "1.5"),
            valid.replace("\"stockUnits\":null", "\"stockUnits\":\"unknown\""),
            valid.replace("\"totalCount\":1", "\"totalCount\":-1"),
            "{\"snapshotId\":\"catalog-v1\"}",
            "this is not JSON",
        )
        for (invalid in invalidResponses) {
            withTestClient(responseClient(invalid)) { client ->
                expectFailure<CatalogException.InvalidResponse> { HttpProductRepository(client).getProducts() }
            }
        }
    }
}
