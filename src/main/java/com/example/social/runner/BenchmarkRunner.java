package com.example.social.runner;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.example.social.domain.entity.ApiLog;
import com.example.social.domain.entity.ApiLogRefined;
import com.example.social.domain.repository.ApiLogRefinedRepository;
import com.example.social.domain.repository.ApiLogRepository;
import com.example.social.domain.repository.ClickHouseLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BenchmarkRunner implements CommandLineRunner {

    private final ApiLogRepository apiLogRepository;
    private final ApiLogRefinedRepository apiLogRefinedRepository;
    private final ClickHouseLogRepository clickHouseRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(String... args) throws Exception {
        log.info("=============================================================");
        log.info("   ULTIMATE BENCHMARK: INSERT vs ETL vs AGGREGATION");
        log.info("=============================================================");

        int newRecordsToAdd = 1_000_000;
        int batchSize = 100_000;

        log.info(">> [SETUP] {} og data points are being prepared in RAM...", newRecordsToAdd);
        List<ApiLog> hugeDataList = new ArrayList<>(newRecordsToAdd);
        Random random = new Random();
        String[] methods = {"GET", "POST", "PUT", "DELETE"};

        for (int i = 0; i < newRecordsToAdd; i++) {
            hugeDataList.add(ApiLog.builder()
                    .id(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .httpMethod(methods[random.nextInt(methods.length)])
                    .path("/api/test/" + i)
                    .statusCode(200)
                    .duration(random.nextInt(1000))
                    .requestBody("Payload " + i)
                    .responseBody("Response " + i)
                    .userId("user-" + i)
                    .clientIp("127.0.0.1")
                    .userAgent("Bench/1.0")
                    .build());
        }
        log.info(">> [SETUP] Completed.");

        log.info("--- PHASE 1: INSERT PERFORMANCE ({} Records) ---", newRecordsToAdd);

        // ELASTIC INSERT
        long esInsertStart = System.currentTimeMillis();
        for (int i = 0; i < newRecordsToAdd; i += batchSize) {
            int end = Math.min(i + batchSize, newRecordsToAdd);
            apiLogRepository.saveAll(hugeDataList.subList(i, end));
            if ((i + batchSize) % 100_000 == 0) log.info("ES Insert: {}...", i + batchSize);
        }
        long esInsertDuration = System.currentTimeMillis() - esInsertStart;
        log.info(">> Elastic Insert Finished: {} ms", esInsertDuration);

        // CLICKHOUSE INSERT
        clickHouseRepository.createRefinedTable();
        long chInsertStart = System.currentTimeMillis();
        for (int i = 0; i < newRecordsToAdd; i += batchSize) {
            int end = Math.min(i + batchSize, newRecordsToAdd);
            clickHouseRepository.saveAll(hugeDataList.subList(i, end));
            if ((i + batchSize) % 100_000 == 0) log.info("CH Insert: {}...", i + batchSize);
        }
        long chInsertDuration = System.currentTimeMillis() - chInsertStart;
        log.info(">> ClickHouse Insert Finished: {} ms", chInsertDuration);

        log.info("--- PHASE 2: AGGREGATION PERFORMANCE ---");

        long esAggStart = System.currentTimeMillis();
        Query aggQuery = NativeQuery.builder()
                .withAggregation("byMethod", Aggregation.of(a -> a
                        .terms(t -> t.field("httpMethod.keyword").size(10))
                ))
                .build();

        SearchHits<ApiLog> esAggResult = elasticsearchOperations.search(aggQuery, ApiLog.class);

        long esAggDuration = System.currentTimeMillis() - esAggStart;
        log.info(">> Elastic Aggregation Finished: {} ms (Hit Count: {})", esAggDuration, esAggResult.getTotalHits());

        long chAggStart = System.currentTimeMillis();
        clickHouseRepository.runAggregationBenchmark();
        long chAggDuration = System.currentTimeMillis() - chAggStart;
        log.info(">> ClickHouse Aggregation Finished: {} ms", chAggDuration);

        log.info("--- PHASE 3: ETL PERFORMANCE ---");

        log.info(">> Elasticsearch ETL Starting...");
        long esReadTime = 0, esProcessTime = 0, esWriteTime = 0;
        long esTotalStart = System.currentTimeMillis();

        Query streamQuery = Query.findAll();
        streamQuery.setPageable(org.springframework.data.domain.PageRequest.of(0, 5000));

        List<ApiLog> esBuffer = new ArrayList<>();
        int esProcessedCount = 0;

        try (SearchHitsIterator<ApiLog> stream = elasticsearchOperations.searchForStream(streamQuery, ApiLog.class)) {
            long tStart = System.currentTimeMillis();
            while (stream.hasNext()) {
                SearchHit<ApiLog> hit = stream.next();
                long tReadDone = System.currentTimeMillis();
                esReadTime += (tReadDone - tStart);

                esBuffer.add(hit.getContent());

                if (esBuffer.size() >= 10_000) {
                    long tProcStart = System.currentTimeMillis();
                    List<ApiLogRefined> refinedLogs = processLogs(esBuffer);
                    long tProcEnd = System.currentTimeMillis();
                    esProcessTime += (tProcEnd - tProcStart);

                    long tWriteStart = System.currentTimeMillis();
                    apiLogRefinedRepository.saveAll(refinedLogs);
                    esWriteTime += (System.currentTimeMillis() - tWriteStart);

                    esProcessedCount += esBuffer.size();
                    if(esProcessedCount % 200_000 == 0) log.info("ES ETL: {}...", esProcessedCount);
                    esBuffer.clear();
                }
                tStart = System.currentTimeMillis();
            }
            if(!esBuffer.isEmpty()) {
                long tProcStart = System.currentTimeMillis();
                List<ApiLogRefined> refinedLogs = processLogs(esBuffer);
                esProcessTime += (System.currentTimeMillis() - tProcStart);

                long tWriteStart = System.currentTimeMillis();
                apiLogRefinedRepository.saveAll(refinedLogs);
                esWriteTime += (System.currentTimeMillis() - tWriteStart);
                esProcessedCount += esBuffer.size();
            }
        }
        long esTotalDuration = System.currentTimeMillis() - esTotalStart;

        log.info(">> ClickHouse ETL Starting...");
        long chReadTime = 0, chProcessTime = 0, chWriteTime = 0;
        long chTotalStart = System.currentTimeMillis();

        long tStart = System.currentTimeMillis();
        List<ApiLog> chRawLogs = clickHouseRepository.findAll();
        chReadTime = (System.currentTimeMillis() - tStart);
        log.info("CH: {} data entries were stored in memory.", chRawLogs.size());

        int chPageSize = 10_000;
        for (int i = 0; i < chRawLogs.size(); i += chPageSize) {
            int end = Math.min(i + chPageSize, chRawLogs.size());
            List<ApiLog> batch = chRawLogs.subList(i, end);

            long tProcStart = System.currentTimeMillis();
            List<ApiLogRefined> refinedLogs = processLogs(batch);
            chProcessTime += (System.currentTimeMillis() - tProcStart);

            long tWriteStart = System.currentTimeMillis();
            clickHouseRepository.saveRefinedAll(refinedLogs);
            chWriteTime += (System.currentTimeMillis() - tWriteStart);

            if ((i + chPageSize) % 200_000 == 0) log.info("CH ETL: {}...", i + chPageSize);
        }
        long chTotalDuration = System.currentTimeMillis() - chTotalStart;

        printReport(newRecordsToAdd, esInsertDuration, chInsertDuration,
                esAggDuration, chAggDuration,
                esProcessedCount, esTotalDuration, esReadTime, esProcessTime, esWriteTime,
                chRawLogs.size(), chTotalDuration, chReadTime, chProcessTime, chWriteTime);
    }

    private List<ApiLogRefined> processLogs(List<ApiLog> rawLogs) {
        List<ApiLogRefined> refinedList = new ArrayList<>(rawLogs.size());
        for (ApiLog log : rawLogs) {
            String userType = "BILINMIYOR";
            try {
                if (log.getUserId() != null && log.getUserId().contains("-")) {
                    int idNum = Integer.parseInt(log.getUserId().split("-")[1]);
                    userType = (idNum % 2 == 0) ? "CIFT" : "TEK";
                }
            } catch (Exception e) {}

            String color = "Black";
            long d = log.getDuration();
            if (d < 200) color = "Green";
            else if (d < 400) color = "Blue";
            else if (d < 600) color = "Yellow";
            else if (d < 800) color = "Orange";
            else color = "Red";

            refinedList.add(ApiLogRefined.builder()
                    .id(UUID.randomUUID().toString())
                    .originalLogId(log.getId())
                    .userType(userType)
                    .durationColor(color)
                    .build());
        }
        return refinedList;
    }

    private void printReport(int insertCount, long esInsert, long chInsert,
                             long esAgg, long chAgg,
                             int esEtlCount, long esEtlTotal, long esEtlRead, long esEtlProc, long esEtlWrite,
                             int chEtlCount, long chEtlTotal, long chEtlRead, long chEtlProc, long chEtlWrite) {


        log.info("\n");
        log.info("##########################################################################################");
        log.info("#                           DETAILED PERFORMANCE REPORT                                  #");
        log.info("##########################################################################################");

        log.info("--- 1. INSERT PERFORMANCE ({} New Records) ---", insertCount);
        log.info(String.format("| %-15s | %-25s | %-20s |", "Technology", "Duration (ms / s)", "Speed (Row/Sec)"));
        log.info("----------------------------------------------------------------------");
        log.info(String.format("| %-15s | %-25s | %-20.0f |", "Elasticsearch",
                String.format("%d / %.2f s", esInsert, esInsert / 1000.0),
                (double)insertCount / (esInsert / 1000.0)));
        log.info(String.format("| %-15s | %-25s | %-20.0f |", "ClickHouse",
                String.format("%d / %.2f s", chInsert, chInsert / 1000.0),
                (double)insertCount / (chInsert / 1000.0)));

        log.info("--- 2. AGGREGATION PERFORMANCE (Group By + Avg) ---");
        log.info(String.format("| %-15s | %-25s | %-20s |", "Technology", "Duration (ms / s)", "Status"));
        log.info("----------------------------------------------------------------------");
        log.info(String.format("| %-15s | %-25s | %-20s |", "Elasticsearch",
                String.format("%d / %.3f s", esAgg, esAgg / 1000.0),
                esAgg < chAgg ? "FASTER" : ""));
        log.info(String.format("| %-15s | %-25s | %-20s |", "ClickHouse",
                String.format("%d / %.3f s", chAgg, chAgg / 1000.0),
                chAgg < esAgg ? "FASTER (Winner)" : ""));

        log.info("--- 3. ETL PERFORMANCE (Full Table Scan & Transform) ---");
        log.info(String.format("| %-12s | %-9s | %-17s | %-17s | %-17s | %-17s |", "Tech", "Count", "TOTAL (ms/s)", "READ (ms/s)", "PROC (ms/s)", "WRITE (ms/s)"));
        log.info("--------------------------------------------------------------------------------------------------------------");

        log.info(String.format("| %-12s | %-9d | %-17s | %-17s | %-17s | %-17s |", "Elastic", esEtlCount,
                String.format("%d/%.1fs", esEtlTotal, esEtlTotal/1000.0),
                String.format("%d/%.1fs", esEtlRead, esEtlRead/1000.0),
                String.format("%d/%.1fs", esEtlProc, esEtlProc/1000.0),
                String.format("%d/%.1fs", esEtlWrite, esEtlWrite/1000.0)));

        log.info(String.format("| %-12s | %-9d | %-17s | %-17s | %-17s | %-17s |", "ClickHouse", chEtlCount,
                String.format("%d/%.1fs", chEtlTotal, chEtlTotal/1000.0),
                String.format("%d/%.1fs", chEtlRead, chEtlRead/1000.0),
                String.format("%d/%.1fs", chEtlProc, chEtlProc/1000.0),
                String.format("%d/%.1fs", chEtlWrite, chEtlWrite/1000.0)));

        log.info("\n##########################################################################################\n");
    }
}