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
package com.cloudskill.sdk.agent.enhancement;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ChatClient.Builder 动态工具合并过程的上下文，可在 {@link AbstractDynamicSkillToolEnhancement} 的钩子中读写。
 */
public final class ChatClientEnhancementContext {

    private final ProceedingJoinPoint joinPoint;
    private final Object builder;
    private List<ToolCallback> manualToolCallbacks;
    private List<ToolCallback> dynamicToolCallbacks;
    private List<ToolCallback> mergedToolCallbacks;

    public ChatClientEnhancementContext(ProceedingJoinPoint joinPoint, Object builder) {
        this.joinPoint = Objects.requireNonNull(joinPoint, "joinPoint");
        this.builder = Objects.requireNonNull(builder, "builder");
        this.manualToolCallbacks = new ArrayList<>();
        this.dynamicToolCallbacks = new ArrayList<>();
        this.mergedToolCallbacks = new ArrayList<>();
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    /** Spring AI {@link org.springframework.ai.chat.client.ChatClient.Builder} 实例 */
    public Object getBuilder() {
        return builder;
    }

    public List<ToolCallback> getManualToolCallbacks() {
        return manualToolCallbacks;
    }

    public void setManualToolCallbacks(List<ToolCallback> manualToolCallbacks) {
        this.manualToolCallbacks = manualToolCallbacks != null ? new ArrayList<>(manualToolCallbacks) : new ArrayList<>();
    }

    public List<ToolCallback> getDynamicToolCallbacks() {
        return dynamicToolCallbacks;
    }

    public void setDynamicToolCallbacks(List<ToolCallback> dynamicToolCallbacks) {
        this.dynamicToolCallbacks = dynamicToolCallbacks != null ? new ArrayList<>(dynamicToolCallbacks) : new ArrayList<>();
    }

    public List<ToolCallback> getMergedToolCallbacks() {
        return Collections.unmodifiableList(mergedToolCallbacks);
    }

    void setMergedToolCallbacksInternal(List<ToolCallback> merged) {
        this.mergedToolCallbacks = merged != null ? new ArrayList<>(merged) : new ArrayList<>();
    }

    public void replaceMergedToolCallbacks(List<ToolCallback> merged) {
        setMergedToolCallbacksInternal(merged);
    }
}
