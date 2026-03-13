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
import java.util.List;
import java.util.Map;

public class Skill {
    private String id;
    private String name;
    private String description;
    private String category;
    private List<String> tags;
    private String usageScenarios;
    private String endpoint;
    private String httpMethod;
    private String requestSchema;
    private String responseSchema;
    private List<Map<String, Object>> parameters;
    private Map<String, String> headers;
    private Integer timeout;
    private Integer retryCount;
    private Boolean enabled;
    private String version;
    private String providerId;
    private Boolean isPublic;
    
    /**
     * 分配的应用ID，为空表示所有应用可用
     */
    private String assignedAppId;
    
    /**
     * 分配的服务名称，为空表示所有服务可用
     */
    private String assignedServiceName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getter methods
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public String getUsageScenarios() { return usageScenarios; }
    public String getEndpoint() { return endpoint; }
    public String getHttpMethod() { return httpMethod; }
    public String getRequestSchema() { return requestSchema; }
    public String getResponseSchema() { return responseSchema; }
    public List<Map<String, Object>> getParameters() { return parameters; }
    public Map<String, String> getHeaders() { return headers; }
    public Integer getTimeout() { return timeout; }
    public Integer getRetryCount() { return retryCount; }
    public Boolean getEnabled() { return enabled; }
    public String getVersion() { return version; }
    public String getProviderId() { return providerId; }
    public Boolean getIsPublic() { return isPublic; }
    public String getAssignedAppId() { return assignedAppId; }
    public String getAssignedServiceName() { return assignedServiceName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setter methods
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setUsageScenarios(String usageScenarios) { this.usageScenarios = usageScenarios; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public void setRequestSchema(String requestSchema) { this.requestSchema = requestSchema; }
    public void setResponseSchema(String responseSchema) { this.responseSchema = responseSchema; }
    public void setParameters(List<Map<String, Object>> parameters) { this.parameters = parameters; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setVersion(String version) { this.version = version; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public void setAssignedAppId(String assignedAppId) { this.assignedAppId = assignedAppId; }
    public void setAssignedServiceName(String assignedServiceName) { this.assignedServiceName = assignedServiceName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
