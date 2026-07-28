package ru.practicum;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatRequestParamDto;
import ru.practicum.ewm.StatResponseDto;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatClientTest {

    private MockWebServer mockWebServer;
    private StatClient statClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final String baseUrl = mockWebServer.url("").toString();


        UriProvider testUriProvider = path -> URI.create(baseUrl + path);

        RestClient restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        statClient = new StatClientImpl(restClient, testUriProvider);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testPostHitSuccess() throws InterruptedException {
        String responseBody = "{\n" +
                "    \"id\": 1,\n" +
                "    \"app\": \"test-app\",\n" +
                "    \"uri\": \"/test\",\n" +
                "    \"ip\": \"127.0.0.1\",\n" +
                "    \"timestamp\": \"2024-01-01 12:00:00\"\n" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        HitDto dto = new HitDto();
        dto.setApp("test-app");
        dto.setUri("/test");
        dto.setIp("127.0.0.1");
        dto.setTimestamp("2024-01-01 12:00:00");

        HitDto result = statClient.postHit(dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test-app", result.getApp());

        var request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/hit", request.getPath());
    }

    @Test
    void testGetStatsSuccess() throws InterruptedException {
        String responseBody = "[\n" +
                "    {\"app\":\"app1\",\"uri\":\"/test1\",\"hits\":5},\n" +
                "    {\"app\":\"app1\",\"uri\":\"/test2\",\"hits\":3}\n" +
                "]";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StatRequestParamDto params = new StatRequestParamDto();
        params.setStart("2024-01-01 00:00:00");
        params.setEnd("2024-01-01 23:59:59");
        params.setUris(Arrays.asList("/test1", "/test2"));
        params.setUnique(false);

        List<StatResponseDto> results = statClient.getStats(params);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("app1", results.get(0).getApp());
        assertEquals(5L, results.get(0).getHits());

        var request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertTrue(request.getPath().startsWith("/stats"));
    }

    @Test
    void testPostHitError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        HitDto dto = new HitDto();
        dto.setApp("test-app");
        dto.setUri("/test");
        dto.setIp("127.0.0.1");
        dto.setTimestamp("2024-01-01 12:00:00");

        HitDto result = statClient.postHit(dto);

        assertNotNull(result);
        assertNull(result.getId());
    }
}