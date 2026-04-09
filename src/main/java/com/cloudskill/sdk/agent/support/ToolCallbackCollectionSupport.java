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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从 Prompt / Builder 等对象上尽力提取已存在的 {@link ToolCallback}，供合并去重使用。
 */
public final class ToolCallbackCollectionSupport {

    private ToolCallbackCollectionSupport() {
    }

    public static List<ToolCallback> extractFromPrompt(org.springframework.ai.chat.prompt.Prompt prompt) {
        if (prompt == null) {
            return Collections.emptyList();
        }
        List<ToolCallback> out = new ArrayList<>();
        Class<?> clazz = prompt.getClass();
        while (clazz != null && clazz != Object.class) {
            for (String name : new String[]{"tools", "toolCallbacks", "toolDefinitions"}) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    addFromFieldValue(out, f.get(prompt));
                    return out;
                } catch (NoSuchFieldException ignored) {
                    // next
                } catch (ReflectiveOperationException e) {
                    return out;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return out;
    }

    /**
     * 从任意 Builder（ChatClient、Graph Agent 等）上反射收集名称含 tool 的字段中的 ToolCallback。
     */
    public static List<ToolCallback> extractFromBuilderLike(Object builder) {
        if (builder == null) {
            return Collections.emptyList();
        }
        List<ToolCallback> out = new ArrayList<>();
        Class<?> clazz = builder.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                String n = f.getName().toLowerCase();
                if (!n.contains("tool")) {
                    continue;
                }
                f.setAccessible(true);
                try {
                    addFromFieldValue(out, f.get(builder));
                } catch (ReflectiveOperationException ignored) {
                    // skip field
                }
            }
            clazz = clazz.getSuperclass();
        }
        return out;
    }

    public static List<ToolCallback> asList(ToolCallback[] arr) {
        if (arr == null || arr.length == 0) {
            return Collections.emptyList();
        }
        List<ToolCallback> out = new ArrayList<>(arr.length);
        Collections.addAll(out, arr);
        return out;
    }

    private static void addFromFieldValue(List<ToolCallback> out, Object v) {
        if (v == null) {
            return;
        }
        if (v instanceof List<?>) {
            for (Object o : (List<?>) v) {
                if (o instanceof ToolCallback) {
                    out.add((ToolCallback) o);
                }
            }
            return;
        }
        if (v.getClass().isArray() && ToolCallback.class.isAssignableFrom(v.getClass().getComponentType())) {
            for (ToolCallback t : (ToolCallback[]) v) {
                if (t != null) {
                    out.add(t);
                }
            }
        }
    }
}
