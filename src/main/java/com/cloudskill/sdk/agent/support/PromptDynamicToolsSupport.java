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
package com.cloudskill.sdk.agent.support;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Spring AI 1.x：工具列表挂在 {@link ToolCallingChatOptions} 上，而非旧版三参数 Prompt 构造。
 */
public final class PromptDynamicToolsSupport {

    private PromptDynamicToolsSupport() {
    }

    /**
     * 使用合并后的工具列表构建新的 {@link Prompt}，并尽量保留原 {@link ChatOptions} 中的模型参数与工具相关字段。
     */
    public static Prompt copyPromptWithMergedToolCallbacks(Prompt original, List<ToolCallback> merged) {
        if (merged == null || merged.isEmpty()) {
            return original;
        }
        ChatOptions opts = original.getOptions();
        ToolCallingChatOptions.Builder b = ToolCallingChatOptions.builder();
        if (opts != null) {
            b.model(opts.getModel())
                    .temperature(opts.getTemperature())
                    .maxTokens(opts.getMaxTokens())
                    .frequencyPenalty(opts.getFrequencyPenalty())
                    .presencePenalty(opts.getPresencePenalty())
                    .topP(opts.getTopP())
                    .topK(opts.getTopK());
            if (opts.getStopSequences() != null) {
                b.stopSequences(opts.getStopSequences());
            }
            if (opts instanceof ToolCallingChatOptions) {
                ToolCallingChatOptions t = (ToolCallingChatOptions) opts;
                if (t.getToolNames() != null) {
                    b.toolNames(t.getToolNames());
                }
                if (t.getToolContext() != null) {
                    b.toolContext(t.getToolContext());
                }
                if (t.getInternalToolExecutionEnabled() != null) {
                    b.internalToolExecutionEnabled(t.getInternalToolExecutionEnabled());
                }
            }
        }
        b.toolCallbacks(merged);
        return new Prompt(original.getInstructions(), b.build());
    }
}
