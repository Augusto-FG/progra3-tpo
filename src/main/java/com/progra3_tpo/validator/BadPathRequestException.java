package com.progra3_tpo.validator;

import com.progra3_tpo.service.PathResponse;

public class BadPathRequestException extends RuntimeException {
    private final PathResponse response;

    public BadPathRequestException(PathResponse response) {
        super(response == null ? "Invalid PathRequest" : response.getMessage());
        this.response = response;
    }

    public PathResponse getResponse() {
        return response;
    }
}
