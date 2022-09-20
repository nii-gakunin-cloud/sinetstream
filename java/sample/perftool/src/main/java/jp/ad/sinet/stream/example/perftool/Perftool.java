/*
 * Copyright (C) 2022 National Institute of Informatics
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jp.ad.sinet.stream.example.perftool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.ad.sinet.stream.api.AsyncMessageReader;
import jp.ad.sinet.stream.api.AsyncMessageWriter;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Metrics;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

///////////////////////////////////////////////////////////////////////////////

@SuppressWarnings("WeakerAccess")
public class Perftool {
    public static final String OPTION_NAME_SERVICE = "service";
    public static final String OPTION_NAME_CONFIG = "config";
    public static final String OPTION_NAME_FORMAT = "format";
    public static final String OPTION_NAME_OUTPUT_COUNT = "output-count";
    public static final String OPTION_NAME_NUM_SAMPLES = "num-samples";
    public static final String OPTION_NAME_PAYLOAD_SIZE = "payload-size";
    public static final String OPTION_NAME_ASYNC_API = "async-api";
    public static final String OPTION_NAME_NUM_THREADS = "num-threads";

    public static final String OPTION_SHORT_NAME_SERVICE = "s";
    public static final String OPTION_SHORT_NAME_FORMAT = "f";
    public static final String OPTION_SHORT_NAME_OUTPUT_COUNT = "c";
    public static final String OPTION_SHORT_NAME_NUM_SAMPLES = "n";
    public static final String OPTION_SHORT_NAME_PAYLOAD_SIZE = "p";
    public static final String OPTION_SHORT_NAME_ASYNC_API = "a";
    public static final String OPTION_SHORT_NAME_NUM_THREADS = "t";

    public static final String OPTION_FORMAT_TSV = "tsv";
    public static final String OPTION_FORMAT_JSON = "json";

    public static final String DEFAULT_FORMAT = OPTION_FORMAT_JSON;
    public static final String DEFAULT_OUTPUT_COUNT = "1";
    public static final String DEFAULT_NUM_SAMPLES = "300";
    public static final String DEFAULT_PAYLOAD_SIZE = "1024";
    public static final String DEFAULT_NUM_THREADS = "1";

    public static final String TOPIC_NAME_BASE = "sinetstream-perftool-";
    public static final Duration RECEIVE_TIMEOUT = Duration.ofSeconds(30);

    private final String service_;
    private final String config_;
    private final int outputCount_;
    private final int numSamples_;
    private final int payloadSize_;
    private final boolean asyncApi_;
    private final int numThreads_;
    private final Output output_;

    ///////////////////////////////////////////////////////////////////////////

    public Perftool(String service,
                    String config,
                    String format,
                    int outpuCount,
                    int numSamples,
                    int payloadSize,
                    boolean asyncApi,
                    int numThreads)
    {
        service_ = service;
        config_ = config;
        outputCount_ = outpuCount;
        numSamples_ = numSamples;
        payloadSize_ = payloadSize;
        asyncApi_ = asyncApi;
        numThreads_ = numThreads;

        output_ =
        (format.equals(OPTION_FORMAT_TSV)) ? new OutputTsv() : new OutputJson();
    }

    ///////////////////////////////////////////////////////////////////////////

    public void measure()
    {
        List<CompletableFuture<List<ResultContainer>>> cfs =
        new LinkedList<CompletableFuture<List<ResultContainer>>>();
        if(asyncApi_){
            for (int i = 0 ; i < numThreads_ ; i += 1){
                int threadNumber = i;
                cfs.add(CompletableFuture.supplyAsync(() -> { 
                            return doAsync(service_,
                                           config_,
                                           outputCount_,
                                           numSamples_,
                                           payloadSize_,
                                           threadNumber
                                           );
                                                      
                        }));
            }
        }
        else{
            for (int i = 0 ; i < numThreads_ ; i += 1){
                int threadNumber = i;
                cfs.add(CompletableFuture.supplyAsync(() -> {
                            return doSync(service_,
                                          config_,
                                          outputCount_,
                                          numSamples_,
                                          payloadSize_,
                                          threadNumber
                                          );
                        }));
            }
        }

        CompletableFuture[] cfsArray =
        cfs.toArray(new CompletableFuture[cfs.size()]);
        CompletableFuture.allOf(cfsArray).join();

        cfs.forEach(result -> {
                try {
                    output_.execute(result.get());
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            });
    }

    ///////////////////////////

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    private static List<ResultContainer> doAsync(String service,
                                                 String config,
                                                 int outputCount,
                                                 int numSamples,
                                                 int payloadSize,
                                                 int threadNumber)
    {
        MessageReaderFactory<byte[]> readerFactory =
        MessageReaderFactory.<byte[]>builder()
            .service(service)
            .topic(TOPIC_NAME_BASE + threadNumber)
            .configName(config)
            .consistency(Consistency.AT_LEAST_ONCE)
            .valueType(SimpleValueType.BYTE_ARRAY)
            .receiveTimeout(RECEIVE_TIMEOUT)
            .build();
        MessageWriterFactory<byte[]> writerFactory =
        MessageWriterFactory.<byte[]>builder()
            .service(service)
            .topic(TOPIC_NAME_BASE + threadNumber)
            .configName(config)
            .consistency(Consistency.AT_LEAST_ONCE)
            .valueType(SimpleValueType.BYTE_ARRAY)
            .build();

        List<ResultContainer> results = new LinkedList<ResultContainer>();
        for (int i = 0; i < outputCount; i += 1){
            try (AsyncMessageReader<byte[]> reader =
                 readerFactory.getAsyncReader())
                {
                    reader.addOnMessageCallback((msg) -> {
                            // to do nothing
                        });

                    try (AsyncMessageWriter<byte[]> writer =
                         writerFactory.getAsyncWriter())
                        {
                            for (int j = 0; j < numSamples; j += 1){
                                byte[] message = getMessage(payloadSize);
                                writer.write(message)
                                .always((s, r, e) -> {
                                        // to do nothing
                                    });

                            }

                            ResultContainer result =
                            new ResultContainer(threadNumber,
                                                writer.getMetrics(),
                                                reader.getMetrics());
                            results.add(result);
                        }
                }
        }
        return results;
    }

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    private static List<ResultContainer> doSync(String service,
                                                String config,
                                                int outputCount,
                                                int numSamples,
                                                int payloadSize,
                                                int threadNumber)
    {
        MessageReaderFactory<byte[]> readerFactory =
        MessageReaderFactory.<byte[]>builder()
            .service(service)
            .topic(TOPIC_NAME_BASE + threadNumber)
            .configName(config)
            .consistency(Consistency.AT_LEAST_ONCE)
            .valueType(SimpleValueType.BYTE_ARRAY)
            .receiveTimeout(RECEIVE_TIMEOUT)
            .build();
        MessageWriterFactory<byte[]> writerFactory =
        MessageWriterFactory.<byte[]>builder()
            .service(service)
            .topic(TOPIC_NAME_BASE + threadNumber)
            .configName(config)
            .consistency(Consistency.AT_LEAST_ONCE)
            .valueType(SimpleValueType.BYTE_ARRAY)
            .build();

        MessageReader<byte[]> reader = readerFactory.getReader();
        Thread readerThread =
        new Thread(() -> {
                Message<byte[]> msg;
                while (Objects.nonNull(msg = reader.read())) {
                    // to do nothing
                }
            });
        readerThread.setDaemon(true);
        readerThread.start();

        List<ResultContainer> results = new LinkedList<ResultContainer>();
        for (int i = 0; i < outputCount; i += 1){
            try (MessageWriter<byte[]> writer =
                 writerFactory.getWriter())
                {
                    writer.resetMetrics();
                    reader.resetMetrics();
                    for (int j = 0; j < numSamples; j += 1){
                        byte[] message = getMessage(payloadSize);
                        writer.write(message);
                    }


                    ResultContainer result =
                    new ResultContainer(threadNumber,
                                        writer.getMetrics(),
                                        reader.getMetrics());
                    results.add(result);
                }
        }

        reader.close();
        return results;
    }

    private static byte[] getMessage(int payloadSize)
    {
        Random random = new Random();
        byte[] bs = new byte[payloadSize];
        random.nextBytes(bs);
        return bs;
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class ResultContainer
    {
        public final int threadNumber_;
        public final Metrics writerMetrics_;
        public final Metrics readerMetrics_;

        ///////////////////////////////////////////////////////////////////////

        public ResultContainer(int threadNumber,
                               Metrics writerMetrics,
                               Metrics readerMetrics)
        {
            threadNumber_ = threadNumber;
            writerMetrics_ = writerMetrics;
            readerMetrics_ = readerMetrics;
            
        }

        ///////////////////////////////////////////////////////////////////////

    }

    ///////////////////////////////////////////////////////////////////////////

    private static interface Output
    {
        public void execute(List<ResultContainer> results)
            throws Exception; //
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class OutputTsv
        implements Output //
    {
        private static final String[] HEADER = {
            "thread_number",
            "writer_start_time",
            "writer_start_time_ms",
            "writer_end_time",
            "writer_end_time_ms",
            "writer_time",
            "writer_time_ms",
            "writer_msg_count_total",
            "writer_msg_count_rate",
            "writer_msg_bytes_total",
            "writer_msg_bytes_rate",
            "writer_msg_size_min",
            "writer_msg_size_max",
            "writer_msg_size_avg",
            "writer_error_count_total",
            "writer_error_count_rate",
            "reader_start_time",
            "reader_start_time_ms",
            "reader_end_time",
            "reader_end_time_ms",
            "reader_time",
            "reader_time_ms",
            "reader_msg_count_total",
            "reader_msg_count_rate",
            "reader_msg_bytes_total",
            "reader_msg_bytes_rate",
            "reader_msg_size_min",
            "reader_msg_size_max",
            "reader_msg_size_avg",
            "reader_error_count_total",
            "reader_error_count_rate"
        };

        private static final String DELIMITER = "\t";
        
        ///////////////////////////////////////////////////////////////////////

        public OutputTsv(){
            System.out.println(String.join(DELIMITER, HEADER));
        }

        ///////////////////////////////////////////////////////////////////////
        // Implementation of interface Output
        public void execute(List<ResultContainer> results){
            for (ResultContainer result : results){
                Metrics writerMetrics = result.writerMetrics_;
                Metrics readerMetrics = result.readerMetrics_;
                String[] outputArray = {
                    String.valueOf(result.threadNumber_),
                    String.valueOf(writerMetrics.getStartTime()),
                    String.valueOf(writerMetrics.getStartTimeMillis()),
                    String.valueOf(writerMetrics.getEndTime()),
                    String.valueOf(writerMetrics.getEndTimeMillis()),
                    String.valueOf(writerMetrics.getTime()),
                    String.valueOf(writerMetrics.getTimeMillis()),
                    String.valueOf(writerMetrics.getMsgCountTotal()),
                    String.valueOf(writerMetrics.getMsgCountRate()),
                    String.valueOf(writerMetrics.getMsgBytesTotal()),
                    String.valueOf(writerMetrics.getMsgBytesRate()),
                    String.valueOf(writerMetrics.getMsgSizeMin()),
                    String.valueOf(writerMetrics.getMsgSizeAvg()),
                    String.valueOf(writerMetrics.getMsgSizeMax()),
                    String.valueOf(writerMetrics.getErrorCountTotal()),
                    String.valueOf(writerMetrics.getErrorCountRate()),
                    String.valueOf(readerMetrics.getStartTime()),
                    String.valueOf(readerMetrics.getStartTimeMillis()),
                    String.valueOf(readerMetrics.getEndTime()),
                    String.valueOf(readerMetrics.getEndTimeMillis()),
                    String.valueOf(readerMetrics.getTime()),
                    String.valueOf(readerMetrics.getTimeMillis()),
                    String.valueOf(readerMetrics.getMsgCountTotal()),
                    String.valueOf(readerMetrics.getMsgCountRate()),
                    String.valueOf(readerMetrics.getMsgBytesTotal()),
                    String.valueOf(readerMetrics.getMsgBytesRate()),
                    String.valueOf(readerMetrics.getMsgSizeMin()),
                    String.valueOf(readerMetrics.getMsgSizeAvg()),
                    String.valueOf(readerMetrics.getMsgSizeMax()),
                    String.valueOf(readerMetrics.getErrorCountTotal()),
                    String.valueOf(readerMetrics.getErrorCountRate())
                };

                System.out.println(String.join(DELIMITER, outputArray));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class OutputJson
        implements Output //
    {
        
        ///////////////////////////////////////////////////////////////////////
        // Implementation of interface Output
        public void execute(List<ResultContainer> results)
            throws Exception //
        {
            for (ResultContainer result : results){
                JsonOutputContainer output = new JsonOutputContainer(result);
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = mapper.writeValueAsString(output);
                System.out.println(jsonString);
            }
        }

        ///////////////////////////////////////////////////////////////////////

        private static class JsonOutputContainer
        {
            public final JsonOutputMetricsContainer writer;
            public final JsonOutputMetricsContainer reader;

            ///////////////////////////////////////////////////////////////////

            public JsonOutputContainer(ResultContainer result)
            {
                this(new JsonOutputMetricsContainer(result.threadNumber_,
                                                    result.writerMetrics_),
                     new JsonOutputMetricsContainer(result.threadNumber_,
                                                    result.readerMetrics_)
                     );
            }

            public JsonOutputContainer(JsonOutputMetricsContainer writerMetrics,
                                       JsonOutputMetricsContainer readerMetrics)
            {
                writer = writerMetrics;
                reader = readerMetrics;
            }

            ///////////////////////////////////////////////////////////////////

        }

        ///////////////////////////////////////////////////////////////////////

        private static class JsonOutputMetricsContainer
        {
            public final int thread_number;
            public final double start_time;
            public final long start_time_ms;
            public final double end_time;
            public final long end_time_ms;
            public final double time;
            public final long time_ms;
            public final long msg_count_total;
            public final double msg_count_rate;
            public final long msg_bytes_total;
            public final double msg_bytes_rate;
            public final long msg_size_min;
            public final long msg_size_max;
            public final double msg_size_avg;
            public final long error_count_total;
            public final double error_count_rate;

            ///////////////////////////////////////////////////////////////////

            public JsonOutputMetricsContainer(int threadNumber,
                                              Metrics metrics)
            {
                thread_number = threadNumber;
                start_time = metrics.getStartTime();
                start_time_ms = metrics.getStartTimeMillis();
                end_time = metrics.getEndTime();
                end_time_ms = metrics.getEndTimeMillis();
                time = metrics.getTime();
                time_ms = metrics.getTimeMillis();
                msg_count_total = metrics.getMsgCountTotal();
                msg_count_rate = metrics.getMsgCountRate();
                msg_bytes_total = metrics.getMsgBytesTotal();
                msg_bytes_rate = metrics.getMsgBytesRate();
                msg_size_min = metrics.getMsgSizeMin();
                msg_size_max = metrics.getMsgSizeMax();
                msg_size_avg = metrics.getMsgSizeAvg();
                error_count_total = metrics.getErrorCountTotal();
                error_count_rate = metrics.getErrorCountRate();
            }

            ///////////////////////////////////////////////////////////////////
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        Options opts = new Options();
        opts.addOption(Option.builder(OPTION_SHORT_NAME_SERVICE)
                           .hasArg()
                           .longOpt(OPTION_NAME_SERVICE)
                           .build());
        opts.addOption(Option.builder()
                           .hasArg()
                           .longOpt(OPTION_NAME_CONFIG)
                           .build());
        opts.addOption(Option.builder(OPTION_SHORT_NAME_FORMAT)
                           .hasArg()
                           .longOpt(OPTION_NAME_FORMAT)
                           .build());
        opts.addOption(Option.builder(OPTION_SHORT_NAME_OUTPUT_COUNT)
                           .hasArg()
                           .longOpt(OPTION_NAME_OUTPUT_COUNT)
                           .build());
        opts.addOption(Option.builder(OPTION_SHORT_NAME_NUM_SAMPLES)
                           .hasArg()
                           .longOpt(OPTION_NAME_NUM_SAMPLES)
                           .build());
        opts.addOption(Option.builder(OPTION_SHORT_NAME_PAYLOAD_SIZE)
                           .hasArg()
                           .longOpt(OPTION_NAME_PAYLOAD_SIZE)
                           .build());
        opts.addOption(Option.builder(OPTION_SHORT_NAME_ASYNC_API)
                           .hasArg(false)
                           .longOpt(OPTION_NAME_ASYNC_API)
                           .build());
        opts.addOption(Option.builder(OPTION_SHORT_NAME_NUM_THREADS)
                           .hasArg()
                           .longOpt(OPTION_NAME_NUM_THREADS)
                           .build());

        CommandLineParser parser = new DefaultParser();
        Perftool perftool = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            String service = cmd.getOptionValue(OPTION_NAME_SERVICE);
            String config = cmd.getOptionValue(OPTION_NAME_CONFIG);
            if (service == null && config == null){
                throw new ParseException("-s/--service must be specified if -c/--config is not specified.");
            }

            String format =
            cmd.getOptionValue(OPTION_NAME_FORMAT, DEFAULT_FORMAT);
            if(!(format.equals(OPTION_FORMAT_JSON) ||
                 format.equals(OPTION_FORMAT_TSV)))
            {
                throw new ParseException("-f/--format must be 'json' or 'tsv'.");
            }

            String outpuCountString =
            cmd.getOptionValue(OPTION_NAME_OUTPUT_COUNT, DEFAULT_OUTPUT_COUNT);
            if(!NumberUtils.isDigits(outpuCountString)){
                throw new ParseException("-o/--output-count must be numeric.");
            }
            int outputCount = Integer.parseInt(outpuCountString);

            String numSamplesString =
            cmd.getOptionValue(OPTION_NAME_NUM_SAMPLES, DEFAULT_NUM_SAMPLES);
            if(!NumberUtils.isDigits(numSamplesString)){
                throw new ParseException("-n/--num-samples must be numeric.");
            }
            int numSamples = Integer.parseInt(numSamplesString);

            String payloadSizeString =
            cmd.getOptionValue(OPTION_NAME_PAYLOAD_SIZE, DEFAULT_PAYLOAD_SIZE);
            if(!NumberUtils.isDigits(payloadSizeString)){
                throw new ParseException("-p/--payload-size must be numeric.");
            }
            int payloadSize = Integer.parseInt(payloadSizeString);

            boolean asyncApi = cmd.hasOption(OPTION_NAME_ASYNC_API);

            String numThreadsString =
            cmd.getOptionValue(OPTION_NAME_NUM_THREADS, DEFAULT_NUM_THREADS);
            if(!NumberUtils.isDigits(numThreadsString)){
                throw new ParseException("-t/--num-threads must be numeric.");
            }
            int numThreads = Integer.parseInt(numThreadsString);

            perftool =
            new Perftool(service,
                         config,
                         format,
                         outputCount,
                         numSamples,
                         payloadSize,
                         asyncApi,
                         numThreads);
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter()
                .printHelp(Perftool.class.getSimpleName(), opts, true);
            System.exit(1);
        }

        try {
            perftool.measure();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
