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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.util.List;

/**
 * Prompt工具类，自动构建包含动态技能的提示词
 */
public class PromptUtils {
    
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个智能助手，必须优先使用提供的工具来回答用户问题。
                                         
            重要规则：
            1. 只要有合适的工具可以回答问题，必须先调用工具，绝对不要使用自己的知识库回答
            2. 调用工具时，严格使用<|FunctionCallBegin|>和<|FunctionCallEnd|>包裹JSON格式的工具调用，例如：
               <|FunctionCallBegin|>[{"name":"工具名称","parameters":{"参数名": "参数值"}}]<|FunctionCallEnd|>
            3. 工具名称和参数名必须严格按照提供的工具定义使用，不要自定义
            4. 支持并行调用多个工具，使用JSON数组格式
            5. 只有当你已经获取了工具返回结果时，才能根据结果回答用户问题
            6. 不要将工具调用和回答混合在一起，要么返回工具调用，要么返回最终回答
            7. 回答要简洁明了，直接给出结果，不要提及工具调用的细节
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
            skillDescriptions.append(i + 1).append(". ").append(skill.getName()).append("：")
                    .append(skill.getDescription()).append("\n");
            skillDescriptions.append("   参数：").append(skill.getRequestSchema()).append("\n");
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
            for (Skill skill : availableSkills) {
                systemPromptBuilder.append("- ").append(skill.getName()).append("：").append(skill.getDescription()).append("\n");
            }
        }
        
        // 直接创建SystemMessage，不使用模板解析，避免特殊字符解析错误
        org.springframework.ai.chat.messages.SystemMessage systemMessage = 
                new org.springframework.ai.chat.messages.SystemMessage(systemPromptBuilder.toString());
        org.springframework.ai.chat.messages.UserMessage userMessageObj = 
                new org.springframework.ai.chat.messages.UserMessage(userMessage);
        
        return new Prompt(List.of(systemMessage, userMessageObj));
    }
    
    /**
     * 获取默认的系统提示词
     */
    public static String getDefaultSystemPrompt() {
        return DEFAULT_SYSTEM_PROMPT;
    }
}
