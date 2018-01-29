package com.chopsticks.exception;

/**
 * @date 2017/9/19
 */
public class BadRequestException extends ChopsticksException {

    private static final int    STATUS = 400;
    private static final String NAME   = "Bad Request";

    public BadRequestException() {
        super(STATUS, NAME);
    }

    public BadRequestException(String message) {
        super(STATUS, NAME, message);
    }

}