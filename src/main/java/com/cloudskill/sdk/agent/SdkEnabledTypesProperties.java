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
package com.cloudskill.sdk.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SDK 启用类型配置属性
 * 支持多种配置方式：
 * 1. all - 启用所有
 * 2. none - 不启用任何
 * 3. 单个类型 - chat-model
 * 4. 逗号分隔多个 - chat-model,chat-client
 * 5. 列表形式 - [chat-model, agent]
 * 
 * @author Cloud Skill Team
 * @version 1.0.0
 */
@ConfigurationProperties(prefix = "cloud.skill")
public class SdkEnabledTypesProperties {
    
    /**
     * 启用的类型列表
     * 支持特殊值：all, none
     * 支持逗号分隔：chat-model,chat-client
     * 支持列表：[chat-model, agent]
     */
    private List<String> enabledTypes = new ArrayList<>();
    
    /**
     * 解析启用的类型集合
     */
    public Set<String> parseEnabledTypes() {
        if (enabledTypes == null || enabledTypes.isEmpty()) {
            return new HashSet<>();
        }
        
        // 扁平化所有配置项（处理逗号分隔的情况）
        Set<String> result = enabledTypes.stream()
            .flatMap(type -> {
                if (type == null || type.trim().isEmpty()) {
                    return Arrays.stream(new String[0]);
                }
                // 按逗号分割
                return Arrays.stream(type.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty());
            })
            .collect(Collectors.toSet());
        
        // 检查特殊值
        if (result.contains("all")) {
            return getAllTypes();
        }
        
        if (result.contains("none")) {
            return new HashSet<>();
        }
        
        return result;
    }
    
    /**
     * 获取所有支持的类型
     */
    public static Set<String> getAllTypes() {
        return new HashSet<>(Arrays.asList(
            "chat-model",
            "chat-client",
            "agent",
            "streaming-chat-model"
        ));
    }
    
    /**
     * 检查是否启用指定类型
     */
    public boolean isEnabled(String type) {
        return parseEnabledTypes().contains(type);
    }
    
    // Getters and Setters
    public List<String> getEnabledTypes() {
        return enabledTypes;
    }
    
    public void setEnabledTypes(List<String> enabledTypes) {
        this.enabledTypes = enabledTypes;
    }
}
