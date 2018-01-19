package com.chopsticks.exception;

import lombok.Data;

@Data
public class ChopsticksException extends RuntimeException {
	protected int    status;
    protected String name;

    public ChopsticksException(int status, String name) {
        this.status = status;
        this.name = name;
    }

    public ChopsticksException(int status, String name, String message) {
        super(message);
        this.status = status;
        this.name = name;
    }

}
