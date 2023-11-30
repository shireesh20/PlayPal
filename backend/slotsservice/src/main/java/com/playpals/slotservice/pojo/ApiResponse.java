package com.playpals.slotservice.pojo;


import lombok.Data;

//This class models the structure of the response returned by each endpoint
@Data
public class ApiResponse {
    private Boolean result;
    private Integer statusCode ;
    private String statusCodeDescription;

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusCodeDescription() {
        return statusCodeDescription;
    }

    public void setStatusCodeDescription(String statusCodeDescription) {
        this.statusCodeDescription = statusCodeDescription;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    private String message;
    private Object response;

}
