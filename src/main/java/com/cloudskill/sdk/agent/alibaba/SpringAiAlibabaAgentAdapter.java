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
package com.cloudskill.sdk.agent.alibaba;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallResult;
import com.cloudskill.sdk.spi.SkillConverter;
import com.cloudskill.sdk.spi.SkillExecutionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.function.Function;

/**
 * Spring AI Alibaba Agent 适配器
 * 支持 ReactAgent 的动态技能注入
 * 
 * @author Cloud Skill Team
 * @version 1.0.0
 */
@Component
@ConditionalOnClass(name = "com.alibaba.cloud.ai.graph.agent.ReactAgent")
public class SpringAiAlibabaAgentAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(SpringAiAlibabaAgentAdapter.class);
    
    private final CloudSkillClient cloudSkillClient;
    private final CloudSkillProperties properties;
    private final List<SkillConverter> converters;
    private final List<SkillExecutionHook> hooks;
    
    /**
     * 构造方法
     */
    public SpringAiAlibabaAgentAdapter(
            CloudSkillClient cloudSkillClient,
            CloudSkillProperties properties,
            ObjectProvider<List<SkillConverter>> convertersProvider,
            ObjectProvider<List<SkillExecutionHook>> hooksProvider) {
        
        this.cloudSkillClient = cloudSkillClient;
        this.properties = properties;
        
        // 初始化转换器
        this.converters = convertersProvider.getIfAvailable(ArrayList::new);
        // 添加默认转换器：直接使用和 McpSkillManager 相同的转换逻辑
        this.converters.add(new SkillConverter() {
            @Override
            public boolean supports(Skill skill) {
                return true;
            }
            
            @Override
            public ToolCallback convert(Skill skill, CloudSkillClient client) {
                // 创建一个动态函数，调用远程的 MCP 技能
                Function<Map<String, Object>, Object> skillFunction = params -> {
                    try {
                        SkillCallResult result = client.invokeSkill(skill.getId(), params);
                        if (result.isSuccess() && result.getData() != null) {
                            return result.getData();
                        }
                        return "Error executing skill " + skill.getName() + ": " + (result.getMessage() != null ? result.getMessage() : "unknown error");
                    } catch (Exception e) {
                        return "Error executing skill " + skill.getName() + ": " + e.getMessage();
                    }
                };
                
                // 构建描述
                StringBuilder description = new StringBuilder();
                if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
                    description.append(skill.getDescription());
                }
                
                // 如果有 responseSchema，只需要告诉大模型输出格式要求
                if (skill.getResponseSchema() != null && !skill.getResponseSchema().isEmpty()) {
                    description.append("\n\n返回结果需要符合指定的 JSON 格式。");
                }
                
                // 构建 ToolCallback
                var builder = FunctionToolCallback.builder(skill.getName(), skillFunction)
                        .description(description.toString())
                        .inputType(Map.class);
                
                // 如果有 requestSchema，添加 inputSchema
                if (skill.getRequestSchema() != null && !skill.getRequestSchema().isEmpty()) {
                    try {
                        var method = builder.getClass().getMethod("inputSchema", String.class);
                        method.invoke(builder, skill.getRequestSchema());
                    } catch (Exception e) {
                        // 如果失败，已经有 inputType 了，不用处理
                    }
                }
                
                return builder.build();
            }
        });
        
        // 初始化钩子
        this.hooks = hooksProvider.getIfAvailable(ArrayList::new);
        
        log.info("SpringAiAlibabaAgentAdapter 初始化完成，支持 ReactAgent 动态技能注入");
    }
    
    /**
     * 为 ReactAgent 注入动态技能
     * 
     * @param agent ReactAgent 实例
     * @return 注入技能后的 Agent
     */
    public ReactAgent injectSkills(ReactAgent agent) {
        try {
            // 获取当前可用的技能列表
            List<Skill> skills = cloudSkillClient.getAllSkills();
            
            if (skills.isEmpty()) {
                log.debug("没有可用技能，跳过注入");
                return agent;
            }
            
            log.info("准备为 ReactAgent 注入{}个技能", skills.size());
            
            // 将技能转换为 ToolCallback
            List<ToolCallback> toolCallbacks = new ArrayList<>();
            for (Skill skill : skills) {
                ToolCallback toolCallback = convertToToolCallback(skill);
                if (toolCallback != null) {
                    toolCallbacks.add(toolCallback);
                    log.debug("转换技能为 ToolCallback: {}", skill.getName());
                }
            }
            
            // 通过反射获取 Agent 的 tools 字段并注入
            injectToolsIntoAgent(agent, toolCallbacks);
            
            log.info("成功为 ReactAgent 注入{}个工具回调", toolCallbacks.size());
            return agent;
            
        } catch (Exception e) {
            log.error("为 ReactAgent 注入技能失败", e);
            return agent;
        }
    }
    
    /**
     * 注入工具到 Agent
     */
    private void injectToolsIntoAgent(ReactAgent agent, List<ToolCallback> toolCallbacks) throws Exception {
        // 尝试通过 setTools 方法注入
        Method setToolsMethod = findMethod(agent.getClass(), "setTools", List.class);
        if (setToolsMethod != null) {
            // 获取现有工具
            List<ToolCallback> existingTools = getExistingTools(agent);
            existingTools.addAll(toolCallbacks);
            
            // 设置工具
            setToolsMethod.invoke(agent, existingTools);
            return;
        }
        
        // 尝试通过反射设置 tools 字段
        Field toolsField = findField(agent.getClass(), "tools");
        if (toolsField != null) {
            toolsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<ToolCallback> existingTools = (List<ToolCallback>) toolsField.get(agent);
            if (existingTools == null) {
                existingTools = new ArrayList<>();
            }
            
            existingTools.addAll(toolCallbacks);
            toolsField.set(agent, existingTools);
            return;
        }
        
        throw new UnsupportedOperationException("无法为 ReactAgent 注入工具：" + agent.getClass().getName());
    }
    
    /**
     * 获取 Agent 现有的工具列表
     */
    @SuppressWarnings("unchecked")
    private List<ToolCallback> getExistingTools(ReactAgent agent) throws Exception {
        // 尝试通过 getTools 方法获取
        Method getToolsMethod = findMethod(agent.getClass(), "getTools");
        if (getToolsMethod != null) {
            try {
                return (List<ToolCallback>) getToolsMethod.invoke(agent);
            } catch (Exception e) {
                log.debug("getTools 方法调用失败：{}", e.getMessage());
            }
        }
        
        // 尝试通过反射获取 tools 字段
        Field toolsField = findField(agent.getClass(), "tools");
        if (toolsField != null) {
            toolsField.setAccessible(true);
            return (List<ToolCallback>) toolsField.get(agent);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 将 Skill 转换为 ToolCallback
     */
    private ToolCallback convertToToolCallback(com.cloudskill.sdk.model.Skill skill) {
        // 使用 SkillConverter 进行转换
        for (SkillConverter converter : converters) {
            if (converter.supports(skill)) {
                return converter.convert(skill, cloudSkillClient);
            }
        }
        
        // 默认转换逻辑
        log.warn("没有找到合适的 SkillConverter，无法转换技能：{}", skill.getName());
        return null;
    }
    
    /**
     * 查找方法，支持父类查找
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
     * 查找字段，支持父类查找
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
}
