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
package com.cloudskill.sdk.agent.alibaba;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * DashScope ChatOptions 工具类
 * 简化设计：只提供通过 builder 直接创建带 tools 的 DashScopeChatOptions
 * 
 * @author Cloud Skill Team
 * @version 1.0.0
 */
public class DashScopeOptionsUtils {
    
    private static final Logger log = LoggerFactory.getLogger(DashScopeOptionsUtils.class);
    
    /**
     * 设置 functionCall 为 auto（通义千问特定配置）
     * 必须设置这个，否则大模型不会自动调用工具
     */
    public static void setFunctionCallAuto(DashScopeChatOptions options) {
        try {
            Method method = options.getClass().getMethod("setFunctionCall", String.class);
            method.invoke(options, "auto");
            log.info("通义千问模型设置 functionCall=auto 成功");
        } catch (NoSuchMethodException e) {
            log.warn("通义千问 setFunctionCall 方法不存在：{}", e.getMessage());
        } catch (Exception e) {
            log.warn("设置 functionCall=auto 失败：{}", e.getMessage());
        }
    }
    
    /**
     * 创建默认的 DashScopeChatOptions 并直接注入工具
     * 通过 builder.withTools() 直接设置，一次到位，避免后续反射注入
     */
    public static DashScopeChatOptions createDefaultOptionsWithTools(List<ToolCallback> toolCallbacks) {
        try {
            Class<?> functionToolClass = Class.forName("com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec$FunctionTool");
            Class<?> functionClass = Class.forName("com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec$FunctionTool$Function");
            
            List<Object> functionTools = new java.util.ArrayList<>();
            
            for (ToolCallback toolCallback : toolCallbacks) {
                org.springframework.ai.tool.definition.ToolDefinition def = toolCallback.getToolDefinition();

                Constructor<?> functionConstructor = functionClass.getDeclaredConstructor(
                    String.class, String.class, Map.class);
                
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> parameters = objectMapper.readValue(def.inputSchema(), Map.class);
                
                Object functionObj = functionConstructor.newInstance(
                    def.description(), def.name(), parameters);
                
                // 通过反射构造 FunctionTool：只用传 function，type 默认为 FUNCTION
                // 官方提供了便捷构造函数：FunctionTool(Function function)
                Constructor<?> functionToolConstructor = functionToolClass.getDeclaredConstructor(
                    functionClass);
                Object functionToolObj = functionToolConstructor.newInstance(functionObj);
                
                functionTools.add(functionToolObj);
            }
            // 使用 builder 直接设置 tools
            Object builder = DashScopeChatOptions.builder()
                    .withModel("qwen-plus")
                    .withTemperature(0.5)
                    .withMaxToken(1000);
            
            // 查找 withTools 方法
            for (Method method : builder.getClass().getMethods()) {
                if (method.getName().equals("withTools")) {
                    method.invoke(builder, functionTools);
                    break;
                }
            }
            
            // build
            Method buildMethod = builder.getClass().getMethod("build");
            DashScopeChatOptions options = (DashScopeChatOptions) buildMethod.invoke(builder);
            
            log.info("通过builder创建默认 DashScopeChatOptions 并成功注入{}个 FunctionTool", functionTools.size());
            
            // 设置 functionCall=auto 让模型自动决定是否调用工具
            setFunctionCallAuto(options);
            
            return options;
        } catch (Exception e) {
            log.error("创建默认 DashScopeChatOptions 并注入工具失败", e);
            return null;
        }
    }
    
    /**
     * 将 Spring AI ToolCallback 列表转换为 DashScope FunctionTool 列表
     * DashScope 要求 DashScopeChatOptions.tools 必须是 List<DashScopeApiSpec.FunctionTool>
     */
    public static List<Object> convertToolCallbacksToDashScopeTools(List<ToolCallback> toolCallbacks) throws Exception {
        Class<?> functionToolClass = Class.forName("com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec$FunctionTool");
        Class<?> functionClass = Class.forName("com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec$FunctionTool$Function");
        
        List<Object> functionTools = new java.util.ArrayList<>();
        
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition def = toolCallback.getToolDefinition();
            
            // 通过反射构造 Function 内部类：description, name, parameters
            Constructor<?> functionConstructor = functionClass.getDeclaredConstructor(
                String.class, String.class, Map.class);
            
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> parameters = objectMapper.readValue(def.inputSchema(), Map.class);
            
            Object functionObj = functionConstructor.newInstance(
                def.description(), def.name(), parameters);
            
            // 通过反射构造 FunctionTool：只用传 function，type 默认为 FUNCTION
            Constructor<?> functionToolConstructor = functionToolClass.getDeclaredConstructor(
                functionClass);
            Object functionToolObj = functionToolConstructor.newInstance(functionObj);
            
            functionTools.add(functionToolObj);
        }
        
        log.debug("转换 {} 个 ToolCallback 为 DashScope FunctionTool", functionTools.size());
        return functionTools;
    }
}
