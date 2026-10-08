# KDoc AI experiment — publication candidate

Kotlin Fest発表「そのKDoc、Javadocのままになっていませんか？〜Kotlinらしい書き方とAI時代の新たな役割〜」のA/E比較で使った初期ソースと、架空の商品データの公開準備です。**HTTP版は実装済み・未計測です。HTTPS公開、公開URLへのlive GET、HTTP版でのLLM実測は未実施です。**

| 場所 | 内容 | 実測との関係 |
| --- | --- | --- |
| `conditions/A/` | KDoc追加前のAndroid/Composeアプリ | 2026-10-08の実測初期ソース |
| `conditions/E/` | 契約を本文で説明するKDocを追加した同じアプリ | 同日の実測初期ソース |
| `publication/` | 出典、ファイルhash、A/E差分、公開範囲 | 公開準備の記録 |
| `docs/data/catalog-v1/` | Pages配信用JSON、limit別ページ、索引 | 公開用に新規生成。実測の通信先ではない |
| `variants/http-pages/` | 実HTTPアダプター、API仕様、コンパイル補助 | A/E実測版から独立した実装済み・未計測のvariant |

A/Eは各条件を独立したGradleプロジェクトとして保持します。5ファイルに追加された18個のKDoc以外の実装は同じです。テンプレート由来のテストKDocは両条件に共通で残っているため、Aは「リポジトリ全体にKDocが一切ない」という意味ではありません。

実測の初期コピーは各73ファイルでした。公開候補は、端末固有のJDK/Kotlinパスと同梱JARを含む共通の`.tools/`6ファイルを除いた各67ファイルです。残したファイルはすべて実測初期コピーとbyte単位で一致します。ファイルhashと除外一覧は[manifest](publication/source-manifest.json)、変更内容は[A/E差分](publication/ae-kdoc.diff)、対応関係は[実測版対応表](publication/MEASURED-VERSIONS.md)に記載しています。公開コピーだけでは実測時の採点・CLI環境一式を再現できません。

ソースは元リポジトリHEAD `19264e1d105165e5976e0afe5117152f0516195f` と、その時点の未コミット実装に由来します。HEADだけで版を同定せずmanifestのhashを使ってください。各条件内のREADME・ADR・検証メモは実測時の内容を保存した歴史資料で、現在の実験実施状況を示すものではありません。課題提出後の回答コード、非公開採点器、認証情報、端末設定、生会話・イベントログは含めていません。

実測版の`HttpProductRepository`は`https://catalog.example.test/v1/products`にGETしますが、注入されたKtor `MockEngine`がアセットを返します。外部APIやAPIキーを使いません。Pages配信用の新しいJSONを追加しても、実測版の通信先や実装は変わりません。

## Pages配信候補

候補は `AkiraIto27/kdoc-ai-experiment`、プロジェクトサイトの候補URLは `https://akiraito27.github.io/kdoc-ai-experiment/` です。リポジトリ作成、push、Pages設定は未実施です。公開する場合の設定案はmainブランチの`/docs`を配信元にする方式です。

ページ化済みJSONの例（公開後に有効になる予定の相対URL）:

- `data/catalog-v1/manifest.json`: 版hash・件数・limit索引。
- `data/catalog-v1/products.json`: 実測fixtureと同じbyte列の全500件。単一ページとしてページ変換器へ渡さないでください。
- `data/catalog-v1/limits/50/first.json`: 既定50件の先頭ページ。
- `data/catalog-v1/limits/1/first.json` / `limits/100/first.json`: 境界値の先頭ページ。
- `data/catalog-v1/limits/<limit>/<nextCursor>.json`: 同じlimitで次ページを取得。
- `data/catalog-v1/empty.json`: 空一覧の正常レスポンス。

生成は新しい配信用ファイルだけを対象にします。Python標準ライブラリのみで、リポジトリルートから`python3 -B scripts/build_pages_data.py`を実行します。A/Eのアセットは読み取り専用で使用します。既存の生成ファイルが異なる場合は上書きせず停止します。

Pagesは静的配信なので、URLクエリからページを作ったり、条件に応じて400/503を返したりするAPIは提供できません。limit/cursorの解決と入力拒否を[HTTPアダプター](variants/http-pages/README.md)に実装しています。[公開API](variants/http-pages/API.md)にエラー・cancel・返却値を記載しています。HTTP通信を導入した版の挙動や計測値を、保存したA/Eの実測結果へ混ぜないでください。

この準備では依存追加、Gradleテスト、Androidビルド、新たなLLM試行を行っていません。既存JARを使うJVMコンパイルは成功しています。別thread/contextの独立担当からHTTP受入8/8と静的2640ページの検証成功が報告されています。最終の公開対象確認は未完了で、合格後に公開へ進めます。実装時の確認範囲は[HTTP実装記録](variants/http-pages/implementation-verification.json)、公開対象は[最終manifest](publication/final-publication-manifest.json)に記載しています。
