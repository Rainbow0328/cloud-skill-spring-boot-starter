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
package com.cloudskill.sdk.core;

import com.cloudskill.sdk.model.SkillChangeEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallRequest;
import com.cloudskill.sdk.model.SkillCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.cloudskill.sdk.core.metrics.SkillMetrics;
import com.cloudskill.sdk.core.resilience.ResilienceManager;
import com.cloudskill.sdk.core.validator.ParameterValidator;
import com.cloudskill.sdk.spi.SkillExecutionHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.Collections;
import java.util.function.Consumer;

public class CloudSkillClient {
    
    private static final Logger log = LoggerFactory.getLogger(CloudSkillClient.class);
    private final CloudSkillProperties properties;
    
    private final ObjectMapper objectMapper;
    private final SkillCache skillCache;
    private final ParameterValidator parameterValidator; // 参数校验器
    
    @Autowired(required = false)
    private SkillMetrics skillMetrics; // 指标收集器
    
    @Autowired(required = false)
    private List<SkillExecutionHook> executionHooks = Collections.emptyList(); // 执行钩子扩展点
    
    private ResilienceManager resilienceManager; // 熔断降级管理器
    private ScheduledExecutorService executorService; // 异步调用和定时任务线程池
    private long lastSyncTime = 0;
    private List<Consumer<SkillChangeEvent>> skillChangeListeners = new ArrayList<>(); // 技能变更监听器列表
    
    public CloudSkillClient(CloudSkillProperties properties) {
        this(properties, null);
    }
    
    public CloudSkillClient(CloudSkillProperties properties, SkillCache skillCache) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        // 确保 ObjectMapper 使用 UTF-8 编码
//        this.objectMapper.setFactory(
//            this.objectMapper.getFactory()
//                .setCharacterEncoding("UTF-8")
//        );
        this.skillCache = skillCache != null ? skillCache : new SkillCache(properties.getCacheExpireTime());
        // 初始化参数校验器，传入配置
        this.parameterValidator = new ParameterValidator(
                objectMapper,
                properties.getValidation().isEnableRequestValidation(),
                properties.getValidation().isEnableResponseValidation(),
                properties.getValidation().isFailOnError()
        );
        this.resilienceManager = initResilienceManager(properties); // 反射初始化熔断降级管理器（可选依赖）
        
