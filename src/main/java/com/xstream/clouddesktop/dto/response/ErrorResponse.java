package com.xstream.clouddesktop.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success;
    private String error;
    private String message;
    private Map<String, String> details;
    private String path;
    private String timestamp;

    public static ErrorResponse of(String error, String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()))
                .build();
    }

    public static ErrorResponse validationError(String message, Map<String, String> fieldErrors, String path) {
        return ErrorResponse.builder()
                .success(false)
                .error("VALIDATION_ERROR")
                .message(message)
                .details(fieldErrors)
                .path(path)
                .timestamp(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()))
                .build();
    }
}
