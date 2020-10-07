#!/bin/sh
#行列の入れ替え
awk -F, '
{
    for (i = 1; i <= NF; i++)
        X[NR,i] = $i
    if (maxNF < NF)
        maxNF = NF
}
END {
    for (i = 1; i <= maxNF; i++) {
        printf("%s", X[1,i])
        for (k = 2; k <= NR; k++)
            printf(",%s", X[k,i])
        printf("\n")
    }
}'
