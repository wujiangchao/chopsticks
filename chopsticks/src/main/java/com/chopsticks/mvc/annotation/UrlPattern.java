package com.chopsticks.mvc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.chopsticks.ioc.annotation.Bean;

/**
 * WebHook url pattern
 *
 * @since 2.0.6-Alpha1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Bean
public @interface UrlPattern {

    /**
     * @return URL patterns
     */
    String[] values() default {"/*"};

}