package org.bscode.springweb.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class RequestResult {
    private final String status;
    @JsonCreator
    public RequestResult(
            @JsonProperty("status") String status){
        this.status = status;
    }
}
