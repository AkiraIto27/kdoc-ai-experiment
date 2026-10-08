# Pages HTTP版（実装済み・未計測）

このディレクトリに独立したHTTPアダプターを実装しています。`conditions/A`と`conditions/E`には手を加えていません。実測初期ソースにも実測結果にも、このHTTP版は含まれません。PagesでHTTPS公開し、このアダプターで公開URLから既定50件を取得しました。依存追加、Android組み込み、新たなLLM計測は行っていません。

公開API・例外仕様は[API.md](API.md)、実装は`src/main/kotlin/com/example/kdoctest/data/pages/`にあります。独立テスト担当は`CatalogHttpTransport`へ別実装を注入できます。

## 配信と取得

Pagesは[静的サイト配信](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages)です。既存の`GET /v1/products?limit=50&cursor=...`と同じサーバー処理は置けません。固定500件についてlimit=1..100の全正常ページを事前生成し、ファイルのURLを取得する設計です。ページ単位のDTOを既存`ProductPageDto.toDomain(responseBytes, requestedLimit)`へ渡します。全500件をこの変換器へ直接渡すと`items.size <= requestedLimit`を満たせません。

2640ページ・約131MBは全limit/cursor契約を満たす唯一の方法ではありません。全500件を一度取得してクライアントで同じ順序・cursorでページ化する方法なら、商品データは約1.28MBで済みます。今回は各ページを一つの実HTTPレスポンスとして取得し、既存ページDTOの件数検証と`responseBytes`の意味を保つため全ページ生成を選んでいます。商品内容がlimitごとに計100回現れるためサイズが増えます。どちらの方式も旧A/E実測とは別条件です。

候補配信ルートは`https://akiraito27.github.io/kdoc-ai-experiment/data/catalog-v1/`です。

| 入力/場面 | アダプターの動作 |
| --- | --- |
| `getProducts()` | `cursor=null, limit=50`。`limits/50/first.json`を取得 |
| limitが1..100 | `limits/<limit>/index.json`で有効なページを解決。入力limitを維持してDTOを変換 |
| limitが0以下/101以上 | HTTP取得前に`CatalogException.InvalidRequest`。UseCase経由の境界エラーは既存の`IllegalArgumentException`のまま |
| null cursor | 同じlimitの`first.json` |
| non-null cursor | 同じlimitの索引に含まれるhashだけを受理し、`<cursor>.json`を取得。文字列を直接任意URLへ連結しない |
| 空文字・不明・別limit・別snapshotのcursor | `CatalogException.InvalidRequest`。索引外URLを試さない |
| 最終ページ | `nextCursor=null`。有効cursorはoffsetが全件数未満のページだけ |
| EMPTY | 入力limitは同様に検証。null cursorの場合に`empty.json`を取得。items空・totalCount=0・nextCursor=null |
| ERROR / NEXT_PAGE_ERROR | 明示的なクライアント障害注入として`Unavailable`。NEXT_PAGE_ERRORは有効な追加ページに対して注入。Pages自身がHTTP503を返すとは扱わない |
| 取得失敗/非2xx | `Unavailable`。Pagesの404は動的APIの400へ読み替えない |
| JSON不正・DTO/件数検証失敗 | `InvalidResponse`。版manifest/索引の構造破損も同様 |
| coroutineキャンセル | 取得・body読み取り・DTO変換の各段階で`CancellationException`を再throw。キャンセルを`Unavailable`や`InvalidResponse`へ変換しない |

並び順は`updatedAt`をInstantとして降順、同値ならID昇順です。cursorは実測版と同じ`SHA-256("catalog-cursor-v1|snapshotId|limit|offset"のUTF-8)`の小文字hexです。snapshotIdは`catalog-v1`。DTO内の商品フィールド、価格の販売単位、在庫null/0は元データからそのまま保存します。

