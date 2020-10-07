#!/bin/sh
set -eu

# BUG: 一言では言い表せないほどややこしいスクリプト

# EXAMPLE:

# CSVFILE is:
# A,B,C,X,Y,Z
# a1,b1,c1,101,201,301
# a1,b1,c2,102,202,302
# a1,b2,c1,103,203,303
# a1,b2,c2,104,204,304
# a2,b1,c1,105,205,305
# a2,b1,c2,106,206,306
# a2,b2,c1,107,207,307
# a2,b2,c2,108,208,308

# ./pick-csv.sh CSVFILE A,B,C C c1,c2 Y
# A-B-{C},c1,c2,c2/c1
# a1-b1-*,201,202,1.005
# a1-b2-*,203,204,1.005
# a2-b1-*,205,206,1.005
# a2-b2-*,207,208,1.005

# $ ./view-csv.sh < test.csv
# +---+----+----+-----+-----+-----+
# |A  | B  | C  | X   | Y   | Z   |
# +---+----+----+-----+-----+-----+
# |a1 | b1 | c1 | 101 | 201 | 301 |
# |a1 | b1 | c2 | 102 | 202 | 302 |
# |a1 | b2 | c1 | 103 | 203 | 303 |
# |a1 | b2 | c2 | 104 | 204 | 304 |
# |a2 | b1 | c1 | 105 | 205 | 305 |
# |a2 | b1 | c2 | 106 | 206 | 306 |
# |a2 | b2 | c1 | 107 | 207 | 307 |
# |a2 | b2 | c2 | 108 | 208 | 308 |
# +---+----+----+-----+-----+-----+
# $ ./pick-csv.sh test.csv A,B,C C c1,c2 Y | ./view-csv.sh
# +--------+-----+-----+-------+
# |A-B-{C} | c1  | c2  | c2/c1 |
# +--------+-----+-----+-------+
# |a1-b1-* | 201 | 202 | 1.005 |
# |a1-b2-* | 203 | 204 | 1.005 |
# |a2-b1-* | 205 | 206 | 1.005 |
# |a2-b2-* | 207 | 208 | 1.005 |
# +--------+-----+-----+-------+

CSVFILE="$1"; shift     # 入力ファイル
KEYCOLLST="$1"; shift   # キーになっている列のリスト(コンマ区切り)
COLUMN1="$1"; shift     # 注目するキー列
KEYLST="$1"; shift      # 注目するキー列の値のリスト(コンマ区切り)
COLUMN2="$1"            # 抽出する列


awk -F"," \
-v KEYCOLLST="$KEYCOLLST" \
-v COLUMN1="$COLUMN1" \
-v KEYLST="$KEYLST" \
-v COLUMN2="$COLUMN2" \
'
BEGIN {
    STDERR = "/dev/stderr"
    nkeycollst = split(KEYCOLLST, keycollst, /,/)
    nkeylst = split(KEYLST, keylst, /,/)
    for (i = 1; i <= nkeylst; i++)
        valid_keys[keylst[i]] = 1
    for (i = 1; i <= nkeycollst; i++)
        if (keycollst[i] == COLUMN1)
            break
    if (i > nkeycollst) {
        print "ERROR: bad column name: " COLUMN1 >STDERR
        onerror = 1
        exit(1)
    }
}
NR==1 {
    for (i = 1; i <= NF; i++) {
        coli2n[i] = $i
        coln2i[$i] = i
    }
    if (!(COLUMN1 in coln2i)) {
        print "ERROR:" NR ": bad column name: " COLUMN1 >STDERR
        onerror = 1
        exit(1)
    }
    if (!(COLUMN2 in coln2i)) {
        print "ERROR:" NR ": bad column name: " COLUMN2 >STDERR
        onerror = 1
        exit(1)
    }
    col1 = coln2i[COLUMN1]
    col2 = coln2i[COLUMN2]
    next
}
function makekey(a, _i, _r) {
    _r = a[coln2i[keycollst[1]]]
    for (_i = 2; _i <= nkeycollst; _i++)
        _r = _r "-" a[coln2i[keycollst[_i]]]
    return _r
}
NR > 1 {
    row[NR] = $0
    if (!($col1 in valid_keys)) {
        print "ERROR:" NR ": skip:because bad key: " $col1, $0 >STDERR
        next
    }
    split($0, tmp)
    key = makekey(tmp)
    if (key in row) {
        print "ERROR:" NR ": DUP: " key >STDERR
        print "ERROR:" lineno[key] ": " row[key] >STDERR
        print "ERROR:" NR ": " $0 >STDERR
    }
    row[key] = $0
    lineno[key] = NR
}
function concat(a, n, fs, _r, _i) {
    _r = a[1]
    for (_i = 2; _i <= n; _i++) {
        _r = _r fs a[_i]
    }
    return _r
}
END {
    if (onerror)
        exit(1)
    for (i = 1; i <= nkeycollst; i++) {
        if (i >= 2)
            printf("-")
        k = keycollst[i]
        if (k == COLUMN1)
            k = "{" k "}"
        printf("%s", k)
    }
    for (i = 1; i <= nkeylst; i++)
        printf(",%s", keylst[i])
    for (i = 2; i <= nkeylst; i++)
        printf(",%s/%s", keylst[i], keylst[1])
    printf("\n");

    for (k = 2; k <= NR; k++) {
        if (used[k])
            continue
        used[k] = 1
        n = split(row[k], tmp)
        if (!(tmp[col1] in valid_keys))
            continue
        tmp[col1] = "*"
        printf("%s", makekey(tmp))
        delete vlst
        for (i = 1; i <= nkeylst; i++) {
            tmp[col1] = keylst[i]
            k2 = makekey(tmp)
            if (k2 in row) {
                used[lineno[k2]] = 1
                n2 = split(row[k2], tmp2)
                v2 = tmp2[col2]
                vlst[i] = v2
            } else {
                v2 = "NA"
            }
            printf(",%s", v2)
        }
        for (i = 2; i <= nkeylst; i++) {
            if (i in vlst && 1 in vlst)
                if (vlst[1] == 0)
                    printf(",DIV0")
                else
                    printf(",%.3f", vlst[i]/vlst[1])
            else
                printf(",NA")
        }
        printf("\n")
    }
}
' "$CSVFILE"
