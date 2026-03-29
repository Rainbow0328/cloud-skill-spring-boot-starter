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
package com.cloudskill.sdk.agent;

import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;
import com.cloudskill.sdk.agent.injection.ChatModelToolInjector;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.listener.RedisSkillChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * 动态技能自动配置
 * 模板方法架构：
 * - AbstractToolInjector - 抽象基类，统一处理工具注入
 * - ChatModelToolInjector - ChatModel 具体实现，AOP 拦截 ChatModel.call(Prompt)
 * - 后续可以扩展 ChatClientToolInjector, ReactAgentToolInjector 等
 */
@Configuration
@ConditionalOnClass(ChatModel.class)
public class DynamicSkillAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicSkillAutoConfiguration.class);
    
    public DynamicSkillAutoConfiguration() {
    }
    
    /**
     * 动态技能 AOP 拦截 - 仅拦截 ChatModel.call(Prompt) 方法自动注入技能
     * 默认开启，除非显式关闭
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloud.skill.dynamic-skills", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Advisor dynamicSkillsAdvisor(McpSkillManager mcpSkillManager,
            SkillCache skillCache,
            CloudSkillClient cloudSkillClient,
            RedisSkillChangeListener skillChangeListener,
            CloudSkillProperties properties) {
        ChatModelToolInjector injector = new ChatModelToolInjector(
                mcpSkillManager, skillCache, cloudSkillClient, skillChangeListener);
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(
                Pointcuts.intersection(
                        Pointcuts.intersection(
                                new ChatModelMethodMatcher(),
                                new PromptArgumentMatcher()
                        ),
                        new DynamicSkillsAnnotationMatcher()
                ),
                injector
        );
        advisor.setOrder(properties.getDynamicSkills() != null 
                ? properties.getDynamicSkills().getOrder() 
                : 0);
        log.info("动态技能自动注入已启用，将拦截 ChatModel.call(Prompt) 并自动注入技能");
        log.info("McpSkillManager has {} skills ready for injection", mcpSkillManager.getSkillTools().size());
        for (var tool : mcpSkillManager.getSkillTools()) {
            log.info("  - {}: {}", tool.getToolDefinition().name(), tool.getToolDefinition().description());
        }
        return advisor;
    }
    
    /**
     * 匹配 ChatModel 中的 call 方法
     */
    private static class ChatModelMethodMatcher extends StaticMethodMatcherPointcut {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            // 匹配 call 方法
            if (!method.getName().equals("call")) {
                return false;
            }
            // 返回类型是 ChatResponse
            return method.getReturnType().getName().contains("ChatResponse");
        }
    }
    
    /**
     * 匹配参数中有 Prompt
     */
    private static class PromptArgumentMatcher extends StaticMethodMatcherPointcut {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            for (Class<?> paramType : method.getParameterTypes()) {
                if (paramType.equals(Prompt.class)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * 动态技能注解匹配器
     * 优先级：方法注解 > 类注解 > 全局配置
     */
    private static class DynamicSkillsAnnotationMatcher extends StaticMethodMatcherPointcut {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            // 1. 检查方法注解
            EnableDynamicSkills methodAnnotation = method.getAnnotation(EnableDynamicSkills.class);
            if (methodAnnotation != null) {
                return methodAnnotation.value();
            }
            
            // 2. 检查类注解
            EnableDynamicSkills classAnnotation = targetClass.getAnnotation(EnableDynamicSkills.class);
            if (classAnnotation != null) {
                return classAnnotation.value();
            }
            
            // 3. 回退到全局配置
            // 这里返回true，因为外层已经有 @ConditionalOnProperty 控制全局开关了
            // 如果全局关闭，整个 Advisor 不会创建，所以能走到这里说明全局开启
            return true;
        }
    }
    
    /**
     * 动态技能 ToolCallbackProvider
     * 让 Spring AI DefaultToolCallingManager 能找到我们的工具
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloud.skill.dynamic-skills", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ToolCallbackProvider dynamicSkillToolCallbackProvider(McpSkillManager mcpSkillManager) {
        return () -> mcpSkillManager.getSkillTools().toArray(new ToolCallback[0]);
    }
}
