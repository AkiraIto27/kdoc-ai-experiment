package com.example.kdoctest.presentation.catalog

import com.example.kdoctest.domain.model.Availability
import com.example.kdoctest.domain.model.AvailabilityStatus
import com.example.kdoctest.domain.model.ProductPrice
import com.example.kdoctest.domain.model.SalesUnit
import com.example.kdoctest.domain.model.SalesUnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductFormatterTest {
    private val box = SalesUnit(SalesUnitType.BOX, piecesPerUnit = 10, minimumOrderUnits = 1)

    @Test
    fun `yen amounts keep whole yen and thousands separators`() {
        assertEquals("1,280円", ProductFormatter.price(ProductPrice(1280, taxIncluded = false)))
        assertEquals("1,234,567円", ProductFormatter.price(ProductPrice(1234567, taxIncluded = true)))
        assertEquals("0円", ProductFormatter.price(ProductPrice(0, taxIncluded = true)))
    }

    @Test
    fun `tax labels follow the supplied price without calculating tax`() {
        assertEquals("税抜", ProductFormatter.taxLabel(ProductPrice(1280, taxIncluded = false)))
        assertEquals("税込", ProductFormatter.taxLabel(ProductPrice(1280, taxIncluded = true)))
    }

    @Test
    fun `sales units show the unit and the number of pieces in that unit`() {
        assertEquals("1箱（10個入）", ProductFormatter.salesUnit(box))
        assertEquals(
            "1パック（6個入）",
            ProductFormatter.salesUnit(SalesUnit(SalesUnitType.PACK, 6, 1)),
        )
        assertEquals("1個", ProductFormatter.salesUnit(SalesUnit(SalesUnitType.PIECE, 1, 1)))
        assertEquals("1箱", ProductFormatter.salesUnit(SalesUnit(SalesUnitType.BOX, 1, 1)))
    }

    @Test
    fun `unknown stock is not displayed as zero or out of stock`() {
        assertEquals(
            "在庫数未確認",
            ProductFormatter.stock(availability(AvailabilityStatus.AVAILABLE, null), box),
        )
        assertEquals(
            "在庫確認中",
            ProductFormatter.stock(availability(AvailabilityStatus.UNKNOWN, null), box),
        )
    }

    @Test
    fun `known stock counts use sales units instead of individual pieces`() {
        assertEquals(
            "在庫 24箱",
            ProductFormatter.stock(availability(AvailabilityStatus.AVAILABLE, 24), box),
        )
        assertEquals(
            "在庫 1,200パック",
            ProductFormatter.stock(
                availability(AvailabilityStatus.AVAILABLE, 1200),
                SalesUnit(SalesUnitType.PACK, 6, 1),
            ),
        )
        assertEquals(
            "在庫なし",
            ProductFormatter.stock(availability(AvailabilityStatus.AVAILABLE, 0), box),
        )
    }

    @Test
    fun `stock and discontinued statuses are explicit`() {
        assertEquals(
            "在庫なし",
            ProductFormatter.stock(availability(AvailabilityStatus.OUT_OF_STOCK, null), box),
        )
        assertEquals(
            "取扱終了",
            ProductFormatter.stock(availability(AvailabilityStatus.DISCONTINUED, null), box),
        )
    }

    private fun availability(status: AvailabilityStatus, stock: Int?) = Availability(
        status = status,
        stockUnits = stock,
        restockAt = null,
    )
}
