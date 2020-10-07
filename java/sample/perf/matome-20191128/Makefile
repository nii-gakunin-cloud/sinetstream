all: plot pdf
plot: summary2.csv
	./graph.sh $<
pdf: summary2.csv
	env FMT=pdf ./graph.sh $<
summary2.csv: summary.csv
	./stat2.sh < $< > $@
	
summary.csv: perftest*.csv
	#../../../../python/sample/perf/stat-perf.sh perftest*.csv | tee $@
	./stat-perf.sh perftest*.csv | tee $@
