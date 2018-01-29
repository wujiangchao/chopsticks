package com.chopsticks.exception;

/**
 * 403 forbidden exception
 *
 * @date 2017/9/18
 */
public class ForbiddenException extends ChopsticksException {

    private static final int STATUS = 403;
    private static final String NAME = "Forbidden";

    public ForbiddenException() {
        super(STATUS, NAME);
    }

    public ForbiddenException(String message){
        super(STATUS, NAME, message);
    }

}
