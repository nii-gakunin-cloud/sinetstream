#!/bin/sh
# CSVファイルを読んでカラムを抜き出す
# usage: ./slice-csv.sh [-v] CSV_FILE COL[,COL[,COL...]]
# example: ./slice-csv.sh stat.csv 1,18
# example: ./slice-csv.sh stat.csv file,bw
set -eu

INVERTED=0
while getopts "v" OPT; do
    case "$OPT" in
    v) INVERTED=1;;
    *) exit 1;;
    esac
done
shift $((OPTIND - 1))

CSV="$1"
FIELDS="${2:-NONE}"

if [ "$FIELDS" = "NONE" ]; then
  head -1 "$CSV" | awk -F , '{ for (i = 1; i <= NF; i++) print i ": " $i }' | column
  exit 1
fi

awk -F , -v FIELDS="$FIELDS" -v INVERTED="$INVERTED" '
BEGIN {
    flen = split(FIELDS, flst, /[, ]/)
    need_resolve = 0
    for (k in flst) {
        if (flst[k]+1-1 != flst[k])
            need_resolve = 1
    }
}
function resolve(header, _hlst, _hlen, _k, _i, _ii) {
    _hlen = split($0, _hlst, /,/)
    for (_k in flst) {
        if (flst[_k]+1-1 == flst[_k])
            continue
        _ii = 0
        for (_i = 1; _i <= _hlen; _i++) {
            if (flst[_k] == _hlst[_i]) {
                _ii = _i
                break
            }
            if (index(_hlst[_i], flst[_k]) > 0) {
                _ii = -_i
            }
        }
        if (_ii == 0) {
            printf("%s is not a valid column name\n", flst[_k])
            exit 1
        }
        flst[_k] = _ii > 0 ? _ii : -_ii
    }
    need_resolve = 0
}
need_resolve == 1 && NR == 1 {
    resolve($0)
}
!INVERTED {
    printf("%s", $flst[1])
    for (i = 2; i <= flen; i++) {
        printf(",%s", $flst[i])
    }
    printf("\n")
}
INVERTED {
    sep = ""
    for (i = 1; i <= NF; i++) {
        hit = 0
        for (k in flst) {
            if (i == flst[k]) {
                hit = 1
                break
            }
        }
        if (!hit) {
            printf("%s%s", sep, $i)
            sep = ","
        }
    }
    printf("\n")
}
' "$CSV"
