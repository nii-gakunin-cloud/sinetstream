#!/bin/sh
# iperfの結果をプロット
set -eu

TZOFF=$((9*60*60))
for client in compute.internal raspberrypi; do
for dir in up down; do
    {
        #grep -l "$client" iperf-*-$dir.json | while read F; do
        #    T=$(jq '.start.timestamp.timesecs' "$F")
        #    BW=$(jq '.end.sum_received.bytes/.end.sum_received.seconds/1024/1024' "$F")
        #    printf "%s %s\n" "$T" "$BW"
        #done
        grep -l "$client" iperf-*-$dir.json | 
            xargs jq --compact-output "[.start.timestamp.timesecs + $TZOFF, .end.sum_received.bytes/.end.sum_received.seconds/1024/1024]" |
            sed 's/[][]//g;s/,/ /'
    } > bw-$client-$dir.tmp
done
done

gnuplot <<'_EOF_'
set term sixel size 1024,1024 #medium
set style data linespoints
set grid
set xlabel "time"
set ylabel "bw[MiB/s]"
set timefmt "%s"
#set format x "%m/%d/%Y %H:%M:%S"
#set format x "%Y-%m-%d\n%H:%M:%S"
set format x "%m-%d\n%H:%M:%S"
set xdata time
plot "bw-compute.internal-down.tmp" using 1:2 title "aws-down", \
     "bw-compute.internal-up.tmp" using 1:2 title "aws-up"
plot "bw-raspberrypi-down.tmp" using 1:2 title "raspi-down", \
     "bw-raspberrypi-up.tmp" using 1:2 title "raspi-up"
_EOF_

for client in compute.internal raspberrypi; do
for dir in up down; do
    rm bw-$client-$dir.tmp
done
done
