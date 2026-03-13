/*
 * Copyright 2024 Cloud Skill Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudskill.sdk.agent;

import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;
import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 动态技能Advisor
 * 拦截ChatModel的调用，自动注入动态技能
 */
public class DynamicSkillsAdvisor extends AbstractPointcutAdvisor {
    
    private final DynamicSkillsAdvice advice;
    private final Pointcut pointcut;
    
    /**
     * 全局模式构造函数，拦截所有ChatModel的call方法
     */
    public DynamicSkillsAdvisor(DynamicSkillsAdvice advice) {
        this.advice = advice;
        this.pointcut = buildGlobalPointcut();
    }
    
    /**
     * 注解模式构造函数，仅拦截标注了@EnableDynamicSkills的类/方法
     */
    public DynamicSkillsAdvisor(DynamicSkillsAdvice advice, boolean annotationMode) {
        this.advice = advice;
        this.pointcut = annotationMode ? buildAnnotationPointcut() : buildGlobalPointcut();
    }
    
    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
    
    @Override
    public Advice getAdvice() {
        return advice;
    }
    
    /**
     * 构建全局切点：拦截所有ChatModel的call方法
     */
    private Pointcut buildGlobalPointcut() {
        return new Pointcut() {
            @Override
            public ClassFilter getClassFilter() {
                // 匹配所有实现了ChatModel接口的类，排除AOP代理类
                return clazz -> ChatModel.class.isAssignableFrom(clazz)
                        && !org.springframework.aop.framework.AopProxy.class.isAssignableFrom(clazz);
            }
            
            @Override
            public MethodMatcher getMethodMatcher() {
                return new MethodMatcher() {
                    @Override
                    public boolean matches(java.lang.reflect.Method method, Class<?> targetClass) {
                        // 匹配所有名为call且参数包含Prompt的方法
                        return "call".equals(method.getName())
                                && java.util.Arrays.stream(method.getParameterTypes())
                                .anyMatch(type -> org.springframework.ai.chat.prompt.Prompt.class.isAssignableFrom(type));
                    }
                    
                    @Override
                    public boolean matches(java.lang.reflect.Method method, Class<?> targetClass, Object... args) {
                        return true;
                    }
                    
                    @Override
                    public boolean isRuntime() {
                        return false;
                    }
                };
            }
        };
    }
    
    /**
     * 构建注解切点：拦截标注了@EnableDynamicSkills的类或方法
     * 如果类上有注解，该类所有方法都匹配；如果方法上有注解，单个方法匹配
     */
    private Pointcut buildAnnotationPointcut() {
        // 类级别的注解匹配：类上有@EnableDynamicSkills注解
        ClassFilter classFilter = new AnnotationClassFilter(EnableDynamicSkills.class, true);
        
        // 方法级别的注解匹配：方法上有@EnableDynamicSkills注解
        MethodMatcher methodMatcher = new AnnotationMethodMatcher(EnableDynamicSkills.class, true);
        
        // 组合切点：
        // 1. 类上有注解的所有方法
        // 2. 方法上有注解的单个方法
        
        // 匹配所有方法的MethodMatcher
        MethodMatcher allMethodsMatcher = new MethodMatcher() {
            @Override
            public boolean matches(java.lang.reflect.Method method, Class<?> targetClass) {
                return true;
            }
            
            @Override
            public boolean matches(java.lang.reflect.Method method, Class<?> targetClass, Object... args) {
                return true;
            }
            
            @Override
            public boolean isRuntime() {
                return false;
            }
        };
        
        // 匹配所有类的ClassFilter
        ClassFilter allClassesFilter = clazz -> true;
        
        return new ComposablePointcut(classFilter, allMethodsMatcher)
                .union(new ComposablePointcut(allClassesFilter, methodMatcher));
    }
}
