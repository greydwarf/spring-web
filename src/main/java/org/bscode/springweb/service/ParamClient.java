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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
class ParamClient {
    private final SpringWebProperties props;
    private final WebClient client;
    public ParamClient(SpringWebProperties props) {
        this.props = props;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                .responseTimeout(Duration.ofSeconds(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(500, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(500, TimeUnit.MILLISECONDS)));
        client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(props.getParamBaseUrl() + "/worker")
                .build();
    }
    public Mono<RequestResult> callWorker(Request req) {
        return client
                .post()
                .body(Mono.just(req), Request.class)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
