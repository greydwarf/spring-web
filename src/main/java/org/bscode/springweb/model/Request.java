package org.bscode.springweb.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.io.Serializable;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request implements Serializable {
    @JsonCreator
    public Request(
            @JsonProperty("callback") String callback){
        this.callback = callback;
    }
    @JsonProperty
    private String callback;
}
