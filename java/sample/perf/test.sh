#!/bin/sh
# テストドライバ
# パラメータを変えながらクライアントノードでProducer/Consumerを実行して結果ファイルをもってくる
set -eu
set -x

# pub    bro    sub
# aws -- aws -- aws

#export JAVA_HOME=/usr/lib/jvm/java-1.8.0/jre
PKGDIR="repo/sinetstream-java/sample/perf/sinetstream-binary-producer-1.2.0"

SERVICE() {
    local qsys="$1"
    local crypto="$2"
    echo "service-$qsys-aws-$crypto"    # TUNE
}

BROKER() {
    local qsys="$1"
    local crypto="$2"
    local key="$qsys-$crypto" 
    case "$key" in
    kafka-plain) echo "kafka-aws:9092" ;;       # TUNE
    mqtt-plain)  echo "tcp://mqtt-aws:1883" ;;  # TUNE
    *) echo "ERROR-$key"; return 1;;
    esac
}

TIMEOUT1=30000    # 30s   read/write操作のタイムアウト
TIMEOUT2=600000   # 600s  測定全体のタイムアウト(初期化の時間は含まない)
DELAY=5000        # 5s subscriberを起動してからpublisherを起動するまでの余裕時間 (must: TIMEOUT1 > DELAY)
TOPICBASE="perftest"
LOGDIR="perftest-$(date '+%Y%m%d%H%M%S')"
LOGPREFIX="perftest"
LIBS_ALL="sinetS sinetA nativS nativA"

#Q=0 # at-most-once
Q=1 # at-least-once
#Q=2 # exactly-once


# must: DELAY < TIMEOUT1
[ $DELAY -lt $TIMEOUT1 ] || exit 1

RUN() {
        local MEMO="$1"; shift
        local PUB_HOST="$1"; shift
        local SUB_HOST="$1"; shift
        local BROKER_SPEC="$1"; shift
        local PROG_PUBLISHER="$1"; shift
        local PROG_SUBSCRIBER="$1"; shift
        local SYNC_MODE="$1"; shift
        local QOS="$1"; shift
        local MSZ="$1"; shift
        local NMSG="$1"; shift
        local FPS="$1"

        local UTOPIC="$TOPICBASE-$(date '+%s')"
        local LOGFILE="$LOGDIR/$LOGPREFIX-$(date '+%Y%m%d-%H%M%S')-$MEMO"

        mkdir -p "$LOGDIR"
        ssh -n "$SUB_HOST" mkdir -p "$LOGDIR"
        ssh -n "$PUB_HOST" mkdir -p "$LOGDIR"

        ssh -n "$SUB_HOST" \
            "$PKGDIR/bin/$PROG_SUBSCRIBER" \
            $BROKER_SPEC \
            --topic_src "$UTOPIC" \
            --qos "$QOS" \
            --nmsg "$NMSG" \
            --timeout1 "$TIMEOUT1" \
            --timeout2 "$((TIMEOUT2 + DELAY))" \
            --logfile "$LOGFILE-sub.csv" &
        local PID_SUB="$!"
        sleep $((DELAY/1000))
        local BEGIN="$(date '+%s')"
        ssh -n "$PUB_HOST" \
            "$PKGDIR/bin/$PROG_PUBLISHER" \
                $BROKER_SPEC \
                --topic_dst "$UTOPIC" \
                --qos "$QOS" \
                --bytes "$MSZ" \
                --nmsg "$NMSG" \
                --timeout1 "$TIMEOUT1" \
                --timeout2 "$TIMEOUT2" \
                --fps "$FPS" \
                --logfile "$LOGFILE-pub.csv" \
                ${PUB_OPT:-} \
                --sync "$SYNC_MODE" || {
            echo "EEE: PUBLISHER FAILED."
        }
        wait "$PID_SUB" || {
            echo "EEE: SUBSCRIBER FAILED."
        }
        local END="$(date '+%s')"
        local ELAPSEDTIME="$((END - BEGIN))"
        echo "ELAPSEDTIME: $MEMO: $ELAPSEDTIME"

        scp "$PUB_HOST:$LOGFILE-pub.csv" "$LOGFILE-pub.csv" || true
        scp "$SUB_HOST:$LOGFILE-sub.csv" "$LOGFILE-sub.csv" || true

        local GUARDTIME=$((ELAPSEDTIME / 2))
        if [ "$GUARDTIME" -gt 60 ]; then
            GUARDTIME=60                # XXX TEKITO-
        fi
        if [ "$GUARDTIME" -lt 10 ]; then
            GUARDTIME=10                # XXX TEKITO-
        fi
        sleep "$GUARDTIME"

        return 0
}

