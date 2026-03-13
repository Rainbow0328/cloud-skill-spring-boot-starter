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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallRequest;
import com.cloudskill.sdk.model.SkillCallResult;
import com.cloudskill.sdk.model.SkillChangeEvent;
import com.cloudskill.sdk.registry.ServiceRegistryManager;
import com.cloudskill.sdk.websocket.CloudSkillWebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CloudSkillClient {
    
    private static final Logger log = LoggerFactory.getLogger(CloudSkillClient.class);
    private final CloudSkillProperties properties;
    
    /**
     * 获取配置属性
     */
    public CloudSkillProperties getProperties() {
        return properties;
    }
    
    private final ObjectMapper objectMapper;
    private final SkillCache skillCache;
    private CloudSkillWebSocketClient webSocketClient;
    private ServiceRegistryManager serviceRegistryManager;
    private long lastSyncTime = 0;
    
    // 技能变更监听器
    private final List<Consumer<SkillChangeEvent>> skillChangeListeners = new ArrayList<>();
    
    public CloudSkillClient(CloudSkillProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        this.skillCache = new SkillCache(properties.getCacheExpireTime());
        
        if (properties.isEnableWebSocket()) {
            initWebSocket();
        }
        
        if (properties.isAutoSync()) {
            syncSkills();
        }
        
        // 初始化服务注册
        if (properties.isEnableServiceRegistry()) {
            initServiceRegistry();
        }
    }
    
    private void initWebSocket() {
        try {
            String wsUrl = properties.getServerUrl().replace("http", "ws") + "/ws/cloud-skill/" + properties.getApiKey();
            this.webSocketClient = new CloudSkillWebSocketClient(wsUrl, this);
            this.webSocketClient.connect();
            log.info("WebSocket client initialized, connecting to: {}", wsUrl);
        } catch (Exception e) {
            log.error("Failed to initialize WebSocket client", e);
        }
    }
    
    /**
     * 同步所有技能
     */
    public void syncSkills() {
        try {
            log.debug("Syncing skills from MCP server...");
            String url = properties.getServerUrl() + "/cloud-skill/v1/skills";
            
            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", properties.getApiKey())
                    .timeout(properties.getCallTimeout())
                    .execute();
            
            if (response.isOk()) {
                List<Skill> skills = objectMapper.readValue(response.body(), new TypeReference<List<Skill>>() {});
                
                // 清空缓存，只保留最新的有权限的技能
                skillCache.clear();
                skills.forEach(skill -> {
                    if (skill.getEnabled()) {
                        skillCache.put(skill.getId(), skill);
                    }
                });
                
                lastSyncTime = System.currentTimeMillis();
                log.info("Successfully synced {} skills", skills.size());
            } else {
                log.error("Failed to sync skills, status: {}, message: {}", response.getStatus(), response.body());
                // 同步失败时不清空缓存，保留旧数据
            }
        } catch (Exception e) {
            log.error("Error syncing skills", e);
            // 同步失败时不清空缓存，保留旧数据
        }
    }
    
    /**
     * 增量同步技能更新（兼容新旧版本）
     */
    public void syncSkillUpdates() {
        try {
            log.debug("Syncing skill updates from MCP server...");
            
            // 优先使用新版增量同步接口
            try {
                syncSkillUpdatesV2();
                return;
            } catch (Exception e) {
                log.debug("New incremental sync failed, falling back to old version: {}", e.getMessage());
            }
            
            // 降级到旧版接口
            syncSkillUpdatesV1();
            
        } catch (Exception e) {
            log.error("Error syncing skill updates", e);
        }
    }
    
    /**
     * 新版增量同步（基于事件）
     */
    private void syncSkillUpdatesV2() throws Exception {
        String url = properties.getServerUrl() + "/cloud-skill/v1/skills/incremental?lastSyncTime=" + lastSyncTime;
        if (properties.getServiceName() != null) {
            url += "&serviceId=" + properties.getServiceName();
        }
        
        HttpResponse response = HttpRequest.get(url)
                .header("X-API-Key", properties.getApiKey())
                .timeout(properties.getCallTimeout())
                .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("Sync failed with status: " + response.getStatus());
        }
        
        // 解析响应
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.body());
        Long serverTime = root.get("serverTime").asLong();
        boolean hasMore = root.get("hasMore").asBoolean();
        com.fasterxml.jackson.databind.JsonNode changesNode = root.get("changes");
        
        int updateCount = 0;
        int deleteCount = 0;
        
        for (com.fasterxml.jackson.databind.JsonNode eventNode : changesNode) {
            String eventType = eventNode.get("eventType").asText();
            String skillId = eventNode.get("skillId").asText();
            Long changeTime = eventNode.get("changeTime").asLong();
            
            switch (eventType) {
                case "SKILL_ADD":
                case "SKILL_UPDATE":
                case "SKILL_ENABLE":
                case "CREATE":
                case "UPDATE":
                case "ENABLE":
                    Skill skill = objectMapper.treeToValue(eventNode.get("skillInfo"), Skill.class);
                    if (skill != null && hasPermission(skill)) {
                        skillCache.put(skillId, skill);
                        updateCount++;
                        log.debug("Skill {} updated from incremental sync", skillId);
                    }
                    break;
                case "SKILL_DELETE":
                case "SKILL_DISABLE":
                case "DELETE":
                case "DISABLE":
                    skillCache.remove(skillId);
                    deleteCount++;
                    log.debug("Skill {} removed from incremental sync", skillId);
                    break;
            }
        }
        
        lastSyncTime = serverTime;
        log.debug("Successfully synced skill updates: {} updated, {} deleted, hasMore: {}", 
                updateCount, deleteCount, hasMore);
        
        // 如果有更多变更，继续同步
        if (hasMore) {
            syncSkillUpdatesV2();
        }
    }
    
    /**
     * 旧版增量同步（兼容）
     */
    private void syncSkillUpdatesV1() throws Exception {
        String url = properties.getServerUrl() + "/cloud-skill/v1/skills/updates?lastSyncTime=" + lastSyncTime;
        
        HttpResponse response = HttpRequest.get(url)
                .header("X-API-Key", properties.getApiKey())
                .timeout(properties.getCallTimeout())
                .execute();
        
        if (response.isOk()) {
            List<Skill> updatedSkills = objectMapper.readValue(response.body(), new TypeReference<List<Skill>>() {});
            
            // 更新或添加新增/修改的技能，先校验权限
            for (Skill skill : updatedSkills) {
                if (hasPermission(skill)) {
                    skillCache.put(skill.getId(), skill);
                    log.debug("Updated skill added to cache: {}", skill.getId());
                } else {
                    skillCache.remove(skill.getId());
                    log.debug("Updated skill has no permission, removed from cache: {}", skill.getId());
                }
            }
            
            lastSyncTime = System.currentTimeMillis();
            log.debug("Successfully synced {} skill updates (old version)", updatedSkills.size());
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
        List<Skill> allSkills = skillCache.getAll();
        List<Skill> allowedSkills = allSkills.stream()
                .filter(this::hasPermission)
                .collect(java.util.stream.Collectors.toList());
        
        if (log.isDebugEnabled()) {
            log.debug("Get all skills: cache has {}, allowed {}", allSkills.size(), allowedSkills.size());
            for (Skill skill : allowedSkills) {
                log.debug("  - Allowed skill: [{}] {} (service: {}, public: {})", 
                        skill.getId(), skill.getName(), skill.getAssignedServiceName(), skill.getIsPublic());
            }
        }
        
        return allowedSkills;
    }
    
    /**
     * 根据ID获取技能
     */
    public Skill getSkill(String skillId) {
        if (properties.isEnableLocalCache()) {
            Skill skill = skillCache.get(skillId);
            if (skill != null) {
                return skill;
            }
        }
        
        try {
            String url = properties.getServerUrl() + "/cloud-skill/v1/skills/" + skillId;
            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", properties.getApiKey())
                    .timeout(properties.getCallTimeout())
                    .execute();
            
            if (response.isOk()) {
                Skill skill = objectMapper.readValue(response.body(), Skill.class);
                skillCache.put(skillId, skill);
                return skill;
            }
        } catch (Exception e) {
            log.error("Error getting skill: {}", skillId, e);
        }
        return null;
    }
    
    /**
     * 调用技能
     */
    public SkillCallResult invokeSkill(String skillId, Map<String, Object> parameters) {
        log.info("准备调用技能: {} - {}", skillId, getSkill(skillId).getName());
        SkillCallRequest request = new SkillCallRequest();
        request.setParameters(parameters);
        return invokeSkill(skillId, request);
    }
    
    /**
     * 调用技能
     */
    public SkillCallResult invokeSkill(String skillId, SkillCallRequest request) {
        log.info("服务端收到技能调用请求: skillId={}", skillId);
        try {
            String url = properties.getServerUrl() + "/cloud-skill/v1/skills/" + skillId + "/invoke";
            
            HttpRequest httpRequest = HttpRequest.post(url)
                    .header("X-API-Key", properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(request.getTimeout() != null ? request.getTimeout() : properties.getCallTimeout());
            
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(httpRequest::header);
            }
            
            if (request.getParameters() != null) {
                httpRequest.body(objectMapper.writeValueAsString(request.getParameters()));
            }
            
            int retryCount = 0;
            int maxRetries = properties.getRetryCount();
            
            while (retryCount <= maxRetries) {
                try (HttpResponse response = httpRequest.execute()) {
                    SkillCallResult result = objectMapper.readValue(response.body(), SkillCallResult.class);
                    return result;
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount > maxRetries) {
                        throw new RuntimeException("Max retry attempts exceeded", e);
                    }
                    log.warn("Skill invoke retry {}/{}: {}", retryCount, maxRetries, e.getMessage());
                    Thread.sleep(100L * retryCount);
                }
            }
        } catch (Exception e) {
            log.error("Error invoking skill: {}", skillId, e);
            SkillCallResult result = new SkillCallResult();
            result.setSuccess(false);
            result.setCode(500);
            result.setMessage("Skill invoke failed: " + e.getMessage());
            result.setTimestamp(LocalDateTime.now());
            log.info("技能调用成功: skillId={}, 耗时={}ms", skillId, result.getDuration());
            return result;
        }
        return null;
    }
    
    /**
     * 注册技能变更监听器
     */
    public void registerSkillChangeListener(Consumer<SkillChangeEvent> listener) {
        skillChangeListeners.add(listener);
    }
    
    /**
     * 移除技能变更监听器
     */
    public void removeSkillChangeListener(Consumer<SkillChangeEvent> listener) {
        skillChangeListeners.remove(listener);
    }
    
    /**
     * 通知所有监听器技能变更事件
     */
    public void notifySkillChange(SkillChangeEvent event) {
        log.debug("Notifying {} listeners of skill change event: {}", 
                skillChangeListeners.size(), event.getChangeType());
        for (Consumer<SkillChangeEvent> listener : skillChangeListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.error("Error notifying skill change listener", e);
            }
        }
    }
    
    /**
     * 处理技能变更事件
     */
    public void handleSkillChangeEvent(SkillChangeEvent event) {
        log.info("Received skill change event: {} - {}", event.getSkillId(), event.getChangeType());
        
        switch (event.getChangeType()) {
            case CREATE:
            case UPDATE:
            case ENABLE:
                if (event.getSkill() != null) {
                    Skill skill = event.getSkill();
                    // 校验权限，只有有权限的技能才加入缓存
                    if (hasPermission(skill)) {
                        skillCache.put(skill.getId(), skill);
                        log.info("Skill {} {}d and added to cache", skill.getId(), event.getChangeType());
                    } else {
                        // 没有权限，确保从缓存中移除
                        skillCache.remove(skill.getId());
                        log.info("Skill {} {}d but no permission, removed from cache", skill.getId(), event.getChangeType());
                    }
                }
                break;
            case DELETE:
            case DISABLE:
                skillCache.remove(event.getSkillId());
                log.info("Skill {} {}d and removed from cache", event.getSkillId(), event.getChangeType());
                break;
        }
        
        // 通知监听器
        notifySkillChange(event);
    }
    
    /**
     * 检查当前应用是否有权限使用该技能
     */
    private boolean hasPermission(Skill skill) {
        if (!Boolean.TRUE.equals(skill.getEnabled())) {
            return false;
        }
        
        // 公开技能
        if (Boolean.TRUE.equals(skill.getIsPublic())) {
            return true;
        }
        
        // 分配给当前服务的技能
        String currentServiceName = properties.getServiceName();
        if (skill.getAssignedServiceName() != null && currentServiceName != null 
                && skill.getAssignedServiceName().equals(currentServiceName)) {
            return true;
        }
        
        // TODO: 支持应用级别的权限校验
        return false;
    }
    
    /**
     * 获取技能缓存
     */
    public Map<String, Skill> getSkillCache() {
        return new HashMap<>(skillCache.getCache());
    }
    
    /**
     * 初始化服务注册
     */
    private void initServiceRegistry() {
        this.serviceRegistryManager = new ServiceRegistryManager(properties);
        this.serviceRegistryManager.start();
    }
    
    /**
     * 关闭客户端
     */
    public void shutdown() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        if (serviceRegistryManager != null) {
            try {
                serviceRegistryManager.destroy();
            } catch (Exception e) {
                log.warn("Failed to destroy service registry manager", e);
            }
        }
    }
    
    /**
     * 获取服务注册管理器
     */
    public ServiceRegistryManager getServiceRegistryManager() {
        return serviceRegistryManager;
    }
}
