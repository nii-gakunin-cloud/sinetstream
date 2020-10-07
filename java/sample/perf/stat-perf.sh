#!/bin/bash
# 生データの統計値を計算する

show_stat() {
awk -F',' -v NMSG="$NMSG" '
#https://qpp.bitbucket.io/post/streaming_algorithm/
function init_var(var_state)
{
    var_state[1] = 0
    var_state[2] = 0
    var_state[3] = 0
    var_state[4] = 0
}
function update_var(v, var_state,
                    _n, _pre, _cur, _m)
{
    _n   = var_state[1]
    _pre = var_state[2]
    _cur = var_state[3]
    _m   = var_state[4]
    _n++
    _pre = _cur
    _cur = v / _n + (1 - 1.0 / _n) * _cur
    _m += (v - _pre) * (v - _cur)
    var_state[1] = _n
    var_state[2] = _pre
    var_state[3] = _cur
    var_state[4] = _m
}
function get_var(var_state,
                 _n, _m)
{
    _n = var_state[1]
    _m = var_state[4]
    return _m / (_n - 1)
}
function get_stdev(var_state)
{
    return sqrt(get_var(var_state))
}

# $1 = read()/write()が戻ってきた時刻
# $2 = バイト数 (バイト数0はread/write失敗を意味する)
# $3 = シーケンス番号
# $4 = write()を呼んだ時の時刻
/^#.*timedout1/ { timedout = or(timedout, 1) }
/^#.*timedout2/ { timedout = or(timedout, 2) }
/^#errcnt/ { split($0, tmp, / /); errcnt = tmp[2]  }
/^[^0-9]/ { next } # skip a non-data line
$1 == 0 || $2 == 0 { next }  # ignore a blank data
start == 0 {
    start = end = prev = $1
    sz0 = $2
    total = $2
    n = 0
    if (sz0 > 0)
        n++
    imin = 1000000
    imax = -1
    if (NF >= 3)
        rec[$3]++
    if (NF >= 4) {
        delay = $1 - $4
        dmin = dsum = dmax = delay
        update_var(delay, dstate)
    }
    next
}
start > 0 {
    total += $2
    if ($2 > 0)
        n++
    end = $1
    interval = $1 - prev; prev = $1
    if (interval < imin)
        imin = interval
    if (imax < interval)
        imax = interval
    update_var(interval, istate)
    if (NF >= 3)
        rec[$3]++
    if (NF >= 4) {
        delay = $1 - $4
        dsum += delay
        if (delay < dmin)
            dmin = delay
        if (delay > dmax)
            dmax = delay
        update_var(delay, dstate)
    }
    next
}
END {
    t = end - start
    printf(",%d", n)
    printf(",%d", total)
    printf(",%d", t)
    printf(",%d", imin)
    printf(",%d", n > 1 ? t/(n-1) : -999)
    printf(",%d", imax)
    printf(",%f", n > 0 ? 1.0*total/n : -999)
    printf(",%f", t > 0 ? ((total - sz0)/1024.0)/t : -999)  # KiB/ms == MiB/s
    printf(",%f", t > 0 ? (n-1)/t : -999)
    printf(",%f", dmin)
    printf(",%f", n > 0 ? dsum/n : -999)
    printf(",%f", dmax)
    printf(",%f", get_stdev(istate))
    printf(",%f", get_stdev(dstate))

    drop = NMSG - n
    printf(",%d", drop)
    printf(",%f", 100.0 * drop / NMSG)

    dup = 0
    for (seq in rec) {
        if (rec[seq] > 1)
            dup += rec[seq] - 1
    }
    printf(",%d", dup)

    printf(",%s", errcnt)

    #note
    printf(",")
    if (timedout > 0) {
        if (and(timedout, 1))
            printf(":timedout1")
        if (and(timedout, 2))
            printf(":timedout2")
    }

    printf("\n")
}
'
}

show_param() {
    local FN="$1"
    IFS="[-.]" read PFX DATE TIME SND RCV QSYS QOS LIB CRYPT MSZ NMSG ROLE GOMI < <(basename "$FN")
    QOS=$(echo "$QOS" | sed 's/qos//')
    printf "%s" "$FN,$SND,$RCV,$QSYS,$QOS,$LIB,$CRYPT,$MSZ,$NMSG,$ROLE"
    #basename "$FN" | ( IFS="[-.]" read PFX DATE TIME SND RCV QSYS QOS LIB CRYPT MSZ NMSG ROLE GOMI && 
    #               QOS=$(echo "$QOS" | sed 's/qos//') &&
    #               printf "%s" "$FN,$SND,$RCV,$QSYS,$QOS,$LIB,$CRYPT,$MSZ,$NMSG,$ROLE" )
}

printf "file,snd,rcv,qsys,qos,lib,crypt,msz,nmsg,role"
printf ",n[msg]"
printf ",total[B]"
printf ",time[ms]"
printf ",imin[ms]"
printf ",iavg[ms]"
printf ",imax[ms]"
printf ",size[B]"
printf ",bw[MiB/s]"
printf ",iops[k msg/s]"
printf ",dmin[ms]"
printf ",davg[ms]"
printf ",dmax[ms]"
printf ",idev[ms]"
printf ",ddev[ms]"
printf ",drop[msg]"
printf ",drop[%%]"
printf ",dup[msg]"
printf ",errcnt"
printf ",note"
printf "\n"
for CSV in "$@"; do
    show_param "$CSV"
    show_stat < "$CSV"
done
