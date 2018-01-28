package com.chopsticks.ioc.annotation;


import java.lang.annotation.*;

/**
 * Bean annotations can be injected
 *
 * @since 1.5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

    String value() default "";

}