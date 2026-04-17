package com.monew.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 1. 이 어노테이션을 어디에 붙일 것인가? (PARAMETER = 메서드의 파라미터)
@Target(ElementType.PARAMETER)

// 2. 이 어노테이션 정보가 언제까지 살아있을 것인가? (RUNTIME = 실행 중에도 참조 가능)
@Retention(RetentionPolicy.RUNTIME)

public @interface LoginUser {
}