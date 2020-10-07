#!/bin/sh

sum2="${1:?summary2.csv must be specified}"
#1  ,2  ,3  ,4 ,5    ,6   ,7,8    ,9   ,10  ,11 ,12  ,13  ,14,15
#snd,rcv,sys,sz,proto,role,n,total,time,imin,avg,imax,size,bw,iops

rm -rf tmp
mkdir -p tmp

case "${FMT:-png}" in
png)
    SFX=png
    FMT_OPT="png size 600,600"
    ;;
pdf)
    SFX=pdf
    FMT_OPT="pdfcairo size 4.8in,3.2in"
    ;;
*)
    echo "Invalid FMT"
    exit 1
    ;;
esac

pickup() {
    local EVL="$1"
    local SND="${SND:-*}"
    local RCV="${RCV:-*}"
    local SYS="${SYS:-*}"
    local SZ="${SZ:-*}"
    local PROTO="${PROTO:-*}"
    local ROLE="${ROLE:-*}"
    local snd rcv sys sz proto role n total time imin avg imax size bw iops
    while IFS="," read snd rcv sys sz proto role n total time imin avg imax size bw iops; do
        case "$snd" in
        $SND) ;;
        *) continue;;
        esac
        case "$rcv" in
        $RCV) ;;
        *) continue;;
        esac
        case "$sys" in
        $SYS) ;;
        *) continue;;
        esac
        case "$sz" in
        $SZ) ;;
        *) continue;;
        esac
        case "$proto" in
        $PROTO) ;;
        *) continue;;
        esac
        case "$role" in
        $ROLE) ;;
        *) continue;;
        esac
        eval $EVL
    done
}

plot1() {
    local SND="$1"
    local RCV="$2"
    local PROTO="$3"
    local ROLE="sub"
    local BASE="$SND-$RCV-$PROTO"
    local SYS
    for SYS in "kafkaN" "kafkaS" "mqttN" "mqttS"; do
        DAT="$BASE-$SYS.dat"
        echo "$DAT"
        pickup 'echo "$sz,$bw"' <"$sum2" >"tmp/tmp.dat"
        if [ -s "tmp/tmp.dat" ]; then
            mv -f "tmp/tmp.dat" "tmp/$DAT"
        fi
    done

    GP="$BASE.gp"
    PNG="$BASE.$SFX"
    cat >"tmp/$GP" <<-__END__
        set datafile separator ","
        set style histogram clustered
        set style fill solid border lc rgb "black"
        set xtics nomirror
        set log y
        set grid ytics
        set title "Writer=$SND Reader=$RCV Crypto=$PROTO"
        set xlabel "message size [B]"
        set ylabel "throughput [MiB/s]"
        set terminal $FMT_OPT
        set output "tmp/$PNG"
        plot "tmp/$SND-$RCV-$PROTO-kafkaS.dat" using 2:xtic(1) with histogram title "SINETStream(Kafka)", \
             "tmp/$SND-$RCV-$PROTO-kafkaN.dat" using 2         with histogram title "Native(Kafka)", \
             "tmp/$SND-$RCV-$PROTO-mqttS.dat"  using 2         with histogram title "SINETStream(MQTT)", \
             "tmp/$SND-$RCV-$PROTO-mqttN.dat"  using 2         with histogram title "Native(MQTT)"
__END__
    gnuplot "tmp/$GP"
}

plot1 aws aws plain
plot1 aws raspi plain
plot1 raspi aws plain

plot1 aws aws tls
plot1 aws raspi tls
plot1 raspi aws tls

plot1 aws aws crypto
plot1 aws raspi crypto
plot1 raspi aws crypto

case "${FMT:-png}" in
pdf) exit 0;;
esac

echo "### SINETStream vs Native"
echo "| pub | sub | proto | msg size [B] | tput SINETStream/Native |"
echo "| ---: | ---: | ---: | ---: | :--- |"
for SND in aws raspi; do
    for RCV in aws raspi; do
        if [ "$SND" = "raspi" -a "$RCV" = "raspi" ]; then
            continue
        fi
        for PROTO in plain; do
            for SYS in kafka mqtt; do
                for SZ in 100 1000 64000; do
                    N=$(SYS="${SYS}N" ROLE=sub pickup 'echo "$bw"' <"$sum2")
                    S=$(SYS="${SYS}S" ROLE=sub pickup 'echo "$bw"' <"$sum2")
                    if [ -n "$N" -a -n "$S" ]; then
                        X="$(echo "0k $S 100 * $N / p" | dc)% =$S/$N"
                    else
                        X="N/A"
                        continue
                    fi
                    echo "| $SND | $RCV | $PROTO | $SZ | $X |"
                done
            done
        done
    done
done

echo "### Crypto/TLS vs Plain"
echo "| pub | sub | system | msg size [B] | tput Crypto/Plain | tpue TLS/Plain |"
echo "| ---: | ---: | ---: | ---: | :--- | :--- |"
for SND in aws raspi; do
    for RCV in aws raspi; do
        if [ "$SND" = "raspi" -a "$RCV" = "raspi" ]; then
            continue
        fi
        for SYS in kafka mqtt; do
            for SZ in 100 1000 64000; do
                P=$(SYS="${SYS}S" PROTO=plain ROLE=sub pickup 'echo "$bw"' <"$sum2")
                C=$(SYS="${SYS}S" PROTO=crypto ROLE=sub pickup 'echo "$bw"' <"$sum2")
                T=$(SYS="${SYS}S" PROTO=tls ROLE=sub pickup 'echo "$bw"' <"$sum2")
                if [ -n "$P" -a -n "$C" -a -n "$T" ]; then
                    X="$(echo "0k $C 100 * $P / p" | dc)% =$C/$P"
                    X="$X | $(echo "0k $T 100 * $P / p" | dc)% =$T/$P"
                else
                    X="N/A | N/A |"
                    continue
                fi
                echo "| $SND | $RCV | $SYS | $SZ | $X |"
            done
        done
    done
done
