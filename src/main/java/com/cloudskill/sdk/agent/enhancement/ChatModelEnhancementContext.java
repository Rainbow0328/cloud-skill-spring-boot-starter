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
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ChatModel 动态工具合并过程的上下文，可在 {@link AbstractDynamicSkillToolEnhancement} 的钩子中读写。
 */
public final class ChatModelEnhancementContext {

    private final ProceedingJoinPoint joinPoint;
    private Prompt originalPrompt;
    private List<ToolCallback> manualToolCallbacks;
    private List<ToolCallback> dynamicToolCallbacks;
    private List<ToolCallback> mergedToolCallbacks;

    public ChatModelEnhancementContext(ProceedingJoinPoint joinPoint, Prompt originalPrompt) {
        this.joinPoint = Objects.requireNonNull(joinPoint, "joinPoint");
        this.originalPrompt = Objects.requireNonNull(originalPrompt, "originalPrompt");
        this.manualToolCallbacks = new ArrayList<>();
        this.dynamicToolCallbacks = new ArrayList<>();
        this.mergedToolCallbacks = new ArrayList<>();
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    public Prompt getOriginalPrompt() {
        return originalPrompt;
    }

    public void setOriginalPrompt(Prompt originalPrompt) {
        this.originalPrompt = Objects.requireNonNull(originalPrompt, "originalPrompt");
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

    /** 合并完成后仍可由 {@link AbstractDynamicSkillToolEnhancement#onAfterChatModelToolsMerged} 替换. */
    public void replaceMergedToolCallbacks(List<ToolCallback> merged) {
        setMergedToolCallbacksInternal(merged);
    }
}
