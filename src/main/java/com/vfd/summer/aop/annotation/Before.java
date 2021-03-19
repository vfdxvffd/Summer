package com.vfd.summer.aop.annotation;

import java.lang.annotation.*;

/**
 * 在切面方法执行之前执行
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Before {
    String value();
}
