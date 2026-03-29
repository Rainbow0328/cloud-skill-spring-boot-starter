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
package com.cloudskill.sdk.util;

import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Prompt工具类，自动构建包含动态技能的提示词
 */
public class PromptUtils {
    
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个智能助手，必须优先使用提供的工具来回答用户问题。
                                             
            重要规则：
            1. 只要有合适的工具可以回答问题，必须先调用工具，绝对不要使用自己的知识库回答
            2. 调用工具时，严格使用<|FunctionCallBegin|>和<|FunctionCallEnd|>包裹 JSON 格式的工具调用，例如：
               <|FunctionCallBegin|>[{"name":"工具名称","parameters":{"参数名": "参数值"}}]<|FunctionCallEnd|>
            3. 工具名称和参数名必须严格按照提供的工具定义使用，不要自定义
            4. 支持并行调用多个工具，使用 JSON 数组格式
            5. 只有当你已经获取了工具返回结果时，才能根据结果回答用户问题
            6. 不要将工具调用和回答混合在一起，要么返回工具调用，要么返回最终回答
            7. 回答要简洁明了，直接给出结果，不要提及工具调用的细节
            8. 注意每个工具的 HTTP 方法（GET/POST/PUT/DELETE），确保参数传递正确
            9. 仔细阅读每个工具的参数说明，包括参数类型、是否必填、默认值等
            """;
    
    private PromptUtils() {
        // 工具类不允许实例化
    }
    
    /**
     * 构建包含动态技能的Prompt
     * @param userMessage 用户消息
     * @param client CloudSkillClient实例
     * @return 构建好的Prompt
     */
    public static Prompt buildPromptWithSkills(String userMessage, CloudSkillClient client) {
        return buildPromptWithSkills(userMessage, DEFAULT_SYSTEM_PROMPT, client);
    }
    
    /**
     * 构建包含动态技能的系统提示词
     * @param skills 可用技能列表
     * @return 构建好的系统提示词
     */
    public static String buildSkillSystemPrompt(List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            // 没有可用技能时，返回纯系统提示词，不包含工具相关内容
            return "你是一个智能助手，请直接回答用户的问题。";
        }
        
        StringBuilder skillDescriptions = new StringBuilder();
        skillDescriptions.append("可用工具列表：\n");
        
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            
            // 构建详细的工具描述
            skillDescriptions.append(i + 1).append(". ").append(skill.getName()).append("：")
                    .append(skill.getDescription()).append("\n");
            
            // 添加 HTTP 方法
            String httpMethod = skill.getHttpMethod() != null ? skill.getHttpMethod() : "POST";
            skillDescriptions.append("   请求方法：").append(httpMethod).append("\n");
            
            // 添加 endpoint 信息（如果有）
            if (skill.getEndpoint() != null && !skill.getEndpoint().isEmpty()) {
                skillDescriptions.append("   端点：").append(skill.getEndpoint()).append("\n");
            }
            
            // 解析 requestSchema，提取详细的参数信息
            try {
                com.fasterxml.jackson.databind.JsonNode schema = new com.fasterxml.jackson.databind.ObjectMapper().readTree(skill.getRequestSchema());
                com.fasterxml.jackson.databind.JsonNode properties = schema.get("properties");
                com.fasterxml.jackson.databind.JsonNode required = schema.get("required");
                
                if (properties != null && properties.isObject()) {
                    skillDescriptions.append("   参数详情：\n");
                    properties.fields().forEachRemaining(entry -> {
                        String paramName = entry.getKey();
                        com.fasterxml.jackson.databind.JsonNode paramNode = entry.getValue();
                        
                        String paramType = paramNode.has("type") ? paramNode.get("type").asText() : "any";
                        String paramDesc = paramNode.has("description") ? paramNode.get("description").asText() : "";
                        
                        // 检查是否必填
                        boolean requiredFlag = false;
                        if (required != null && required.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode reqNode : required) {
                                if (reqNode.asText().equals(paramName)) {
                                    requiredFlag = true;
                                    break;
                                }
                            }
                        }
                        
                        skillDescriptions.append("     - ").append(paramName)
                            .append(" (").append(paramType).append(")")
                            .append(requiredFlag ? " [必填]" : " [可选]")
                            .append("：").append(paramDesc).append("\n");
                    });
                }
                
                // 添加响应示例（如果有）- 暂时注释，Skill 类没有这个方法
                // if (skill.getResponseExample() != null && !skill.getResponseExample().isEmpty()) {
                //     skillDescriptions.append("   响应示例：").append(skill.getResponseExample()).append("\n");
                // }
            } catch (Exception e) {
                // 解析失败，使用简化的参数信息
                if (skill.getParameters() != null && !skill.getParameters().isEmpty()) {
                    skillDescriptions.append("   参数：[");
                    skillDescriptions.append(skill.getParameters().stream()
                        .map(p -> (String) p.get("name"))
                        .collect(java.util.stream.Collectors.joining(", ")));
                    skillDescriptions.append("]\n");
                }
            }
            
            skillDescriptions.append("\n");
        }
        
        return DEFAULT_SYSTEM_PROMPT + "\n\n" + skillDescriptions;
    }
    
    /**
     * 构建包含动态技能的Prompt，使用自定义系统提示词
     * @param userMessage 用户消息
     * @param customSystemPrompt 自定义系统提示词
     * @param client CloudSkillClient实例
     * @return 构建好的Prompt
     */
    public static Prompt buildPromptWithSkills(String userMessage, String customSystemPrompt, CloudSkillClient client) {
        List<Skill> availableSkills = client.getAllSkills();
        
        // 构建系统提示词
        StringBuilder systemPromptBuilder = new StringBuilder(customSystemPrompt);
        
        // 添加工具列表
        if (!availableSkills.isEmpty()) {
            systemPromptBuilder.append("\n\n现在可以使用的工具列表：\n");
            for (int i = 0; i < availableSkills.size(); i++) {
                Skill skill = availableSkills.get(i);
                systemPromptBuilder.append("- ").append(skill.getName()).append("：").append(skill.getDescription()).append("\n");
                
                // 解析 requestSchema，提取参数名
                try {
                    JsonNode schema = new com.fasterxml.jackson.databind.ObjectMapper().readTree(skill.getRequestSchema());
                    JsonNode properties = schema.get("properties");
                    if (properties != null && properties.isObject()) {
                        systemPromptBuilder.append("  参数：[");
                        java.util.Iterator<String> fieldNames = properties.fieldNames();
                        boolean first = true;
                        while (fieldNames.hasNext()) {
                            if (!first) {
                                systemPromptBuilder.append(", ");
                            }
                            systemPromptBuilder.append(fieldNames.next());
                            first = false;
                        }
                        systemPromptBuilder.append("]\n");
                    }
                } catch (Exception e) {
                    // 解析失败，忽略
                }
            }
        }
        
        // 直接创建SystemMessage，不使用模板解析，避免特殊字符解析错误
        SystemMessage systemMessage =
                new SystemMessage(systemPromptBuilder.toString());
        UserMessage userMessageObj =
                new UserMessage(userMessage);
        
        return new Prompt(List.of(systemMessage, userMessageObj));
    }
    
    /**
     * 获取默认的系统提示词
     */
    public static String getDefaultSystemPrompt() {
        return DEFAULT_SYSTEM_PROMPT;
    }
}
