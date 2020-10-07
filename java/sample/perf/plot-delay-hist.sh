#!/bin/sh
# 生データのメッセージ遅延時間のヒストグラム
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

sed '/^#/d' "$CSV" | awk -F, '
$2 > 0 {
    d = $1 - $4
    #d = sprintf("%.0e", d)+0  #有効数字1桁
    #d = int(sprintf("%.1e", d)+0)  #有効数字2桁
    d = int(d / 10) * 10  #10刻み
    #d = int(d / 50) * 50  #50刻み
    n[d]++
}
END {
    for (d in n)
        printf("%d,%d\n", d, n[d])
}' | sort -n -t, -k1 | gnuplot 9<&0 <<__EOF__
${TERMINAL:+set terminal $TERMINAL}
${OUTPUT:+set output '$OUTPUT'}
set datafile separator ","
set title "$CSV"
set style fill solid
set xlabel "Delay[ms]"
set ylabel "Freq"
#set key right top
unset key
set grid
plot "/dev/fd/9" with fillsteps
pause mouse close
__EOF__
}

for X; do
    PLOT "$X"
done
