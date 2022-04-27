### 広域データ収集・解析プログラム開発支援ソフトウェアパッケージ<br>「SINETStream」デモパッケージ最新版公開のお知らせ

国立情報学研究所 クラウド基盤研究開発センターでは，
モバイル網をSINETの足回りとして活用するモバイルSINETを利用するアプリケーションの開発者を支援するために，
広域データ収集・解析プログラム開発支援ソフトウェアパッケージ
「SINETStream」を開発しています．

この取り組みでは，本パッケージの提供するAPIを利用することにより，
モバイルSINETを介したデータの収集・蓄積・解析を行うプログラム開発を容易にすることを目指しています．

この度，SINETStreamを用いたIoTシステムを構築するためのデモパッケージ最新版を公開いたしました．

https://github.com/nii-gakunin-cloud/sinetstream-demo

本パッケージで提供する主な機能等は以下の通りです．

* numerical sensor data<br>
    数値センサデータを収集，可視化するIoTシステムを構築することができます．
    Raspberry Piに接続したセンサーで計測した数値をサーバに送信し，ZabbixまたはGrafanaで可視化します．
    *  Sensor:<br>
       SINETStreamライブラリを利用して温度湿度センサー(DHT11/SHT3x)，
       CO２センサー(SCD41)の計測値をサーバに送信する実装例と手順書を提供します．
    * Server:<br>
       Kafkaブローカで受信したセンサーの計測値を SINETStreamライブラリを利用してZabbix/Grafanaでグラフ表示などの可視化を行う手順と資材を提供します．
* video streaming<br>
    動画像データを収集，加工，可視化するIoTシステムを構築することができます．
    Raspberry Piのカメラで撮影した画像をサーバのGPUノードで処理し，その結果をクライアントで表示するシステムを構築します．
    * Sensor:<br>
	画像をサーバに送信するSINETStreamライブラリと実行手順を示します．
    * Server:<br>
	Kafkaブローカで受信した画像をSINETStreamライブラリを利用してGPUノードの OpenPose/YOLOv5 で処理する手順と資材を提供します．
    * Viewer:<br>
       SINETStreamライブラリを利用してサーバ(Kafkaブローカ)の画像をクライアントで表示する Pythonプログラムと実行手順を示します．
* option<br>
    本パッケージの任意の設定項目やテストツールなどに関する手順書と資材です．
    * Server:<br>
	サーバ部分の任意の設定項目(Kafkaブローカの構築、メッセージの保存，
	MQTT (Mosquitto)メッセージのKafkaブローカ転送)に関する手順書とその資材を提供します．
    * Producer:<br>
        テストデータ(動画ファイルから切り出した画像)をサーバ(Kafkaブローカ)に送信する環境を構築する手順を示します．
    * Consumer:<br>
        サーバ(Kafkaブローカ)送られたテキストデータをクライアントで表示するPythonプログラムと実行手順を示します．

本パッケージが，モバイルSINETを利用したデータ解析環境構築等のお役にたてれば幸いです．
本パッケージのご利用やご質問については以下をご参照ください．

SINETStreamサイト：[https://www.sinetstream.net/](https://www.sinetstream.net/)

国立情報学研究所では，引き続き，モバイルSINETをご活用いただくためのソフトウェアやAPI等の整備を進めて参ります．
どうぞよろしくお願いいたします．
