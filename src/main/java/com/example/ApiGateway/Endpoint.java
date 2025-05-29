package com.example.ApiGateway;

public class Endpoint {
    private String pathPattern;
    private String methodType;

    public Endpoint(String pathPattern, String methodType) {
        this.pathPattern = pathPattern;
        this.methodType = methodType;
    }

    public Endpoint() {}

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }
}
