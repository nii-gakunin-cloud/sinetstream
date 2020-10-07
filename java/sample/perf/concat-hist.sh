#!/bin/sh
# plot-hist.shで作ったグラフを1枚にまとめる
set -ue

FORMAT="${1:-pdf}"

for col in bw drop delay; do

    FLST=""
    for client in aws raspi; do
     for qsys in kafka mqtt; do
      for xxx in plain sync async; do
        FLST="$FLST $client-$qsys-$xxx-$col.$FORMAT"
      done
     done
    done

    OFILE="perftest-hist-all-$col.$FORMAT"

    case "$FORMAT" in
    png)
	for x in $FLST; do
	    GEOM=$(identify "$x" | cut -d' ' -f4)
	    break
	done

	# 3x means plain, sync, and async.
	montage -tile 3x -geometry $GEOM $FLST $OFILE
	;;
    pdf)
	pdfunite $FLST $OFILE
	;;
    *)
	echo "$FMT is not supported"
	;;
    esac
done
