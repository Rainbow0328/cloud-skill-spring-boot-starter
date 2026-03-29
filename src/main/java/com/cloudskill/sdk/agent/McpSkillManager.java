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

import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP 技能管理器，提供动态技能的加载和管理
 */
public class McpSkillManager {
    
    private static final Logger log = LoggerFactory.getLogger(McpSkillManager.class);
    private final CloudSkillClient cloudSkillClient;
    private final List<ToolCallback> skillTools = new ArrayList<>();
    
    /**
     * 构造方法
     */
    public McpSkillManager(CloudSkillClient cloudSkillClient) {
        this.cloudSkillClient = cloudSkillClient;
        refreshSkillTools();
        
        // 注册技能变更监听器，实时更新技能列表
        cloudSkillClient.registerSkillChangeListener(event -> {
            log.info("Received skill change event: {} - {}, refreshing skill tools", 
                    event.getSkillId(), event.getChangeType());
            refreshSkillTools();
        });
    }
    
    /**
     * 刷新技能工具列表
     */
    public void refreshSkillTools() {
        skillTools.clear();
        List<Skill> skills = cloudSkillClient.getAllSkills();
        for (Skill skill : skills) {
            // 将 Skill 转换为 ToolCallback
            ToolCallback toolCallback = convertSkillToToolCallback(skill);
            if (toolCallback != null) {
                skillTools.add(toolCallback);
                log.info("Registered skill tool: {} - {}", skill.getName(), skill.getDescription());
            }
        }
        log.info("Refreshed {} skill tools", skillTools.size());
    }
    
    /**
     * 将 Skill 转换为 ToolCallback
     * 支持 Spring AI 和 Spring AI Alibaba
     */
    private ToolCallback convertSkillToToolCallback(Skill skill) {
        // 创建一个动态函数，调用远程的 MCP 技能
        // 使用 Object 作为返回类型，兼容 Spring AI Alibaba
        Function<Map<String, Object>, Object> skillFunction = params -> {
            try {
                if (params == null) {
                    log.warn("大模型调用工具，但 params 是 null! skill={}", skill.getName());
                } else {
                    log.info("大模型调用工具，params 不为 null: skill={}, size={}, params={}", 
                        skill.getName(), params.size(), params);
                }
                log.debug("Invoking skill: {} with params: {}", skill.getName(), params);
                    
                // 调用远程技能服务
                SkillCallResult result = cloudSkillClient.invokeSkill(skill.getId(), params);
                    
                log.debug("Skill invocation result: {}", result);
                if (result.isSuccess() && result.getData() != null) {
                    return result.getData();
                }
                return "Error executing skill " + skill.getName() + ": " + (result.getMessage() != null ? result.getMessage() : "unknown error");
            } catch (Exception e) {
                log.error("Skill invocation failed: {}", skill.getName(), e);
                return "Error executing skill " + skill.getName() + ": " + e.getMessage();
            }
        };
            
        // 构建描述，包含输出格式信息
        StringBuilder description = new StringBuilder();
        if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
            description.append(skill.getDescription());
        }
        
        // 如果有 responseSchema，只需要告诉大模型输出格式要求，不需要放完整 schema
        // 完整 schema 只会干扰大模型理解输入参数
        if (skill.getResponseSchema() != null && !skill.getResponseSchema().isEmpty()) {
            description.append("\n\n返回结果需要符合指定的 JSON 格式。");
        }
          
        // 使用 FunctionToolCallback.builder 直接构建
           var builder = FunctionToolCallback.builder(skill.getName(), skillFunction)
                   .description(description.toString())
                   .inputType(Map.class); // inputType 总是必填，不管有没有 inputSchema
          
           // 如果有 requestSchema，覆盖输入 schema
           // inputType 设置为 Map，inputSchema 提供详细参数定义
           if (skill.getRequestSchema() != null && !skill.getRequestSchema().isEmpty()) {
               try {
                   // 反射调用 inputSchema，避免编译错误
                   var method = builder.getClass().getMethod("inputSchema", String.class);
                   method.invoke(builder, skill.getRequestSchema());
                   log.info("技能 {} 成功调用 inputSchema: {}", skill.getName(), skill.getRequestSchema());
               } catch (Exception e) {
                   log.warn("inputSchema method not available, use inputType only: {}", e.getMessage());
                   // 已经设置了 inputType，不用再设置了
               }
           } else {
               log.info("技能 {} 没有 requestSchema，只使用 inputType", skill.getName());
           }
          
        return builder.build();
    }
    
    /**
     * 获取当前已加载的技能工具列表
     */
    public List<ToolCallback> getSkillTools() {
        return new ArrayList<>(skillTools);
    }
}
