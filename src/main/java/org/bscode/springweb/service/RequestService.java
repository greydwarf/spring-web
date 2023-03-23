package org.bscode.springweb.service;

import lombok.extern.slf4j.Slf4j;
import org.bscode.springweb.config.SpringWebProperties;
import org.bscode.springweb.model.Request;
import org.bscode.springweb.model.RequestResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class RequestService {
    ParamClient paramClient;
    CallbackClient callbackClient;
    ArchiveClient archiveClient;

    public RequestService(SpringWebProperties props) {
        paramClient = new ParamClient(props);
        callbackClient = new CallbackClient(props);
        archiveClient = new ArchiveClient(props);
    }

    public Mono<RequestResult> scheduleWork(Request req) {
        if (req.getCallback() == null) {
            return paramClient.callWorker(req);
        } else {
            return callWorkerAndCallback(req);
        }
    }

    private Mono<RequestResult> callWorkerAndCallback(Request req) {
        paramClient.callWorker(req)
                .onErrorReturn(new RequestResult("ERROR"))
                .flatMap(result -> archiveClient.callArchive(result))
                .flatMap(result -> callbackClient.callCallback(req.getCallback(), result))
                .subscribeOn(Schedulers.single())
                .subscribe(str -> log.info("Callback returned {}", str));
        return Mono.just(new RequestResult("In process"));
    }
}
