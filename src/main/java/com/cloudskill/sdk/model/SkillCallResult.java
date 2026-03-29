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
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 技能调用结果
 * 忽略未知字段以支持版本兼容性
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillCallResult {
    private Boolean success;
    private Integer code;
    private String message;
    private Object data;
    private Long duration;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String requestId;
    
    // 新增字段
    private String skillVersion; // 实际调用的技能版本
    private String errorCode; // 标准化错误码
    private Map<String, Object> debugInfo; // 调试信息，debug模式下返回
    private Boolean fromCache; // 是否来自缓存

    // Getter methods
    public Boolean getSuccess() { return success; }
    public boolean isSuccess() { return Boolean.TRUE.equals(success); }
    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public Long getDuration() { return duration; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getRequestId() { return requestId; }
    public String getSkillVersion() { return skillVersion; }
    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getDebugInfo() { return debugInfo; }
    public Boolean getFromCache() { return fromCache; }

    // Setter methods
    public void setSuccess(Boolean success) { this.success = success; }
    public void setCode(Integer code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
    public void setData(Object data) { this.data = data; }
    public void setDuration(Long duration) { this.duration = duration; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setSkillVersion(String skillVersion) { this.skillVersion = skillVersion; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setDebugInfo(Map<String, Object> debugInfo) { this.debugInfo = debugInfo; }
    public void setFromCache(Boolean fromCache) { this.fromCache = fromCache; }
    
    // 静态方法：创建成功结果
    public static SkillCallResult success(Object data) {
        SkillCallResult result = new SkillCallResult();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("Success");
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    // 静态方法：创建错误结果
    public static SkillCallResult error(Integer code, String message) {
        SkillCallResult result = new SkillCallResult();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
