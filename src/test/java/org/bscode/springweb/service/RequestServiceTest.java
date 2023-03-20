package org.bscode.springweb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.bscode.springweb.config.SpringWebProperties;
import org.bscode.springweb.model.Request;
import org.bscode.springweb.model.RequestResult;
import org.junit.jupiter.api.BeforeEach;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestServiceTest {
    private MockWebServer mockWebServer;
    private RequestService service;
    SpringWebProperties properties = new SpringWebProperties();
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setupMockWebServer() {
        mockWebServer = new MockWebServer();

        properties = new SpringWebProperties();
        properties.setBaseUrl(mockWebServer.url("/").url().toString());

        service = new RequestService(properties);
    }

    @Test
    void noCallbackMakesCorrectRequest() throws InterruptedException, JsonProcessingException {
        final var ret = new RequestResult("OK");
        final var workRequest = new Request(null);

        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(ret))
        );

        final var requestResult = service.scheduleWork(workRequest).block();

        RecordedRequest webRequest = mockWebServer.takeRequest();

        assertEquals(requestResult, ret);
        assertThat(webRequest.getMethod()).isEqualTo("POST");
        assertThat(webRequest.getPath()).isEqualTo("/worker/");
    }

    // If the worker client returns a status code 500, so should we
    @Test
    void noCallbackHandles500() throws InterruptedException, JsonProcessingException {
        final var ret = mapper.writeValueAsString(new RequestResult("ERROR"));
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(500)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(ret)
        );

        assertThatThrownBy( () -> service.scheduleWork(new Request(null)).block())
                .isInstanceOf(WebClientResponseException.class);

        RecordedRequest webRequest = mockWebServer.takeRequest();

        assertThat(webRequest.getMethod()).isEqualTo("POST");
        assertThat(webRequest.getPath()).isEqualTo("/worker/");
    }

    // If the worker client returns a status code 4xx, we return "error"
    @Test
    void noCallbackHandles4xx() throws InterruptedException, JsonProcessingException {
        final var ret = mapper.writeValueAsString(new RequestResult("ERROR"));
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(HttpStatus.FORBIDDEN.value())
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(ret)
        );

        final var result = service.scheduleWork(new Request(null)).block();
        RecordedRequest webRequest = mockWebServer.takeRequest();

        assertThat(webRequest.getMethod()).isEqualTo("POST");
        assertThat(webRequest.getPath()).isEqualTo("/worker/");
        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
    }
    @Test
    void callbackMakesCorrectRequest() throws InterruptedException, JsonProcessingException {
        final var initialExpected = new RequestResult("In process");
        final var workerRequest = new Request(properties.getBaseUrl() + "callback");
        final var workerResultSent = new RequestResult("OK");
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        final var initialRet = service.scheduleWork(workerRequest).block();
        assertEquals(initialExpected, initialRet);

        RecordedRequest recordedWorkerRequest = mockWebServer.takeRequest();
        assertThat(recordedWorkerRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedWorkerRequest.getPath()).isEqualTo("/worker/");
        assertEquals(mapper.writeValueAsString(workerRequest), recordedWorkerRequest.getBody().readUtf8());

        RecordedRequest callbackResult = mockWebServer.takeRequest();
        assertThat(callbackResult.getMethod()).isEqualTo("POST");
        assertThat(callbackResult.getPath()).isEqualTo("/callback");
        assertEquals(mapper.writeValueAsString(workerResultSent), callbackResult.getBody().readUtf8());
    }

    // If the worker client returns a status code 500, so should we
    @Test
    void callbackHandles500FromWorker() throws InterruptedException, JsonProcessingException {
        final var initialExpected = new RequestResult("In process");
        final var workerRequest = new Request(properties.getBaseUrl() + "callback");
        final var workerResultSent = new RequestResult("ERROR");
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(500)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        final var initialRet = service.scheduleWork(workerRequest).block();
        assertEquals(initialExpected, initialRet);

        RecordedRequest recordedWorkerRequest = mockWebServer.takeRequest();
        assertThat(recordedWorkerRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedWorkerRequest.getPath()).isEqualTo("/worker/");
        assertEquals(mapper.writeValueAsString(workerRequest), recordedWorkerRequest.getBody().readUtf8());

        RecordedRequest callbackResult = mockWebServer.takeRequest();
        assertThat(callbackResult.getMethod()).isEqualTo("POST");
        assertThat(callbackResult.getPath()).isEqualTo("/callback");
        assertEquals(mapper.writeValueAsString(workerResultSent), callbackResult.getBody().readUtf8());
    }

    // If the worker client returns a status code 4xx, we return "error"
    @Test
    void callbackHandles4xxFromWorker() throws InterruptedException, JsonProcessingException {
        final var initialExpected = new RequestResult("In process");
        final var workerRequest = new Request(properties.getBaseUrl() + "callback");
        final var workerResultSent = new RequestResult("ERROR");
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(401)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        final var initialRet = service.scheduleWork(workerRequest).block();
        assertEquals(initialExpected, initialRet);

        RecordedRequest recordedWorkerRequest = mockWebServer.takeRequest();
        assertThat(recordedWorkerRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedWorkerRequest.getPath()).isEqualTo("/worker/");
        assertEquals(mapper.writeValueAsString(workerRequest), recordedWorkerRequest.getBody().readUtf8());

        RecordedRequest callbackResult = mockWebServer.takeRequest();
        assertThat(callbackResult.getMethod()).isEqualTo("POST");
        assertThat(callbackResult.getPath()).isEqualTo("/callback");
        assertEquals(mapper.writeValueAsString(workerResultSent), callbackResult.getBody().readUtf8());
    }

    @Test
    void callbackHandles4xxFromCallback() throws InterruptedException, JsonProcessingException {
        final var initialExpected = new RequestResult("In process");
        final var workerRequest = new Request(properties.getBaseUrl() + "callback");
        final var workerResultSent = new RequestResult("OK");
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        mockWebServer.enqueue(
                new MockResponse().setResponseCode(500)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(mapper.writeValueAsString(workerResultSent))
        );

        final var initialRet = service.scheduleWork(workerRequest).block();
        assertEquals(initialExpected, initialRet);

        RecordedRequest recordedWorkerRequest = mockWebServer.takeRequest();
        assertThat(recordedWorkerRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedWorkerRequest.getPath()).isEqualTo("/worker/");
        assertEquals(mapper.writeValueAsString(workerRequest), recordedWorkerRequest.getBody().readUtf8());

        RecordedRequest callbackResult = mockWebServer.takeRequest();
        assertThat(callbackResult.getMethod()).isEqualTo("POST");
        assertThat(callbackResult.getPath()).isEqualTo("/callback");
        assertEquals(mapper.writeValueAsString(workerResultSent), callbackResult.getBody().readUtf8());
    }
}