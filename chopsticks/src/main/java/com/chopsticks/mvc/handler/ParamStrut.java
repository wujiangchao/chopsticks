package com.chopsticks.mvc.handler;

import com.chopsticks.mvc.annotation.BodyParam;
import com.chopsticks.mvc.annotation.CookieParam;
import com.chopsticks.mvc.annotation.HeaderParam;
import com.chopsticks.mvc.annotation.MultipartParam;
import com.chopsticks.mvc.annotation.Param;
import com.chopsticks.mvc.annotation.PathParam;
import com.chopsticks.mvc.http.Request;

import lombok.Builder;

@Builder
public class ParamStrut {
	Param          param;
    PathParam      pathParam;
    BodyParam      bodyParam;
    HeaderParam    headerParam;
    CookieParam    cookieParam;
    MultipartParam multipartParam;
    Class<?>       argType;
    String         paramName;
    Request request;
}