IPERF_SERVER="172.30.2.218"  # iperf3 --server --daemon
IPERF() {
        local CLIENT="$1"
        local SERVER="$2"
        local LOGFILE="$LOGDIR/iperf-$(date '+%Y%m%d-%H%M%S')"
        ssh -n "$CLIENT" "mkdir -p $LOGDIR && iperf3 --json --logfile $LOGFILE-up.json --client $SERVER"
        ssh -n "$CLIENT" "mkdir -p $LOGDIR && iperf3 --json --logfile $LOGFILE-down.json --client $SERVER --reverse"
        mkdir -p "$LOGDIR"
        scp "$CLIENT:$LOGFILE-up.json" "$LOGFILE-up.json" || true
        scp "$CLIENT:$LOGFILE-down.json" "$LOGFILE-down.json" || true
}

XFACTOR=0.8

RUNS() {
        local PUB_HOST="$1"; shift
        local SUB_HOST="$1"; shift
        local QSYS="$1"; shift
        local QOS="$1"; shift
        local CRYPTO="$1"; shift
        local MSZ="$1"; shift
        local NMSG="$1"; shift
        local LIBS="$1"

        #local FPS
        #local BW
        #case "$PUB_HOST" in
        #aws)   BW=1000000000;;
        #raspi) BW=10000000;;
        #esac
        #FPS=$(dc -e "$BW $MSZ / $XFACTOR * 0k 1/ p")
        local FPS=0   # full speed

        IPERF "$PUB_HOST" "$IPERF_SERVER"
        if [ "$PUB_HOST" != "$SUB_HOST" ]; then
            IPERF "$SUB_HOST" "$IPERF_SERVER"
        fi

        local SERVICE="$(SERVICE $QSYS $CRYPTO)"
        local BROKER="$(BROKER $QSYS $CRYPTO)"
        local LIB
        for LIB in $LIBS; do
            local SYNC_MODE
            case "$LIB" in
            *S) SYNC_MODE="1";;
            *A) SYNC_MODE="0";;
            esac
            case "$LIB" in 
            sinetS)
                RUN "$PUB_HOST-$SUB_HOST-$QSYS-qos$QOS-$LIB-$CRYPTO-$MSZ-$NMSG" \
                    "$PUB_HOST" \
                    "$SUB_HOST" \
                    "--service $SERVICE" \
                    sinetstream-binary-producer \
                    sinetstream-binary-consumer \
                    "$SYNC_MODE" "$QOS" "$MSZ" "$NMSG" "$FPS"
                ;;
            sinetA)
                RUN "$PUB_HOST-$SUB_HOST-$QSYS-qos$QOS-$LIB-$CRYPTO-$MSZ-$NMSG" \
                    "$PUB_HOST" \
                    "$SUB_HOST" \
                    "--service $SERVICE" \
                    sinetstream-binary-async-producer \
                    sinetstream-binary-async-consumer \
                    "$SYNC_MODE" "$QOS" "$MSZ" "$NMSG" "$FPS"
                ;;
            nativS|nativA)
                RUN "$PUB_HOST-$SUB_HOST-$QSYS-qos$QOS-$LIB-$CRYPTO-$MSZ-$NMSG" \
                    "$PUB_HOST" \
                    "$SUB_HOST" \
                    "--broker $BROKER" \
                    $QSYS-binary-producer \
                    $QSYS-binary-consumer \
                    "$SYNC_MODE" "$QOS" "$MSZ" "$NMSG" "$FPS"
                ;;
            esac
        done
}