        // 初始化定时任务线程池
        this.executorService = Executors.newScheduledThreadPool(3, r -> {
            Thread thread = new Thread(r);
            thread.setName("cloud-skill-scheduler-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
        
        // 对扩展点按优先级排序
        AnnotationAwareOrderComparator.sort(executionHooks);
        
        if (properties.isAutoSync()) {
            // 延迟 5 秒执行技能同步，确保 Admin 已完全启动
            executorService.schedule(() -> {
                try {
                    syncSkills();
                } catch (Exception e) {
                    log.warn("首次技能同步失败，将在下次定时同步时重试：{}", e.getMessage());
                }
            }, 5, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 同步所有技能
     * @return 全局时间戳
     * @throws IllegalStateException 如果响应中不包含全局时间戳
     */
    public Long syncSkills() {
        try {
            log.debug("Syncing skills from MCP server...");
            String url = properties.getServerUrl() + "/cloud-skill/v1/sdk/skills";
            
            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .header("X-IP-Address", properties.getServiceIp())
                    .timeout(properties.getCallTimeout())
                    .execute();
            
            if (response.isOk()) {
                String body = response.body();
                
                // 解析响应，必须是包含 globalTimestamp 的对象格式
                JsonNode jsonNode = objectMapper.readTree(body);
                
                if (!jsonNode.isObject() || !jsonNode.has("skills")) {
                    throw new IllegalStateException("Invalid response format: expected {\"skills\": [...], \"globalTimestamp\": ...}, but got: " + body);
                }
                
                List<Skill> skills = objectMapper.convertValue(jsonNode.get("skills"), new TypeReference<List<Skill>>() {});
                
                // 必须包含全局时间戳
                if (!jsonNode.has("globalTimestamp")) {
                    throw new IllegalStateException("Missing globalTimestamp in response");
                }
                
                Long globalTimestamp = jsonNode.get("globalTimestamp").asLong();
                
                // 清空缓存，只保留最新的有权限的技能
                skillCache.clear();
                skills.forEach(skill -> {
                    if (skill.getEnabled()) {
                        skillCache.put(skill.getId(), skill);
                    }
                });
                
                // 更新本地时间戳为全局时间戳
                lastSyncTime = globalTimestamp;
                log.info("Successfully synced {} skills, globalTimestamp: {}", skills.size(), globalTimestamp);
                
                return globalTimestamp;
            } else {
                log.error("Failed to sync skills, status: {}, message: {}", response.getStatus(), response.body());
                // 同步失败时不清空缓存，保留旧数据
                throw new IllegalStateException("Failed to sync skills: HTTP " + response.getStatus());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error syncing skills", e);
            throw new IllegalStateException("Error syncing skills: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取服务端全局时间戳
     * @return 全局时间戳，如果获取失败返回null
     */
    public Long getGlobalTimestamp() {
        try {
            String url = properties.getServerUrl() + "/cloud-skill/v1/sdk/global-timestamp";
            
            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .timeout(properties.getCallTimeout())
                    .execute();
            
            if (response.isOk()) {
                String body = response.body();
                JsonNode jsonNode = objectMapper.readTree(body);
                
                if (jsonNode.has("globalTimestamp")) {
                    return jsonNode.get("globalTimestamp").asLong();
                }
            }
            
            log.warn("Failed to get global timestamp, status: {}", response.getStatus());
            return null;
        } catch (Exception e) {
            log.error("Error getting global timestamp", e);
            return null;
        }
    }
    
    /**
     * 获取所有可用技能
     */
    public List<Skill> getAllSkills() {
        // 如果缓存为空或者距离上次同步超过5分钟，强制全量同步
        long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
        if (!properties.isEnableLocalCache() || skillCache.isEmpty() || lastSyncTime < fiveMinutesAgo) {
            try {
                syncSkills();
            } catch (Exception e) {
                log.error("Failed to sync skills when getting all skills", e);
                // 同步失败时如果缓存有数据，继续使用缓存数据
            }
        }
        
        // 过滤掉没有权限的技能
        List<Skill> allSkills = new ArrayList<>(skillCache.getAll().values());
        List<Skill> allowedSkills = allSkills.stream()
                .filter(this::hasPermission)
                .collect(java.util.stream.Collectors.toList());
        
        if (log.isDebugEnabled()) {
            log.debug("Get all skills: cache has {}, allowed {}", allSkills.size(), allowedSkills.size());
            for (Skill skill : allowedSkills) {
                log.debug("  - Allowed skill: [{}] {} (public: {})", 
                        skill.getId(), skill.getName(), skill.getIsPublic());
            }
        }
        
        return allowedSkills;
    }
    
    /**
     * 调用技能
     */
    public SkillCallResult invokeSkill(String skillId, Map<String, Object> parameters) {
        log.info("准备调用技能：{}", skillId);
        log.info("大模型传递的参数 params: size={}, params={}", 
            parameters != null ? parameters.size() : "null", 
            parameters != null ? parameters : "null");
            
        // 构建请求对象
        SkillCallRequest request = new SkillCallRequest();
        request.setParameters(parameters);
        
        if (request.getParameters() == null) {
            log.warn("request.parameters is null after setParameters!");
        } else {
            log.info("request.parameters set successfully: size={}", request.getParameters().size());
        }
                
        // 直接调用，不从缓存获取技能信息，由 admin 平台返回结果
        return invokeSkill(skillId, request);
    }
    
    /**
     * 调用技能（内部方法）
     */
    public SkillCallResult invokeSkill(String skillId, SkillCallRequest request) {
        log.info("发起技能调用：skillId={}", skillId);
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorCode = null;
        SkillCallResult result = null;
    
        try {
            // 1. 从缓存获取技能信息（包含 endpoint）
            Skill skill = getSkillFromCache(skillId);
            if (skill == null) {
                log.error("技能不存在或不在缓存中：skillId={}", skillId);
                throw new RuntimeException("Skill not found: " + skillId);
            }
            
            // 2. 检查技能是否启用
            if (!Boolean.TRUE.equals(skill.getEnabled())) {
                throw new RuntimeException("Skill is disabled: " + skillId);
            }
            
            // 3. 检查权限（使用现有的 hasPermission() 方法）
            if (!Boolean.TRUE.equals(skill.getIsPublic())) {
                // 私有技能：检查是否有权限
                if (!hasPermission(skill)) {
                    log.error("无权限调用技能：skillId={}", skillId);
                    throw new RuntimeException("No permission to invoke skill: " + skillId);
                }
            }
            
            // 4. 直接调用 Provider endpoint（不再经过 Admin）
            return executeProviderCall(skill, request);
            
        } catch (Exception e) {
            log.error("技能调用失败：skillId={}", skillId, e);
            success = false;
            errorCode = e.getMessage();
            
            result = new SkillCallResult();
            result.setSuccess(false);
            result.setCode(500);
            result.setMessage("Skill invoke failed: " + e.getMessage());
            result.setTimestamp(LocalDateTime.now());
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
    
            // 记录指标
            if (skillMetrics != null && skillMetrics.isMetricsEnabled()) {
                skillMetrics.recordSkillInvoke(skillId, success, duration, errorCode);
            }
        }
    }
    
    /**
     * 直接调用 Provider 的 endpoint
     */
    private SkillCallResult executeProviderCall(Skill skill, SkillCallRequest request) {
        // 1. 从 skill 中获取 endpoint
        String providerUrl = skill.getEndpoint();
        if (providerUrl == null || providerUrl.isEmpty()) {
            throw new RuntimeException("Provider endpoint is null or empty: skillId=" + skill.getId());
        }
        
        log.debug("直接调用 Provider: skillId={}, endpoint={}", skill.getId(), providerUrl);
        
        // 2. 根据 skill 的 HttpMethod 构建对应的 HTTP 请求
        String httpMethod = skill.getHttpMethod() != null ? skill.getHttpMethod().toUpperCase() : "POST";
        log.info("使用 HTTP 方法：{} 调用 Provider: skillId={}, url={}", httpMethod, skill.getId(), providerUrl);
        
        // 对于 GET/DELETE，如果有参数，先手动拼接到 URL query
        StringBuilder urlBuilder = new StringBuilder(providerUrl);
        if (("GET".equals(httpMethod) || "DELETE".equals(httpMethod)) 
                && request.getParameters() != null && !request.getParameters().isEmpty()) {
            boolean first = !providerUrl.contains("?");
            for (var entry : request.getParameters().entrySet()) {
                if (first) {
                    urlBuilder.append("?");
                    first = false;
                } else {
                    urlBuilder.append("&");
                }
                urlBuilder.append(java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8));
                urlBuilder.append("=");
                urlBuilder.append(java.net.URLEncoder.encode(String.valueOf(entry.getValue()), java.nio.charset.StandardCharsets.UTF_8));
            }
            providerUrl = urlBuilder.toString();
            log.info("GET/DELETE 参数拼接完成：{}", providerUrl);
        }
        
        // 构建基础请求（参数已经拼接到 URL，不再需要 body）
        HttpRequest httpRequest;
        switch (httpMethod) {
            case "GET":
                httpRequest = HttpRequest.get(providerUrl)
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .header("X-IP-Address", properties.getServiceIp())
                    .timeout(skill.getTimeout() != null ? skill.getTimeout() : properties.getCallTimeout());
                break;
            case "PUT":
                httpRequest = HttpRequest.put(providerUrl)
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .header("X-IP-Address", properties.getServiceIp())
                    .timeout(skill.getTimeout() != null ? skill.getTimeout() : properties.getCallTimeout());
                break;
            case "DELETE":
                httpRequest = HttpRequest.delete(providerUrl)
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .header("X-IP-Address", properties.getServiceIp())
                    .timeout(skill.getTimeout() != null ? skill.getTimeout() : properties.getCallTimeout());
                break;
            case "POST":
            default:
                httpRequest = HttpRequest.post(providerUrl)
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .header("X-IP-Address", properties.getServiceIp())
                    .timeout(skill.getTimeout() != null ? skill.getTimeout() : properties.getCallTimeout());
                break;
        }
        
        // 3. 添加自定义 headers（如果有）
        if (skill.getHeaders() != null) {
            skill.getHeaders().forEach(httpRequest::header);
        }
        
        // 4. 添加请求参数（根据 HTTP 方法决定参数位置）
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            if (!("GET".equals(httpMethod) || "DELETE".equals(httpMethod))) {
                // POST/PUT 请求：参数放在 body 中
                // GET/DELETE 已经在前面手动拼接到 URL 了
                try {
                    String paramsJson = objectMapper.writeValueAsString(request.getParameters());
                    httpRequest.body(paramsJson);
                    log.debug("POST/PUT 请求参数（body）：{}", paramsJson);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize parameters", e);
                }
            }
            // GET/DELETE：参数已经拼接到 URL，不需要处理，也不应该有 body
        }
        
        // 5. 执行调用
        log.info("执行 Provider 调用：skillId={}, method={}, url={}", 
            skill.getId(), skill.getHttpMethod(), providerUrl);
        
        HttpResponse response = httpRequest.execute();
        
        // 6. 解析响应
        try {
            String responseBody = response.body();
            log.debug("Provider 响应：status={}, body={}", response.getStatus(), responseBody);
            
            if (response.getStatus() == 200) {
                // 成功响应
                SkillCallResult result = objectMapper.readValue(responseBody, SkillCallResult.class);
                result.setSuccess(true);
                return result;
            } else {
                // 错误响应
                log.error("Provider 调用失败：skillId={}, status={}, body={}", 
                    skill.getId(), response.getStatus(), responseBody);
                throw new RuntimeException("Provider call failed: status=" + response.getStatus());
            }
        } catch (Exception e) {
            log.error("解析 Provider 响应失败", e);
            throw new RuntimeException("Failed to parse Provider response", e);
        }
    }
    
    /**
     * 从缓存获取技能
     */
    private Skill getSkillFromCache(String skillId) {
        if (properties.isEnableLocalCache()) {
            Skill skill = skillCache.get(skillId);
            if (skill != null) {
                return skill;
            }
        }
        return null;
    }
    
    /**
     * 检查当前应用是否有权限使用该技能
     * 注意：这个方法现在只检查技能是否启用和公开，因为服务级别的权限校验由服务端完成
     */
    private boolean hasPermission(Skill skill) {
        if (!Boolean.TRUE.equals(skill.getEnabled())) {
            return false;
        }
        
        // 公开技能
        if (Boolean.TRUE.equals(skill.getIsPublic())) {
            return true;
        }
        
        // 非公开技能：服务端已经根据 t_skill_assignment 表过滤了没有权限的技能
        // 所以这里直接返回 true
        return true;
    }
    
    /**
     * 获取配置属性
     */
    public CloudSkillProperties getProperties() {
        return properties;
    }
    
    /**
     * 注册技能变更监听器
     * @param listener 技能变更事件消费者
     */
    public void registerSkillChangeListener(Consumer<SkillChangeEvent> listener) {
        skillChangeListeners.add(listener);
        log.info("Registered skill change listener: {}", listener.getClass().getName());
    }
    
    /**
     * 处理技能变更事件（由 WebSocket 或其他机制触发）
     * @param event 技能变更事件
     */
    public void handleSkillChangeEvent(SkillChangeEvent event) {
        log.info("Handling skill change event: skillId={}, changeType={}", 
                event.getSkillId(), event.getChangeType());
        
        // 通知所有监听器
        for (Consumer<SkillChangeEvent> listener : skillChangeListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.error("Error invoking skill change listener", e);
            }
        }
    }
    
    /**
     * 反射初始化 ResilienceManager（可选依赖）
     * 如果 Resilience4j 不在类路径中，返回 null
     */
    private ResilienceManager initResilienceManager(CloudSkillProperties properties) {
        try {
            Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreaker");
            return new ResilienceManager(properties);
        } catch (ClassNotFoundException e) {
            log.info("Resilience4j not found in classpath, circuit breaker and rate limiter disabled");
            return null;
        } catch (Exception e) {
            log.warn("Failed to initialize ResilienceManager: {}", e.getMessage());
            return null;
        }
    }
}
