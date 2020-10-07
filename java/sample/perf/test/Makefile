INTERVAL_NS=1000000
N=1000
all:: data hist plot
plot:: hist
	gnuplot -e 'set term sixel size 1024,1024; set grid; set xlabel "Jitter[us]"; set ylabel "Freq"; set datafile separator ","; plot "hist" with boxes'
data: TestSched.class
	java TestSched $(INTERVAL_NS) $(N) > data
hist: data
	awk -F , 'BEGIN {w=100000; imin=99999; imax=-99999}; {i=int($$2/w); hist[i]++; if(imin>i)imin=i; if(imax<i)imax=i}; END {for (i=imin; i<=imax; i++) print ((i+0.5)*w) "," hist[i]+0}' data > hist
TestSched.class: TestSched.java
	javac TestSched.java
clean::
	-rm -f data hist

CSV=tmp.csv
VIEW=../view-csv.sh
PICK=../pick-csv.sh
test:
	$(VIEW) $(CSV)
	$(PICK) $(CSV) snd,rcv,qsys,lib lib native,sinetS,sinetA bw | $(VIEW)
	$(PICK) $(CSV) snd,rcv,qsys,lib qsys kafka,mqtt bw | $(VIEW)
