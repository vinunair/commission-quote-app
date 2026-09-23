package com.example.commissionquote.exception;

public class VendorUnavailableException extends RuntimeException {

    public VendorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
