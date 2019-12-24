# SINETStream のライブデモ

## 温湿度センサーデータの収集・可視化

SINETStreamを利用すると、広域ネットワークを経由したデータの収集・解析を行うためのアプリケーションプログラムを容易に開発することができます。以下のデモでは、Raspberry Piに接続した温湿度センサーが測定したデータをクラウドに継続的に収集し、可視化しています。このアプリケーションプログラムでは、SINETStreamのAPIを利用することにより、センサーから収集されるデータをクラウドのサーバへ書き込む機能、およびサーバに収集されたデータを可視化プログラムに読み込む機能を実装しています。

<canvas id="myChart1" width="600" height="250"></canvas>
<canvas id="myChart2" width="600" height="250"></canvas>

<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.24.0/moment.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@2.9.3/dist/Chart.min.js" integrity="sha256-R4pqcOYV8lt7snxMQO/HSbVCFRPMdrhAFMH+vr9giYI=" crossorigin="anonymous"></script>
<script type="text/javascript" src="https://github.com/nagix/chartjs-plugin-colorschemes/releases/download/v0.4.0/chartjs-plugin-colorschemes.min.js"></script>
<script src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
<script src="{{ '/docs/livedemo/livedemo.js' | relative_url }}"></script>
