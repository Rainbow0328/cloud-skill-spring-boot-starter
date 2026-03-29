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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

/**
 * ChatModel 工具注入器
 * 通过 AOP 拦截 ChatModel.call(Prompt) 自动注入动态技能
 *
 * @author Cloud Skill Team
 * @version 1.0.0
 */
public class ChatModelToolInjector extends AbstractToolInjector implements MethodBeforeAdvice {

    private static final Logger log = LoggerFactory.getLogger(ChatModelToolInjector.class);

    public ChatModelToolInjector(McpSkillManager mcpSkillManager,
                                com.cloudskill.sdk.core.SkillCache skillCache,
                                com.cloudskill.sdk.core.CloudSkillClient cloudSkillClient,
                                com.cloudskill.sdk.listener.RedisSkillChangeListener skillChangeListener) {
        super(mcpSkillManager, skillCache, cloudSkillClient, skillChangeListener);
    }

    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        if (!(target instanceof ChatModel)) {
            return;
        }

        Prompt prompt = findPrompt(args);
        if (prompt == null) {
            return;
        }

        String methodName = method.getName();
        String returnTypeName = method.getReturnType().getName();

        // 只匹配 call 方法，返回类型是 ChatResponse
        if (!"call".equals(methodName) || !returnTypeName.contains("ChatResponse")) {
            return;
        }

        log.debug("[ChatModelToolInjector] Intercepted ChatModel.call(), prompt: {}",
                prompt.getContents().length() > 100
                        ? prompt.getContents().substring(0, 100) + "..."
                        : prompt.getContents());

        injectTools(prompt, target, args);
    }

    /**
     * 从方法参数中找到 Prompt
     */
    private Prompt findPrompt(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Prompt) {
                return (Prompt) arg;
            }
        }
        return null;
    }
}
