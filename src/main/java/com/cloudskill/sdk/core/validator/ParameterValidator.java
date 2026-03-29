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
package com.cloudskill.sdk.core.validator;

import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON Schema参数校验器
 * 基于技能的requestSchema和responseSchema对输入输出进行合法性校验
 */
@Slf4j
public class ParameterValidator {
    
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final ConcurrentHashMap<String, JsonSchema> requestSchemaCache;
    private final ConcurrentHashMap<String, JsonSchema> responseSchemaCache;
    private boolean enableRequestValidation = true;
    private boolean enableResponseValidation = true;
    private boolean failOnError = true;
    
    public ParameterValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.requestSchemaCache = new ConcurrentHashMap<>();
        this.responseSchemaCache = new ConcurrentHashMap<>();
    }
    
    public ParameterValidator(ObjectMapper objectMapper, boolean enableRequestValidation, boolean enableResponseValidation, boolean failOnError) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.requestSchemaCache = new ConcurrentHashMap<>();
        this.responseSchemaCache = new ConcurrentHashMap<>();
        this.enableRequestValidation = enableRequestValidation;
        this.enableResponseValidation = enableResponseValidation;
        this.failOnError = failOnError;
    }
    
    /**
     * 校验技能调用请求参数
     * @param skill 技能信息
     * @param parameters 调用参数
     * @throws IllegalArgumentException 校验失败时抛出异常
     */
    public void validateRequest(Skill skill, Map<String, Object> parameters) {
        // 如果未启用请求校验，跳过
        if (!enableRequestValidation) {
            log.debug("Request validation is disabled, skip validation for skill {}", skill.getId());
            return;
        }
        
        // 如果没有配置Schema，跳过校验
        if (!StringUtils.hasText(skill.getRequestSchema())) {
            log.debug("Skill {} has no request schema, skip request validation", skill.getId());
            return;
        }
        
        try {
            // 获取或编译Schema
            JsonSchema schema = getOrCreateRequestSchema(skill);
            
            // 转换参数为JsonNode
            JsonNode paramNode = objectMapper.valueToTree(parameters);
            
            // 执行校验
            Set<ValidationMessage> validationMessages = schema.validate(paramNode);
            
            if (!validationMessages.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("Request validation failed for skill ")
                        .append(skill.getId())
                        .append(": ");
                for (ValidationMessage message : validationMessages) {
                    errorMsg.append(message.getMessage()).append("; ");
                }
                String errorMessage = errorMsg.toString();
                
                if (failOnError) {
                    throw new IllegalArgumentException(errorMessage);
                } else {
                    log.warn("Request validation failed but failOnError is disabled: {}", errorMessage);
                }
            } else {
                log.debug("Request validation passed for skill {}", skill.getId());
            }
            
        } catch (IllegalArgumentException e) {
            if (failOnError) {
                throw e;
            } else {
                log.warn("Request validation failed but failOnError is disabled: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to validate request for skill {}", skill.getId(), e);
            // Schema本身有问题时，警告但不阻止调用
            log.warn("Skill {} has invalid request schema, skip validation", skill.getId());
        }
    }
    
    /**
     * 校验技能调用响应结果
     * @param skill 技能信息
     * @param result 调用结果
     * @throws IllegalArgumentException 校验失败时抛出异常
     */
    public void validateResponse(Skill skill, SkillCallResult result) {
        // 如果未启用响应校验，跳过
        if (!enableResponseValidation) {
            log.debug("Response validation is disabled, skip validation for skill {}", skill.getId());
            return;
        }
        
        // 如果没有配置响应Schema，跳过校验
        if (!StringUtils.hasText(skill.getResponseSchema())) {
            log.debug("Skill {} has no response schema, skip response validation", skill.getId());
            return;
        }
        
        // 如果调用失败，不需要校验响应
        if (!result.isSuccess()) {
            log.debug("Skill {} call failed, skip response validation", skill.getId());
            return;
        }
        
        try {
            // 获取或编译响应Schema
            JsonSchema schema = getOrCreateResponseSchema(skill);
            
            // 转换结果为JsonNode
            JsonNode resultNode = objectMapper.valueToTree(result.getData());
            
            // 执行校验
            Set<ValidationMessage> validationMessages = schema.validate(resultNode);
            
            if (!validationMessages.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("Response validation failed for skill ")
                        .append(skill.getId())
                        .append(": ");
                for (ValidationMessage message : validationMessages) {
                    errorMsg.append(message.getMessage()).append("; ");
                }
                String errorMessage = errorMsg.toString();
                
                if (failOnError) {
                    throw new IllegalArgumentException(errorMessage);
                } else {
                    log.warn("Response validation failed but failOnError is disabled: {}", errorMessage);
                }
            } else {
                log.debug("Response validation passed for skill {}", skill.getId());
            }
            
        } catch (IllegalArgumentException e) {
            if (failOnError) {
                throw e;
            } else {
                log.warn("Response validation failed but failOnError is disabled: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to validate response for skill {}", skill.getId(), e);
            // Schema本身有问题时，警告但不阻止返回结果
            log.warn("Skill {} has invalid response schema, skip validation", skill.getId());
        }
    }
    
    /**
     * 兼容旧方法，保留原有接口
     * @deprecated 建议使用 {@link #validateRequest(Skill, Map)} 代替
     */
    @Deprecated
    public void validate(Skill skill, Map<String, Object> parameters) {
        validateRequest(skill, parameters);
    }
    
    /**
     * 校验参数并返回校验结果，不抛出异常
     * @param skill 技能信息
     * @param parameters 调用参数
     * @return 校验结果，null表示校验通过
     */
    public String validateQuietly(Skill skill, Map<String, Object> parameters) {
        try {
            validateRequest(skill, parameters);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
    
    /**
     * 获取或创建请求Schema实例
     */
    private JsonSchema getOrCreateRequestSchema(Skill skill) throws JsonProcessingException {
        String cacheKey = skill.getId() + ":request:" + skill.getVersion();
        JsonSchema schema = requestSchemaCache.get(cacheKey);
        
        if (schema == null) {
            JsonNode schemaNode = objectMapper.readTree(skill.getRequestSchema());
            schema = schemaFactory.getSchema(schemaNode);
            requestSchemaCache.put(cacheKey, schema);
            log.debug("Compiled request JSON schema for skill {} version {}", skill.getId(), skill.getVersion());
        }
        
        return schema;
    }
    
    /**
     * 获取或创建响应Schema实例
     */
    private JsonSchema getOrCreateResponseSchema(Skill skill) throws JsonProcessingException {
        String cacheKey = skill.getId() + ":response:" + skill.getVersion();
        JsonSchema schema = responseSchemaCache.get(cacheKey);
        
        if (schema == null) {
            JsonNode schemaNode = objectMapper.readTree(skill.getResponseSchema());
            schema = schemaFactory.getSchema(schemaNode);
            responseSchemaCache.put(cacheKey, schema);
            log.debug("Compiled response JSON schema for skill {} version {}", skill.getId(), skill.getVersion());
        }
        
        return schema;
    }
    
    /**
     * 清理缓存
     * @param skillId 技能ID，null表示清理所有缓存
     */
    public void clearCache(String skillId) {
        if (skillId == null) {
            requestSchemaCache.clear();
            responseSchemaCache.clear();
            log.info("All JSON schema caches cleared");
        } else {
            // 移除该技能的所有缓存
            requestSchemaCache.entrySet().removeIf(entry -> entry.getKey().startsWith(skillId + ":"));
            responseSchemaCache.entrySet().removeIf(entry -> entry.getKey().startsWith(skillId + ":"));
            log.info("JSON schema caches cleared for skill {}", skillId);
        }
    }
}
