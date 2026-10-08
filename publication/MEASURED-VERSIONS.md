# 実測版との対応・公開範囲

## 公開コピーの出典

採用したのは、2026-10-08の`ae-revised-20261008-8e5372b9/initial/A`と`initial/E`です。課題実行後のtrialディレクトリや回答コードを採用していません。保存されたpreparation-sealのhashに各73ファイルが一致することを読み取り確認しました。

| 実測コホート | 用途 | 採用スナップショットとの関係 |
| --- | --- | --- |
| `ae-revised-20261008-8e5372b9` | 改訂promptでの先行20試行 | A/E各73ファイルが一致 |
| `ae-add20-20261008-502ff090` | 同じpromptでの追加20試行 | A/E各73ファイルが先行コホートと一致 |
| `ae-replace-a1-20261008-6ca2e234` | A1を置換するための追加1試行 | 保存されたA/E各73ファイルが先行コホートと一致 |

3コホートの対応は今回の読み取り照合結果です。再実験を実施したという意味ではありません。A1置換は事後の選択で、実行履歴は41試行、置換後の集計は40試行です。同じ1課題の反復を独立した40事例として扱わないでください。この公開候補では試行別結果や統計の新たな評価を加えていません。

実測に記録されたmodel/effortは`gpt-6.1-sol`/`xhigh`、CLIは`codex-cli 0.160.1`です。共通prompt SHA-256は`47ce6c9c607b13654326b7ddf6317f53dd0523dc31ed57d64a490e883354dd06`、runner SHA-256は`ea18016b927b7ce965429b3284af538e7ba4ed08e737ba2f4c5d123db9451a19`です。prompt/runner本体、隠し採点器、実測ログ、回答コードはこの公開候補へコピーしていません。

元Git HEADは`19264e1d105165e5976e0afe5117152f0516195f`ですが、重要実装は未コミットでした。公開版の同定には[source-manifest.json](source-manifest.json)のファイルhashを使います。現ローカルの対応67ファイルはEと一致していますが、公開コピーは保存済み初期スナップショットから作り、元リポジトリは変更していません。

## A/E差分と保持内容

| ファイル | Eに追加されたKDoc |
| --- | ---: |
| `MainActivity.kt` | 2 |
| `presentation/catalog/CatalogViewModel.kt` | 6 |
| `domain/repository/ProductRepository.kt` | 6 |
| `domain/usecase/LoadCatalogPage.kt` | 2 |
| `data/repository/HttpProductRepository.kt` | 2 |
| 合計 | 18 |

Eから追加KDocだけを取り除くとAと一致します。5ファイル以外はA/Eでbyte単位で同じです。テンプレートのテストKDocは共通です。実差分は[ae-kdoc.diff](ae-kdoc.diff)、コピー後の照合記録は[copy-verification.json](copy-verification.json)を参照してください。

公開する各67ファイルは、実測初期コピーとbyte単位で一致します。共通の`.tools/run-api-test`と`.tools/lib/`5個のJARは公開対象から除外しました。これらは端末固有パスを使う簡易実行環境で、除外してもアプリのソース・アセット・通常テスト・Gradle構成は変わりません。除外ファイルの相対名/hash/サイズをmanifestに記録しました。公開コピーだけで採点環境を再現できるとは主張しません。

元の履歴、`.git`、認証・端末設定、`local.properties`、キャッシュ、ビルド出力、非公開採点・完成回答、生会話・イベントログ、個人の絶対パスは公開候補に含めていません。アプリソースのライセンスは今回付与していません。

## 実測APIとPages版の違い

| 項目 | 保存したA/E | 新規Pages/HTTP variant |
| --- | --- | --- |
| Repository API | `getProducts(cursor: String? = null, limit: Int = 50)` | 同じdomain APIを実装 |
| データ取得 | Ktor MockEngineが500商品assetからページ生成 | 静的ページDTO JSONをGETするアダプターを実装。公開URLから既定50件を取得済み |
| limit | 1..100、既定50 | 同じ範囲の全正常ページを事前生成 |
| cursor | snapshot/limit/offsetを含むSHA-256 | 同じ計算結果を索引とページ名に使用 |
| 並び順 | 更新Instant降順、ID昇順 | 同じ順序で生成 |
| 失敗 | MockEngineの400/固定503 | クライアント入力拒否・障害注入。Pagesの動的400/503とはしない |
| cancellation | 既存RepositoryはCancellationExceptionを再throw | 同条件を実装。独立HTTP受入8/8成功の報告あり |
| 計測 | 保存したA/Eの実測に対応 | 未計測。実測結果に混ぜない |

根拠（A側相対パス、Eも同じ実装）:

- `domain/repository/ProductRepository.kt`:6 — 既定値50とcursor=null。
- `domain/usecase/LoadCatalogPage.kt`: 入力limitの`require`。Repositoryと例外型が異なる。
- `data/remote/CatalogMockEngine.kt`:40–44、56–79、98–101 — limit・cursor・ページ化・並び順。
- `data/remote/CatalogCursor.kt`: SHA-256の入力形式。
- `data/dto/ProductMapper.kt`:16–22 — ページのitems数・一意性・nextCursor等の検証。
- `data/repository/HttpProductRepository.kt`:18–46 — 入力拒否、HTTP状態/body、cancel再throw、DTO変換。

同じAPI形状でも通信先を変更した版は追加条件です。PagesのJSON hashと生成規模は[pages-build.json](pages-build.json)、実装と未実施範囲は[HTTP版仕様](../variants/http-pages/README.md)に分けています。独立HTTP受入8/8、静的2640ページ、初回公開対象確認が成功しました。HTTPS公開と公開URLへのlive GETも確認済みです。HTTP版でのLLM実測は未実施です。
