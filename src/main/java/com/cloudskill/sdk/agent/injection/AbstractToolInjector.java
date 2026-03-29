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
 * limitations under the the License.
 */
package com.cloudskill.sdk.agent.injection;

import com.cloudskill.sdk.agent.McpSkillManager;
import com.cloudskill.sdk.agent.alibaba.DashScopeOptionsUtils;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.listener.RedisSkillChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽象工具注入器
 * 模板方法模式，统一处理工具注入逻辑，方便扩展不同场景
 *
 * 子类只需要实现：
 * 1. 获取要注入的 Prompt
 * 2. 获取现有 ChatOptions
 * 3. 替换 Prompt 完成注入
 *
 * 功能：
 * - 注入前版本校验：对比 Redis 全局时间戳，过期自动触发全量同步
 * - 模板方法处理注入逻辑，复用通用代码
 *
 * @author Cloud Skill Team
 * @version 1.0.0
 */
public abstract class AbstractToolInjector {

    private static final Logger log = LoggerFactory.getLogger(AbstractToolInjector.class);

    protected final McpSkillManager mcpSkillManager;
    protected final SkillCache skillCache;
    protected final CloudSkillClient cloudSkillClient;
    protected final RedisSkillChangeListener skillChangeListener;

    public AbstractToolInjector(McpSkillManager mcpSkillManager,
                                SkillCache skillCache,
                                CloudSkillClient cloudSkillClient,
                                RedisSkillChangeListener skillChangeListener) {
        this.mcpSkillManager = mcpSkillManager;
        this.skillCache = skillCache;
        this.cloudSkillClient = cloudSkillClient;
        this.skillChangeListener = skillChangeListener;
    }

    /**
     * 获取要注入的动态工具列表
     * 注入前会先校验版本，过期自动同步
     * @return 动态工具列表
     */
    protected List<ToolCallback> getDynamicTools() {
        // 版本校验，如果过期会触发全量同步
        checkVersionAndSync();
        return mcpSkillManager.getSkillTools();
    }

    /**
     * 校验版本，如果和 Redis 全局时间戳不一致，触发全量同步
     */
    protected void checkVersionAndSync() {
        long localTimestamp = skillChangeListener.getLocalTimestamp();
        Long redisTimestamp = cloudSkillClient.getGlobalTimestamp();
        
        if (redisTimestamp == null || redisTimestamp <= localTimestamp) {
            // 本地版本已经是最新，不需要同步
            return;
        }
        
        // 版本过期，触发全量同步
        log.info("检测到版本过期：localTimestamp={}, redisTimestamp={}, 触发全量同步", localTimestamp, redisTimestamp);
        try {
            Long newTimestamp = cloudSkillClient.syncSkills();
            skillChangeListener.setLocalTimestamp(newTimestamp);
            // McpSkillManager 会 refreshSkillTools，从缓存重新读取
            mcpSkillManager.refreshSkillTools();
            log.info("全量同步完成，获取到 {} 个技能", mcpSkillManager.getSkillTools().size());
        } catch (Exception e) {
            log.error("全量同步失败，使用当前缓存继续", e);
        }
    }

    /**
     * 模板方法：注入工具到 Prompt
     * @param prompt 目标 Prompt
     * @param target 目标对象（ChatModel 或其他）
     * @param args 方法参数
     */
    public void injectTools(Prompt prompt, Object target, Object[] args) {
        List<ToolCallback> dynamicTools = getDynamicTools();
        if (dynamicTools.isEmpty()) {
            log.debug("[{}] No dynamic skills available, skipping injection", getClass().getSimpleName());
            return;
        }

        log.info("[{}] Found {} dynamic skills to inject", getClass().getSimpleName(), dynamicTools.size());
        for (ToolCallback tool : dynamicTools) {
            log.debug("  - {}", tool.getToolDefinition().name());
        }

        ChatOptions options = prompt.getOptions();
        log.debug("[{}] Prompt options: {}", getClass().getSimpleName(),
                options != null ? options.getClass().getName() : "null");

        if (options == null) {
            createNewPromptWithTools(prompt, dynamicTools, target, args);
        } else {
            mergeToolsIntoExistingOptions(prompt, dynamicTools, target, args);
        }
    }

