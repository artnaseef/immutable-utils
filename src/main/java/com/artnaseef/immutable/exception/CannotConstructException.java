package com.artnaseef.immutable.exception;

public class CannotConstructException extends RuntimeException {
    public CannotConstructException(String s) {
        super(s);
    }

    public CannotConstructException(String s, Throwable cause) {
        super(s, cause);
    }
}
