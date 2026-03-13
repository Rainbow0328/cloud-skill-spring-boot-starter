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
package com.cloudskill.sdk.agent.aop;

import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;
import com.cloudskill.sdk.agent.context.SkillContext;
import com.cloudskill.sdk.agent.context.SkillContextHolder;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import java.util.List;

/**
 * 动态技能AOP拦截器
 * 拦截标注@EnableDynamicSkills的方法，注入技能上下文
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DynamicSkillsAspect {
    
    private final CloudSkillClient cloudSkillClient;
    
    @Around("@annotation(enableDynamicSkills)")
    public Object around(ProceedingJoinPoint joinPoint, EnableDynamicSkills enableDynamicSkills) throws Throwable {
        log.debug("Intercepted method with @EnableDynamicSkills annotation");
        
        // 获取技能上下文
        SkillContext context = SkillContextHolder.getContext();
        context.setConfig(enableDynamicSkills);
        context.setEnabled(true);
        
        try {
            // 获取可用技能列表
            List<Skill> skills = cloudSkillClient.getAllSkills();
            if (CollectionUtils.isEmpty(skills)) {
                log.debug("No available skills found, proceed with normal chat");
                return joinPoint.proceed();
            }
            
            // 过滤排除的技能
            if (enableDynamicSkills.exclude() != null && enableDynamicSkills.exclude().length > 0) {
                skills = skills.stream()
                        .filter(skill -> !List.of(enableDynamicSkills.exclude()).contains(skill.getName()))
                        .toList();
            }
            
            context.setSkills(skills);
            log.debug("Loaded {} available skills into context", skills.size());
            
            // 执行原方法
            Object result = joinPoint.proceed();
            
            // 处理返回结果
            return processResult(result, context);
            
        } catch (Exception e) {
            log.error("Error in DynamicSkillsAspect", e);
            // 发生异常时降级，直接执行原方法
            return joinPoint.proceed();
        } finally {
            // 清除上下文
            SkillContextHolder.clear();
        }
    }
    
    /**
     * 处理方法返回结果
     */
    private Object processResult(Object result, SkillContext context) {
        // 流式响应暂时直接返回，后续版本支持
        if (result instanceof Flux) {
            log.debug("Stream response detected, dynamic skills for stream will be supported in future version");
            return result;
        }
        
        // 同步响应直接返回，由ChatModel代理处理工具调用
        return result;
    }
}
