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
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.Skill;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 动态技能增强逻辑
 * 自动注入动态技能，完全依赖Spring AI原生工具调用机制
 */
public class DynamicSkillsAdvice implements MethodInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicSkillsAdvice.class);
    private final CloudSkillClient cloudSkillClient;
    private final boolean debug;
    private final List<SkillConverter> converters;
    private final List<SkillExecutionHook> hooks;
    private final SkillCallMetrics metrics;
    
    /**
     * 构造方法
     */
    public DynamicSkillsAdvice(CloudSkillClient cloudSkillClient, 
                               CloudSkillProperties properties,
                               ObjectProvider<List<SkillConverter>> convertersProvider,
                               ObjectProvider<List<SkillExecutionHook>> hooksProvider,
                               ObjectProvider<SkillCallMetrics> metricsProvider) {
        this.cloudSkillClient = cloudSkillClient;
        this.debug = properties.isDebug();
        
        // 初始化转换器
        this.converters = convertersProvider.getIfAvailable(ArrayList::new);
        this.converters.add(new DefaultSkillConverter());
        
        // 初始化钩子
        this.hooks = hooksProvider.getIfAvailable(ArrayList::new);
        
        // 初始化统计
        this.metrics = metricsProvider.getIfAvailable(SkillCallMetrics::new);
    }
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 只拦截ChatModel的call方法
        if (!"call".equals(invocation.getMethod().getName())) {
            return invocation.proceed();
        }
        
        Object[] args = invocation.getArguments();
        if (args.length == 0 || !(args[0] instanceof Prompt)) {
            return invocation.proceed();
        }
        
        Prompt prompt = (Prompt) args[0];
        
        // 获取方法或类上的注解
        EnableDynamicSkills annotation = getAnnotation(invocation);
        
        // 获取当前可用的动态技能
        List<Skill> skills = cloudSkillClient.getAllSkills();
        if (skills.isEmpty()) {
            log.debug("没有可用的动态技能，跳过注入");
            return invocation.proceed();
        }
        
        // 根据注解过滤技能
        if (annotation != null) {
            skills = filterSkills(skills, annotation);
        }
        
        if (skills.isEmpty()) {
            log.debug("过滤后没有可用的动态技能，跳过注入");
            return invocation.proceed();
        }
        
        // 输出加载到的技能列表，方便校验
        log.info("加载到{}个动态技能：", skills.size());
        skills.forEach(skill -> log.info("  - [{}] {}", skill.getId(), skill.getName()));
        
        // 将Skill转换为ToolCallback
        List<ToolCallback> toolCallbacks = skills.stream()
                .map(this::convertToToolCallback)
                .collect(Collectors.toList());
        
        // 注入工具到Prompt中
        injectToolsIntoPrompt(prompt, toolCallbacks);
        
        // 直接执行原方法，Spring AI会自动处理工具调用
        return invocation.proceed();
    }
    
    /**
     * 获取方法或类上的注解
     */
    private EnableDynamicSkills getAnnotation(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        // 先找方法上的注解
        EnableDynamicSkills annotation = method.getAnnotation(EnableDynamicSkills.class);
        if (annotation != null) {
            return annotation;
        }
        // 再找类上的注解
        return invocation.getThis().getClass().getAnnotation(EnableDynamicSkills.class);
    }
    
    /**
     * 根据注解过滤技能
     */
    private List<Skill> filterSkills(List<Skill> skills, EnableDynamicSkills annotation) {
        List<String> include = Arrays.asList(annotation.value());
        List<String> exclude = Arrays.asList(annotation.exclude());
        
        return skills.stream()
                .filter(skill -> include.isEmpty() || include.contains(skill.getId()))
                .filter(skill -> exclude.isEmpty() || !exclude.contains(skill.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 通用工具注入方法，支持所有ChatOptions实现
     */
    private void injectToolsIntoPrompt(Prompt prompt, List<ToolCallback> toolCallbacks) {
        ChatOptions options = prompt.getOptions();
        
        try {
            // 情况1：Options存在，尝试注入
            if (options != null) {
                injectToolsIntoOptions(options, toolCallbacks);
                return;
            }
            
            // 情况2：Options不存在，尝试创建适合当前模型的Options
            ChatOptions newOptions = createDefaultChatOptions();
            if (newOptions != null) {
                injectToolsIntoOptions(newOptions, toolCallbacks);
                // 通过反射设置到Prompt中
                Field optionsField = Prompt.class.getDeclaredField("modelOptions");
                optionsField.setAccessible(true);
                optionsField.set(prompt, newOptions);
            }
            
        } catch (Exception e) {
            if (debug) {
                log.warn("注入动态技能失败，ChatOptions类型: {}, 错误: {}",
                        options != null ? options.getClass().getName() : "null",
                        e.getMessage(), e);
            } else {
                log.warn("注入动态技能失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 注入工具到ChatOptions
     */
    private void injectToolsIntoOptions(ChatOptions options, List<ToolCallback> toolCallbacks) throws Exception {
        // 获取现有工具
        List<ToolCallback> existingTools = getExistingTools(options);
        
        // 合并工具
        if (existingTools != null && !existingTools.isEmpty()) {
            toolCallbacks.addAll(existingTools);
        }
        
        // 设置工具
        setToolsToOptions(options, toolCallbacks);
        
        // 尝试设置toolChoice为auto
        setToolChoiceAuto(options);
        
        log.info("成功注入{}个动态技能到Chat请求中", toolCallbacks.size());
    }
    
    /**
     * 获取现有工具
     */
    private List<ToolCallback> getExistingTools(ChatOptions options) {
        // 尝试通过getTools方法获取
        Method getToolsMethod = findMethod(options.getClass(), "getTools");
        if (getToolsMethod != null) {
            try {
                return (List<ToolCallback>) getToolsMethod.invoke(options);
            } catch (Exception e) {
                log.debug("getTools方法调用失败: {}", e.getMessage());
            }
        }
        
        // 尝试通过反射获取fields
        Field toolsField = findField(options.getClass(), "tools");
        if (toolsField != null) {
            try {
                toolsField.setAccessible(true);
                return (List<ToolCallback>) toolsField.get(options);
            } catch (Exception e) {
                log.debug("反射获取tools字段失败: {}", e.getMessage());
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 设置工具到ChatOptions
     */
    private void setToolsToOptions(ChatOptions options, List<ToolCallback> toolCallbacks) throws Exception {
        // 尝试通过setTools方法设置
        Method setToolsMethod = findMethod(options.getClass(), "setTools", List.class);
        if (setToolsMethod != null) {
            setToolsMethod.invoke(options, toolCallbacks);
            return;
        }
        
        // 尝试通过反射设置fields
        Field toolsField = findField(options.getClass(), "tools");
        if (toolsField != null) {
            toolsField.setAccessible(true);
            toolsField.set(options, toolCallbacks);
            return;
        }
        
        throw new UnsupportedOperationException("ChatOptions类型不支持工具注入: " + options.getClass().getName());
    }
    
    /**
     * 创建适合当前环境的默认ChatOptions
     */
    private ChatOptions createDefaultChatOptions() {
        List<String> supportedOptionsClasses = List.of(
                "org.springframework.ai.openai.OpenAiChatOptions",
                "com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions",
                "com.baidubce.qianfan.springai.chat.QianFanChatOptions",
                "com.tencent.supersonic.ai.chat.SupersonicChatOptions",
                "com.zhipu.ai.springai.chat.ZhipuAiChatOptions"
        );
        
        for (String className : supportedOptionsClasses) {
            try {
                Class<?> optionsClass = Class.forName(className);
                return (ChatOptions) optionsClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                // 类不存在，跳过
            } catch (Exception e) {
                log.debug("创建{}实例失败: {}", className, e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * 设置toolChoice为auto，特别适配通义千问等模型
     */
    private void setToolChoiceAuto(ChatOptions options) {
        try {
            // 尝试通义千问的特殊配置
            if (options.getClass().getName().contains("DashScopeChatOptions")) {
                // 通义千问需要设置functionCall为"auto"
                try {
                    Method method = options.getClass().getMethod("setFunctionCall", String.class);
                    method.invoke(options, "auto");
                    log.debug("通义千问模型设置functionCall=auto成功");
                    return;
                } catch (NoSuchMethodException e) {
                    log.debug("通义千问setFunctionCall方法不存在: {}", e.getMessage());
                }
            }
            
            // 尝试通用的set方法名
            List<String> methodNames = List.of("setToolChoice", "setToolCall", "setFunctionCall");
            for (String methodName : methodNames) {
                try {
                    Method method = options.getClass().getMethod(methodName, String.class);
                    method.invoke(options, "auto");
                    log.debug("设置{}=auto成功", methodName);
                    return;
                } catch (NoSuchMethodException e) {
                    // 继续尝试下一个方法名
                }
            }
            
            // 尝试枚举类型的toolChoice
            try {
                Method method = options.getClass().getMethod("setToolChoice", Class.forName("org.springframework.ai.chat.model.ToolChoice"));
                Object autoEnum = Enum.valueOf((Class<Enum>) Class.forName("org.springframework.ai.chat.model.ToolChoice"), "AUTO");
                method.invoke(options, autoEnum);
                log.debug("设置ToolChoice.AUTO成功");
            } catch (Exception e) {
                log.debug("设置枚举类型toolChoice失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.debug("设置toolChoice失败: {}", e.getMessage());
        }
    }
    
    /**
     * 查找方法，支持父类方法查找
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * 查找字段，支持父类字段查找
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    

    
    /**
     * 将Skill转换为Spring AI的ToolCallback
     */
    private ToolCallback convertToToolCallback(Skill skill) {
        Function<Map<String, Object>, Object> function = params -> {
            long startTime = System.currentTimeMillis();
            boolean success = false;
            Object result = null;
            Throwable throwable = null;
            
            try {
                log.info("执行动态技能调用: [{}] {}", skill.getId(), skill.getName());
                log.debug("调用参数: {}", params);
                
                // 执行前置钩子
                Map<String, Object> processedParams = params;
                for (SkillExecutionHook hook : hooks) {
                    processedParams = hook.beforeExecution(skill, processedParams);
                    if (processedParams == null) {
                        log.info("技能调用被钩子终止: {}", skill.getName());
                        return null;
                    }
                }
                
                // 查找合适的转换器执行转换
                for (SkillConverter converter : converters) {
                    if (converter.supports(skill)) {
                        ToolCallback callback = converter.convert(skill, cloudSkillClient);
                        if (callback instanceof FunctionToolCallback) {
                            FunctionToolCallback functionCallback = (FunctionToolCallback) callback;
                            // 反射获取function
                            try {
                                Field functionField = FunctionToolCallback.class.getDeclaredField("function");
                                functionField.setAccessible(true);
                                Function<Map<String, Object>, Object> originalFunction = (Function<Map<String, Object>, Object>) functionField.get(functionCallback);
                                result = originalFunction.apply(processedParams);
                                success = true;
                                log.info("技能调用成功: {}", skill.getName());
                                return result;
                            } catch (Exception e) {
                                log.error("执行技能调用失败: {}", skill.getName(), e);
                                throw new RuntimeException("技能执行失败", e);
                            }
                        }
                    }
                }
                
                throw new IllegalStateException("没有找到合适的Skill转换器: " + skill.getName());
                
            } catch (Throwable t) {
                throwable = t;
                log.error("技能调用失败: {}", skill.getName(), t);
                throw t;
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                
                // 执行后置钩子
                for (SkillExecutionHook hook : hooks) {
                    result = hook.afterExecution(skill, params, result, throwable);
                }
                
                // 记录统计
                metrics.recordCall(skill.getId(), duration, success);
            }
        };
        
        // 构建完整的技能描述，包含参数说明，帮助模型理解如何调用
        StringBuilder description = new StringBuilder(skill.getDescription());
        
        // 添加参数说明
        if (skill.getParameters() != null && !skill.getParameters().isEmpty()) {
            description.append(" 参数：");
            skill.getParameters().forEach(param -> {
                description.append(param.get("name")).append("(").append(param.get("type")).append(")：")
                        .append(param.get("description")).append("，");
            });
            // 移除最后一个逗号
            if (description.length() > 0) {
                description.setLength(description.length() - 1);
            }
        }
        
        return FunctionToolCallback.builder(
                        skill.getName(),
                        function
                )
                .description(description.toString())
                .inputType(Map.class)
                .build();
    }
}
