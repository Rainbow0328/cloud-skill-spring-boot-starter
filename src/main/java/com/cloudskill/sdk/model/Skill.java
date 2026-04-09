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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 技能模型
 * 忽略未知字段以支持版本兼容性
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private String description;
    private String category;
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
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 新增扩展字段
    private String protocol; // 协议类型：HTTP/GRPC/MQ/CUSTOM 等，默认 HTTP
    private String contentType; // 请求内容类型：application/json 等，默认 application/json
    private Map<String, Object> metadata; // 扩展元数据，存储自定义配置
    private Boolean requireAuth; // 是否需要额外身份验证，默认 false
    private Integer rateLimit; // 技能级限流配置（QPS），默认不限制
    private List<String> requiredContext; // 执行需要的上下文参数列表
    private Map<String, String> parameterMapping; // 参数映射规则，支持上下文参数自动填充
    private String errorHandlingStrategy; // 错误处理策略：FAIL_FAST/RETRY/FALLBACK/IGNORE
    private String source; // 技能来源：swagger/springmvc/manual 等
    private String sourcePath; // 技能来源路径（如 URL 路径、类名。方法名等）

    // Getter methods
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setter methods
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
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
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // 新增字段的Getter/Setter
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Boolean getRequireAuth() { return requireAuth; }
    public void setRequireAuth(Boolean requireAuth) { this.requireAuth = requireAuth; }
    public Integer getRateLimit() { return rateLimit; }
    public void setRateLimit(Integer rateLimit) { this.rateLimit = rateLimit; }
    public List<String> getRequiredContext() { return requiredContext; }
    public void setRequiredContext(List<String> requiredContext) { this.requiredContext = requiredContext; }
    public Map<String, String> getParameterMapping() { return parameterMapping; }
    public void setParameterMapping(Map<String, String> parameterMapping) { this.parameterMapping = parameterMapping; }
    public String getErrorHandlingStrategy() { return errorHandlingStrategy; }
    public void setErrorHandlingStrategy(String errorHandlingStrategy) { this.errorHandlingStrategy = errorHandlingStrategy; }
    
    // source 和 sourcePath 的 Getter/Setter
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
}
