package com.chopsticks.mvc.hook;

import java.lang.reflect.Method;

import com.chopsticks.mvc.http.Request;
import com.chopsticks.mvc.http.Response;
import com.chopsticks.mvc.route.Route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Signature {
	private static final String LAMBDA = "$$Lambda$";

    private Route    route;
    private Method   action;
    private Request  request;
    private Response response;
    private Object[] parameters;

    public Request request() {
        return request;
    }

    public Response response() {
        return response;
    }

    public boolean next() {
        return true;
    }

    public void setRoute(Route route) throws Exception {
        this.route = route;
        this.action = route.getAction();
        if (null != this.action &&
                !this.action.getDeclaringClass().getName().contains(LAMBDA)) {
            this.parameters = MethodArgument.getArgs(this);
        }
}
}
