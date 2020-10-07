#!/bin/sh
# 生データのメッセージ遅延時間 vs シーケンス番号のグラフをプロット
set -eu

TERMINAL="sixel size 800,600"
OUTPUT=""

while getopts "T:O:" OPT ; do
    case "$OPT" in
    T) TERMINAL="$OPTARG";;
    O) OUTPUT="$OPTARG";;
    *) echo "INVALID OPTION: $OPT" exit 1;;
    esac
done
shift $((OPTIND - 1))

PLOT() {
CSV="$1"  # consumer's CSV

# delete size=0 records
sed '/^#/d' "$CSV" | sort -n -t, -k3 | awk -F, '{ if (last+1 != $3) print ""; last = $3; if ($2 > 0) print; else print "" }' | gnuplot 9<&0 <<__EOF__

${TERMINAL:+set terminal $TERMINAL}
${OUTPUT:+set output '$OUTPUT'}
set datafile separator ","
set title "$CSV"
set style fill solid
set xlabel "SeqNo"
set ylabel "Delay[ms]"
#set key right top
unset key
set grid
#plot "$CSV" using (column(3)):(column(1)-column(4)) with linespoints
plot "/dev/fd/9" using (column(3)):(column(1)-column(4)) with linespoints
pause mouse close
__EOF__
}

for X; do
    PLOT "$X"
done
