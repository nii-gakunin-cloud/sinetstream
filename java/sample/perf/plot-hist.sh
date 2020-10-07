#!/bin/sh
# ややこしいスループット vs メッセージサイズのヒストグラムを表示する
# ややこしいメッセージ欠損% vs メッセージサイズのヒストグラムを表示する

#FORMAT="sixel"	# sixel対応端末用
FORMAT="pdf"	# 論文用

while getopts "F:" OPT; do
    case "$OPT" in
    F) FORMAT="$OPTARG";;
    *) exit 1;;
    esac
done
shift $((OPTIND - 1))
STAT_CSV="${1:-stat.csv}"

case "$FORMAT" in
sixel) ;;
png) ;;
svg) ;;
pdf) ;;
*) echo "$0: $FORMAT is not supported"; exit 1;;
esac

# throughput vs msgsz
#                    +-------------+
#                    |w/encryption |
#             +------+------+------+
#             |plain |sync  |async |
# +-----+-----+------+------+------+
# |aws  |kafka|nativS|plain |      |
# |     |     |sinetS|tls   |      |
# |     |     |nativA|aes   |      |
# |     |     |sinetA|tlsaes|      |
# |     +-----+      |      |      |
# |     |mqtt |      |      |      |
# +-----+-----+      |      |      |
# |raspi|kafka|      |      |      |
# |     +-----+      |      |      |
# |     |mqtt |      |      |      |
# +-----+-----+------+------+------+

getYMIN() {
    # title=client-lib-crypto-stat
    case "$1" in
    *-enc-bw) echo "0";;
    aws-*-bw) echo "0.01";;
    raspi-*-bw) echo "0.001";;
    *-drop) echo "0";;
    *-delay) echo "0";;
    *) echo "BAD TITLE: $1"; exit 1;;
    esac
}
getYMAX() {
    # title=client-lib-crypto-stat
    case "$1" in
    *-enc-bw) echo "";;
    aws-*-bw) echo "1000";;
    raspi-*-bw) echo "100";;
    *-drop) echo "";;
    *-delay) echo "";;
    *) echo "BAD TITLE: $1" >&2; exit 1;;
    esac
    return

    echo ""; return    # XXX: logYにすると数値がわからなくなるのでlinerYでプロットして、棒グラフがつぶれないように上限を設定しない(auto scale)。

    case "$1" in
    #*aws*) echo 90;;   # xxx manual tuning
    *aws*) echo 60;;   # xxx manual tuning
    *raspi*) echo 3;;  # xxx manual tuning
    *) ;;
    esac
}

get_colno() {
    local F="$1"
    local K="$2"
    local BUF
    local COL=1
    head -1 "$F" | tr ',' '\n' | while read BUF; do
        case "$BUF" in
        $K) echo "$COL"; break;;
        esac
        COL=$((COL + 1))
    done
}

get_color() {
    local COL="$1"
    local I="$2"

    # 一番左の列は defaultの2, 3, 4, 5、中央と右は podoの6, 7, 8, 9でお願いします。
    case "$COL" in
    # colorseq=default
    nativeS)	echo 2;;
    sinetS)	echo 3;;
    nativeA)	echo 4;;
    sinetA)	echo 5;;
    # colorseq=podo
    plain)	echo 6;;
    tls)	echo 7;;
    aes)	echo 8;;
    tlsaes)	echo 9;;
    #
    *)		echo "$I";;
    esac
}

COL_BW=$(get_colno "$STAT_CSV" "bw*")
COL_DROP=$(get_colno "$STAT_CSV" "drop?%?")
COL_DELAY=$(get_colno "$STAT_CSV" "davg*")

PICK_CELL() {
    local FILTER="$1"
    local CX="$2"
    grep -- "$FILTER" "$STAT_CSV" |
      awk -F , -v CX="$CX" '{ HIT++; printf("%s", $CX) }; END{if (HIT==0) printf("-999");if (HIT != 1) exit(1)}'
}

