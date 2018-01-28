package com.chopsticks.ioc.annotation;



import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.chopsticks.ioc.Injector;

/**
 * 凡是标注了 InjectFieldWith 的第三方 Annotation，就被允许进行自定义注入字段
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectWith {

    Class<? extends Injector> value();

}