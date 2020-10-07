#!/bin/sh

# file,n,total[B],time[ms],imin[ms],avg[ms],imax[ms],size[B],bw[MiB/s],iops[msg/s]
summary_csv="$1"
awk -F"," '
BEGIN {
    OFS=","
    print "snd,rcv,sys,sz,proto,role,n,total,time,imin,avg,imax,size,bw,iops"
}
NR==1 { next }   # skip header
{
    file=$1
    sub(/.csv/, "", file)
    n=split(file, a, "-")
    snd=a[4]
    rcv=a[5]
    sys=a[6]
    sz=a[7]
    if (a[8] == "pub" || a[8] == "sub") {
        proto="plain"
        role=a[8]
    } else {
        proto=a[8]
        role=a[9]
    }
    rest=""
    for (i = 2; i <= NF; i++) {
        rest = rest sprintf("%s%s", OFS, $i)
    }
    print snd, rcv, sys, sz, proto, role rest
}
' "$summary_csv"