####
TEST_SINETA_FASTER_THAN_NATIVA() {
#WARNING:この測定を実行するとブローカーがディスクフルになる
TIMEOUT1=30000 # 30s
TIMEOUT2=70000 # 70s
IPERF() {
    :
}
for I in 1 2 3 4 5; do
#    pub   sub   qsys  qos crypt    msz    nmsg     libs
export PUB_OPT="--extra throttling"
RUNS aws   aws   kafka $Q  plain    100    8000000  "sinetA nativA"
RUNS aws   aws   mqtt  $Q  plain    100    50000    "sinetA nativA"
RUNS aws   aws   kafka $Q  plain    1000   500000   "sinetA nativA"
RUNS aws   aws   mqtt  $Q  plain    1000   50000    "sinetA nativA"
RUNS aws   aws   kafka $Q  plain    10000  100000   "sinetA nativA"
RUNS aws   aws   mqtt  $Q  plain    10000  5000     "sinetA nativA"
export PUB_OPT=""
RUNS aws   aws   kafka $Q  plain    100    8000000  "sinetA nativA"
RUNS aws   aws   mqtt  $Q  plain    100    50000    "sinetA nativA"
RUNS aws   aws   kafka $Q  plain    1000   500000   "sinetA nativA"
RUNS aws   aws   mqtt  $Q  plain    1000   50000    "sinetA nativA"
RUNS aws   aws   kafka $Q  plain    10000  100000   "sinetA nativA"
RUNS aws   aws   mqtt  $Q  plain    10000  5000     "sinetA nativA"
done
exit
#参考過去データ
#perftest-20200904012055+20200903014609                                                perftest-20200906000528
#+---------------------------------------+-----------+-----------+---------------+     +---------------------------------------+-----------+-----------+---------------+
#|snd-rcv-qsys-qos-{lib}-crypt-msz-role  | nativA    | sinetA    | sinetA/nativA |     |snd-rcv-qsys-qos-{lib}-crypt-msz-role  | nativA    | sinetA    | sinetA/nativA |
#+---------------------------------------+-----------+-----------+---------------+     +---------------------------------------+-----------+-----------+---------------+
#|aws-aws-kafka-1-*-plain-100-pub        | 13.832261 | 6.220112  | 0.450         |     |aws-aws-kafka-1-*-plain-100-pub        | 11.283161 | 6.532158  | 0.579         |
#|aws-aws-kafka-1-*-plain-100-sub        | 13.572726 | 6.326904  | 0.466         |     |aws-aws-kafka-1-*-plain-100-sub        | 11.059543 | 6.600592  | 0.597         |
#|aws-aws-mqtt-1-*-plain-100-pub         | 0.081781  | 0.086892  | 1.062         |<   >|aws-aws-mqtt-1-*-plain-100-pub         | 0.079212  | 0.086362  | 1.090         |
#|aws-aws-mqtt-1-*-plain-100-sub         | 0.064846  | 0.087034  | 1.342         |<< >>|aws-aws-mqtt-1-*-plain-100-sub         | 0.070059  | 0.086497  | 1.235         |
#|aws-aws-kafka-1-*-plain-1000-pub       | 56.220934 | 39.576803 | 0.704         |     |aws-aws-kafka-1-*-plain-1000-pub       | 51.848029 | 42.700377 | 0.824         |
#|aws-aws-kafka-1-*-plain-1000-sub       | 45.179626 | 35.843554 | 0.793         |     |aws-aws-kafka-1-*-plain-1000-sub       | 44.930187 | 36.303257 | 0.808         |
#|aws-aws-mqtt-1-*-plain-1000-pub        | 0.792367  | 0.859234  | 1.084         |<    |aws-aws-mqtt-1-*-plain-1000-pub        | 0.868665  | 0.863473  | 0.994         |
#|aws-aws-mqtt-1-*-plain-1000-sub        | 0.776496  | 0.861092  | 1.109         |<< >>|aws-aws-mqtt-1-*-plain-1000-sub        | 0.762388  | 0.864517  | 1.134         |
#|aws-aws-kafka-1-*-plain-10000-pub      | 64.960602 | 68.137925 | 1.049         |<    |aws-aws-kafka-1-*-plain-10000-pub      | 59.382957 | 59.346869 | 0.999         |
#|aws-aws-kafka-1-*-plain-10000-sub      | 65.064477 | 61.592730 | 0.947         |     |aws-aws-kafka-1-*-plain-10000-sub      | 59.382957 | 57.390264 | 0.966         |
#|aws-aws-mqtt-1-*-plain-10000-pub       | 6.324981  | 6.182040  | 0.977         |     |aws-aws-mqtt-1-*-plain-10000-pub       | 6.891725  | 5.731505  | 0.832         |
#|aws-aws-mqtt-1-*-plain-10000-sub       | 5.054451  | 6.227564  | 1.232         |<<   |aws-aws-mqtt-1-*-plain-10000-sub       | 6.040985  | 5.812326  | 0.962         |
#|raspi-raspi-kafka-1-*-plain-100-pub    | 1.135254  | 0.467138  | 0.411         |     |raspi-raspi-kafka-1-*-plain-100-pub    | 1.093914  | 0.435371  | 0.398         |
#|raspi-raspi-kafka-1-*-plain-100-sub    | 0.773321  | 0.665294  | 0.860         |     |raspi-raspi-kafka-1-*-plain-100-sub    | 0.723199  | 0.516571  | 0.714         |
#|raspi-raspi-mqtt-1-*-plain-100-pub     | 0.007379  | 0.003961  | 0.537         |     |raspi-raspi-mqtt-1-*-plain-100-pub     | 0.007773  | 0.003872  | 0.498         |
#|raspi-raspi-mqtt-1-*-plain-100-sub     | 0.007743  | 0.003972  | 0.513         |     |raspi-raspi-mqtt-1-*-plain-100-sub     | 0.007989  | 0.003883  | 0.486         |
#|raspi-raspi-kafka-1-*-plain-1000-pub   | 1.323490  | 0.998541  | 0.754         |     |raspi-raspi-kafka-1-*-plain-1000-pub   | 1.330708  | 1.133652  | 0.852         |
#|raspi-raspi-kafka-1-*-plain-1000-sub   | 1.170353  | 0.987929  | 0.844         |     |raspi-raspi-kafka-1-*-plain-1000-sub   | 1.170353  | 1.170353  | 1.000         |
#|raspi-raspi-mqtt-1-*-plain-1000-pub    | 0.085335  | 0.044684  | 0.524         |     |raspi-raspi-mqtt-1-*-plain-1000-pub    | 0.090243  | 0.044915  | 0.498         |
#|raspi-raspi-mqtt-1-*-plain-1000-sub    | 0.088328  | 0.044871  | 0.508         |     |raspi-raspi-mqtt-1-*-plain-1000-sub    | 0.093771  | 0.045071  | 0.481         |
#|raspi-raspi-kafka-1-*-plain-10000-pub  | 0.950136  | 0.866966  | 0.912         |     |raspi-raspi-kafka-1-*-plain-10000-pub  | 0.865199  | 0.778213  | 0.899         |
#|raspi-raspi-kafka-1-*-plain-10000-sub  | 0.951386  | 0.875796  | 0.921         |     |raspi-raspi-kafka-1-*-plain-10000-sub  | 0.864127  | 0.786364  | 0.910         |
#|raspi-raspi-mqtt-1-*-plain-10000-pub   | 0.769111  | 0.454819  | 0.591         |     |raspi-raspi-mqtt-1-*-plain-10000-pub   | 0.877088  | 0.468019  | 0.534         |
#|raspi-raspi-mqtt-1-*-plain-10000-sub   | 0.780744  | 0.461750  | 0.591         |     |raspi-raspi-mqtt-1-*-plain-10000-sub   | 0.670110  | 0.473149  | 0.706         |
}
#TEST_SINETA_FASTER_THAN_NATIVA

