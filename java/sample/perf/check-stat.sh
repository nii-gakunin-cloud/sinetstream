#!/bin/sh
# stat.csvをみて
#   測定時間が長すぎ・短すぎ
#   遅延時間が長すぎ
#   エラー発生
# した測定項目を表示する
# 総通信量の計算をする。

STAT_FILE="$1"

#file,snd,rcv,qsys,qos,lib,crypt,msz,nmsg,role,n[msg],total[B],time[ms],imin[ms],iavg[ms],imax[ms],size[B],bw[MiB/s],iops[k msg/s],dmin[ms],davg[ms],dmax[ms],idev[ms],ddev[ms],drop[msg],drop[%],dup[msg],errcnt,note


awk -F, '
NR>1 {
    i = 1
    file = $(i++)
    snd = $(i++)
    rcv = $(i++)
    qsys = $(i++)
    qos = $(i++)
    lib = $(i++)
    crypt = $(i++)
    msz = $(i++)
    nmsg = $(i++)
    role = $(i++)

    r_n = $(i++)
    r_total = $(i++)
    r_time = $(i++)
    r_imin = $(i++)
    r_avg = $(i++)
    r_imax = $(i++)
    r_size = $(i++)
    r_bw = $(i++)
    r_iops = $(i++)
    r_dmin = $(i++)
    r_davg = $(i++)
    r_dmax = $(i++)
    r_idev = $(i++)
    r_ddev = $(i++)
    r_drop = $(i++)
    r_dropp = $(i++)
    r_dup = $(i++)
    r_errcnt = $(i++)
    r_note = $(i++)
    nf = i-1

    printf("%s:", file)
    if (r_time < 30000)
        printf(" short-time(%d <30)", r_time/1000)
    else if (r_time > 60000)
        printf(" long-time(%d >60)", r_time/1000)
    if (r_davg > 1000 || r_ddev > 1000)
        printf(" delay(avg=%s,std=%s)", r_davg, r_ddev)
    if (r_drop > 0)
        printf(" drop(%s)", r_drop)
    if (r_dup > 0)
        printf(" dup(%s)", r_dup)
    if (r_errcnt > 0)
        printf(" errcnt(%s)", r_errcnt)
    if (r_note != "")
        printf(" note[%s]", r_note)
    printf("\n")

    total_snd[snd] += r_total
    total_rcv[rcv] += r_total
    hosts[snd] = 1
    hosts[rcv] = 1

    if (index(file, "-pub") > 0) {
        sub(/-pub/, "-*", file)
        pub_errcnt_list[file] = r_errcnt
        pub_drop_list[file] = r_drop
        file_list[file] = 1
    } else if (index(file, "-sub") > 0) {
        sub(/-sub/, "-*", file)
        sub_drop_list[file] = r_drop
        file_list[file] = 1
    }
}
END {
    printf("ERRCNT AND DROP:\n")
    for (file in file_list) {
        pub_errcnt = pub_errcnt_list[file]
        pub_drop = pub_drop_list[file]
        sub_drop = sub_drop_list[file]
        if (pub_errcnt != "" && pub_errcnt != pub_drop || pub_drop != sub_drop) {
            printf("%s: pub_errcnt=%s pub_drop=%s sub_drop=%s\n", file, pub_errcnt, pub_drop, sub_drop)
        }
    }
    printf("SUMMARY:\n")
    for (host in hosts) {
        printf("%s -> broker: %d MiB\n", host, total_snd[host]/1024/1024)
        printf("%s <- broker: %d MiB\n", host, total_rcv[host]/1024/1024)
    }
}
' "$STAT_FILE"
