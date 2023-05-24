# SINETStream CLI (Python版,Java版)

## 概要

本ツールは
Python版は
[SINETStream Writer/ReaderAPI](https://www.sinetstream.net/docs/userguide/api-python.html)
を
Java版は
[SINETStream Writer/Reader API](https://www.sinetstream.net/docs/userguide/api-java.html)
をつかって指定のブローカ・トピックに対してメッセージをpublish/subscribeする。
ブローカ等のオプション情報は `.sinetstream_config.yml` ファイルで指定できるが、
コマンドラインからも指定できる。

## 前提条件

* Python版
    * Python3.8以上
    * SINETStream v1.7.0(Python)
* Java版
    * Java11以上
    * SINETStream v1.7.3(Java)
* [SINETStream API 設定ファイル](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/docs/userguide/config.md) (データ送受信先の接続情報等が記述されているもの)

## ビルド

Python版ではビルドの必要はない。

Java版では次のコマンドを実行してソースコードからビルドするか、SINETStream公式サイトからビルド生成物のzipファイルをダウンロードする。

```
$ ./gradlew build
```

` build/distributions/sinetstream_cli-*` にビルドしたsinetstream_cliコマンドが
zip, tar アーカイブ形式で作成されている。

## インストール

Python版ではpipをつかってネットワークインストールするかsinetstream_cliスクリプトを直接実行する。

```
# ネットワークインストール
$ pip install --user sinetstream_cli
```

```
# SINETStreamがインストール済みの場合はインストールせずスクリプトを直接実行できる
$ python src/sinetstream_cli/sinetstream_cli.py ...
```

Java版ではビルド生成物のアーカイブファイルをインストール先のディレクトリに展開する。

```
# 展開
$ tar xvf /path/to/sinetstream_cli-*tar
 or
$ unzip /path/to/sinetstream_cli-*zip
```

展開したディレクトリの `bin/sinetstream_cli` が実行スクリプトになっている。

```
# 実行
$ sinetstream_cli-*/bin/sinetstream_cli ...
```

## 書式

sinetstream-cli write
[--service *SERVICE*]
[--config *CONFIG*]
[--text]
[--file *FILE*]
[--message *MESSAGE*]
[--line]
[*KEY*=*VALUE* [*KEY*=*VALUE* ...]]

sinetstream_cli read
[--service *SERVICE*]
[--config *CONFIG*]
[--text]
[--verbose]
[--raw]
[--file *DIR*]
[--count *COUNT*]
[*KEY*=*VALUE* [*KEY*=*VALUE* ...]]

## 説明

`sinetstream-cli write` を実行すると標準入力からメッセージを読み込んでSINETStream APIをつかってブローカーにメッセージを送信する。

`sinetstream-cli read` を実行するとSINETStream APIをつかってブローカーからメッセージを受信して標準出力にメッセージを出力する。


## オプション

* `--service` *SERVICE*
    * SINETStream API で使用するサービス名
    * CONFIGもSERVICEも指定されていない場合は実行時に `./.sinetstream_config.yml` が作成されダミーのサービスが定義される。
        * 終了時にこのファイルは削除される。
        * sinetstream_cliをおなじディレクトリで複数実行した場合は一時作成された `./.sinetstream_config.yml` は共有される。
* --config *CONFIG*
    * SINETStream API で使用するコンフィグ名
    * 指定するとコンフィグサーバーから設定を取得する。
* --text
    * value_typeをTEXTに設定して入出力はテキストとして扱われる。
* --file *PATH*
    * writeのときは指定されたファイルからメッセージを読み込む。
    * readのときは指定されたディレクトリの下にメッセージを保存する。ファイル名は "トピック名-ランダム文字列-シリアル番号" である。
      (トピック名はパーセントエンコーディングされる。)
* --message *MESSAGE*
    * writeのとき、コマンドラインから送信メッセージを指定する。
    * 省略時は標準入力から送信メッセージを読み込む。
* --line
    * writeのとき、標準入力から送信メッセージを読み込むとき1行ごとに送信する。
* --verbose
    * 受信メッセージを出力するときにトピック名とシリアル番号を表示する。
* --raw
    * 受信メッセージをだけを表示する。
* --count *COUNT*
    * COUNT数のメッセージを受信したら終了する。
* *KEY*=*VALUE*
    * SINETStreamの設定パラメータを個別に指定する。
    * 値はYAMLで書く。
    * 複数階層になるパラメータを指定するときは `.` で連結する。
    * 例:
        * ブローカーにmqtt.example.netを指定する: `brokers=mqtt.example.net`
        * ブローカーにkafka1.example.netとkafka2.example.netを指定する: `brokers=[kafka1.example.net,kafka2.example.net]`
        * 圧縮アルゴリズムにzstdを指定する: `compression.algorithm=zstd`

## 実行例

### コマンドラインから1メッセージを送信

#### <read側での入力内容>

1メッセージを受信したらコマンドが終了するように `--count 1` を指定している。

```
$ sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1

```

#### <write側での入力内容>

`--message` オプションをつかってコマンドラインで送信メッセージを指定している。

```
$ sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --message 'this is a test message.'
$
```

#### <read側での出力内容>

```
[#1] Received on "test"
this is a test message.
$
```




### ./.sinetstream_config.ymlを参照

#### <前処理の内容>

```
$ echo '{"test-1":{"value_type":"text","type":"mqtt","brokers":"mqtt","topic":"test"}}' >.sinetstream_config.yml
```

#### <read側での入力内容>

```
$ sinetstream_cli read --service test-1 --text --count 1
[#1] Received on "test"
this is a test message.

```

#### <write側での入力内容>

```
$ sinetstream_cli write --service test-1 --text --message 'this is a test message.'
$
```

#### <read側での出力内容>

```
[#1] Received on "test"
this is a test message.
$
```

#### <後処理の内容>

```
$ rm .sinetstream_config.yml
$
```

### ファイルの内容を送信

#### <前処理の内容>

```
$ echo 'this is a test message.' >message.txt
$
```

#### <read側での入力内容>

```
$ sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1

```

#### <write側での入力内容>

`--file` オプションをつかって送信するファイルを指定している。

```
$ sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --file message.txt
rm message.txt
$
```

#### <read側での出力内容>

```
[#1] Received on "test"
this is a test message.
$
```

#### <後処理の内容>

```
$ rm message.txt
$
```

### 受信時にメッセージ内容だけを表示

#### <read側での入力内容>

`--raw` オプションをつかって `[#1] Received on "test"` のようなメッセージのメタ情報表示を抑制している。

```
$ sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1 --raw

```

#### <write側での入力内容>

```
$ sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --message 'this is a test message.'
$
```

#### <read側での出力内容>

```
this is a test message.
$
```

### バイナリファイルの送受信


#### <前処理の内容>

```
$ dd if=/dev/urandom bs=1024 count=16 of=message.bin
16+0 records in
16+0 records out
16384 bytes (16 kB, 16 KiB) copied, 0.000612424 s, 26.8 MB/s
$
```

#### <read側での入力内容>


`--raw` オプションをつけないと出力にメタ情報表示が含まれてバイナリデータが壊れる。

```
$ sinetstream_cli read type=mqtt brokers=mqtt topic=test --count 1 --raw >receivd.bin

```

#### <write側での入力内容>

テキストではなくバイナリメッセージを送るときは `--text` オプションをつけない。

```
$ sinetstream_cli write type=mqtt brokers=mqtt topic=test <message.bin
$
```

#### <read側での出力内容>

```

$
```

#### <後処理の内容>

```
$ openssl sha1 *.bin
SHA1(message.bin)= c99366ce6c44d7cf10a13174a10e168bd100e4aa
SHA1(receivd.bin)= c99366ce6c44d7cf10a13174a10e168bd100e4aa
$ rm message.bin receivd.bin
$
```

### 受信メッセージをファイルに保存

#### <read側での入力内容>

`--file` オプションをつかうと受信メッセージをファイルに保存できる。

```
$ mkdir recved && sinetstream_cli read --text type=mqtt brokers=mqtt topic=test/sub --count 2 --file recved

```

#### <write側での入力内容>

```
$ sinetstream_cli write --text type=mqtt brokers=mqtt topic=test/sub --message 'this is the first message.'
$ sinetstream_cli write --text type=mqtt brokers=mqtt topic=test/sub --message 'this is the second message.'
$
```

#### <read側での出力内容>

```
$
```

#### <後処理の内容>


トピック名に含まれている `/` は `%2F` に置き換えられる。

```
$ ls -alF recved
total 16
cat recved/test%2Fsub-*-1
drwxrwxr-x 2 koie koie 4096 Nov 17 15:03 ./
drwxrwxr-x 3 koie koie 4096 Nov 17 15:03 ../
-rw-rw-r-- 1 koie koie   26 Nov 17 15:03 test%2Fsub-VUOcR96miVaaHXrC-1
-rw-rw-r-- 1 koie koie   27 Nov 17 15:03 test%2Fsub-VUOcR96miVaaHXrC-2
$ cat recved/test%2Fsub-*-1
this is the first message.
$ cat recved/test%2Fsub-*-2
this is the second message.
$ rm -rf recved
$
```

### パラメータ指定

#### <read側での入力内容>

```
$ sinetstream_cli read --text type=mqtt brokers=mqtt data_compression=true compression.algorithm=zstd topic=test --count 1

```

#### <write側での入力内容>

深いところにあるパラメータを指定する場合は `.` でつなぐ。
この例では圧縮アルゴリズムにzstdを指定している。

```
$ sinetstream_cli write --text type=mqtt brokers=mqtt data_compression=true compression.algorithm=zstd topic=test --message 'this is a test message.'
$
```

#### <read側での出力内容>

```
[#1] Received on "test"
this is a test message.
$
```

コマンドラインでの設定を `.sinetstream_config.yml` にしたとすると次のようになる:

```
service-1:
    value_type: text
    type: mqtt
    brokers: mqtt
    data_compression: true
    compression:
        algorithm: zstd
    topic: test
```

### YAMLパラメータ

#### <read側での入力内容>

```
$ sinetstream_cli read --text type=kafka brokers='[kafka1,kafka2]' topic=test --count 1

```

#### <write側での入力内容>

パラメータ指定で値は(SINETStream設定ファイルとおなじように)YAMLとして解釈される。
この例ではKafkaのブローカ指定で2つのサーバーを指定している。

```
$ sinetstream_cli write --text type=kafka brokers='[kafka1,kafka2]' topic=test --message 'this is a test message.'
...
$
```

#### <read側での出力内容>

```
...
[#1] Received on "test"
this is a test message.
...
$
```

### 1行ずつ送信

#### <read側での入力内容>

```
$ sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1

```

#### <write側での入力内容>

標準入力から複数行からなるデータを流し込んでも1メッセージとしてあつかわれる。

```
$ echo 'aaa
> bbb
> ccc' | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test
$
```

#### <read側での出力内容>

```
[#1] Received on "test"
aaa
bbb
ccc
$
```

`--line` オプションを指定すると1行ずつ送信できる。

#### <read側での入力内容>

```
$ sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 3

```

#### <write側での入力内容>

```
$ echo 'aaa
> bbb
> ccc' | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --line
$
```

#### <read側での出力内容>

```
[#1] Received on "test"
aaa
[#2] Received on "test"
bbb
[#3] Received on "test"
ccc
$
```

### JSONを送受信

`jq -c` をつかって1行にまとめてから送信すると
受信側は行単位でJSONを処理できて都合がよい。

* 参照 :  [jqコマンド](https://stedolan.github.io/jq/)

#### <read側での入力内容>

```
$ sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 2 --raw | while read X; do echo "$X" | jq 'add'; done

```

#### <write側での入力内容>

```
$ echo '[1,2,3]' | jq -c . | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test
$ echo '[10,20,30]' | jq -c . | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test
$
```

#### <read側での出力内容>

```
6
60
$
```
