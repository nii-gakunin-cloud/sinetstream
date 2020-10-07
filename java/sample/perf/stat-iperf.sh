#!/bin/sh
#iperf --jsonの出力を解析して平均スループットとスループットの標準偏差を計算する
set -eu
IPERF_JSON="${1:--}"

cat "$IPERF_JSON" |
jq '.intervals[].sum | .bytes / .seconds' |
awk '
{
    printf("%d: %f [MiB/s]\n", NR, $1/1024/1024)
    x[NR] = $1
    sum += $1
}
END {
    avg = sum / NR
    for (i in x) {
        xx = x[i] - avg
        var += xx * xx
    }
    var /= (NR - 1)
    printf("avg=%f std=%f [MiB/s]\n", avg/1024/1024, sqrt(var)/1024/1024)
}'

