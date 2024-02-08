<!--
Copyright (C) 2023 National Institute of Informatics

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

# ndarray plugin

[NumPy](https://numpy.org/)のndarrayをSINETStreamで扱うためのプラグイン

## 利用例

### 設定ファイル

```yaml
header:
  version: 2
config:
  ndarray-1:
    type: kafka
    brokers: kafka.example.org
    topic: test-ndarray
    consistency: AT_LEAST_ONCE
    value_type: ndarray

  ndarray-2:
    type: mqtt
    brokers: mqtt.example.org
    topic: test-ndarray
    consistency: AT_LEAST_ONCE
    value_type: ndarray
```

### producer

```python
import numpy as np
from sinetstream import MessageWriter

SERVICE = "ndarray-1"
data = np.array([1, 2, 3, 4, 5])
with MessageWriter(service=SERVICE) as writer:
    writer.publish(data)
```

### consumer

```python
import numpy as np
from sinetstream import MessageReader

SERVICE = "ndarray-1"
with MessageReader(service=SERVICE) as reader:
    for msg in reader:
        print(np.array2string(msg.value))
```
