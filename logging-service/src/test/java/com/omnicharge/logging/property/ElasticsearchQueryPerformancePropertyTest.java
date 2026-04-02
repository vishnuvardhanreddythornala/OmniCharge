package com.omnicharge.logging.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.logging.service.LogFileWriterService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Elasticsearch query performance.
 * 
 * Property 22: Elasticsearch Query Performance
 * For any search query in Elasticsearch spanning the last 24 hours,
 * the system should return results within 2 seconds.
 * 
 * Validates: Requirement 10.5
 * 
 * Prerequisites:
 * - Elasticsearch must be running on localhost:9200
 * - Run: docker-compose -f docker-compose-elk.yml up -d
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("property-test")
@Tag("Feature: production-grade-centralized-logging, Property 22: Elasticsearch Query Performance")
class ElasticsearchQueryPerformancePropertyTest {

    @Autowired
    private LogFileWriterService logFileWriterService;

    private static final String ELASTICSEARCH_URL = "http://localhost:9200";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Property(tries = 100)
    @Tag("Feature: production-grade-centralized-logging, Property 22: Elasticsearch Query Performance")
    void queryPerformanceWithinTwoSeconds(
            @ForAll @StringLength(min = 5, max = 20) String serviceName,
            @ForAll("logLevels") String level,
            @ForAll @IntRange(min = 1, max = 100) int pageSize
    ) {
        Assume.that(isElasticsearchAvailable());

        // Build query for last 24 hours
        String searchQuery = String.format("""
                {
                  "query": {
                    "bool": {
                      "must": [
                        {
                          "range": {
                            "@timestamp": {
                              "gte": "now-24h",
                              "lte": "now"
                            }
                          }
                        }
                      ],
                      "filter": [
                        { "term": { "level": "%s" } }
                      ]
                    }
                  },
                  "size": %d,
                  "sort": [{ "@timestamp": "desc" }]
                }
                """, level, pageSize);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();

            long queryTime = endTime - startTime;

            // Property: Query should complete within 2 seconds
            assertTrue(queryTime < 2000,
                    String.format("Query took %d ms (should be < 2000ms) for level=%s, size=%d",
                            queryTime, level, pageSize));

            if (response.statusCode() == 200) {
                JsonNode searchResult = objectMapper.readTree(response.body());
                int tookMs = searchResult.get("took").asInt();

                // Property: Elasticsearch internal query time should also be < 2 seconds
                assertTrue(tookMs < 2000,
                        String.format("Elasticsearch query took %d ms (should be < 2000ms)", tookMs));
            }

        } catch (IOException | InterruptedException e) {
            fail("Query execution failed: " + e.getMessage());
        }
    }

    @Property(tries = 50)
    @Tag("Feature: production-grade-centralized-logging, Property 22: Elasticsearch Query Performance")
    void complexQueryPerformanceWithMultipleFilters(
            @ForAll @StringLength(min = 5, max = 20) String serviceName,
            @ForAll("logLevels") String level,
            @ForAll @StringLength(min = 10, max = 30) String traceId
    ) {
        Assume.that(isElasticsearchAvailable());

        // Complex query with multiple filters
        String searchQuery = String.format("""
                {
                  "query": {
                    "bool": {
                      "must": [
                        {
                          "range": {
                            "@timestamp": {
                              "gte": "now-24h",
                              "lte": "now"
                            }
                          }
                        }
                      ],
                      "filter": [
                        { "term": { "level": "%s" } },
                        { "wildcard": { "service": "*%s*" } }
                      ]
                    }
                  },
                  "size": 50,
                  "sort": [{ "@timestamp": "desc" }],
                  "aggs": {
                    "services": {
                      "terms": { "field": "service" }
                    },
                    "levels": {
                      "terms": { "field": "level" }
                    }
                  }
                }
                """, level, serviceName.substring(0, Math.min(5, serviceName.length())));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();

            long queryTime = endTime - startTime;

            // Property: Even complex queries with aggregations should complete within 2 seconds
            assertTrue(queryTime < 2000,
                    String.format("Complex query took %d ms (should be < 2000ms)", queryTime));

            if (response.statusCode() == 200) {
                JsonNode searchResult = objectMapper.readTree(response.body());
                int tookMs = searchResult.get("took").asInt();

                assertTrue(tookMs < 2000,
                        String.format("Elasticsearch complex query took %d ms (should be < 2000ms)", tookMs));
            }

        } catch (IOException | InterruptedException e) {
            fail("Complex query execution failed: " + e.getMessage());
        }
    }

    @Property(tries = 50)
    @Tag("Feature: production-grade-centralized-logging, Property 22: Elasticsearch Query Performance")
    void traceIdCorrelationQueryPerformance(
            @ForAll @StringLength(min = 10, max = 30) String traceId
    ) {
        Assume.that(isElasticsearchAvailable());

        // Query to find all logs with same traceId (common use case for debugging)
        String searchQuery = String.format("""
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "traceId": "%s" } }
                      ]
                    }
                  },
                  "size": 1000,
                  "sort": [{ "@timestamp": "asc" }]
                }
                """, traceId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();

            long queryTime = endTime - startTime;

            // Property: TraceId correlation queries should be fast (< 2 seconds)
            assertTrue(queryTime < 2000,
                    String.format("TraceId query took %d ms (should be < 2000ms)", queryTime));

            if (response.statusCode() == 200) {
                JsonNode searchResult = objectMapper.readTree(response.body());
                int tookMs = searchResult.get("took").asInt();

                assertTrue(tookMs < 2000,
                        String.format("Elasticsearch traceId query took %d ms (should be < 2000ms)", tookMs));
            }

        } catch (IOException | InterruptedException e) {
            fail("TraceId query execution failed: " + e.getMessage());
        }
    }

    @Provide
    Arbitrary<String> logLevels() {
        return Arbitraries.of("INFO", "WARN", "ERROR", "DEBUG", "TRACE");
    }

    private boolean isElasticsearchAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_URL + "/_cluster/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
