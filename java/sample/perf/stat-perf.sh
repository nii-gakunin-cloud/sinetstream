#!/bin/sh

show_stat() {
printf "%s" "$CSV"
awk -F',' '
/^[^0-9]/ { next } # skip a non-data line
start == 0 {
    start = end = prev = $1
    total = 0
    n = 0
    imin = 1000000
    imax = -1
    next
}
start > 0 {
    total += $2
    end = $1
    interval = $1 - prev; prev = $1
    if (interval < imin)
        imin = interval
    if (imax < interval)
        imax = interval
    n++
    next
}
END {
    mib = total/1024.0/1024.0
    t = (end-start)/1000
    #printf("nmsg: %d\n", n)
    #printf("total: %f MiB (%d B)\n", mib, total)
    #printf("time: %f s\n", t)
    #printf("size: %f B/msg\n", 1.0*total/n)
    #printf("BW: %f MiB/s\n", mib/t)
    #printf("IOPS: %f msg/s\n", n/t)
    #printf("interval(min,avg,max): %d ms, %f ms, %d ms\n", imin, (end-start)/n, imax)
    printf(",%d", n)
    printf(",%d", total)
    printf(",%d", end-start)
    printf(",%d", imin)
    printf(",%d", n > 0 ? (end-start)/n : -999)
    printf(",%d", imax)
    printf(",%f", n > 0 ? 1.0*total/n : -999)
    printf(",%f", t > 0 ? mib/t : -999)
    printf(",%f", t > 0 ? n/t : -999)
    printf("\n")
}
'
}

printf "file"
printf ",n"
printf ",total[B]"
printf ",time[ms]"
printf ",imin[ms]"
printf ",avg[ms]"
printf ",imax[ms]"
printf ",size[B]"
printf ",bw[MiB/s]"
printf ",iops[msg/s]"
printf "\n"
for CSV in "$@"; do
    #echo "### $CSV"
    show_stat < "$CSV"
done