GNUPLOT_SETTING() {
    local TITLE="$1"; shift
    local XLABLE="$1"; shift
    local YLABLE="$1"; shift
    local YSCALE="$1"; shift

    local YMIN=$(getYMIN "$TITLE")
    local YMAX=$(getYMAX "$TITLE")
    case "$FORMAT" in
    sixel) echo "set term sixel size 1536,1024 medium" ;;
    svg)
	#echo "set term svg size 1536,1024 font 'Arial,20'"
	echo "set term svg size 1536,1024 fontscale 3.5"
	echo "set output '$TITLE.svg'"
        ;;
    png)
	#echo "set term png size 1536,1024 font 'Arial,20'"
	echo "set term png size 1536,1024 fontscale 3.5"
	echo "set output '$TITLE.png'"
        ;;
    pdf)
	echo "set term pdfcairo size 4.8in,3.2in"
	echo "set output '$TITLE.pdf'"
	;;
    esac
    # http://gnuplot.sourceforge.net/demo/histograms.html
    # http://gnuplot.sourceforge.net/demo/histograms.2.gnu
    cat <<-__EOF__
	set datafile separator ","
	set boxwidth 0.9 absolute
	set style fill solid 1.00 border lt -1
	set key fixed right top vertical Right noreverse noenhanced autotitle nobox
	set style histogram clustered gap 1 title textcolor lt -1
	set datafile missing '-'
	set style data histograms
	#set xtics border in scale 0,0 nomirror rotate by -45  autojustify
	set xtics  norangelimit
	set xtics   ()
	set title "$TITLE"
	#set xrange [ * : * ] noreverse writeback
	#set x2range [ * : * ] noreverse writeback
	#set yrange [ 0.00000 : 300000. ] noreverse writeback
	#set y2range [ * : * ] noreverse writeback
	#set zrange [ * : * ] noreverse writeback
	#set cbrange [ * : * ] noreverse writeback
	#set rrange [ * : * ] noreverse writeback
	#NO_ANIMATION = 1
	#plot 'X$TMPDAT' using 6:xtic(1) ti col, '' u 12 ti col, '' u 13 ti col, '' u 14 ti col
	set yrange [$YMIN:$YMAX]
	set xlabel "$XLABLE"
	set ylabel "$YLABLE"
	set key left top reverse Left spacing 1
	set grid y
__EOF__
    case "$YSCALE" in
    log) echo "set log y";;
    *) echo "unset log y";;
    esac
}

LEN() {
    local N=0
    for X in "$@"; do
        N=$((N + 1))
    done
    echo "$N"
}

PLOT() {
    local TITLE="$1"; shift
    local DAT="$1"; shift
    local XLABLE="$1"; shift
    local YLABLE="$1"; shift
    local YSCALE="$1"; shift

    echo "title: $TITLE"
    cat "$DAT"

    {
        GNUPLOT_SETTING "$TITLE" "$XLABLE" "$YLABLE" "$YSCALE"

	case "$TITLE" in
	*-plain-*)          echo "set colorseq default";;
	*-sync-*|*-async-*) echo "set colorseq podo";;
	*-enc-*)            echo "set colorseq default";;
	esac

        echo "fn(x) = sprintf('%.1e', x)"
        printf "plot "
        SEP=""
        local I0=2
        local I=$I0
        local N="$(LEN "$@")"
        for COL in "$@"; do
            printf "$SEP '$DAT' using $I:xtic(1) title col linecolor $(get_color "$COL" "$I")"
            SEP=","
	    case "$YSCALE" in
	    log)
		#printf "$SEP '$DAT' using (column(0)-1):$I:(fn(column($I))) with labels font 'Arial,16' center offset first (0.05 + (($I-$I0)/1.2 - ($N-1) / 2.0) / ($N)),char 0.5 notitle"
		printf "$SEP '$DAT' using (column(0)-1):$I:(fn(column($I))) with labels rotate by 90 offset first (0.05 + (($I-$I0)/1.2 - ($N-1) / 2.0) / ($N)),char 2.0 notitle"
		SEP=","
		;;
	    esac
            I=$((I+1))
        done
        printf "\n"
    } | gnuplot
}

#file,snd,rcv,qsys,qos,lib,crypt,msz,nmsg,role
#perftest-20200827-195823-aws-aws-kafka-qos1-sinetS-plain-100-200000-pub.csv,aws,aws,kafka,1,sinetS,plain,100,200000,pub
#,n[msg],total[B],time[ms],imin[ms],iavg[ms],imax[ms],size[B],bw[MiB/s],iops[k msg/s],dmin[ms],davg[ms],dmax[ms],idev[ms],ddev[ms],drop[msg],drop[%],dup[msg],errcnt,note

