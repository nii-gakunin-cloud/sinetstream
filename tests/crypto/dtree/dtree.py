import numpy as np
import pandas as pd

df = pd.read_csv("result.csv")
X_name = ["writer","reader","algorithm","key_length","mode","padding","kd_algorithm","kd_salt_bytes","kd_iteration","kd_prf"]
y_name = "result"

import sklearn.preprocessing
res = ["x", "x"]
for col in ["writer","reader","algorithm"             ,"mode","padding","kd_algorithm"                               ,"kd_prf","result"]:
    le = sklearn.preprocessing.LabelEncoder()
    le = le.fit(df[col])
    df[col] = le.transform(df[col])
    print(f"{col}:")
    for i,k in enumerate(le.classes_):
        print(f"  {i}={k}")
        if col == "result":
            res[i] = k

X_train = df[X_name]
y_train = df[y_name]

import sklearn.tree
dtcls = sklearn.tree.DecisionTreeClassifier(max_depth=3)  # max_depth may be tuned.
dtcls.fit(X_train, y_train)

f = sklearn.tree.export_graphviz(dtcls, out_file = 'dtree.dot', feature_names = X_name,
                           class_names = res, filled = True, rounded = True)
# http://www.webgraphviz.com/
