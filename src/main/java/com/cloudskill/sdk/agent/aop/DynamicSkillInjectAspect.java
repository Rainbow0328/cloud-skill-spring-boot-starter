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
 * distributed under an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudskill.sdk.agent.aop;

import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;
import com.cloudskill.sdk.agent.enhancement.AbstractDynamicSkillToolEnhancement;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * 将 ChatModel / ChatClient / Agent.Builder 拦截委托给 {@link AbstractDynamicSkillToolEnhancement}，具体合并与扩展点见该类。
 *
 * 对于 {@code mode=annotation}:
 * - 第一层切面拦截被 {@code @EnableDynamicSkills} 标注的方法/类，用 ThreadLocal 标记需要注入
 * - 第二层切面拦截 {@code ChatModel.call}，检查 ThreadLocal 标记决定是否注入
 */
@Aspect
public class DynamicSkillInjectAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(DynamicSkillInjectAspect.class);

    private final AbstractDynamicSkillToolEnhancement enhancement;

    /**
     * ThreadLocal 标记当前调用链是否需要注入动态技能
     * 配合 @EnableDynamicSkills 注解使用，精准控制
     */
    public static final ThreadLocal<Boolean> ENABLED_IN_CURRENT_CALL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * ThreadLocal 标记动态技能已经在 ChatClient.Builder 阶段注入过了
     * 后续 ChatModel.call 可以跳过重复注入
     */
    public static final ThreadLocal<Boolean> ALREADY_INJECTED_IN_BUILD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public DynamicSkillInjectAspect(AbstractDynamicSkillToolEnhancement enhancement) {
        this.enhancement = enhancement;
    }

    /**
     * 第一层：拦截 @EnableDynamicSkills 标注的方法/类，标记 ThreadLocal
     * 增加 execution(* *(..)) 确保能匹配到所有方法，再结合注解过滤
     */
    @Around("execution(* *(..)) && (@within(com.cloudskill.sdk.agent.annotation.EnableDynamicSkills) || @annotation(com.cloudskill.sdk.agent.annotation.EnableDynamicSkills))")
    public Object aroundAnnotatedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查注解值
        EnableDynamicSkills annotation = null;
        if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature) {
            org.aspectj.lang.reflect.MethodSignature ms = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
            annotation = ms.getMethod().getAnnotation(EnableDynamicSkills.class);
            if (annotation == null) {
                annotation = joinPoint.getTarget().getClass().getAnnotation(EnableDynamicSkills.class);
            }
        }
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(EnableDynamicSkills.class);
        }

        boolean enabled = annotation != null && annotation.value();
        Boolean old = ENABLED_IN_CURRENT_CALL.get();
        if (enabled) {
            log.debug("DynamicSkill: @EnableDynamicSkills found on {}.{}, marked ENABLED for current call",
                    joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        }
        try {
            ENABLED_IN_CURRENT_CALL.set(enabled);
            return joinPoint.proceed();
        } finally {
            // 用完严谨清理：旧值为null直接remove，避免内存泄漏
            if (old == null) {
                ENABLED_IN_CURRENT_CALL.remove();
            } else {
                ENABLED_IN_CURRENT_CALL.set(old);
            }
            if (enabled) {
                log.debug("DynamicSkill: {}.{} done, restored ThreadLocal",
                        joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
            }
        }
    }

    /**
     * 第二层：拦截 ChatModel.call(Prompt / String)，执行动态注入。
     * <p>
     * 业务代码常写 {@code chatModel.call("...")}，走的是 {@code call(String)}，
     * 若只切 {@code call(Prompt)} 则永远不会进入增强（表现为只有 @EnableDynamicSkills 日志、无 Intercepted ChatModel）。
     * 
     * 如果ChatClient已经注入过，这里直接跳过，并清理ThreadLocal标记
     */
    @Around("execution(* org.springframework.ai.chat.model.ChatModel.call(org.springframework.ai.chat.prompt.Prompt)) "
            + "|| execution(* org.springframework.ai.chat.model.ChatModel.call(String))")
    public Object aroundChatModelCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取标记：ChatClient是否已经注入
        Boolean alreadyInjected = ALREADY_INJECTED_IN_BUILD.get();
        try {
            if (alreadyInjected != null && alreadyInjected) {
                // ChatClient已经注入过，直接跳过，不需要重复处理
                log.debug("DynamicSkill: ChatModel.call() skipped (already injected in ChatClient.Builder)");
                return joinPoint.proceed();
            }

            // 需要执行增强，才打日志
            log.debug("DynamicSkill: Intercepted ChatModel.call() on {}", joinPoint.getTarget().getClass().getName());
            Object result = enhancement.enhanceChatModelCall(joinPoint);
            log.debug("DynamicSkill: ChatModel.call() completed");
            return result;
        } finally {
            // 无论是否跳过，调用完成后立刻remove标记，保证ThreadLocal干净
            // 符合：ChatClient(打标记) → ChatModel(用完清理) 的生命周期顺序
            ALREADY_INJECTED_IN_BUILD.remove();
        }
    }

    /**
     * 拦截 ChatClient.Builder.build()
     * 无论走Customizer还是AOP注入，只要ChatClient构建完成，就标记后续ChatModel不需要重复注入
     * 标记由后续ChatModel切面在执行完后清理
     */
    @Around("execution(* org.springframework.ai.chat.client.ChatClient$Builder.build())")
    public Object aroundChatClientBuild(ProceedingJoinPoint joinPoint) throws Throwable {

        Boolean oldSkip = ALREADY_INJECTED_IN_BUILD.get();
        try {
            ALREADY_INJECTED_IN_BUILD.set(true);
            if (enhancement.shouldInjectForChatClientBuild()) {
                log.debug("DynamicSkill: Intercepted ChatClient.Builder.build() on {}", joinPoint.getTarget().getClass().getName());
                Object result = enhancement.enhanceChatClientBuild(joinPoint);
                log.debug("DynamicSkill: ChatClient.Builder.build() completed");
                return result;
            } else {
                log.debug("DynamicSkill: ChatClient.Builder.build() skipped enhancement (already injected via ChatClientCustomizer), marked for ChatModel skip");
                return joinPoint.proceed();
            }
        } finally {
            if (oldSkip != null) {
                // 如果原来有值，恢复原来的（嵌套build的情况）
                ALREADY_INJECTED_IN_BUILD.set(oldSkip);
            }
        }
    }

    @Override
    public int getOrder() {
        return enhancement.getOrder() - 1;  // 我们要先执行，所以order更小
    }
}