### IEEE Access向け暗号化比較グラフ
for client in aws raspi; do
for lib in kafka mqtt; do
  COL="$COL_BW"
  YLABEL="throughput[MiB/s]"
  YSCALE="liner"
  TITLE="$client-$lib-enc-bw"
  DAT="$TITLE.dat"
  {
  printf "MSZ,Plain(S),TLS(S),AES(S),TLS+AES(S),Plain(A),TLS(A),AES(A),TLS+AES(A)\n"
  printf "1000,"
  PICK_CELL "-$client-$lib-qos1-sinetS-plain-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetS-tls-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetS-aes-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetS-tlsaes-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetA-plain-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetA-tls-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetA-aes-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetA-tlsaes-1000-.*-sub" "$COL"
  printf "\n"
  } > "$DAT"
  PLOT "$TITLE" "$DAT" "msgsz[B]" "$YLABEL" "$YSCALE" "Plain(S)" "TLS(S)" "AES(S)" "TLS+AES(S)" "Plain(A)" "TLS(A)" "AES(A)" "TLS+AES(A)"
done
done

exit

#１：暗号化比較なし＋sync＋async（今と同じ）、sync＋暗号化比較あり、async＋暗号化比較あり
#２：クライアントxシステム (aws,raspi)x(kafka,mqtt)
#３：100, 1000, 10000
#４：スループット
#５：１列目：(native,sinetstream)x(sync,async)，２・3列目：sinetstream plain, tls, encryption, tls+encryptionの4つ

### 1列目
for client in aws raspi; do
for lib in kafka mqtt; do
for col in bw drop delay; do
  case "$col" in
  bw) COL="$COL_BW"; YLABEL="throughput[MiB/s]"; YSCALE="log" ;;
  drop) COL="$COL_DROP"; YLABEL="drop[%]"; YSCALE="liner" ;;
  delay) COL="$COL_DELAY"; YLABEL="delay[ms]"; YSCALE="log" ;;
  *) COL=XXX; YLABEL="XXX" ;;
  esac

  TITLE="$client-$lib-plain-$col"
  DAT="$TITLE.dat"
  {
  printf "MSZ,nativeS,sinetS,nativeA,sinetA\n"
  printf "100,"
  PICK_CELL "-$client-$lib-qos1-nativS-plain-100-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetS-plain-100-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-nativA-plain-100-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetA-plain-100-.*-sub" "$COL"
  printf "\n"
  printf "1000,"
  PICK_CELL "-$client-$lib-qos1-nativS-plain-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetS-plain-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-nativA-plain-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetA-plain-1000-.*-sub" "$COL"
  printf "\n"
  printf "10000,"
  PICK_CELL "-$client-$lib-qos1-nativS-plain-10000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetS-plain-10000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-nativA-plain-10000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinetA-plain-10000-.*-sub" "$COL"
  printf "\n"
  } > "$DAT"
  PLOT "$TITLE" "$DAT" "msgsz[B]" "$YLABEL" "$YSCALE" nativeS sinetS nativeA sinetA
done
done
done

### 2,3列目

for client in aws raspi; do
for lib in kafka mqtt; do
for api in S A; do
for col in bw drop delay; do
  case "$col" in
  bw) COL="$COL_BW"; YLABEL="throughput[MiB/s]"; YSCALE="log" ;;
  drop) COL="$COL_DROP"; YLABEL="drop[%]"; YSCALE="liner" ;;
  delay) COL="$COL_DELAY"; YLABEL="delay[ms]"; YSCALE="log" ;;
  *) COL=XXX; YLABEL="XXX" ;;
  esac

  case "$api" in
  S) apiname="sync";;
  A) apiname="async";;
  *) apiname="xxx";;
  esac
  TITLE="$client-$lib-$apiname-$col"
  DAT="$TITLE.dat"
  {
  printf "MSZ,Plain,TLS,AES,TLS+AES\n"
  printf "100,"
  PICK_CELL "-$client-$lib-qos1-sinet$api-plain-100-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-tls-100-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-aes-100-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-tlsaes-100-.*-sub" "$COL"
  printf "\n"
  printf "1000,"
  PICK_CELL "-$client-$lib-qos1-sinet$api-plain-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-tls-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-aes-1000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-tlsaes-1000-.*-sub" "$COL"
  printf "\n"
  printf "10000,"
  PICK_CELL "-$client-$lib-qos1-sinet$api-plain-10000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-tls-10000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-aes-10000-.*-sub" "$COL"
  printf ","
  PICK_CELL "-$client-$lib-qos1-sinet$api-tlsaes-10000-.*-sub" "$COL"
  printf "\n"
  } > "$DAT"
  PLOT "$TITLE" "$DAT" "msgsz[B]" "$YLABEL" "$YSCALE" plain tls aes tlsaes
done
done
done
done

exit 0