####
TEST_RASPI_MQTT_NATIVE_ASYNC_IS_LOSSY() {
IPERF() {
    :
}
#    pub   sub   qsys  qos crypt    msz    nmsg     libs
export PUB_OPT="--extra throttling"
RUNS raspi raspi mqtt  $Q  plain    100    3000     "nativA"
export PUB_OPT=""
RUNS raspi raspi mqtt  $Q  plain    100    3000     "nativA sinetA"
exit
}
#TEST_RASPI_MQTT_NATIVE_ASYNC_IS_LOSSY

####
TEST_AWS_MQTT_SYNC_PLAIN_IS_SLOW_8K_16K() {
IPERF() {
    :
}
#export PUB_OPT="--extra nodelay" #このオプションをつけると改善する
for MSZ_NMSG in \
    "100   10000" \
    "200   10000" \
    "300   10000" \
    "500   10000" \
    "700   10000" \
    "1000  10000" \
    "2000  10000" \
    "3000  10000" \
    "5000  10000" \
    "7000  10000" \
    "8000  10000" \
    "8100  10000" \
    "8200  10000" \
    "8300  10000" \
    "8400  10000" \
    "8500  10000" \
    "8600  10000" \
    "8700  10000" \
    "8800  10000" \
    "8900  10000" \
    "9000  10000" \
    "9100  10000" \
    "9200  10000" \
    "9300  10000" \
    "9400  10000" \
    "9500  10000" \
    "9600  10000" \
    "9700  10000" \
    "9800  10000" \
    "9900  10000" \
    "10000 10000" \
    "11000 10000" \
    "12000 10000" \
    "13000 10000" \
    "14000 10000" \
    "15000 10000" \
    "16000 10000" \
    "17000 10000" \
    "18000 10000" \
    "19000 10000" \
    "20000 10000" \
    "30000 10000" \
    "50000 10000" \
    "64000 10000" \
; do
RUNS aws aws mqtt $Q plain $MSZ_NMSG "nativS sinetS"
done
exit
}
#TEST_AWS_MQTT_SYNC_PLAIN_IS_SLOW_8K_16K

