package com.chopsticks.exception;

/**
 * 500 internal error exception
 *
 * @date 2017/9/18
 */
public class InternalErrorException extends ChopsticksException {

    public static final int STATUS = 500;
    private static final String NAME = "Internal Error";

    public InternalErrorException() {
        super(STATUS, NAME);
    }

    public InternalErrorException(String message) {
        super(STATUS, NAME, message);
    }

}
