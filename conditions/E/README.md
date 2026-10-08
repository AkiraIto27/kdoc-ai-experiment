# KDocTest

KDocの記述条件を比較するための、架空の業務用備品カタログアプリです。Android / Jetpack Compose / MVVMを使用し、データ取得、ドメイン、画面の責務を分離しています。

## 現在の実装

- 自作の固定500商品をJSONとして同梱。外部API・画像配信サービス・APIキーは不要です。
- Ktor MockEngineでHTTPレスポンスを返し、JSON解析、DTOからドメインへの変換、UseCase、ViewModel、Composeまで実際に実行します。
- 50件ずつの一覧表示、追加読み込み、更新、在庫不明と欠品の区別、販売単位ごとの価格表示に対応します。
- 空結果、初回取得失敗、追加取得失敗を固定シナリオで再現できます。
- 共通ベースの実装にはKDocを付けていません。7条件の生成、12課題の採点環境、AIエージェントの計測ランナーは次の段階です。

## 構成

```text
app/src/main/
  assets/catalog/products.json       固定データ
  java/com/example/kdoctest/
    data/                            HTTPモック、DTO、変換、Repository実装
    domain/                          Androidに依存しないモデル、Repository契約、UseCase
    presentation/catalog/            ViewModel、画面状態、Compose一覧
    di/                              依存関係の組み立てとHTTPクライアントの寿命管理
    ui/theme/                        アプリテーマ
scripts/generate_catalog.py          固定JSONの再生成と整合性チェック
```

ドメイン層からdata / presentation層を参照しません。Gradleモジュールは既存の`:app`を維持し、パッケージで責務を分離しています。設計判断は[ADR](docs/adr/0001-catalog-baseline.md)に記録しています。

## 実行と確認

既存のGradle WrapperとAndroid SDKを使用します。`local.properties`は端末固有設定のためGit管理対象外です。

```sh
python3 scripts/generate_catalog.py --check
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
```

APKは`app/build/outputs/apk/debug/app-debug.apk`です。接続済みのエミュレーターまたは端末でUIテストを実行する場合:

```sh
./gradlew :app:connectedDebugAndroidTest --console=plain
```

実行済みの検証結果は[検証記録](docs/verification.md)を参照してください。

## モックAPIの契約

`GET https://catalog.example.test/v1/products`をMockEngineが処理します。実際のネットワーク接続は発生しません。

| 項目 | 契約 |
| --- | --- |
| `limit` | 省略時50、1〜100の整数 |
| `cursor` | 初回は省略。次ページは前回の`nextCursor`をそのまま渡す |
| ページ終了 | `nextCursor == null` |
| カーソル | スナップショットとページサイズに対応。不正な値や異なるページサイズとの組み合わせは400 |
| 並び順 | `updatedAt`降順、同時刻なら`id`昇順 |
| `price.amountYen` | `salesUnit`で示す販売単位1つあたりの円価格。税込・税抜は`taxIncluded` |
| `stockUnits` | 販売単位での数量。`null`は在庫不明、`0`は在庫なし |
| 日時 | 固定のUTC ISO 8601値。実行時刻に依存しない |

再実行しても同じ商品・並び順・ページを返します。説明文、名称、在庫、規格は本プロジェクト用の架空データです。

## 固定シナリオ

通常起動では500商品を返します。デバッグビルドではActivityの`catalog_scenario` extraでシナリオを指定できます。プロセスを停止してから起動すると、新規試行として再現できます。

```sh
adb shell am force-stop com.example.kdoctest
adb shell am start -n com.example.kdoctest/.MainActivity --es catalog_scenario empty
```

| 値 | 応答 |
| --- | --- |
| 未指定 / `normal` | 固定500商品 |
| `empty` | 200、空一覧 |
| `error` | 常に503 |
| `next-page-error` | 初回成功、2ページ目以降503 |

失敗シナリオは再試行しても同じ失敗を返します。時間経過や乱数で成功に変わりません。通常起動へ戻す場合はextraなしでプロセスを起動し直します。リリースビルドは常に通常シナリオです。

## KDoc比較へ進む際の境界

アプリの描画速度とAIエージェントが消費するトークン数は別の指標です。本アプリだけではエージェントの精度・トークンを計測しません。

比較時は同じ実装・データ・通常テストを使い、KDocだけを差し替えます。各課題の試行は新しい会話と初期コードから開始します。他条件のコード、採点用の正解、設計資料をエージェントへ見せる範囲は、実験セットを作成するときに固定してください。
