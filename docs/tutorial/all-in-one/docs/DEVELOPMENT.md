# 開発者向けドキュメント

## CI イメージ

CI テスト向けのイメージ。TLS/mTLS を含む全機能をサポートする。

- Kafka (9092, 9093) / Mosquitto (1883, 8883) を Plaintext + SSL で提供
- Private CA によるサーバー/クライアント証明書の自動生成
- 証明書配布用 HTTP サーバー (8080)

### 環境変数

| 環境変数 | デフォルト値 | 説明 |
|---|---|---|
| `ENABLE_BROKER` | `true` | Kafka/Mosquitto の起動制御 |
| `ENABLE_SSL` | `true` | SSL/TLS セットアップの実行制御 |
| `GENERATE_CLIENT_CERTS` | `true` | クライアント証明書の生成制御 |
| `SSL_CLIENT_AUTH` | `requested` | Kafka クライアント認証モード |
| `BROKER_HOSTNAME` | `$(hostname -f)` | ブローカーのホスト名（証明書 SAN に使用） |

## レジストリへの push

ビルド定義は `docker-bake.hcl` に記述されている。
`docker-bake.hcl` の `VERSION` 変数にはデフォルト値があるが、
ビルド時には `VERSION` ファイルの値を環境変数として明示的に渡す。

### CI イメージ

CI イメージは `bake --push` で amd64/arm64 のマルチプラットフォームイメージとして直接 push できる:

```console
docker buildx bake ci-multiplatform --push
```

### Tutorial イメージ

Tutorial イメージは amd64/arm64 と arm で異なる Dockerfile を使用する。
`docker buildx` は `platforms` 指定時にマニフェストリスト（OCI Image Index）を生成し、
`docker manifest create` のソースとして使用できないため、
アーキテクチャ別にローカルに読み込み・push した後、`docker manifest` で統合する。

前提条件として、マルチアーキテクチャのエミュレーション環境をセットアップする:

```console
docker run --privileged --rm tonistiigi/binfmt --install all
```

ビルド・push・統合:

```console
# VERSION ファイルから環境変数を設定（docker-bake.hcl の VERSION 変数を上書き）
export VERSION=$(cat VERSION)

# 1. アーキテクチャ別にローカルに読み込み・push
docker buildx bake tutorial-amd64 && docker push <amd64-tag>
docker buildx bake tutorial-arm64 && docker push <arm64-tag>
docker buildx bake tutorial-arm   && docker push <arm-tag>

# 2. マニフェストで統合
docker manifest create <tag> <amd64-tag> <arm64-tag> <arm-tag>
docker manifest push <tag>
```

具体的なタグ名は「タグ付けルール」の節を参照。
CI では `.gitlab-ci.yml` がこの手順を自動化している。

## タグ付けルール

バージョンはプロジェクトルートの `VERSION` ファイルで管理する。
`docker-bake.hcl` にもデフォルト値があるが、ビルド時には `VERSION` ファイルから
環境変数 `VERSION` を設定して使用する。

GitLab CI では git tag が設定されている場合、`$CI_COMMIT_TAG` の値（`v` プレフィックスは除去）を
VERSION として使用する。tag がない場合は `VERSION` ファイルの値を使用する。

| ソース | 優先度 | 説明 |
|---|---|---|
| `$CI_COMMIT_TAG` | 高（CI のみ） | git tag の値（`v` プレフィックス除去、例: `v1.10.0` → `1.10.0`） |
| `VERSION` ファイル | 通常 | バージョン番号（例: `1.10.0`） |

`docker-bake.hcl` の変数:

| 変数 | デフォルト値 | 説明 |
|---|---|---|
| `REGISTRY` | `harbor.vcloud.nii.ac.jp/sinetstream` | レジストリのパス |
| `VERSION` | `1.10.0` | バージョン（環境変数 `VERSION` で上書き） |
| `DATE` | ビルド日（自動生成） | 日付タグ（YYYYMMDD 形式） |

ローカルビルド時のタグ（`docker buildx bake` で付与）:
- `ss-tutorial:latest`
- 上記レジストリ付きタグ（`docker-bake.hcl` の `VERSION` デフォルト値に基づく）

Tutorial イメージのタグ（マニフェスト統合後）:
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:1`
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:1.10`
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:1.10.0`
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:1.10.0-20260324`

Tutorial アーキテクチャ別タグ（中間成果物）:
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:1.10.0-20260324-amd64`
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:1.10.0-20260324-arm64`
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:1.10.0-20260324-arm`

CI イメージのタグ:
- `harbor.vcloud.nii.ac.jp/sinetstream/tutorial:ci-20260324`