####
TEST_ALL() {
for crypto in plain; do
#    pub   sub   qsys  qos crypt    msz    nmsg     libs
RUNS aws   aws   kafka $Q  $crypto  100    200000   "$LIBS_ALL"
RUNS aws   aws   mqtt  $Q  $crypto  100    50000    "$LIBS_ALL"
RUNS aws   aws   kafka $Q  $crypto  1000   200000   "$LIBS_ALL"
RUNS aws   aws   mqtt  $Q  $crypto  1000   50000    "$LIBS_ALL"
RUNS aws   aws   kafka $Q  $crypto  10000  100000   "$LIBS_ALL"
RUNS aws   aws   mqtt  $Q  $crypto  10000  5000     "$LIBS_ALL"

#RUNS raspi raspi kafka $Q  $crypto  100    4000     "$LIBS_ALL"
#RUNS raspi raspi mqtt  $Q  $crypto  100    3000     "$LIBS_ALL"
#RUNS raspi raspi kafka $Q  $crypto  1000   2000     "$LIBS_ALL"
#RUNS raspi raspi mqtt  $Q  $crypto  1000   3000     "$LIBS_ALL"
#RUNS raspi raspi kafka $Q  $crypto  10000  2000     "$LIBS_ALL"
#RUNS raspi raspi mqtt  $Q  $crypto  10000  1000     "$LIBS_ALL"
done
for crypto in tls aes tlsaes; do
#    pub   sub   qsys  qos crypt    msz    nmsg     libs
RUNS aws   aws   kafka $Q  $crypto  100    200000   "sinetS sinetA"
RUNS aws   aws   mqtt  $Q  $crypto  100    50000    "sinetS sinetA"
RUNS aws   aws   kafka $Q  $crypto  1000   100000   "sinetS sinetA"
RUNS aws   aws   mqtt  $Q  $crypto  1000   50000    "sinetS sinetA"
RUNS aws   aws   kafka $Q  $crypto  10000  100000   "sinetS sinetA"
RUNS aws   aws   mqtt  $Q  $crypto  10000  5000     "sinetS sinetA"

#RUNS raspi raspi kafka $Q  $crypto  100    2000     "sinetS sinetA"
#RUNS raspi raspi mqtt  $Q  $crypto  100    2000     "sinetS sinetA"
#RUNS raspi raspi kafka $Q  $crypto  1000   2000     "sinetS sinetA"
#RUNS raspi raspi mqtt  $Q  $crypto  1000   2000     "sinetS sinetA"
#RUNS raspi raspi kafka $Q  $crypto  10000  5000     "sinetS sinetA"
#RUNS raspi raspi mqtt  $Q  $crypto  10000  1000     "sinetS sinetA"
done
exit
}
TEST_ALL
####

