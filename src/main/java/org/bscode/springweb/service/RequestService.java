package org.bscode.springweb.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.bscode.springweb.config.SpringWebProperties;
import org.bscode.springweb.model.Request;
import org.bscode.springweb.model.RequestResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RequestService {
    private final WebClient client;
    private final SpringWebProperties props;
    public RequestService(SpringWebProperties props) {
        this.props = props;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)));
        client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    public Mono<RequestResult> scheduleWork(Request req) {
        if (req.getCallback() == null) {
            return callWorkerAsynchronously(req);
        } else {
            return callWorkerAndCallback(req);
        }
    }

    private Mono<RequestResult> callWorkerAndCallback(Request req) {
        final Mono<RequestResult> workerMono = callWorkerAsynchronously(req);
        workerMono.onErrorReturn(new RequestResult("ERROR")).flatMap(result -> callCallbackAsynchronously(req.getCallback(), result))
                .subscribeOn(Schedulers.single()).subscribe(str -> log.info("Callback returned {}", str));
        return Mono.just(new RequestResult("In process"));
    }

    private Mono<String> callCallbackAsynchronously(String callback, RequestResult result) {
        RequestBodySpec bodySpec = client.post().uri(callback);
        RequestHeadersSpec<?> headersSpec = bodySpec.body(Mono.just(result), RequestResult.class);
        return headersSpec.header(
                        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToMono(String.class);
                    } else if (response.statusCode().is4xxClientError()) {
                        log.error("Status code for callback: {}", response.statusCode());
                        return Mono.just("ERROR");
                    } else {
                        return response.createException().flatMap(Mono::error);
                    }
                });
    }


    private Mono<RequestResult> callWorkerAsynchronously(Request req) {
        RequestBodySpec bodySpec = client.post().uri(props.getBaseUrl() + "/worker/");
        RequestHeadersSpec<?> headersSpec = bodySpec.body(Mono.just(req), Request.class);
        return headersSpec.header(
                        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToMono(RequestResult.class);
                    } else if (response.statusCode().is4xxClientError()) {
                        log.error("Status code for callback: {}", response.statusCode());
                        return Mono.just(new RequestResult("ERROR"));
                    } else {
                        return response.createException().flatMap(Mono::error);
                    }
                });
    }
}
