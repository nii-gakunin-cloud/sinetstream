#!/bin/sh

# "stat.sh logx" reads logx,
# and write result.csv, result.txt, encode.csv and decode.csv.

LOGFILE="$1"

# LOGFILE: 1writer 2reader 3service 4count 5result 6comment
cat "$LOGFILE" result-android.txt | grep 'TEST:RESULT:' | sed 's/^.*TEST:RESULT: //' >result.txt
cat result.txt | sort -k2,2 -k1,1 -k3,3 -k4,4 | (
  echo "#writer,reader,service,i,result,comment"
  while IFS=" " read -r a b c d e f; do
      #echo "$a,$b,$c,$d,$e,$f"
      echo "$a,$b,$c,0,$e,$f"
  done | sort | uniq
) >result.csv

column -t -s, result.csv >result-csv.txt

#1writer,2reader,3service,4i,5result,6comment
(
  sed -n '/^[[:alpha:]].*:$/{s/:$//;p}' dot.sinetstream_config.yml
  cat result.csv
) | awk -F, '
/^#/{next}
NF==1 { 
    service_list[$1]=1
    next
}
{
    lang_list[$1]=1
    lang_list[$2]=1
    service_list[$3]=1
}
$2=="encode" && $5=="OK" {
    enc_ok[$1,$3]++
    enc_ng[$1,$3]+=0
    next
}
$2=="encode" && $5=="NG" {
    enc_ok[$1,$3]+=0
    enc_ng[$1,$3]++
    next
}
$5=="OK" {
    dec_ok[$1,$2,$3]++
    dec_ng[$1,$2,$3]+=0
    next
}
$5=="NG" {
    dec_ok[$1,$2,$3]+=0
    dec_ng[$1,$2,$3]++
    next
}
END {
    delete lang_list["encode"]

    file="encode.csv"
    printf("service") >file
    for (lang in lang_list)
        printf(",%s", lang) >>file
    printf("\n") >>file

    for (service in service_list) {
        printf("%s", service) >>file
        for (lang in lang_list) {
            ok=enc_ok[lang,service]
            ng=enc_ng[lang,service]
            if (ok == 0 && ng == 0)
                printf(",-") >>file
            else
                printf(",%s/%s", ok, ng) >>file
        }
        printf("\n") >>file
    }

    file="decode.csv"
    printf("lang") >file
    for (lang in lang_list)
        printf(",%s", lang) >>file
    printf("\n") >>file

    for (snd in lang_list) {
        printf("%s", snd) >>file
        for (rcv in lang_list) {
            ok = ng = 0
            ngs = ""
            for (service in service_list) {
                o = dec_ok[snd,rcv,service]
                n = dec_ng[snd,rcv,service]
                ok += o
                ng += n
                if (n > 0)
                    ngs = ngs " " service
            }
            sub(/ /, "", ngs)
            if (ok == 0 && ng == 0)
                printf(",-") >>file
            else {
                printf(",%s/%s", ok, ng) >>file
                if (ngs != "")
                    printf("(%s)", ngs) >>file
            }
        }
        printf("\n") >>file
    }

}
'

./view-csv.sh encode.csv decode.csv
