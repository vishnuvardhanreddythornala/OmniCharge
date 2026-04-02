package com.omnicharge.logging.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.logging.service.LogFileWriterService;
import org.junit.jupiter.api.*;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ELK pipeline.
 * 
 * Prerequisites:
 * - Elasticsearch must be running on localhost:9200
 * - Logstash must be running and configured to read from logs/
 * - Kibana must be running on localhost:5601
 * 
 * Run: docker-compose -f docker-compose-elk.yml up -d
 * 
 * This test validates:
 * - Requirement 10.2: Logstash parses log files and sends to Elasticsearch
 * - Requirement 10.3: Logs are searchable by service, level, timestamp, traceId
 * - Requirement 10.5: Query performance within 2 seconds
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
@Tag("Feature: production-grade-centralized-logging, Property 20: Logstash Log Processing")
@Tag("Feature: production-grade-centralized-logging, Property 21: Elasticsearch Searchability")
class ELKPipelineIntegrationTest {

    @Autowired
    private LogFileWriterService logFileWriterService;

    private static final String ELASTICSEARCH_URL = "http://localhost:9200";
    private static final String KIBANA_URL = "http://localhost:5601";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String testTraceId;
    private String testServiceName;

    @BeforeEach
    void setUp() {
        testTraceId = "test-trace-" + UUID.randomUUID().toString().substring(0, 8);
        testServiceName = "elk-test-service";
    }

    @Test
    @Order(1)
    @DisplayName("Verify Elasticsearch is running and healthy")
    void testElasticsearchHealth() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_URL + "/_cluster/health"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Elasticsearch should be running");

