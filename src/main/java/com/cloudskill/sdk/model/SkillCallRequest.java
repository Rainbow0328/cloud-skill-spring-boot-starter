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
package com.cloudskill.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * 技能调用请求
 * 忽略未知字段以支持版本兼容性
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillCallRequest {
    private String skillId; // 技能ID
    private Map<String, Object> parameters;
    private Map<String, String> headers;
    private Integer timeout;
    private Boolean async = false;
    
    // 新增字段
    private String requestId; // 全局请求ID，用于链路追踪
    private Map<String, Object> context; // 上下文参数，透传到技能执行端
    private Boolean skipCache; // 是否跳过缓存，强制调用最新结果
    private String fallbackValue; // 降级返回值，调用失败时使用

    // Getter methods
    public String getSkillId() { return skillId; }
    public Map<String, Object> getParameters() { return parameters; }
    public Map<String, String> getHeaders() { return headers; }
    public Integer getTimeout() { return timeout; }
    public Boolean getAsync() { return async; }
    public String getRequestId() { return requestId; }
    public Map<String, Object> getContext() { return context; }
    public Boolean getSkipCache() { return skipCache; }
    public String getFallbackValue() { return fallbackValue; }

    // Setter methods
    public void setSkillId(String skillId) { this.skillId = skillId; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public void setAsync(Boolean async) { this.async = async; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    public void setSkipCache(Boolean skipCache) { this.skipCache = skipCache; }
    public void setFallbackValue(String fallbackValue) { this.fallbackValue = fallbackValue; }
}
