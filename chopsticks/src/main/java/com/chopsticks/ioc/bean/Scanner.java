package com.chopsticks.ioc.bean;

import lombok.Builder;
import lombok.Data;

import java.lang.annotation.Annotation;

/**
 * @date 2017/10/19
 */
@Data
@Builder
public class Scanner {

    private String                      packageName;
    private boolean                     recursive;
    private Class<?>                    parent;
    private Class<? extends Annotation> annotation;
}