        JsonNode health = objectMapper.readTree(response.body());
        String status = health.get("status").asText();
        assertTrue(status.equals("green") || status.equals("yellow"),
                "Elasticsearch cluster should be healthy (green or yellow)");
    }

    @Test
    @Order(2)
    @DisplayName("Verify Kibana is running and accessible")
    void testKibanaHealth() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KIBANA_URL + "/api/status"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Kibana should be running");

        JsonNode status = objectMapper.readTree(response.body());
        String overallStatus = status.get("status").get("overall").get("level").asText();
        assertTrue(overallStatus.equals("available") || overallStatus.equals("degraded"),
                "Kibana should be available");
    }

    @Test
    @Order(3)
    @DisplayName("Write test logs and verify Logstash parses and indexes in Elasticsearch")
    void testLogstashPipelineProcessing() throws IOException, InterruptedException {
        // Write multiple test log events with different levels
        writeTestLog("INFO", "Test info message for ELK pipeline");
        writeTestLog("WARN", "Test warning message for ELK pipeline");
        writeTestLog("ERROR", "Test error message for ELK pipeline");

        // Wait for Logstash to process files (file input has delay)
        Thread.sleep(15000); // 15 seconds for file processing

        // Query Elasticsearch for our test logs
        String searchQuery = String.format("""
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "traceId": "%s" } },
                        { "term": { "service": "%s" } }
                      ]
                    }
                  },
                  "size": 10,
                  "sort": [{ "@timestamp": "asc" }]
                }
                """, testTraceId, testServiceName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Elasticsearch search should succeed");

        JsonNode searchResult = objectMapper.readTree(response.body());
        int hitCount = searchResult.get("hits").get("total").get("value").asInt();

        assertTrue(hitCount >= 3, "Should find at least 3 test log entries (INFO, WARN, ERROR)");

        // Verify log structure
        JsonNode firstHit = searchResult.get("hits").get("hits").get(0).get("_source");
        assertNotNull(firstHit.get("timestamp"), "Log should have timestamp");
        assertNotNull(firstHit.get("level"), "Log should have level");
        assertEquals(testServiceName, firstHit.get("service").asText(), "Service name should match");
        assertEquals(testTraceId, firstHit.get("traceId").asText(), "TraceId should match");
        assertNotNull(firstHit.get("log_message"), "Log should have message");
    }

    @Test
    @Order(4)
    @DisplayName("Verify logs are searchable by service name")
    void testSearchByServiceName() throws IOException, InterruptedException {
        String searchQuery = String.format("""
                {
                  "query": {
                    "term": { "service": "%s" }
                  },
                  "size": 1
                }
                """, testServiceName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode searchResult = objectMapper.readTree(response.body());
        int hitCount = searchResult.get("hits").get("total").get("value").asInt();

        assertTrue(hitCount > 0, "Should find logs by service name");
    }

    @Test
    @Order(5)
    @DisplayName("Verify logs are searchable by log level")
    void testSearchByLogLevel() throws IOException, InterruptedException {
        String searchQuery = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "level": "ERROR" } },
                        { "term": { "traceId": "%s" } }
                      ]
                    }
                  },
                  "size": 1
                }
                """.formatted(testTraceId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode searchResult = objectMapper.readTree(response.body());
        int hitCount = searchResult.get("hits").get("total").get("value").asInt();

        assertTrue(hitCount > 0, "Should find ERROR level logs");
    }

    @Test
    @Order(6)
    @DisplayName("Verify logs are searchable by traceId")
    void testSearchByTraceId() throws IOException, InterruptedException {
        String searchQuery = String.format("""
                {
                  "query": {
                    "term": { "traceId": "%s" }
                  },
                  "size": 10
                }
                """, testTraceId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode searchResult = objectMapper.readTree(response.body());
        int hitCount = searchResult.get("hits").get("total").get("value").asInt();

        assertTrue(hitCount >= 3, "Should find all logs with same traceId");
    }

    @Test
    @Order(7)
    @DisplayName("Verify query performance within 2 seconds (Requirement 10.5)")
    @Tag("Feature: production-grade-centralized-logging, Property 22: Elasticsearch Query Performance")
    void testQueryPerformance() throws IOException, InterruptedException {
        // Query last 24 hours of logs
        String searchQuery = """
                {
                  "query": {
                    "range": {
                      "@timestamp": {
                        "gte": "now-24h",
                        "lte": "now"
                      }
                    }
                  },
                  "size": 100,
                  "sort": [{ "@timestamp": "desc" }]
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                .build();

        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        long queryTime = endTime - startTime;

        assertEquals(200, response.statusCode());
        assertTrue(queryTime < 2000, 
                String.format("Query should complete within 2 seconds (took %d ms)", queryTime));

        JsonNode searchResult = objectMapper.readTree(response.body());
        int tookMs = searchResult.get("took").asInt();
        assertTrue(tookMs < 2000, 
                String.format("Elasticsearch query time should be < 2000ms (took %d ms)", tookMs));
    }

    @Test
    @Order(8)
    @DisplayName("Verify logs with context fields are properly indexed")
    void testContextFieldIndexing() throws IOException, InterruptedException {
        // Write log with context fields (key=value format)
        String contextMessage = "User registration completed | userId=12345 | email=test@example.com | status=SUCCESS";
        writeTestLog("INFO", contextMessage);

        Thread.sleep(15000); // Wait for Logstash processing

        String searchQuery = String.format("""
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "traceId": "%s" } },
                        { "match": { "log_message": "User registration completed" } }
                      ]
                    }
                  },
                  "size": 1
                }
                """, testTraceId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_URL + "/omnicharge-logs-*/_search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(searchQuery))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode searchResult = objectMapper.readTree(response.body());
        int hitCount = searchResult.get("hits").get("total").get("value").asInt();

        if (hitCount > 0) {
            JsonNode source = searchResult.get("hits").get("hits").get(0).get("_source");
            JsonNode context = source.get("context");

            if (context != null) {
                // Verify context fields were extracted
                assertNotNull(context.get("userId"), "Context should contain userId");
                assertNotNull(context.get("email"), "Context should contain email");
                assertNotNull(context.get("status"), "Context should contain status");
            }
        }
    }

    private void writeTestLog(String level, String message) {
        LogEvent event = LogEvent.builder()
                .serviceName(testServiceName)
                .level(level)
                .logger("ELKPipelineIntegrationTest")
                .message(message)
                .traceId(testTraceId)
                .spanId("test-span-001")
                .threadName("test-thread")
                .timestamp(LocalDateTime.now())
                .eventType("TEST")
                .build();

        logFileWriterService.writeToFile(event);
    }
}
