#!/bin/sh
#csvファイルを表形式に整形する
set -eu

AWK=awk
TBL=tbl
NROFF=nroff
SED=sed

HEADER=1

show_table_tbl()
{
        $AWK -F, -v HEADER="$HEADER" '
        BEGIN {
            SEP = ","
        }
        NR==1 {
                print ".TS";
                print "tab(" SEP ");";
                for (i = 1; i <= NF; i++)
                        printf("|l");
                print "|.";
                print "_";
                for (i = 1; i <= NF; i++)
                        printf("%s%s", $i, SEP);
                print "";
                if (HEADER)
                    print "_";
                next;
        }
        {
            for (i = 1; i <= NF; i++)
                printf("%s%s", $i, SEP);
            print "";
        }
        END {
                if (NR > 0) {
                        print "_";
                        print ".TE";
                } else {
                        #print "<EMPTY>";
                }
        }' | $TBL | $NROFF | $SED '/^$/d'
}

while getopts "h" OPT; do
    case "$OPT" in
    h) HEADER=0 ;;
    *) exit 1;;
    esac
done
shift $((OPTIND - 1))

export LANG=C
if [ $# -eq 0 ]; then
        show_table_tbl
else
        for F in "$@"; do
                printf "[%s]\n" "$F"
                show_table_tbl <"$F"
        done
fi
