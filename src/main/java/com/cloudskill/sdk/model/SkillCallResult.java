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

import java.time.LocalDateTime;

public class SkillCallResult {
    private Boolean success;
    private Integer code;
    private String message;
    private Object data;
    private Long duration;
    private LocalDateTime timestamp;
    private String requestId;

    // Getter methods
    public Boolean getSuccess() { return success; }
    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public Long getDuration() { return duration; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getRequestId() { return requestId; }

    // Setter methods
    public void setSuccess(Boolean success) { this.success = success; }
    public void setCode(Integer code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
    public void setData(Object data) { this.data = data; }
    public void setDuration(Long duration) { this.duration = duration; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
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
