#!/bin/sh
set -eux

peer="${1-kafka-aws}"

DATE() {
   date "+%Y-%m-%d %H:%M:%S"
}

DATE
ping -c 100 "$peer"
DATE
iperf3 --json --interval 10 --time 100 --client "$peer"
DATE
iperf3 --json --interval 10 --time 100 --client "$peer" --reverse
DATE

exit 0
