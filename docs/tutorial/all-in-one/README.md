# SINETStream Tutorial Container

SINETStream のチュートリアル環境として、メッセージブローカー（Apache Kafka / Eclipse Mosquitto）を提供するコンテナイメージ。

## 概要

- Kafka (9092) / Mosquitto (1883) を Plaintext で提供
- SSH サーバー、ユーザーアカウント (user01) を含む
- コンテナ内で user01 として操作する場合は `docker exec -it -u user01 <container> bash`

> **注意**: Kafka 4.x の KRaft モードでは CONTROLLER リスナーがデフォルトで 9093 を使用する。
> 本コンテナイメージでは CONTROLLER リスナーを 19093 に変更している。

## 環境変数

| 環境変数 | デフォルト値 | 説明 |
|---|---|---|
| `ENABLE_BROKER` | `true` | Kafka/Mosquitto の起動制御 |
| `ENABLE_SSHD` | `false` | SSH サーバーの起動制御 |

## ビルド

`docker buildx bake` でネイティブプラットフォーム向けにビルドし、ローカルの Docker デーモンに読み込む:

```console
docker buildx bake
```

ビルドされたイメージには `ss-tutorial:latest` タグが付与される。
バージョン付きタグ（`<registry>/tutorial:<version>` 等）も付与されるが、
これは `docker-bake.hcl` の `VERSION` 変数のデフォルト値に基づく。
レジストリへの push 時のバージョン管理については [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) を参照。

## マルチアーキテクチャ対応

| アーキテクチャ | Dockerfile | ベースイメージ |
|---|---|---|
| linux/amd64 | Dockerfile | Rocky Linux 10 |
| linux/arm64 | Dockerfile | Rocky Linux 10 |
| linux/arm | Dockerfile.debian | Debian trixie |

## ディレクトリ構成

```
.
├── Dockerfile              # Rocky Linux ベース（amd64/arm64）
├── Dockerfile.debian       # Debian ベース（arm）
├── VERSION                 # バージョン番号
├── docker-bake.hcl         # Docker Bake ビルド定義
├── LICENSE                 # Apache License 2.0 + サードパーティライセンス
├── NOTICE                  # 著作権表示・帰属情報
├── docs/
│   └── DEVELOPMENT.md      # 開発者向けドキュメント
└── scripts/
    ├── tutorial/
    │   ├── init.sh             # Rocky Linux 用エントリポイント
    │   ├── init-debian.sh      # Debian 用エントリポイント
    │   └── supervisord.conf    # Supervisor 設定（sshd + kafka + mosquitto）
    └── ci/
        ├── init.sh             # Rocky Linux 用エントリポイント
        ├── init-debian.sh      # Debian 用エントリポイント
        └── supervisord.conf    # Supervisor 設定（kafka + mosquitto）
```

## ライセンス

本プロジェクトは [Apache License 2.0](LICENSE) の下で公開されています。

コンテナイメージには以下のサードパーティソフトウェアが含まれます。
詳細は [LICENSE](LICENSE) ファイルの THIRD-PARTY LICENSES セクションを参照してください。

| ソフトウェア | ライセンス |
|-------------|-----------|
| Eclipse Mosquitto | Eclipse Public License 2.0 |
| Apache Kafka | Apache License 2.0 |
| OpenJDK | GPL 2.0 with Classpath Exception |
| Supervisor | Repoze Public License (BSD-derived) |
