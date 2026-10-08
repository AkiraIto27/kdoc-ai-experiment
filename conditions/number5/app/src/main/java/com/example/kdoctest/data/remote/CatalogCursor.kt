package com.example.kdoctest.data.remote

import java.security.MessageDigest

internal fun catalogCursor(snapshotId: String, limit: Int, offset: Int): String {
    val identity = "catalog-cursor-v1|$snapshotId|$limit|$offset".toByteArray(Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256").digest(identity)
    return digest.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
