##!/bin/sh
set -exu

TYPESCRIPT="typescript"
script -c ./test.sh "$TYPESCRIPT"
LOGDIR=$(sed -n '/LOGDIR=/{p;q;}' "$TYPESCRIPT" | cut -d= -f2 | tr -d '\r')
cd "$LOGDIR"
cp "../test.sh" "."
cp "../$TYPESCRIPT" "."
../stat-perf.sh perftest-*.csv > stat.csv
../check-stat.sh stat.csv > chk
