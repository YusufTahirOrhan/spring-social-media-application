package com.example.social.domain.repository;

import com.example.social.domain.entity.ApiLog;
import com.example.social.domain.entity.ApiLogRefined;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class ClickHouseLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClickHouseLogRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ApiLog log) {
        String sql = """
            INSERT INTO api_logs (
                id, `timestamp`, httpMethod, `path`, statusCode, duration, 
                requestHeader, requestBody, responseHeader, responseBody, 
                userId, clientIp, userAgent
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String logId = (log.getId() != null) ? log.getId() : UUID.randomUUID().toString();

        jdbcTemplate.update(sql,
                logId,
                Timestamp.from(log.getTimestamp()),
                log.getHttpMethod(),
                log.getPath(),
                log.getStatusCode(),
                log.getDuration(),
                log.getRequestHeader(),
                log.getRequestBody(),
                log.getResponseHeader(),
                log.getResponseBody(),
                log.getUserId(),
                log.getClientIp(),
                log.getUserAgent()
        );
    }

    public void saveAll(List<ApiLog> logs) {
        String sql = """
            INSERT INTO api_logs (
                id, `timestamp`, httpMethod, `path`, statusCode, duration, 
                requestHeader, requestBody, responseHeader, responseBody, 
                userId, clientIp, userAgent
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ApiLog log = logs.get(i);
                String logId = (log.getId() != null) ? log.getId() : UUID.randomUUID().toString();

                ps.setObject(1, logId);
                ps.setTimestamp(2, Timestamp.from(log.getTimestamp()));
                ps.setString(3, log.getHttpMethod());
                ps.setString(4, log.getPath());
                ps.setInt(5, log.getStatusCode());
                ps.setLong(6, log.getDuration());
                ps.setString(7, log.getRequestHeader());
                ps.setString(8, log.getRequestBody());
                ps.setString(9, log.getResponseHeader());
                ps.setString(10, log.getResponseBody());
                ps.setString(11, log.getUserId());
                ps.setString(12, log.getClientIp());
                ps.setString(13, log.getUserAgent());
            }

            @Override
            public int getBatchSize() {
                return logs.size();
            }
        });
    }

    public void createRefinedTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS default.api_logs_refined (
                id String,
                originalLogId String,
                userType String,
                durationColor String
            ) ENGINE = MergeTree() ORDER BY id
        """;
        jdbcTemplate.execute(sql);
    }

    public java.util.List<ApiLog> findAll() {
        String sql = "SELECT * FROM default.api_logs";

        return jdbcTemplate.query(sql, (rs, rowNum) -> ApiLog.builder()
                .id(rs.getString("id"))
                .userId(rs.getString("userId"))
                .duration(rs.getLong("duration"))
                .build()
        );
    }

    public void saveRefinedAll(java.util.List<ApiLogRefined> logs) {
        String sql = """
            INSERT INTO default.api_logs_refined (id, originalLogId, userType, durationColor)
            VALUES (?, ?, ?, ?)
        """;

        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                ApiLogRefined log = logs.get(i);
                ps.setString(1, log.getId());
                ps.setString(2, log.getOriginalLogId());
                ps.setString(3, log.getUserType());
                ps.setString(4, log.getDurationColor());
            }

            @Override
            public int getBatchSize() {
                return logs.size();
            }
        });
    }

    public void runAggregationBenchmark() {
        String sql = """
            SELECT 
                httpMethod, 
                count(*) as count, 
                avg(duration) as avgDuration 
            FROM default.api_logs 
            GROUP BY httpMethod
        """;

        jdbcTemplate.query(sql, (rs) -> {
            while(rs.next()) {
                rs.getString("httpMethod");
                rs.getLong("count");
                rs.getDouble("avgDuration");
            }
            return null;
        });
    }

}