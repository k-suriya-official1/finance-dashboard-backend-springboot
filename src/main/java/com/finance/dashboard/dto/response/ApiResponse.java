package com.finance.dashboard.dto.response;

import java.time.LocalDateTime;


public class ApiResponse<T> {

    private LocalDateTime timestamp;
    private boolean success;
    private String message;
    private T data;

    
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(LocalDateTime timestamp, boolean success, String message, T data) {
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
        this.success = success;
        this.message = message;
        this.data = data;
    }

    

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }



    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(LocalDateTime.now(), true, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Operation successful", data);
    }
}