package com.example.kdoctest.data.remote

import kotlinx.serialization.json.Json

internal val catalogJson = Json {
    ignoreUnknownKeys = true
    isLenient = false
    coerceInputValues = false
}
