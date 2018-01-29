package com.chopsticks.exception;

/**
 * 404 not found exception
 *
 * @date 2017/9/18
 */
public class NotFoundException extends ChopsticksException {

    public static final int    STATUS = 404;
    private static final String NAME   = "Not Found";

    public NotFoundException(String message) {
        super(STATUS, NAME, message);
    }

}
