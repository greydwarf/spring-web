package org.bscode.springweb.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bscode.springweb.model.Request;
import org.bscode.springweb.model.RequestResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/worker")
public class WorkerController {
    @PostMapping()
    public Mono<RequestResult> handleWorkRequest(@RequestBody Request req) {
        log.debug("in HandleWorkRequest");
        return Mono
                .just(new RequestResult("OK"))
                .delayElement(Duration.ofSeconds(2));
    }
}
