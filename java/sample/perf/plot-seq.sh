#!/bin/sh
# 生データのメッセージ到着時間 vs シーケンス番号のグラフをプロットする
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

if [ -n "$TERMINAL" ]; then
    case "$TERMINAL" in
    *png*) OSFX=".png";;
    *svg*) OSFX=".svg";;
    esac
fi

PLOT1() {
local CSV="$1"  # consumer's CSV
#開始時刻を得る
MIN=$(sed '/^#/d' "$CSV" | cut -d, -f4 | sed '/^$/d' | sort -n | head -1)

if [ -n "${OSFX-}" -a -z "${OUTPUT-}" ]; then
    local OUTPUT="${CSV%.csv}${OSFX}"
fi

# delete size=0 records
awk -F, 'NR > 1 { if ($2 > 0) print; else print "" }' "$CSV" >"tmp-$CSV"

gnuplot <<__EOF__
${TERMINAL:+set terminal $TERMINAL}
${OUTPUT:+set output '$OUTPUT'}
set datafile separator ","
set title "$CSV"
set style fill solid
set xlabel "Time [ms]"
set ylabel "SeqNo"
set key left top
set grid
plot "tmp-$CSV" using (column(4)-$MIN.0):(column(3)) title "send" with fillsteps, \
     "tmp-$CSV" using (column(1)-$MIN.0):(column(3)) title "comp/recv" with steps
pause mouse close
__EOF__

rm "tmp-$CSV"
}

PLOT2() {
local PUB_CSV="$1"
local SUB_CSV="$2"

#開始時刻を得る
MIN=$(sed '/^#/d' "$PUB_CSV" "$SUB_CSV"| cut -d, -f4 | sed '/^$/d' | sort -n | head -1)

if [ -n "${OSFX-}" -a -z "${OUTPUT-}" ]; then
    local OUTPUT="${PUB_CSV%-pub.csv}-pubsub${OSFX}"
fi

# delete size=0 records
awk -F, 'NR > 1 { if ($2 > 0) print; else print "" }' "$PUB_CSV" >"tmp-$PUB_CSV"
awk -F, 'NR > 1 { if ($2 > 0) print; else print "" }' "$SUB_CSV" >"tmp-$SUB_CSV"

gnuplot <<__EOF__
${TERMINAL:+set terminal $TERMINAL}
${OUTPUT:+set output '$OUTPUT'}
set datafile separator ","
set title "$PUB_CSV\n$SUB_CSV"
set style fill solid
set xlabel "Time [ms]"
set ylabel "SeqNo"
set key left top
set grid
plot "tmp-$PUB_CSV" using (column(4)-$MIN.0):(column(3)) title "send" with fillsteps, \
     "tmp-$PUB_CSV" using (column(1)-$MIN.0):(column(3)) title "comp" with steps, \
     "tmp-$SUB_CSV" using (column(1)-$MIN.0):(column(3)) title "recv" with steps
pause mouse close
__EOF__

rm "tmp-$PUB_CSV" "tmp-$SUB_CSV"
}

while [ $# -gt 0 ]; do
    if [ $# -eq 1 ]; then
        PLOT1 "$1"
        shift
        continue
    fi

    case "$1" in
    *pub*)
        if [ "${1/-pub/-sub}" = "$2" ]; then
            PLOT2 "$1" "$2"
            shift 2
            continue
        fi
        ;;
    esac
    PLOT1 "$1"
    shift
done