    /**
     * 创建新 Prompt 并注入工具
     * 当 options == null 时走这里
     */
    protected void createNewPromptWithTools(Prompt prompt, List<ToolCallback> dynamicTools, Object target, Object[] args) {
        try {
            ChatOptions newOptions;

            // 特殊处理：DashScopeChatModel 需要 DashScopeChatOptions 并且 tools 必须是 DashScopeApiSpec.FunctionTool
            if (isDashScopeChatModel(target)) {
                newOptions = DashScopeOptionsUtils.createDefaultOptionsWithTools(dynamicTools);
                if (newOptions == null) {
                    log.warn("[{}] Failed to create DashScopeChatOptions with tools, skipping injection", getClass().getSimpleName());
                    return;
                }
                log.info("[{}] Created DashScopeChatOptions with {} tools via builder", getClass().getSimpleName(), dynamicTools.size());
            } else {
                // 标准 Spring AI：创建默认 ChatOptions
                newOptions = createDefaultChatOptions();
                if (newOptions == null) {
                    log.warn("[{}] Failed to create default ChatOptions, skipping injection", getClass().getSimpleName());
                    return;
                }
                mergeToolsIntoOptions(newOptions, dynamicTools);
                log.info("[{}] Created default ChatOptions with {} tools", getClass().getSimpleName(), dynamicTools.size());
            }

            // 获取 messages 字段
            Field messagesField = Prompt.class.getDeclaredField("messages");
            messagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Message> messages =
                    (List<Message>) messagesField.get(prompt);

            // 通过反射替换 args 中的 Prompt 为新 Prompt
            int promptIndex = -1;
            for (int i = 0; i < ((Object[]) args).length; i++) {
                if (((Object[]) args)[i] instanceof Prompt) {
                    promptIndex = i;
                    break;
                }
            }
            if (promptIndex >= 0) {
                Prompt newPrompt = new Prompt(messages, newOptions);
                ((Object[]) args)[promptIndex] = newPrompt;
                log.info("[{}] Created new Prompt with {} tools injected successfully", getClass().getSimpleName(), dynamicTools.size());
            }
        } catch (Exception e) {
            log.warn("[{}] Failed to replace Prompt argument: {}", getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 合并工具到现有的 ChatOptions
     * 当 options != null 时走这里
     */
    protected void mergeToolsIntoExistingOptions(Prompt prompt, List<ToolCallback> dynamicTools, Object target, Object[] args) {
        ChatOptions options = prompt.getOptions();

        // 特殊处理：DashScopeChatOptions 需要特殊转换
        if (options != null && isDashScopeChatOptions(options)) {
            mergeToolsIntoDashScopeOptions(options, dynamicTools);
            log.info("[{}] Merged {} DashScope tools into existing DashScopeChatOptions", getClass().getSimpleName(), dynamicTools.size());
            return;
        }

        // 标准 Spring AI：直接合并 ToolCallback
        mergeToolsIntoOptions(options, dynamicTools);
        log.info("[{}] Merged {} tools into existing ChatOptions", getClass().getSimpleName(), dynamicTools.size());
    }

    /**
     * 合并工具到标准 Spring AI ChatOptions
     */
    @SuppressWarnings("unchecked")
    protected void mergeToolsIntoOptions(ChatOptions options, List<ToolCallback> dynamicTools) {
        try {
            // 尝试通过 getTools 方法获取现有工具
            Method getToolsMethod = findMethod(options.getClass(), "getTools");
            List<ToolCallback> existingTools = null;
            if (getToolsMethod != null) {
                existingTools = (List<ToolCallback>) getToolsMethod.invoke(options);
            }

            // 合并工具
            List<ToolCallback> mergedTools = new ArrayList<>();
            if (existingTools != null) {
                mergedTools.addAll(existingTools);
            }
            mergedTools.addAll(dynamicTools);

            // 尝试通过 setTools 方法设置
            Method setToolsMethod = findMethod(options.getClass(), "setTools", List.class);
            if (setToolsMethod != null) {
                setToolsMethod.invoke(options, mergedTools);
                return;
            }

            // 尝试反射设置 tools 字段
            Field toolsField = findField(options.getClass(), "tools");
            if (toolsField != null) {
                toolsField.setAccessible(true);
                toolsField.set(options, mergedTools);
                return;
            }

            log.warn("[{}] No setTools method or tools field found, cannot merge tools", getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("[{}] Failed to merge tools into existing ChatOptions: {}", getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 合并工具到 DashScopeChatOptions
     * DashScope 要求 tools 必须是 List<DashScopeApiSpec.FunctionTool>
     */
    protected void mergeToolsIntoDashScopeOptions(ChatOptions options, List<ToolCallback> dynamicTools) {
        try {
            // 使用 DashScopeOptionsUtils 转换工具
            List<Object> dashScopeTools = DashScopeOptionsUtils.convertToolCallbacksToDashScopeTools(dynamicTools);

            // 获取现有工具
            Method getToolsMethod = findMethod(options.getClass(), "getTools");
            List<Object> existingTools = null;
            if (getToolsMethod != null) {
                existingTools = (List<Object>) getToolsMethod.invoke(options);
            }

            // 合并工具
            List<Object> mergedTools = new ArrayList<>();
            if (existingTools != null) {
                mergedTools.addAll(existingTools);
            }
            mergedTools.addAll(dashScopeTools);

            // 设置回去
            Method setToolsMethod = findMethod(options.getClass(), "setTools", List.class);
            if (setToolsMethod != null) {
                setToolsMethod.invoke(options, mergedTools);
                return;
            }

            // 反射设置
            Field toolsField = findField(options.getClass(), "tools");
            if (toolsField != null) {
                toolsField.setAccessible(true);
                toolsField.set(options, mergedTools);
                return;
            }

            log.warn("[{}] No setTools method found for DashScopeChatOptions", getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("[{}] Failed to merge tools into DashScopeChatOptions: {}", getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 创建默认 ChatOptions
     */
    protected ChatOptions createDefaultChatOptions() {
        try {
            return ChatOptions.builder()
                    .build();
        } catch (Exception e) {
            log.error("{} 创建默认 ChatOptions 失败", getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 检查是否是 DashScopeChatModel
     */
    protected boolean isDashScopeChatModel(Object target) {
        return target != null && target.getClass().getName().contains("DashScopeChatModel");
    }

    /**
     * 检查是否是 DashScopeChatOptions
     */
    protected boolean isDashScopeChatOptions(ChatOptions options) {
        return options != null && options.getClass().getName().contains("DashScopeChatOptions");
    }

    /**
     * 查找方法
     */
    protected Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name)) {
                    if (paramTypes.length == 0 || method.getParameterCount() == paramTypes.length) {
                        return method;
                    }
                }
            }
            return null;
        }
    }

    /**
     * 查找字段
     */
    protected Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superClazz = clazz.getSuperclass();
            if (superClazz != null) {
                return findField(superClazz, name);
            }
            return null;
        }
    }

    /**
     * 获取 McpSkillManager 供子类使用
     */
    protected McpSkillManager getMcpSkillManager() {
        return mcpSkillManager;
    }
}
