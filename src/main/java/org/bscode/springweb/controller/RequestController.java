package org.bscode.springweb.controller;

import lombok.AllArgsConstructor;
import org.bscode.springweb.model.Request;
import org.bscode.springweb.model.RequestResult;
import org.bscode.springweb.service.RequestService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/request")
public class RequestController {
    private final RequestService service;
    @PostMapping("/")
    public Mono<RequestResult> handleRequest(@RequestBody Request req) {
        return service.scheduleWork(req);
    }
}
