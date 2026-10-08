package com.example.kdoctest.data.pages

import com.example.kdoctest.data.dto.ProductPageDto
import com.example.kdoctest.data.dto.toDomain
import com.example.kdoctest.data.remote.CatalogScenario
import com.example.kdoctest.data.remote.catalogCursor
import com.example.kdoctest.data.remote.catalogJson
import com.example.kdoctest.domain.model.ProductPage
import com.example.kdoctest.domain.repository.CatalogException
import com.example.kdoctest.domain.repository.ProductRepository
import java.net.URI
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int

/** Separate, unmeasured variant fetching precomputed page DTOs from GitHub Pages. */
class PagesProductRepository(
    baseUrl: String = "https://akiraito27.github.io/kdoc-ai-experiment/data/catalog-v1/",
    private val transport: CatalogHttpTransport = UrlConnectionCatalogTransport(),
    private val scenario: CatalogScenario = CatalogScenario.NORMAL,
) : ProductRepository {
    private val root = normalizeBaseUrl(baseUrl)
    private val metadataMutex = Mutex()
    private var snapshot: Snapshot? = null
    private val indexes = mutableMapOf<Int, LimitIndex>()

    override suspend fun getProducts(cursor: String?, limit: Int): ProductPage {
        currentCoroutineContext().ensureActive()
        if (limit !in 1..100) throw CatalogException.InvalidRequest("limit must be between 1 and 100")
        if (cursor != null && !HASH.matches(cursor)) {
            throw CatalogException.InvalidRequest("cursor is invalid")
        }
        if (scenario == CatalogScenario.ERROR) throw CatalogException.Unavailable()
        val metadata = loadSnapshot()
        if (scenario == CatalogScenario.EMPTY) {
            if (cursor != null) throw CatalogException.InvalidRequest("cursor is invalid for an empty catalog")
            val bytes = fetch("empty.json")
            return decode {
                metadata.verifyFile("empty.json", bytes)
                val dto = decodeDto(bytes)
                require(dto.snapshotId == metadata.id)
                require(dto.items.isEmpty() && dto.pageInfo.totalCount == 0 && dto.pageInfo.nextCursor == null)
                dto.toDomain(bytes.size, limit)
            }
        }
        val index = loadIndex(metadata, limit)
        val descriptor = index.pages[cursor]
            ?: throw CatalogException.InvalidRequest("cursor does not belong to this limit and snapshot")
        if (scenario == CatalogScenario.NEXT_PAGE_ERROR && cursor != null) {
            throw CatalogException.Unavailable()
        }
        val path = "limits/$limit/${descriptor.file}"
        val bytes = fetch(path)
        return decode {
            metadata.verifyFile(path, bytes)
            val dto = decodeDto(bytes)
            require(dto.snapshotId == metadata.id)
            require(dto.pageInfo.totalCount == metadata.totalCount)
            require(dto.pageInfo.nextCursor == descriptor.nextCursor)
            require(dto.items.size == descriptor.itemCount)
            val page = dto.toDomain(bytes.size, limit)
            require(page.items.zipWithNext().all { (left, right) ->
                left.updatedAt > right.updatedAt ||
                    (left.updatedAt == right.updatedAt && left.id < right.id)
            })
            page
        }
    }

    private suspend fun loadSnapshot(): Snapshot = metadataMutex.withLock {
        snapshot ?: run {
            val bytes = fetch("manifest.json")
            val decoded = decode { parseSnapshot(bytes) }
            currentCoroutineContext().ensureActive()
            decoded.also { snapshot = it }
        }
    }

    private suspend fun loadIndex(metadata: Snapshot, limit: Int): LimitIndex = metadataMutex.withLock {
        indexes[limit] ?: run {
            val path = "limits/$limit/index.json"
            val bytes = fetch(path)
            val decoded = decode {
                metadata.verifyFile(path, bytes)
                parseIndex(bytes, metadata, limit)
            }
            currentCoroutineContext().ensureActive()
            decoded.also { indexes[limit] = it }
        }
    }

    private suspend fun fetch(path: String): ByteArray {
        currentCoroutineContext().ensureActive()
        val response = try {
            transport.get(root + path)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw CatalogException.Unavailable(failure)
        }
        currentCoroutineContext().ensureActive()
        if (response.statusCode !in 200..299) throw CatalogException.Unavailable()
        return response.body
    }

    private suspend fun <T> decode(block: () -> T): T {
        currentCoroutineContext().ensureActive()
        val result = try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw CatalogException.InvalidResponse(failure)
        }
        currentCoroutineContext().ensureActive()
        return result
    }

    private fun decodeDto(bytes: ByteArray): ProductPageDto =
        catalogJson.decodeFromString(bytes.decodeToString(throwOnInvalidSequence = true))

    private fun parseSnapshot(bytes: ByteArray): Snapshot {
        val json = parseObject(bytes)
        require(json.number("formatVersion") == 1)
        val id = json.string("snapshotId")
        val count = json.number("totalCount")
        require(id == "catalog-v1" && count == 500)
        require(json.number("defaultLimit") == 50)
        require(json.number("minimumLimit") == 1 && json.number("maximumLimit") == 100)
        val source = json.obj("sourceFixture")
        require(source.string("file") == "products.json")
        val files = json.obj("files").mapValues { (_, element) ->
            val value = element as? JsonObject ?: error("Invalid file metadata")
            val hash = value.string("sha256")
            val size = value.number("bytes")
            require(HASH.matches(hash) && size > 0)
            FileDigest(hash, size)
        }
        require(files["products.json"] == FileDigest(source.string("sha256"), source.number("bytes")))
        require(source.string("sha256") == SOURCE_FIXTURE_SHA256 && source.number("bytes") == 1_276_881)
        require(files.containsKey("empty.json"))
        val normal = json.obj("normal")
        val limitIndexes = normal.obj("limitIndexes")
        require(limitIndexes.keys == (1..100).map(Int::toString).toSet())
        for (limit in 1..100) {
            val path = "limits/$limit/index.json"
            require(limitIndexes.string(limit.toString()) == path && files.containsKey(path))
        }
        require(normal.number("pageCount") == (1..100).sumOf { (count + it - 1) / it })
        val empty = json.obj("empty")
        require(empty.string("file") == "empty.json")
        require(empty["acceptsOnlyNullCursor"] == JsonPrimitive(true))
        return Snapshot(id, count, files)
    }

    private fun parseIndex(bytes: ByteArray, metadata: Snapshot, limit: Int): LimitIndex {
        val json = parseObject(bytes)
        require(json.string("snapshotId") == metadata.id)
        require(json.number("limit") == limit && json.number("totalCount") == metadata.totalCount)
        require(json.string("firstPage") == "first.json")
        val pageValues = json["pages"] as? JsonArray ?: error("Missing pages")
        val offsets = (0 until metadata.totalCount step limit).toList()
        require(pageValues.size == offsets.size)
        val pages = mutableMapOf<String?, PageDescriptor>()
        val acceptedCursors = mutableMapOf<String, String>()
        for ((position, offset) in offsets.withIndex()) {
            val value = pageValues[position] as? JsonObject ?: error("Invalid page descriptor")
            val inputCursor = if (offset == 0) null else catalogCursor(metadata.id, limit, offset)
            val file = if (inputCursor == null) "first.json" else "$inputCursor.json"
            val end = minOf(offset + limit, metadata.totalCount)
            val next = if (end < metadata.totalCount) catalogCursor(metadata.id, limit, end) else null
            require(value.nullableString("inputCursor") == inputCursor)
            require(value.string("file") == file && value.number("itemCount") == end - offset)
            require(value.nullableString("nextCursor") == next)
            require(metadata.files["limits/$limit/$file"] ==
                FileDigest(value.string("sha256"), value.number("bytes")))
            pages[inputCursor] = PageDescriptor(file, end - offset, next)
            if (inputCursor != null) acceptedCursors[inputCursor] = file
        }
        val cursors = json.obj("cursors")
        require(cursors.keys == acceptedCursors.keys)
        require(acceptedCursors.all { (key, value) -> cursors.string(key) == value })
        return LimitIndex(pages)
    }

    private data class FileDigest(val sha256: String, val bytes: Int)

    private data class Snapshot(val id: String, val totalCount: Int, val files: Map<String, FileDigest>) {
        fun verifyFile(path: String, body: ByteArray) {
            val expected = files[path] ?: error("Missing file hash")
            require(body.size == expected.bytes && sha256(body) == expected.sha256)
        }
    }

    private data class PageDescriptor(val file: String, val itemCount: Int, val nextCursor: String?)
    private data class LimitIndex(val pages: Map<String?, PageDescriptor>)

    private companion object {
        val HASH = Regex("[0-9a-f]{64}")
        const val SOURCE_FIXTURE_SHA256 = "5102e643ca1beaedaf98535f893fd5c0d874e013d0acfc51ab98e3f5740c378d"

        fun normalizeBaseUrl(value: String): String {
            val uri = try {
                URI(value)
            } catch (failure: Exception) {
                throw IllegalArgumentException("Invalid Pages base URL", failure)
            }
            require(uri.scheme == "https" && !uri.host.isNullOrBlank())
            require(uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null)
            require(uri.normalize() == uri && uri.rawPath.orEmpty().split('/').none { it == ".." || it == "." })
            return uri.toASCIIString().trimEnd('/') + "/"
        }

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

        fun parseObject(bytes: ByteArray): JsonObject =
            catalogJson.parseToJsonElement(bytes.decodeToString(throwOnInvalidSequence = true)) as? JsonObject
                ?: error("Expected JSON object")

        fun JsonObject.obj(key: String): JsonObject = this[key] as? JsonObject ?: error("Missing object: $key")

        fun JsonObject.string(key: String): String {
            val value = this[key] as? JsonPrimitive ?: error("Missing string: $key")
            require(value.isString)
            return value.content
        }

        fun JsonObject.nullableString(key: String): String? {
            val value: JsonElement = this[key] ?: error("Missing value: $key")
            if (value == JsonNull) return null
            require(value is JsonPrimitive && value.isString)
            return value.content
        }

        fun JsonObject.number(key: String): Int {
            val value = this[key] as? JsonPrimitive ?: error("Missing number: $key")
            require(!value.isString && value != JsonNull)
            return value.int
        }
    }
}
