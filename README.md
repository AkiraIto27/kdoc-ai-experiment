# KDoc AI experiment

Kotlin Fest発表「そのKDoc、Javadocのままになっていませんか？〜Kotlinらしい書き方とAI時代の新たな役割〜」のKDoc比較用初期ソースと、公開した架空の商品データです。6条件の初期ソースを収録しています。**HTTP版は実装済みで、PagesのHTTPS公開と公開URLへのlive GETを確認しました。HTTP版でのLLM実測は未実施で、追加実験は保留中です。**

この商品データはフィクションです（実在の人物・団体・商品などとは関係ありません）

| 現在の条件ディレクトリ | KDocの内容 |
| --- | --- |
| [1-no-kdoc](conditions/1-no-kdoc/) | 比較対象18箇所へのKDoc追加なし |
| [2-simple-kdoc](conditions/2-simple-kdoc/) | 名前・型を言い換える要約と短いparam/return |
| [3-concise-contract-kdoc](conditions/3-concise-contract-kdoc/) | 詳細な契約KDocと同じ仕様事項を短く記述 |
| [4-detailed-contract-kdoc](conditions/4-detailed-contract-kdoc/) | 契約を本文とタグで詳しく説明した既存KDoc |
| [5-contract-with-rationale-kdoc](conditions/5-contract-with-rationale-kdoc/) | 簡潔な契約KDocの全文と、根拠付き設計理由・背景 |
| [6-used-androidx-skill-kdoc](conditions/6-used-androidx-skill-kdoc/) | AndroidX公式`ktdoc_quality` Skillで生成し、18箇所・87契約項目を保持して凍結した日本語KDoc |

各条件は独立したGradleプロジェクトです。比較対象は同じ5ファイル・18箇所で、それ以外の実装・データ・通常テスト・設定は共通です。`1-no-kdoc`は比較対象へのKDoc追加がないという意味です。テンプレート由来のテストKDocは全条件に残っています。`4-detailed-contract-kdoc`は③より詳しい表現の旧Eを指し、③と契約の仕様事項は同じです。⑤は③をそのまま保ち、資料に根拠がある背景を5箇所に追加しています。

## KDoc比較実験の結果（各100件・計600件）

[HTMLレポート](docs/results/kdoc-six-conditions-600.html)／[600件CSV](docs/results/all600-trials.csv)／[統合JSON](docs/results/kdoc-six-conditions-600.json)／[集計・再試行・留保](docs/results/README.md)を追加しました。①〜⑤は既存20件ずつを保持し各80件追加、⑥は100件です。選択600枠はすべて固定11チェック合格です。

`gpt-6.1-sol / xhigh`、CLI `0.162.0-alpha.2`、毎回新しい初期コピーと会話、測定並列度1。時間はCLI起動から終了まで、品質は別コンテキストで採点しました。この測定はKtor MockEngineとテスト用Repositoryを使い、外部の商品HTTP APIは呼びません。Skillは⑥のKDoc生成時だけに使用しています。[Skill固定版・生成コスト・KDoc差分・実行環境](publication/condition6-600/)を記録しています。

一時的なモデルcapacity失敗2 attemptは別履歴へ保存して再試行しました（追加502 attempt／選択500枠）。過去と今回の計測時期・サーバー負荷・キャッシュの差があるため、平均の差から因果や有意差は主張しません。

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

## JavadocとKDocのタグ比較

### 共通タグとKDoc固有タグ

| **タグ** | **Javadoc** | **KDoc** | **KDocでの書き方・補足** |
| --- | --- | --- | --- |
| `@param` | **○** | **○** | 型引数は`@param T`（`<T>`は付けない） |
| `@return` | **○** | **○** | 戻り値 |
| `@throws` | **○** | **○** | すべての例外を書く必要はない |
| `@exception` | **○** | **○** | `@throws`と同じ |
| `@see` | **○** | **○** | 関連項目、参照リンク |
| `@since` | **○** | **○** | 導入されたバージョン |
| `@author` | **○** | **○** | 記述者（作者） |
| `@property` | **×** | **○** | クラスのプロパティ |
| `@constructor` | **×** | **○** | プライマリコンストラクタ |
| `@receiver` | **×** | **○** | 拡張関数のレシーバ |
| `@sample` | **×** | **○** | サンプルとして関数を埋め込む |
| `@suppress` | **×** | **○** | ドキュメントから除外する |

### Javadoc専用タグとKDocでの書き方

| **タグ** | **Javadoc** | **KDoc** | **KDocでの書き方・補足** |
| --- | --- | --- | --- |
| `@hidden` | **○** | **×** | KDocでは`@suppress`で代わりにする |
| `@deprecated` | **○** | **×** | `@Deprecated`アノテーションを使う |
| `@provides` / `@uses` | **○** | **×** | モジュール用 |
| `{@link}` / `{@linkplain}` | **○** | **×** | `[Foo]`、`[ラベル][Foo]` |
| `{@code}` / `{@literal}` | **○** | **×** | バッククォートで書く |
| `{@snippet}` | **○** | **×** | コードブロックか`@sample`で書く |
| `{@inheritDoc}` | **○** | **×** | 書かなければ親の説明を引き継ぐ |
| `{@summary}` | **○** | **×** | 最初の段落が要約になる |
| `{@return}` | **○** | **×** | `@return`で書く |
