/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.repository")
public class LoggingTemplate {

    @Getter
    @Delegate
    @NonNull
    private final JdbcTemplate jdbcTemplate;

    private static final AtomicLong traceReqId = new AtomicLong();

    @SneakyThrows
    private void run(String sql, Runnable op) {
        run(sql, () -> {
            op.run();
            return null;
        });
    }

    @SneakyThrows
    private <T> T run(Callable<T> op, String... sql) {
        return run(Stream.of(sql).collect(Collectors.joining(";\n")), op);
    }

    @SneakyThrows
    private <T> T run(String sql, Callable<T> op) {
        final long reqId = traceReqId.incrementAndGet();
        logBefore(reqId, sql);

        Duration duration = Duration.ZERO;
        DataAccessException error = null;
        try {
            final long pre = System.nanoTime();
            try {
                return op.call();
            } finally {
                long post = System.nanoTime();
                duration = Duration.ofNanos(post - pre);
            }
        } catch (DataAccessException e) {
            error = e;
            throw e;
        } finally {
            logAfter(reqId, sql, duration, error);
        }
    }

    private void logBefore(long reqId, String sql) {
        if (!log.isDebugEnabled()) return;

        if (sql.endsWith("\n")) sql = sql.substring(0, sql.length() - 1);

        if (log.isTraceEnabled()) {
            String trace = stackTrace();
            log.trace("before request #{}: '{}'\n{}", reqId, sql, trace);
        } else {
            log.debug("before request #{}: '{}'", reqId, sql);
        }
    }

    private void logAfter(long reqId, String sql, Duration elapsed, DataAccessException error) {
        if (!log.isDebugEnabled()) return;

        if (sql.endsWith("\n")) sql = sql.substring(0, sql.length() - 1);

        final String time = elapsed == null ? "" : "%.2f ms".formatted(elapsed.toNanos() / 1_000_000d);

        final String errMsg = error == null
                ? ""
                : " (ERROR %s: %s)".formatted(error.getClass().getSimpleName(), error.getMessage());
        if (error != null && log.isTraceEnabled() && !(error instanceof EmptyResultDataAccessException)) {
            log.trace("after request #{} ({}): '{}'{}", reqId, time, sql, errMsg, error);
        } else {
            log.debug("after request #{} ({}): '{}'{}", reqId, time, sql, errMsg);
        }
    }

    private String stackTrace() {
        String[] callers = Stream.of(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::toString)
                .filter(s -> s.startsWith("org.geoserver.")
                        && !s.startsWith("org.geoserver.filters.")
                        && !s.startsWith("org.geoserver.security.filter."))
                .filter(s -> !(s.contains("LoggingTemplate.log")
                        || s.contains("LoggingTemplate.run")
                        || s.contains("LoggingTemplate.stackTrace")))
                .toArray(String[]::new);
        return IntStream.range(0, callers.length)
                .mapToObj(i -> "  ".repeat(i) + callers[i])
                .collect(Collectors.joining("\n"));
    }

    public void execute(final String sql) throws DataAccessException {
        run(sql, () -> jdbcTemplate.execute(sql));
    }

    @Nullable
    public <T> T query(final String sql, final ResultSetExtractor<T> rse) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, rse));
    }

    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        run(sql, () -> jdbcTemplate.query(sql, rch));
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, rowMapper));
    }

    public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForStream(sql, rowMapper));
    }

    public Map<String, Object> queryForMap(String sql) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForMap(sql));
    }

    @Nullable
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForObject(sql, rowMapper));
    }

    @Nullable
    public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForObject(sql, requiredType));
    }

    public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForList(sql, elementType));
    }

    public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForList(sql));
    }

    public int update(final String sql) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.update(sql));
    }

    public int[] batchUpdate(final String... sql) throws DataAccessException {
        return run(() -> jdbcTemplate.batchUpdate(sql), sql);
    }

    @Nullable
    public <T> T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, pss, rse));
    }

    @Nullable
    public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, args, argTypes, rse));
    }

    @Nullable
    public <T> T query(String sql, ResultSetExtractor<T> rse, @Nullable Object... args) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, rse, args));
    }

    public void query(String sql, @Nullable PreparedStatementSetter pss, RowCallbackHandler rch)
            throws DataAccessException {
        run(sql, () -> jdbcTemplate.query(sql, pss, rch));
    }

    public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
        run(sql, () -> jdbcTemplate.query(sql, args, argTypes, rch));
    }

    public void query(String sql, RowCallbackHandler rch, @Nullable Object... args) throws DataAccessException {
        run(sql, () -> jdbcTemplate.query(sql, rch, args));
    }

    public <T> List<T> query(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, pss, rowMapper));
    }

    public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, args, argTypes, rowMapper));
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.query(sql, rowMapper, args));
    }

    public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object... args)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForStream(sql, rowMapper, args));
    }

    @Nullable
    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForObject(sql, args, argTypes, rowMapper));
    }

    @Nullable
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object... args)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForObject(sql, rowMapper, args));
    }

    @Nullable
    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForObject(sql, args, argTypes, requiredType));
    }

    public <T> T queryForObject(String sql, Class<T> requiredType, @Nullable Object... args)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForObject(sql, requiredType, args));
    }

    public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForMap(sql, args, argTypes));
    }

    public Map<String, Object> queryForMap(String sql, @Nullable Object... args) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForMap(sql, args));
    }

    public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForList(sql, args, argTypes, elementType));
    }

    public <T> List<T> queryForList(String sql, Class<T> elementType, @Nullable Object... args)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForList(sql, elementType, args));
    }

    public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForList(sql, args, argTypes));
    }

    public List<Map<String, Object>> queryForList(String sql, @Nullable Object... args) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.queryForList(sql, args));
    }

    public int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.update(sql, pss));
    }

    public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.update(sql, args, argTypes));
    }

    public int update(String sql, @Nullable Object... args) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.update(sql, args));
    }

    public int[] batchUpdate(String sql, final BatchPreparedStatementSetter pss) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.batchUpdate(sql, pss));
    }

    public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.batchUpdate(sql, batchArgs));
    }

    public int[] batchUpdate(String sql, List<Object[]> batchArgs, final int[] argTypes) throws DataAccessException {
        return run(sql, () -> jdbcTemplate.batchUpdate(sql, batchArgs, argTypes));
    }

    public <T> int[][] batchUpdate(
            String sql,
            final Collection<T> batchArgs,
            final int batchSize,
            final ParameterizedPreparedStatementSetter<T> pss)
            throws DataAccessException {
        return run(sql, () -> jdbcTemplate.batchUpdate(sql, batchArgs, batchSize, pss));
    }
}
