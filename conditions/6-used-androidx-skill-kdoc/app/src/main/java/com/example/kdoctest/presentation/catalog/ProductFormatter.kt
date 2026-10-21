package com.example.kdoctest.presentation.catalog

import com.example.kdoctest.domain.model.Availability
import com.example.kdoctest.domain.model.AvailabilityStatus
import com.example.kdoctest.domain.model.ProductPrice
import com.example.kdoctest.domain.model.SalesUnit
import com.example.kdoctest.domain.model.SalesUnitType
import java.util.Locale

internal object ProductFormatter {
    fun price(price: ProductPrice): String = String.format(Locale.JAPAN, "%,d円", price.amountYen)

    fun taxLabel(price: ProductPrice): String = if (price.taxIncluded) "税込" else "税抜"

    fun salesUnit(unit: SalesUnit): String {
        val quantity = if (unit.type != SalesUnitType.PIECE && unit.piecesPerUnit > 1) {
            "（${unit.piecesPerUnit}個入）"
        } else {
            ""
        }
        return "1${unitName(unit.type)}$quantity"
    }

    fun stock(availability: Availability, unit: SalesUnit): String = when (availability.status) {
        AvailabilityStatus.AVAILABLE -> when (val stock = availability.stockUnits) {
            null -> "在庫数未確認"
            0 -> "在庫なし"
            else -> "在庫 ${String.format(Locale.JAPAN, "%,d", stock)}${unitName(unit.type)}"
        }
        AvailabilityStatus.OUT_OF_STOCK -> "在庫なし"
        AvailabilityStatus.UNKNOWN -> "在庫確認中"
        AvailabilityStatus.DISCONTINUED -> "取扱終了"
    }

    private fun unitName(type: SalesUnitType): String = when (type) {
        SalesUnitType.PIECE -> "個"
        SalesUnitType.PACK -> "パック"
        SalesUnitType.BOX -> "箱"
    }
}
