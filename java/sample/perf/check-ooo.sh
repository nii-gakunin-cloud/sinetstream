#!/bin/sh
# *-sub.csvをみてシーケンス番号が連続しているか確認するスクリプト

CHECK() {
    awk -F, '
    BEGIN {
        MAX=-1
    }
    /^#/ {next}
    NF >= 3 {
        SEQ = $3
        if (SEQ < MAX) {
            printf("%s: out-of-order: seq=%d last=%d\n", FILENAME, SEQ, MAX)
        } else if (MAX + 1 < SEQ) {
            printf("%s: jump: seq=%d last=%d\n", FILENAME, SEQ, MAX)
        }
        if (MAX < SEQ)
            MAX = SEQ
        RCVED[SEQ]++
    }
    END {
        for (I = 0; I <= MAX; I++) {
            if (RCVED[I] == 0)
                printf("%s: drop: seq=%d\n", FILENAME, I)
            if (RCVED[I] > 1)
                printf("%s: dup: seq=%d *%d\n", FILENAME, I, RCVED[I])
        }
    }
    ' "$1"
}

for CSV; do
    CHECK "$CSV"
done
