package org.bscode.springweb.controller;


import lombok.AllArgsConstructor;
import org.bscode.springweb.model.Request;
import org.bscode.springweb.model.RequestResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/echo")
public class EchoController {
    @PostMapping("/")
    public Mono<String> handleEcho(@RequestBody RequestResult req) {
        return Mono.just("[BS] Request complete: " + req.getStatus() + "\n");
    }
}