返却する`ProductPage`は既存と同じ`items, nextCursor, totalCount, snapshotId, responseBytes`です。`responseBytes`は取得したページbodyをDTO変換へ渡すbyte列の長さで、LLM token数ではありません。manifest/索引の取得量は含めません。HTTP圧縮や通信記録は未検証です。正常ページの再エンコードに伴うbyte列の違いはあり得るため、旧MockEngineと新HTTP版の通信量が同じとは主張しません。manifestとlimit索引は成功後にキャッシュし、ページbodyは毎回GETします。

既存の依存にはKtorの実HTTP用engineがありません。新依存は禁止されているので、標準の`HttpURLConnection`と既存coroutines/serializationを使用しています。GETはdaemon threadで実行し、`suspendCancellableCoroutine`で待ちます。cancel時はworkerへinterruptし、別daemon threadでdisconnectを依頼します。プラットフォームのdisconnectがブロックしても、呼び出し側の取消完了を待たせません。接続終了はプラットフォーム実装とtimeoutに依存するため、呼び出し取消と接続切断の両方を独立担当が検証します。read/connect timeoutは既定各10秒、body上限は4MiBです。

manifestは固定fixture版hashを検証し、索引とページbodyは公開manifestのhash/byte数を検証します。索引内の全ページdescriptorをlimit・offsetから再計算したcursorに照合します。DTOの件数・snapshot・次cursor・並び順も検証します。hashによるファイル整合性確認であり、署名付きmanifestや新たな認証方式ではありません。

取得したページを表示する場合の失敗時一覧保持、古い応答破棄、同時追加取得制御は既存ViewModelの責務を保ちます。接続後の一体動作は新HTTP版として別途検証します。

## 公開手順案と未実施事項

生成JSONをmainブランチの`/docs`から配信する案です。データは全架空で認証・キーは不要です。自動deploy workflowは追加していません。公開siteは[GitHub Pagesの1GB上限](https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits)以下に収める必要があります。実測fixtureは約1.28MBで、全limitの事前生成は商品bodyが約100回現れるため、約128MBに索引を加えた規模になります。正確なサイズは`publication/pages-build.json`に記録します。

元500件JSONは`docs/data/catalog-v1/products.json`、生成器は`scripts/build_pages_data.py`として公開候補に含まれています。各A/Eアセットもbyte一致で保持します。公開データ版のhashは`5102e643ca1beaedaf98535f893fd5c0d874e013d0acfc51ab98e3f5740c378d`です。

リポジトリの作成、push、Pages有効化、deploy完了、公開URLへのlive GETを確認しました。別thread/contextの独立HTTP受入8/8・静的2640ページ・初回公開対象確認も成功しています。実装担当は機能テストを作成せず、依頼された最小live取得smokeだけを実行しました。Androidへの組み込みと新たなLLM計測は未実施です。

## 既存ツールでのコンパイル

`compile.py`はA/Eの共有ソースを読み取るだけで、出力をこのvariantの`build/classes`に置きます。Kotlin compiler、serialization compiler plugin、coroutines-core-jvm 1.10.2、serialization-json/core-jvm 1.9.0の既存JARを引数で指定します。依存解決・download・Gradle daemon起動は行いません。

```text
python3 -B variants/http-pages/compile.py \
  --kotlin-home <installed-kotlin-distribution> \
  --java-home <installed-jdk> \
  --classpath <existing-coroutines-and-serialization-jars>
```

今回の確認はKotlin 2.3.10/JDK 25.0.2、JVM target 11のコンパイルのみです。アプリのGradle設定はKotlin 2.2.10のままで、Androidビルド互換性まで確認したとは扱いません。

## 支払い不要の構成

公開repoのPagesは[GitHub Freeで利用可能](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages)です。公開site上限1GB・月100GBのsoft bandwidth limit等の[Pages制限](https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits)内で教育用の架空fixtureを配信します。[標準GitHub-hosted runnerは公開repoおよびPagesで無料](https://docs.github.com/en/billing/concepts/product-billing/github-actions)です。今回はbranchの`/docs`配信案で、独自workflow、large runner、追加artifact/cache、有料host、独自ドメイン、APIキーは追加していません。料金設定や認証を変更していません。制限超過時に有料リソースへ自動移行する処理もありません。
