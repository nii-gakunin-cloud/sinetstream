<!--
Copyright (C) 2020 National Institute of Informatics

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

[日本語](livedemo.md)

# Live demo of SINETStream

## Collection and visualization of temperature and humidity sensor data

With SINETStream you can easily develop a program that collect and analyze data from sensors distributed over a wide area network.
There are two programs, a reader and a writer, working for this demo.
The writer program running on a Raspberry Pi with temperature and humidity sensors continuously sends data to a messaging server called "SINETStream server" on AWS.
The reader program on AWS then receives the data from the SINETStream server and the processed data are periodically visualized here.
These programs use the SINETStream API to implement such functions as writing data from the sensor devices to the SINETStream server, and reading data from the server for visualization.

<canvas id="myChart1" width="600" height="250"></canvas>
<canvas id="myChart2" width="600" height="250"></canvas>

<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.24.0/moment.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@2.9.3/dist/Chart.min.js" integrity="sha256-R4pqcOYV8lt7snxMQO/HSbVCFRPMdrhAFMH+vr9giYI=" crossorigin="anonymous"></script>
<script type="text/javascript" src="https://github.com/nagix/chartjs-plugin-colorschemes/releases/download/v0.4.0/chartjs-plugin-colorschemes.min.js"></script>
<script src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
<script src="{{ '/docs/livedemo/livedemo.js' | relative_url }}"></script>
