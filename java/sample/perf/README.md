**準備中** (2019-12-12 15:15:40 JST)

# 性能測定

## ビルド方法

クライアント・マシン上で以下のことを行う:

- まずSINETStream Javaビルドする。
- このディレクトリ(`sinetstream/java/sample/perf`)で `make` を実行する。

## 実行方法

- KafkaブローカとMQTTブローカを用意して立ち上げる。
- `~/.sinetstream_config.yml` をクライアント・マシン上に作成する。
    - サンプル: `sinetstream/java/sample/perf/dot.sinetstream_config.yml`
- `~/.ssh/config` を編集して、テストスクリプトを実行するマシンからクライアント・マシンにSSHログインできるようにする。
  ```
  Host *
    ServerAliveInterval 10
    Compression yes
  Host aws2
    Hostname 172.30.2.43
    User centos
  Host raspi
    Hostname localhost
    User pi
    Port 20022
  ```
- テストスクリプト `test.sh` を編集する:
    - SERVICE を環境にあわせて設定する
    - BROKER を環境にあわせて設定する
    - HOST を環境にあわせて設定する
- テストスクリプトを実行する: `script -c ./test.sh`
- クライアント・マシン上に作成されたテスト結果(`perftest-*.csv`)を集める。
    - このCSVファイルのフォーマットは以下のとおり:
        - 送信時刻または受信時刻 (ms)
        - メッセージサイズ (B)
- CSVを解析して統計値を得る:
    - `sinetstream/python/sample/perf/stat-perf.sh perftest-*.csv` を実行すると平均スループットなどが得られる。
