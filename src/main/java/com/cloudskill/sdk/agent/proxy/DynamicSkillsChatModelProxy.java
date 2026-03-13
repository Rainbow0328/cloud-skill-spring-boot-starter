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
package com.cloudskill.sdk.agent.proxy;

import com.cloudskill.sdk.agent.context.SkillContext;
import com.cloudskill.sdk.agent.context.SkillContextHolder;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallResult;
import com.cloudskill.sdk.util.PromptUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChatModel代理
 * 自动注入技能信息，处理工具调用
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicSkillsChatModelProxy implements ChatModel {
    
    private final ChatModel delegate;
    private final CloudSkillClient cloudSkillClient;
    private final ObjectMapper objectMapper;
    
    // 工具调用解析正则 - 支持多种格式
    private static final Pattern TOOL_PATTERN = Pattern.compile("调用工具：([^\\n\\r]+)");
    private static final Pattern PARAM_PATTERN = Pattern.compile("参数：(\\{.*\\})", Pattern.DOTALL);
    // 通义千问标准格式
    private static final Pattern QWEN_TOOL_PATTERN = Pattern.compile("<\\|FunctionCallBegin\\|>\\s*(\\[.*?\\])\\s*<\\|FunctionCallEnd\\|>", Pattern.DOTALL);
    
    @Override
    public ChatResponse call(Prompt prompt) {
        List<Skill> skills;
        
        // 优先使用上下文的技能（注解模式）
        if (SkillContextHolder.hasContext()) {
            SkillContext context = SkillContextHolder.getContext();
            if (!context.isEnabled() || CollectionUtils.isEmpty(context.getSkills())) {
                // 上下文没有可用技能，尝试使用全局所有技能
                skills = cloudSkillClient.getAllSkills();
            } else {
                skills = context.getSkills();
            }
        } else {
            // 没有上下文，使用全局模式，加载所有可用技能
            skills = cloudSkillClient.getAllSkills();
        }
        
        if (CollectionUtils.isEmpty(skills)) {
            // 没有可用技能，直接调用原方法
            log.debug("No available skills, proceed with normal chat");
            return delegate.call(prompt);
        }
        
        try {
            // 第一步：增强Prompt，注入技能信息
            Prompt enhancedPrompt = enhancePromptWithSkills(prompt, skills);
            log.debug("Enhanced prompt with {} skills", skills.size());
            
            // 第二步：调用大模型
            ChatResponse response = delegate.call(enhancedPrompt);
            
            // 第三步：检查是否有工具调用
            String responseText = getResponseText(response);
            if (responseText != null && 
                (responseText.contains("调用工具：") || responseText.contains("<|FunctionCallBegin|>"))) {
                log.debug("Tool calls detected in model response");
                
                // 第四步：解析并执行工具调用
                String toolResult = executeToolCall(responseText);
                if (toolResult != null) {
                    // 第五步：构建新的Prompt，包含工具结果
                    Prompt newPrompt = buildPromptWithToolResults(enhancedPrompt, responseText, toolResult);
                    
                    // 第六步：再次调用大模型生成最终回答
                    log.debug("Calling model again with tool results");
                    return delegate.call(newPrompt);
                }
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing dynamic skills in ChatModel proxy", e);
            // 发生异常时降级，使用原始调用
            return delegate.call(prompt);
        }
    }
    
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        // 流式调用暂时直接返回，后续版本支持
        log.debug("Stream call detected, dynamic skills for stream will be supported in future version");
        return delegate.stream(prompt);
    }
    
    @Override
    public ChatOptions getDefaultOptions() {
        return delegate.getDefaultOptions();
    }
    
    /**
     * 增强Prompt，注入技能信息
     */
    private Prompt enhancePromptWithSkills(Prompt prompt, List<Skill> skills) {
        List<Message> messages = new ArrayList<>(prompt.getInstructions());
        
        // 构建包含技能描述的系统消息
        String skillSystemPrompt = PromptUtils.buildSkillSystemPrompt(skills);
        SystemMessage skillSystemMessage = new SystemMessage(skillSystemPrompt);
        
        // 将技能系统消息添加到最前面
        messages.add(0, skillSystemMessage);
        
        return new Prompt(messages, prompt.getOptions());
    }
    
    /**
     * 获取响应文本
     */
    private String getResponseText(ChatResponse response) {
        if (response == null || CollectionUtils.isEmpty(response.getResults())) {
            return null;
        }
        
        Generation generation = response.getResult();
        if (generation == null || generation.getOutput() == null) {
            return null;
        }
        
        return generation.getOutput().getText();
    }
    
    /**
     * 执行工具调用
     */
    private String executeToolCall(String responseText) {
        try {
            final String[] toolName = {null};
            final String[] paramJson = {null};
            
            // 先尝试匹配通义千问标准格式
            Matcher qwenMatcher = QWEN_TOOL_PATTERN.matcher(responseText);
            if (qwenMatcher.find()) {
                String functionJson = qwenMatcher.group(1).trim();
                List<Map<String, Object>> functionCalls = objectMapper.readValue(functionJson, new TypeReference<List<Map<String, Object>>>() {});
                if (!functionCalls.isEmpty()) {
                    Map<String, Object> functionCall = functionCalls.get(0);
                    toolName[0] = (String) functionCall.get("name");
                    Map<String, Object> parameters = (Map<String, Object>) functionCall.get("parameters");
                    paramJson[0] = objectMapper.writeValueAsString(parameters);
                }
            } 
            // 匹配中文提示格式
            else {
                // 解析工具名称
                Matcher toolMatcher = TOOL_PATTERN.matcher(responseText);
                if (!toolMatcher.find()) {
                    return null;
                }
                toolName[0] = toolMatcher.group(1).trim();
                    
                    // 解析参数
                    Matcher paramMatcher = PARAM_PATTERN.matcher(responseText);
                    if (!paramMatcher.find()) {
                        return null;
                    }
                    paramJson[0] = paramMatcher.group(1).trim();
            }
            
            log.info("检测到工具调用: {}, 参数: {}", toolName[0], paramJson[0]);
            
            // 查找对应的技能
            List<Skill> availableSkills = cloudSkillClient.getAllSkills();
            Skill skill = availableSkills.stream()
                    .filter(s -> s.getName().equalsIgnoreCase(toolName[0]))
                    .findFirst()
                    .orElse(null);
            
            if (skill == null) {
                log.warn("未找到对应的技能: {}", toolName);
                return null;
            }
            
            // 解析参数
            Map<String, Object> params = objectMapper.readValue(paramJson[0], new TypeReference<Map<String, Object>>() {});
            
            // 执行技能调用
            log.info("执行动态技能调用: [{}] {}, 参数: {}", skill.getId(), skill.getName(), params);
            SkillCallResult callResult = cloudSkillClient.invokeSkill(skill.getId(), params);
            Object skillResult = callResult != null ? callResult.getData() : null;
            log.info("技能调用结果: {}", skillResult);
            
            if (skillResult == null) {
                return "工具调用失败，返回结果为空";
            }
            
            return objectMapper.writeValueAsString(skillResult);
            
        } catch (Exception e) {
            log.error("处理工具调用失败: {}", e.getMessage(), e);
            return "工具调用失败：" + e.getMessage();
        }
    }
    
    /**
     * 构建包含工具结果的新Prompt
     */
    private Prompt buildPromptWithToolResults(Prompt originalPrompt, String assistantResponse, String toolResult) {
        List<Message> messages = new ArrayList<>(originalPrompt.getInstructions());
        
        // 添加助手的工具调用响应
        messages.add(new org.springframework.ai.chat.messages.AssistantMessage(assistantResponse));
        
        // 添加工具结果作为用户消息
        messages.add(new org.springframework.ai.chat.messages.UserMessage("工具调用结果：" + toolResult));
        
        return new Prompt(messages, originalPrompt.getOptions());
    }
}