####
TEST_ALL_OLD() {
for crypto in plain; do
#    pub   sub  qsys  qos crypt    msz   nmsg     libs
RUNS aws   aws  kafka $Q  $crypto  100   200000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  $crypto  100   10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  $crypto  1000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  $crypto  1000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  $crypto  5000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  $crypto  5000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  $crypto  64000 5000     "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  $crypto  64000 5000     "$LIBS_ALL"

#RUNS raspi raspi kafka $Q  $crypto  100   2000    "$LIBS_ALL"
#RUNS raspi raspi mqtt  $Q  $crypto  100   10000   "$LIBS_ALL"
#RUNS raspi raspi kafka $Q  $crypto  1000  1000    "$LIBS_ALL"
#RUNS raspi raspi mqtt  $Q  $crypto  1000  10000   "$LIBS_ALL"
#RUNS raspi raspi kafka $Q  $crypto  5000  10000   "$LIBS_ALL"
#RUNS raspi raspi mqtt  $Q  $crypto  5000  1000    "$LIBS_ALL"
#RUNS raspi raspi kafka $Q  $crypto  64000 500     "$LIBS_ALL"
#RUNS raspi raspi mqtt  $Q  $crypto  64000 500     "$LIBS_ALL"
done
for crypto in tls aes tlsaes; do
#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS aws   aws  kafka $Q  $crypto  100   200000   "sinetS sinetA"
RUNS aws   aws  mqtt  $Q  $crypto  100   10000    "sinetS sinetA"
RUNS aws   aws  kafka $Q  $crypto  1000  100000   "sinetS sinetA"
RUNS aws   aws  mqtt  $Q  $crypto  1000  10000    "sinetS sinetA"
RUNS aws   aws  kafka $Q  $crypto  5000  100000   "sinetS sinetA"
RUNS aws   aws  mqtt  $Q  $crypto  5000  10000    "sinetS sinetA"
RUNS aws   aws  kafka $Q  $crypto  64000 5000     "sinetS sinetA"
RUNS aws   aws  mqtt  $Q  $crypto  64000 5000     "sinetS sinetA"

#RUNS raspi raspi kafka $Q  $crypto  100   2000    "sinetS sinetA"
#RUNS raspi raspi mqtt  $Q  $crypto  100   10000   "sinetS sinetA"
#RUNS raspi raspi kafka $Q  $crypto  1000  1000    "sinetS sinetA"
#RUNS raspi raspi mqtt  $Q  $crypto  1000  10000   "sinetS sinetA"
#RUNS raspi raspi kafka $Q  $crypto  5000  10000   "sinetS sinetA"
#RUNS raspi raspi mqtt  $Q  $crypto  5000  1000    "sinetS sinetA"
#RUNS raspi raspi kafka $Q  $crypto  64000 500     "sinetS sinetA"
#RUNS raspi raspi mqtt  $Q  $crypto  64000 500     "sinetS sinetA"
done
exit
}
#TEST_ALL_OLD
####


TEST_ALL_OLD_OLD() {
for Q in 0 1 2; do

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS aws   aws  kafka $Q  plain  100   200000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  plain  100   10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  plain  1000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  plain  1000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  plain  5000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  plain  5000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  plain  64000 5000     "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  plain  64000 5000     "$LIBS_ALL"

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS aws   aws  kafka $Q  tls    100   200000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tls    100   10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  tls    1000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tls    1000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  tls    5000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tls    5000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  tls    64000 5000     "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tls    64000 5000     "$LIBS_ALL"

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS aws   aws  kafka $Q  aes    100   200000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  aes    100   10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  aes    1000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  aes    1000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  aes    5000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  aes    5000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  aes    64000 5000     "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  aes    64000 5000     "$LIBS_ALL"

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS aws   aws  kafka $Q  tlsaes 100   200000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tlsaes 100   10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  tlsaes 1000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tlsaes 1000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  tlsaes 5000  100000   "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tlsaes 5000  10000    "$LIBS_ALL"
RUNS aws   aws  kafka $Q  tlsaes 64000 5000     "$LIBS_ALL"
RUNS aws   aws  mqtt  $Q  tlsaes 64000 5000     "$LIBS_ALL"

####################

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS raspi aws  kafka $Q  plain  100   1000     "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  plain  100   1000     "$LIBS_ALL"
RUNS raspi aws  kafka $Q  plain  1000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  plain  1000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  plain  5000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  plain  5000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  plain  64000 200      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  plain  64000 200      "$LIBS_ALL"

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS raspi aws  kafka $Q  tls    100   1000     "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tls    100   1000     "$LIBS_ALL"
RUNS raspi aws  kafka $Q  tls    1000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tls    1000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  tls    5000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tls    5000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  tls    64000 200      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tls    64000 200      "$LIBS_ALL"

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS raspi aws  kafka $Q  aes    100   1000     "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  aes    100   1000     "$LIBS_ALL"
RUNS raspi aws  kafka $Q  aes    1000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  aes    1000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  aes    5000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  aes    5000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  aes    64000 200      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  aes    64000 200      "$LIBS_ALL"

#    pub   sub  qsys  qos crypt  msz   nmsg     libs
RUNS raspi aws  kafka $Q  tlsaes 100   1000     "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tlsaes 100   1000     "$LIBS_ALL"
RUNS raspi aws  kafka $Q  tlsaes 1000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tlsaes 1000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  tlsaes 5000  500      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tlsaes 5000  200      "$LIBS_ALL"
RUNS raspi aws  kafka $Q  tlsaes 64000 200      "$LIBS_ALL"
RUNS raspi aws  mqtt  $Q  tlsaes 64000 200      "$LIBS_ALL"

####################

done
}
#TEST_ALL_OLD_OLD

exit
