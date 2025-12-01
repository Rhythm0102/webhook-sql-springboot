package com.example.webhooksql.client;

public class SubmitSolutionRequest {

    private String finalQuery;

    public SubmitSolutionRequest() {
    }

    public SubmitSolutionRequest(String finalQuery) {
        this.finalQuery = finalQuery;
    }

    public String getFinalQuery() {
        return finalQuery;
    }

    public void setFinalQuery(String finalQuery) {
        this.finalQuery = finalQuery;
    }
}
