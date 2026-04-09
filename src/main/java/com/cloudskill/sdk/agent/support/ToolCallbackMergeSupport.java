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

import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合并用户手动注册的 {@link ToolCallback} 与代理/平台预注册的动态技能工具，并按工具名去重。
 * <p>
 * 去重策略：同名工具以<strong>手动注入</strong>为准（后序覆盖预注册），其余预注册工具按原名加入。
 */
public final class ToolCallbackMergeSupport {

    private ToolCallbackMergeSupport() {
    }

    /**
     * @param manualFirst   用户手动注入的工具（优先保留同名）
     * @param preRegistered 代理或云端预注册的动态工具
     */
    public static List<ToolCallback> mergePreferManual(
            List<ToolCallback> manualFirst,
            List<ToolCallback> preRegistered) {
        Map<String, ToolCallback> byName = new LinkedHashMap<>();
        if (preRegistered != null) {
            for (ToolCallback t : preRegistered) {
                if (t == null) {
                    continue;
                }
                String key = toolName(t);
                byName.putIfAbsent(key, t);
            }
        }
        if (manualFirst != null) {
            for (ToolCallback t : manualFirst) {
                if (t == null) {
                    continue;
                }
                byName.put(toolName(t), t);
            }
        }
        return new ArrayList<>(byName.values());
    }

    public static String toolName(ToolCallback callback) {
        if (callback == null) {
            return "";
        }
        try {
            if (callback.getToolDefinition() != null && callback.getToolDefinition().name() != null) {
                return callback.getToolDefinition().name();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "tool-" + System.identityHashCode(callback);
    }
}
