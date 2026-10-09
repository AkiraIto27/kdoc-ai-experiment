# KDoc AI experiment

Kotlin Fest発表「そのKDoc、Javadocのままになっていませんか？〜Kotlinらしい書き方とAI時代の新たな役割〜」のKDoc比較用初期ソースと、公開した架空の商品データです。5条件のディレクトリ名をKDocの内容が分かる名前に整理しました。**HTTP版は実装済みで、PagesのHTTPS公開と公開URLへのlive GETを確認しました。HTTP版でのLLM実測は未実施で、追加実験は保留中です。**

この商品データはフィクションです（実在の人物・団体・商品などとは関係ありません）

| 現在の条件ディレクトリ | KDocの内容 |
| --- | --- |
| [1-no-kdoc](conditions/1-no-kdoc/) | 比較対象18箇所へのKDoc追加なし |
| [2-simple-kdoc](conditions/2-simple-kdoc/) | 名前・型を言い換える要約と短いparam/return |
| [3-concise-contract-kdoc](conditions/3-concise-contract-kdoc/) | 詳細な契約KDocと同じ仕様事項を短く記述 |
| [4-detailed-contract-kdoc](conditions/4-detailed-contract-kdoc/) | 契約を本文とタグで詳しく説明した既存KDoc |
| [5-contract-with-rationale-kdoc](conditions/5-contract-with-rationale-kdoc/) | 簡潔な契約KDocの全文と、根拠付き設計理由・背景 |

各条件は独立したGradleプロジェクトです。比較対象は同じ5ファイル・18箇所で、それ以外の実装・データ・通常テスト・設定は共通です。`1-no-kdoc`は比較対象へのKDoc追加がないという意味です。テンプレート由来のテストKDocは全条件に残っています。`4-detailed-contract-kdoc`は③より詳しい表現の旧Eを指し、③と契約の仕様事項は同じです。⑤は③をそのまま保ち、資料に根拠がある背景を5箇所に追加しています。

## 補助ファイルとHTTP実装の役割

| 場所 | 用途 |
| --- | --- |
| [.publication-allowlist](.publication-allowlist) | 現在の公開対象を列挙する相対パス一覧。アプリの設定や自動削除処理ではありません。初回公開時の2,897件に、後から公開済みの209件と今回の移行台帳を反映しています。 |
| [compile.py](variants/http-pages/compile.py) | 既存Kotlin compiler/JARでHTTP版をコンパイルする補助。条件④の共通8ソースとHTTP版2ソースを読み、`variants/http-pages/build/classes`へ出力します。 |
| [implementation-verification.json](variants/http-pages/implementation-verification.json) | 過去のJVMコンパイル、独立HTTP受入、公開・live取得確認の記録。作成時の旧パスとhashを保存しています。 |
| [data/pages](variants/http-pages/src/main/kotlin/com/example/kdoctest/data/pages/) | HTTP版の実装本体。`CatalogHttpTransport.kt`がGETと取消を扱い、`PagesProductRepository.kt`がPagesデータの取得・hash・ページ契約を検証します。 |
| [docs/data/catalog-v1](docs/data/catalog-v1/) | 公開配信用JSON・limit別ページ・索引。実測版の通信先ではありません。 |

## Pages配信候補

公開先は[AkiraIto27/kdoc-ai-experiment](https://github.com/AkiraIto27/kdoc-ai-experiment)、Pagesは[https://akiraito27.github.io/kdoc-ai-experiment/](https://akiraito27.github.io/kdoc-ai-experiment/)です。mainブランチの`/docs`を配信元とし、deploy完了を確認しました。初回公開commitは`a5d562ac59ddac2b6d4e3207cf877d472cbfec40`です。

ページ化済みJSONの相対URL:

- `data/catalog-v1/manifest.json`: 版hash・件数・limit索引。
- `data/catalog-v1/products.json`: 実測fixtureと同じbyte列の全500件。単一ページとしてページ変換器へ渡さないでください。
- `data/catalog-v1/limits/50/first.json`: 既定50件の先頭ページ。
- `data/catalog-v1/limits/1/first.json` / `limits/100/first.json`: 境界値の先頭ページ。
- `data/catalog-v1/limits/<limit>/<nextCursor>.json`: 同じlimitで次ページを取得。
- `data/catalog-v1/empty.json`: 空一覧の正常レスポンス。

生成は新しい配信用ファイルだけを対象にします。Python標準ライブラリのみで、リポジトリルートから`python3 -B scripts/build_pages_data.py`を実行します。`1-no-kdoc`と`4-detailed-contract-kdoc`のアセットは読み取り専用で使用します。既存の生成ファイルが異なる場合は上書きせず停止します。

Pagesは静的配信なので、URLクエリからページを作ったり、条件に応じて400/503を返したりするAPIは提供できません。limit/cursorの解決と入力拒否を[HTTPアダプター](variants/http-pages/README.md)に実装しています。[公開API](variants/http-pages/API.md)にエラー・cancel・返却値を記載しています。HTTP通信を導入した版の挙動や計測値を、保存したA/Eの実測結果へ混ぜないでください。

HTTP版の実装・公開時には、依存追加、Gradleテスト、Androidビルド、新たなLLM試行は行っていません。既存JARを使うJVMコンパイル、別thread/contextでのHTTP受入8/8・静的2640ページ・初回公開対象の独立確認が成功しています。公開後はmanifest・既定50件ページ・全500件JSONのHTTP 200とbyte/hash一致、およびHTTPアダプターによる既定50件取得を確認しました。実装時の確認範囲は[HTTP実装記録](variants/http-pages/implementation-verification.json)、当時の公開状態は[状態記録](publication/publication-state.json)、初回公開対象のhashは[初回manifest](publication/final-publication-manifest.json)に記載しています。これらは今回の改名作業で再実行した結果ではありません。
