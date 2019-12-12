#!/bin/sh
set -eu
#set -x
stat_out="$1"
awk -F , '
/^file,/{next}
/^perftest-/ {
    sub(/.csv/, "", $1) 
    split($1, a, "-")
    snd = a[4]
    rcv = a[5]
    prog = a[6]
    sz = a[7]
    if ($1 ~ /.*-pub/)
        role = "pub"
    else if ($1 ~ /.*-sub/)
        role = "sub"
    else
        role = "XXX"
    #print snd,rcv,prog,sz,role
    n[snd,rcv,prog,sz,role] = $2
    bw[snd,rcv,prog,sz,role] = $9
}
function show3(snd, rcv, prog, sz) {
    nsnd = n[snd,rcv,prog,sz,"pub"]
    nrcv = n[snd,rcv,prog,sz,"sub"]
    tput = nrcv > 0 ? bw[snd,rcv,prog,sz,"sub"] : "NA"
    lost = nrcv > 0 ? (1 - nrcv / nsnd) * 100 : "NA"
    printf(",%s,%s%%", tput, lost)
}
function show2(snd, rcv, prog) {
    printf("%s", prog)
    show3(snd, rcv, prog, 100)
    show3(snd, rcv, prog, 1000)
    show3(snd, rcv, prog, 64000)
    printf("\n")
}
function show(snd, rcv) {
    printf("*** %s -> %s\n", snd, rcv)
    show2(snd, rcv, "kafkaS")
    show2(snd, rcv, "kafkaN")
    show2(snd, rcv, "mqttS")
    show2(snd, rcv, "mqttN")
}
END {
    show("aws", "aws")
    show("aws", "raspi")
    show("raspi", "aws")
    show("raspi", "raspi")
}
' "$stat_out"
