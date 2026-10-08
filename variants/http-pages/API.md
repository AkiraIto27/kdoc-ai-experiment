# HTTP variant public API — implementation contract

Package: `com.example.kdoctest.data.pages`.

Files (only under this variant):

- `src/main/kotlin/com/example/kdoctest/data/pages/PagesProductRepository.kt`
- `src/main/kotlin/com/example/kdoctest/data/pages/CatalogHttpTransport.kt`

```kotlin
data class CatalogHttpResponse(val statusCode: Int, val body: ByteArray)

fun interface CatalogHttpTransport {
    suspend fun get(url: String): CatalogHttpResponse
}

class UrlConnectionCatalogTransport(
    connectTimeoutMillis: Int = 10_000,
    readTimeoutMillis: Int = 10_000,
    maximumBodyBytes: Int = 4 * 1024 * 1024,
) : CatalogHttpTransport

class PagesProductRepository(
    baseUrl: String = "https://akiraito27.github.io/kdoc-ai-experiment/data/catalog-v1/",
    transport: CatalogHttpTransport = UrlConnectionCatalogTransport(),
    scenario: CatalogScenario = CatalogScenario.NORMAL,
) : ProductRepository {
    override suspend fun getProducts(cursor: String?, limit: Int): ProductPage
}
```

呼び出し既定値は継承元`ProductRepository`の`cursor=null, limit=50`です。テストは`CatalogHttpTransport`へ別実装を注入して、外部通信なしでURL・返却・例外を確認できます。実HTTPS輸送はJDK標準`HttpURLConnection`を使用します。baseUrlはhttpsのみを受理し、query・fragment・userInfoを拒否します。GETはredirectを追跡しません。

取得順序は`manifest.json` → `limits/<limit>/index.json` → `limits/<limit>/first.json`または`<cursor>.json`です。EMPTYはmanifest → `empty.json`です。取得したmanifestとlimit索引を成功後にキャッシュします。URL解決に索引の任意path文字列を使用せず、規定pathと64桁小文字hexだけを使います。

例外仕様:

- limit範囲外・空/非hex cursor・検証済み索引にないcursor: `CatalogException.InvalidRequest`。範囲外は通信なし。
- ERROR: limitとcursor構文の検証後、通信なしで`Unavailable`。NEXT_PAGE_ERROR:有効な追加cursorを索引で確認後、ページGETをせず`Unavailable`。
- 輸送失敗・HTTP非2xx: `CatalogException.Unavailable`。
- manifest/索引/DTOの不正、hash不一致、items過多、不整合: `CatalogException.InvalidResponse`。
- `CancellationException`: 同じ例外を再throw。標準輸送は取消時にconnectionをdisconnectし、読み取り完了を待たずsuspend呼び出しをキャンセルする。
- constructorの不正baseUrl・timeout/body上限: `IllegalArgumentException`。

正常返却は既存`ProductPage`を使います。ページbodyのSHA-256と索引のbyte数を検証し、ページbodyのbyte長を`responseBytes`にします。これはtoken数や圧縮後wire byte数ではありません。metadataの通信量は含めません。

このAPIは旧A/E実測版とは別variantです。独立担当のHTTP受入8/8が成功し、公開済みPagesから既定50件を取得するlive smokeも成功しました。HTTP版での新LLM計測は未実施です。
