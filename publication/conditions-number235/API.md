# ②・③・⑤で共通のAPI

宣言と実装は①/A・④/Eと同じです。ここでは既存の公開APIだけを記載し、実験課題の回答コードや非公開採点条件は含めません。

| 型 | 共通の宣言・責務 |
| --- | --- |
| `domain.repository.ProductRepository` | `suspend fun getProducts(cursor: String? = null, limit: Int = 50): ProductPage`。ページ取得の境界 |
| `domain.usecase.LoadCatalogPage` | `LoadCatalogPage(repository: ProductRepository)`。`suspend operator fun invoke(cursor: String? = null, limit: Int = 50): ProductPage`。件数上限検証と取得委譲 |
| `data.repository.HttpProductRepository` | `HttpProductRepository(client: HttpClient)`。注入されたクライアントで取得・解析・変換 |
| `domain.model.ProductPage` | 商品、次カーソル、全件数、スナップショットID、応答本文のbyte数 |

型名の前には共通の`com.example.kdoctest.`が付きます。各条件の同じ相対パスに宣言があります。

入力・返却・例外は既存④/EのKDocを基準にしています。UseCaseの範囲外入力は`IllegalArgumentException`、具体Repositoryの範囲外入力は`CatalogException.InvalidRequest`であり、同一の例外契約にはしていません。[仕様事項対応表](specification-map.json)で③への保持を確認できます。

通常アプリは注入されたKtor MockEngineで固定JSONを読みます。`https://catalog.example.test/v1/products`への要求が実際の外部接続になる構成ではありません。応答本文のbyte数をモデルtokenとして扱いません。HTTP Pages variantや非公開採点器をこのAPIへ混ぜていません。
