<!--
Copyright (C) 2020-2021 National Institute of Informatics

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

[日本語](TUTORIAL-android-step2-webbrowser.md)

# TUTORIAL - A web interface for the sensor data visualization

<em>Table of contents</em>
<pre>
1. Introduction
2. Connecting to the web server
3. The main window
3.1 Window layout
3.2 Edit the window layout
4. The add-graph window
4.1 Window layout
4.2 Sensor types
</pre>


## 1. Introduction

The back-end system, deployed by the STEP2 tutorial container,
provides a web interface for the sensor data visualization.
By connecting to the designated URL from a web browser, user can
monitor sensor readout values per type being plotted as real-time
graphs.


## 2. Connecting to the web server

While the back-end system is up and running, connect to the
following URL from a web browser on an observing PC.
You can use your smartphone or tablet instead, of course.

```
    http://<server_address>/chart.html
```

The `server_address` element of the URL, specified by IP address
or FQDN (Fully Qualified Domain Name), is for the host machine
which runs the container image for the back-end system.
If the port number of the web server is not using the standard `80`,
specify the `server_address` as `<address>:<port>` format.

If you cannot connect to the web server even if the URL is correct,
it is likely to be a generic network problem.
Check for the end-end IP reachability (routing and firewall).
If you specify the `server_address` by FQDN, check if DNS works.


## 3. The main window
### 3.1 Window layout

The main window has header and body, where body is constructed as
vertical list of graphs per sensor type.

![Main window](images/step2/graph_main_window.png)

\<Legends\>
1. Graph title
2. Graph display region for the sensor readout values
3. Delete button for the graph display region
4. Add button for the graph display region

> **NOTE**
> <br>
> Some UI parts on the main window (such like buttons), are retrieved
> from a public server.
> If the main window is incomplete or looks different from the image
> shown above, make sure the observing PC is reachable to the Internet.

While the Android application "Sensor" is running, sensor readout
values are almost periodically published to the `Broker`, and then
processed by the back-end server to store them sequentially.
As long as the new sensor readout values are added in the database,
graph display region will be automatically updated.


### 3.2 Edit the window layout

In the main window, there are three graph display regions as the
initial setup.

* step_counter
* light
* accelerometer

To adapt the available sensor implementations on the Android device,
or to adapt the user-desired sensor types, user can edit the layout
of the main window body.
That is, you can remove unnecessary graph regions, or add some custom
graph regions with specified sensor types.

The modified window layout is saved in the local storage of your web
browser. So if you restart the web browser and connect to the web
server next time, saved layout is deployed.


## 4. The add-graph window
### 4.1 Window layout

By pressing the "Plus" button on the top-right corner of the main
window, the add-graph window as shown below will pop up.

![AddGraph window](images/step2/graph_add_window.png)

\<Legends\>
1. Sensor Type: Set the sensor type as a search key to extract data.
2. Graph Title: Set the title to be displayed on the top-left corner
of this graph.
3. `ADD` button: Commit the edit work and close this window.
4. `CLOSE` button: Cancel the edit work and close this window.

Both `Sensor Type` and `Graph Title` are mandatory parameters.
As for other items `Graph Type` and `Color`, you can change values
based on your preferences.
Once you have set parameters, press the `ADD` button to commit.
You can cancel at any time by pressing the `CLOSE` button.

> **NOTE**
> <br>
> `Sensor Type` value must be one of the `TypeName` shown in the
> next section. If there is a TYPO here, the graph data will not be
> displayed at all due to the search key mismatch.


### 4.2 Sensor types

Following is the list of sensor type available on this tutorial.

* The 1st column `Type` and the 2nd column `Symbol` corresponds to
the constants defined in the Android developer document
[Sensor](https://developer.android.com/reference/android/hardware/Sensor).

* Watch the 3rd column `TypeName` in the table.
This value (such like `accelerometer`) is the one that you must set
as the `Sensor Type`, in the add-graph window mentioned in the
previous section.

* The 4th column `AndroidVersion` shows the supporting Android version.
Available sensor types may vary in the future Android versions.


|Type|Symbol|TypeName|AndroidVersion|
|---|---|---|---|
|1|Sensor.TYPE_ACCELEROMETER|accelerometer||
|2|Sensor.TYPE_MAGNETIC_FIELD|magnetic_field||
|3|Sensor.TYPE_ORIENTATION|orientation||
|4|Sensor.TYPE_GYROSCOPE|gyroscope||
|5|Sensor.TYPE_LIGHT|light||
|6|Sensor.TYPE_PRESSURE|pressure||
|7|Sensor.TYPE_TEMPERATURE|temperature||
|8|Sensor.TYPE_PROXIMITY|proximity||
|9|Sensor.TYPE_GRAVITY|gravity||
|10|Sensor.TYPE_LINEAR_ACCELERATION|linear_acceleration||
|11|Sensor.TYPE_ROTATION_VECTOR|rotation_vector||
|12|Sensor.TYPE_RELATIVE_HUMIDITY|relative_humidity||
|13|Sensor.TYPE_AMBIENT_TEMPERATURE|ambient_temperature||
|14|Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED|magnetic_field_uncalibrated||
|15|Sensor.TYPE_GAME_ROTATION_VECTOR|game_rotation_vector||
|16|Sensor.TYPE_GYROSCOPE_UNCALIBRATED|gyroscope_uncalibrated||
|17|Sensor.TYPE_SIGNIFICANT_MOTION|significant_motion||
|18|Sensor.TYPE_STEP_DETECTOR|step_detector||
|19|Sensor.TYPE_STEP_COUNTER|step_counter||
|20|Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR|geomagnetic_rotation_vector||
|21|Sensor.TYPE_HEART_RATE|heart_rate|Android 4.4W+|
|28|Sensor.TYPE_POSE_6DOF|pose_6dof|Android 7.0+|
|29|Sensor.TYPE_STATIONARY_DETECT|stationary_detect|Android 7.0+|
|30|Sensor.TYPE_MOTION_DETECT|motion_detect|Android 7.0+|
|31|Sensor.TYPE_HEART_BEAT|heart_beat|Android 7.0+|
|34|Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT|low_latency_offbody_detect|Android 8.0+|
|35|Sensor.TYPE_ACCELEROMETER_UNCALIBRATED|accelerometer_uncalibrated|Android 8.0+|
|36|Sensor.TYPE_HINGE_ANGLE|hinge_angle|Android 11+|

