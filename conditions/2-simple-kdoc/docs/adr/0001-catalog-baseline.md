# ADR-0001: 再現可能な備品カタログの共通実装

- 日付: 2026-09-12
- 状態: accepted（ユーザーの実装依頼に基づく、依頼範囲内の実装判断）
- 決定権者: ユーザー。Compose / MVVM / クリーンアーキテクチャ、固定データによるKDoc比較は会話内で指定・合意済み。
- 対象: 共通アプリ。7条件の生成、12課題の採点環境、比較本試験の実行は含めない。

## 判断軸

1. 条件間の再現性と外部依存の少なさ
2. JSON・境界値・非同期処理を扱う実務的な構成
3. 一覧アプリとして必要な実装量と維持費
4. KDoc以外の要因を増やさず検証できること
5. 既存プロジェクトとの互換性と変更の戻しやすさ

## 選択肢の比較

| 選択肢 | 再現性 | 実務的な処理 | 実装・運用負担 | 判断 |
| --- | --- | --- | --- | --- |
| 現在のHello画面を維持 | 高 | データ処理なし | 最小 | 依頼の一覧・API処理を満たさず不採用 |
| 外部の公開API | 更新・通信・制限の影響あり | HTTPを含む | 利用条件確認と環境依存が増える | 今回は不採用 |
| 別プロセスのローカルHTTPサーバー | データは固定できる | ソケット通信まで通る | 起動・ポート・端末接続の管理が必要 | 通信層自体の検証時まで保留 |
| Repositoryが完成済みListを返すフェイク | 高 | JSON解析・変換を通らない | 小 | 単体テストには使用、アプリの共通データ経路には不採用 |
| Ktor MockEngine + 固定JSON | 高、ネットワーク不要 | HTTP要求/応答、解析、変換を通る | 小〜中 | 採用。ソケット/TLS/DNSは評価対象外 |
| 起動時にランダム商品を生成 | seed管理で再現可能 | 構造を作りやすい | 実行環境への依存を監視する必要 | 実行時生成は不採用 |
| 自作スクリプトで固定JSONを事前生成 | bytesまで比較可能 | 複数状態を自然に含められる | JSONが約1〜2MB増える | 採用。`--check`で再現性を確認 |

| 構造の選択肢 | 利点 | トレードオフ / 判断 |
| --- | --- | --- |
| 単一app内をdata / domain / presentation / diへ分離 | 既存構成を保ち、責務を追いやすい | コンパイラーによる層境界の強制はない。小規模の共通基盤として採用 |
| 各層をGradleモジュールに分割 | 層依存をビルドで強制可能 | Gradle設定とモジュール横断探索が増えるため現時点は保留 |
| 手動のViewModel FactoryによるDI | 小規模で生成コードがなく接続が明示的 | 依存が大幅に増えれば保守負担増。今回は採用 |
| Hilt等のDIフレームワーク | 大規模で依存管理しやすい | 本アプリでは追加設定・コード生成の費用が上回るため保留 |
| StateFlow + 明示的な追加読み込み | 状態・失敗・再試行を直接検証できる | Pagingライブラリの自動最適化は使わない。固定500件で採用 |
| Pagingライブラリ | 大きなデータや自動ロードに適する | 今回の規模と課題では仕組みが増えるため保留 |

## 決定と接続先

- JSON解析: kotlinx.serialization。Kotlin DTOとserializerを型付けし、欠損とenumを検証する。手書きJSON解析は保守負担、Gson/Moshiへの置き換えは比較上の利益がないため不採用。
- バージョン: 既存AGP 9.3.2 / Kotlin Compose 2.2.10を維持。Kotlin serialization plugin 2.2.10、Ktor 3.2.3、kotlinx.serialization 1.9.0、coroutines 1.10.2を明示し、実際のビルドで互換性を検証する。最新版への更新は目的に含めない。
- [`app/build.gradle.kts`](../../app/build.gradle.kts) / [`libs.versions.toml`](../../gradle/libs.versions.toml)が依存設定の実体。
- [`Product.kt`](../../app/src/main/java/com/example/kdoctest/domain/model/Product.kt)がUIへ渡すドメイン型。DTOはdata層に閉じる。
- [`CatalogViewModelFactory.kt`](../../app/src/main/java/com/example/kdoctest/di/CatalogViewModelFactory.kt)で依存を組み立て、ViewModelの終了時にHTTPクライアントを閉じる。
- 自作データ500件、標準50件/ページ、最大100件/ページ、日時固定。仕様の詳細は[README](../../README.md)。
- 架空の商品・名称・説明だけを使用し、外部画像や実在の顧客・会社情報は含めない。
- ベースにKDocは追加しない。将来の条件比較では意味情報の対応と閲覧範囲を別途固定する。

## 根拠と未確認事項

2026-09-12に確認した一次資料:

- [Ktor client testing](https://ktor.io/docs/client-testing.html): MockEngineが実接続なしで要求に対応する応答を返せること。
- [Android test doubles](https://developer.android.com/training/testing/fundamentals/test-doubles): 外部依存を置き換えて再現性を高めるテスト構成。
- [Android manual DI](https://developer.android.com/training/dependency-injection/manual): 小さな依存関係を明示的に組み立てる構成。
- [Android data layer](https://developer.android.com/topic/architecture/data-layer): Repositoryを境界にデータ取得を分離する構成。

KDocの有無や表現によるAIの性能差は未測定。この構成を採用したこと自体を性能改善の証拠にはしない。スキーマ・画面が課題に対して簡単すぎるかは、比較実験の予備試行で確認する。

## 検証・復帰・再検討

- 固定JSONの再生成一致、500件の参照/在庫整合、ページ欠落・重複、境界値、エラーとキャンセル、ViewModelの状態遷移を確認する。
- ビルド、Lint、エミュレーターでの一覧・ページ追加・空/失敗表示を確認し、実行結果を[検証記録](../verification.md)に残す。
- ローカルの変更であり、自動コミット・Pushは行わない。復帰時は基準コミット`19264e1`との差分を確認し、ユーザー変更を保った範囲で戻す。破壊的な一括リセットは行わない。
- 複数画面、永続化、大規模データ、実ネットワーク評価が必要になった場合、DI/モジュール/Paging/通信方式を再検討する。
