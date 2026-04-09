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
 * Graph Agent 等构建器上动态工具合并的预留上下文（与 ChatModel/ChatClient 钩子语义一致，便于后续接入）。
 */
public final class AgentEnhancementContext {

    private final ProceedingJoinPoint joinPoint;
    private final Object agentBuilder;
    private List<ToolCallback> manualToolCallbacks;
    private List<ToolCallback> dynamicToolCallbacks;
    private List<ToolCallback> mergedToolCallbacks;

    public AgentEnhancementContext(ProceedingJoinPoint joinPoint, Object agentBuilder) {
        this.joinPoint = Objects.requireNonNull(joinPoint, "joinPoint");
        this.agentBuilder = Objects.requireNonNull(agentBuilder, "agentBuilder");
        this.manualToolCallbacks = new ArrayList<>();
        this.dynamicToolCallbacks = new ArrayList<>();
        this.mergedToolCallbacks = new ArrayList<>();
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    public Object getAgentBuilder() {
        return agentBuilder;
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
