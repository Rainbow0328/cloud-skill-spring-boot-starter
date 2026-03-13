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

import java.util.Map;

public class SkillCallRequest {
    private Map<String, Object> parameters;
    private Map<String, String> headers;
    private Integer timeout;
    private Boolean async = false;

    // Getter methods
    public Map<String, Object> getParameters() { return parameters; }
    public Map<String, String> getHeaders() { return headers; }
    public Integer getTimeout() { return timeout; }
    public Boolean getAsync() { return async; }

    // Setter methods
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public void setAsync(Boolean async) { this.async = async; }
}
