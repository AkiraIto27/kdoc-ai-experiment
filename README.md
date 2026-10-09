# KDoc AI experiment

Kotlin Fest発表「そのKDoc、Javadocのままになっていませんか？〜Kotlinらしい書き方とAI時代の新たな役割〜」のKDoc比較用初期ソースと、公開した架空の商品データです。5条件のディレクトリ名をKDocの内容が分かる名前に整理しました。**HTTP版は実装済みで、PagesのHTTPS公開と公開URLへのlive GETを確認しました。HTTP版でのLLM実測は未実施で、追加実験は保留中です。**

| 現在の条件ディレクトリ | 旧名・出力上の番号 | KDocの内容 | 旧パスを公開したcommit |
| --- | --- | --- | --- |
| [1-no-kdoc](conditions/1-no-kdoc/) | `A`・① | 比較対象18箇所へのKDoc追加なし | `a5d562ac59ddac2b6d4e3207cf877d472cbfec40` |
| [2-simple-kdoc](conditions/2-simple-kdoc/) | `number2`・② | 名前・型を言い換える要約と短いparam/return | `e68aec4a803e1d0a5258a07198d1b77fad7808e2` |
| [3-concise-contract-kdoc](conditions/3-concise-contract-kdoc/) | `number3`・③ | 詳細な契約KDocと同じ仕様事項を短く記述 | `e68aec4a803e1d0a5258a07198d1b77fad7808e2` |
| [4-detailed-contract-kdoc](conditions/4-detailed-contract-kdoc/) | `E`・④ | 契約を本文とタグで詳しく説明した既存KDoc | `a5d562ac59ddac2b6d4e3207cf877d472cbfec40` |
| [5-contract-with-rationale-kdoc](conditions/5-contract-with-rationale-kdoc/) | `number5`・⑤ | 簡潔な契約KDocの全文と、根拠付き設計理由・背景 | `e68aec4a803e1d0a5258a07198d1b77fad7808e2` |

各条件は独立したGradleプロジェクトです。比較対象は同じ5ファイル・18箇所で、それ以外の実装・データ・通常テスト・設定は共通です。`1-no-kdoc`は比較対象へのKDoc追加がないという意味です。テンプレート由来のテストKDocは全条件に残っています。`4-detailed-contract-kdoc`は③より詳しい表現の旧Eを指し、③と契約の仕様事項は同じです。⑤は③をそのまま保ち、資料に根拠がある背景を5箇所に追加しています。

## 旧名と記録の読み方

改名前の全5条件は[commit e68aec4](https://github.com/AkiraIto27/kdoc-ai-experiment/tree/e68aec4a803e1d0a5258a07198d1b77fad7808e2/conditions)にあります。今回の整理はパスの変更です。各条件67ファイルの内容と実行属性、過去の測定記録・出力は保存しています。移行元・移行先と条件ごとの照合hashは[移行台帳](publication/condition-name-migration.json)に記載しています。

`publication/`内の既存manifest・diff・対応表と、HTTP版の`implementation-verification.json`は作成時点の記録として原文を保持しています。そこにある`A`、`E`、`number2`、`number3`、`number5`のパスは上表で読み替えてください。旧パスのリンクをたどる場合は[改名前のpublication](https://github.com/AkiraIto27/kdoc-ai-experiment/tree/e68aec4a803e1d0a5258a07198d1b77fad7808e2/publication)を参照してください。特に[②・③・⑤の準備記録](https://github.com/AkiraIto27/kdoc-ai-experiment/blob/e68aec4a803e1d0a5258a07198d1b77fad7808e2/publication/conditions-number235/README.md)にある「未実施」は文書作成時点の状態です。

実測の初期コピーは各73ファイルでした。公開版は、端末固有のJDK/Kotlinパスと同梱JARを含む共通の`.tools/`6ファイルを除いた各67ファイルです。残したファイルはすべて実測初期コピーとbyte単位で一致します。旧A/Eのファイルhashと除外一覧は[manifest](publication/source-manifest.json)、変更内容は[旧A/E差分](publication/ae-kdoc.diff)、対応関係は[実測版対応表](https://github.com/AkiraIto27/kdoc-ai-experiment/blob/e68aec4a803e1d0a5258a07198d1b77fad7808e2/publication/MEASURED-VERSIONS.md)、②・③・⑤のhashは[追加条件manifest](publication/conditions-number235/source-manifest.json)に記載しています。公開コピーだけでは実測時の採点・CLI環境一式を再現できません。

ソースは元リポジトリHEAD `19264e1d105165e5976e0afe5117152f0516195f` と、その時点の未コミット実装に由来します。HEADだけで版を同定せずmanifestのhashを使ってください。各条件内のREADME・ADR・検証メモは実測時の内容を保存した歴史資料で、現在の実験実施状況を示すものではありません。課題提出後の回答コード、非公開採点器、認証情報、端末設定、生会話・イベントログは含めていません。

実測版の`HttpProductRepository`は`https://catalog.example.test/v1/products`にGETしますが、注入されたKtor `MockEngine`がアセットを返します。外部APIやAPIキーを使いません。Pages配信用の新しいJSONを追加しても、実測版の通信先や実装は変わりません。

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
