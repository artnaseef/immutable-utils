package com.artnaseef.immutable.exception;

public class CannotMutateException extends RuntimeException {
    public CannotMutateException(String s, Throwable cause) {
        super(s, cause);
    }
}
