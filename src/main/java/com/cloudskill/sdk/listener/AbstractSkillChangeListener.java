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
 * distributed under an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the the License.
 */
package com.cloudskill.sdk.listener;

import com.cloudskill.sdk.agent.ToolCache;
import com.cloudskill.sdk.cache.GlobalCacheVersion;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.model.SkillChangeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 技能变更监听器抽象基类
 * 模板方法模式，统一处理技能变更消息，方便扩展不同消息队列（Redis、RabbitMQ、Kafka 等）
 *
 * 子类只需要：
 * 1. 实现消息接收
 * 2. 调用 {@link #handleMessage(SkillChangeMessage)} 处理消息
 * 3. 实现 {@link #subscribe()} 订阅方法
 *
 * @author Cloud Skill Team
 * @version 1.0.0
 */
public abstract class AbstractSkillChangeListener {

    private static final Logger log = LoggerFactory.getLogger(AbstractSkillChangeListener.class);

    protected final ObjectMapper objectMapper;
    protected final SkillCache skillCache;
    protected final CloudSkillClient cloudSkillClient;
    protected final ToolCache toolCache;
    protected final Environment environment;
    protected final ScheduledExecutorService scheduledExecutor;

    /**
     * 本地时间戳（用于版本连续性校验）
     */
    protected volatile long localTimestamp = 0;

    public AbstractSkillChangeListener(ObjectMapper objectMapper,
                                      SkillCache skillCache,
                                      CloudSkillClient cloudSkillClient,
                                      ToolCache toolCache,
                                      Environment environment,
                                      ScheduledExecutorService scheduledExecutor) {
        this.objectMapper = objectMapper;
        this.skillCache = skillCache;
        this.cloudSkillClient = cloudSkillClient;
        this.toolCache = toolCache;
        this.environment = environment;
        this.scheduledExecutor = scheduledExecutor;
    }

    /**
     * 处理技能变更消息 - 模板方法
     * 子类接收消息后反序列化为 {@link SkillChangeMessage}，然后调用此方法
     *
     * @param message 技能变更消息
     */
    protected void handleMessage(SkillChangeMessage message) {
        try {
            log.info("Received skill change message: type={}, version={}, previousVersion={}",
                    message.getOperationType(),
                    message.getTimestamp(),
                    message.getPreviousTimestamp());

            // 版本连续性校验
            if (!validateVersionContinuity(message)) {
                log.warn("Version discontinuity detected, triggering full sync: expectedVersion={}, receivedVersion={}",
                        localTimestamp, message.getPreviousTimestamp());
                triggerFullSync();
                return;
            }

            // 处理不同类型的消息
            handleSkillChange(message);

            // 更新本地版本号
            localTimestamp = message.getTimestamp();

        } catch (Exception e) {
            log.error("Failed to process skill change message", e);
        }
    }

    /**
     * 版本连续性校验
     */
    private boolean validateVersionContinuity(SkillChangeMessage message) {
        return message.getPreviousTimestamp() == localTimestamp;
    }

    /**
     * 处理技能变更
     */
    private void handleSkillChange(SkillChangeMessage message) {
        switch (message.getOperationType()) {
            case PROVIDER_OFFLINE:
                handleProviderOffline(message);
                break;

            case PROVIDER_RECOVERY:
                handleProviderRecovery(message);
                break;

            case PROVIDER_FULL_RELOAD:
                handleProviderFullReload(message);
                break;

            case SKILL_DELETE_BY_PROVIDER:
                handleSkillDeleteByProvider(message);
                break;

            case INFO_UPDATE:
                handleSkillUpdate(message);
                break;

            case ASSIGN:
                handleAssign(message);
                break;

            case UNASSIGN:
                handleUnassign(message);
                break;

            default:
                log.warn("Unknown operation type: {}", message.getOperationType());
        }
    }

    /**
     * 处理 Provider 下线
     */
    private void handleProviderOffline(SkillChangeMessage message) {
        String providerId = message.getProviderId();

        scheduleTask(() -> {
            // 标记该 Provider 的所有技能为不可用
            skillCache.setProviderStatus(providerId, "unavailable");
            // 原子更新双缓存，保证一致性
            synchronized (this) {
                // SkillCache已更新，批量移除该Provider的工具
                toolCache.removeSkillsByProvider(providerId);
                // 版本更新为消息携带的admin全局版本（后续需要从消息中获取admin版本）
                // 临时实现，后续会从消息体中提取admin返回的globalVersion
                long adminGlobalVersion = System.currentTimeMillis();
                GlobalCacheVersion.update(adminGlobalVersion);
            }
            log.info("Provider marked unavailable: providerId={}", providerId);
        }, 0, 5000);
    }

    /**
     * 处理 Provider 恢复
     */
    private void handleProviderRecovery(SkillChangeMessage message) {
        String providerId = message.getProviderId();

        scheduleTask(() -> {
            // 清除 Provider 的不可用标记
            skillCache.removeProviderStatus(providerId);
            // 原子更新双缓存，保证一致性
            synchronized (this) {
                // Provider 恢复需要全量刷新，因为不知道具体哪些技能恢复了
                toolCache.refreshCache();
                // 版本更新为消息携带的admin全局版本（后续需要从消息中获取admin版本）
                // 临时实现，后续会从消息体中提取admin返回的globalVersion
                long adminGlobalVersion = System.currentTimeMillis();
                GlobalCacheVersion.update(adminGlobalVersion);
            }
            log.info("Provider recovery applied: providerId={}", providerId);
        }, 0, 5000);
    }

    /**
     * 处理 Provider 全量更新
     */
    private void handleProviderFullReload(SkillChangeMessage message) {
        String providerId = message.getProviderId();
        List<String> scopes = message.getScopes();

        scheduleTask(() -> {
            // 清理该 Provider 的所有技能缓存
            skillCache.evictSkillsByProvider(providerId);
            // 原子更新双缓存，保证一致性
            synchronized (this) {
                // SkillCache已更新，刷新ToolCache
                toolCache.refreshCache();
                // 版本更新为消息携带的admin全局版本（后续需要从消息中获取admin版本）
                // 临时实现，后续会从消息体中提取admin返回的globalVersion
                long adminGlobalVersion = System.currentTimeMillis();
                GlobalCacheVersion.update(adminGlobalVersion);
            }
            log.info("Provider full-reload cache cleanup completed: providerId={}, scopes={}", providerId, scopes);
        }, 0, 5000);
    }

    /**
     * 处理按 Provider 批量删除技能
     */
    private void handleSkillDeleteByProvider(SkillChangeMessage message) {
        String providerId = message.getProviderId();

        scheduleTask(() -> {
            // 清理该 Provider 的所有技能缓存
            skillCache.evictSkillsByProvider(providerId);
            // Provider 批量删除，批量移除工具缓存
            toolCache.removeSkillsByProvider(providerId);
            log.info("Provider skills removed from cache: providerId={}", providerId);
        }, 0, 5000);
    }

    /**
     * 处理技能更新
     */
    private void handleSkillUpdate(SkillChangeMessage message) {
        com.cloudskill.sdk.model.Skill skill = message.getSkill();
        List<String> assignedServices = message.getAssignedServices();

        scheduleTask(() -> {
            // 使用服务名称进行校验
            String currentServiceName = getCurrentServiceName();
            if (assignedServices != null && isServiceMatched(currentServiceName, assignedServices)) {
                skillCache.putSkill(skill);
                // 原子更新双缓存，保证一致性
                synchronized (this) {
                    // SkillCache已更新，增量更新单个技能
                    toolCache.updateSkill(skill.getId());
                    // 全局版本号递增，所有缓存自动失效
                    // 临时使用时间戳作为admin版本，后续从消息中获取真实版本
                GlobalCacheVersion.update(System.currentTimeMillis());
                }
                log.info("Skill updated in local cache: skillId={}", skill.getId());
            }
        }, 0, 5000);
    }

    /**
     * 处理技能分配
     */
    private void handleAssign(SkillChangeMessage message) {
        com.cloudskill.sdk.model.Skill skill = message.getSkill();
        List<String> assignedServices = message.getAssignedServices();

        // 使用服务名称进行校验
        String currentServiceName = getCurrentServiceName();
        if (assignedServices == null || !isServiceMatched(currentServiceName, assignedServices)) {
            return;  // 与当前服务无关
        }

        scheduleTask(() -> {
            skillCache.putSkill(skill);
            // 原子更新双缓存，保证一致性
            synchronized (this) {
                // 增量更新单个技能
                toolCache.updateSkill(skill.getId());
                long adminGlobalVersion = System.currentTimeMillis();
                GlobalCacheVersion.update(adminGlobalVersion);
            }
            log.info("Skill assignment synced: skillId={}", skill.getId());
        }, 0, 3000);
    }

    /**
     * 处理技能取消分配
     */
    private void handleUnassign(SkillChangeMessage message) {
        Long skillId = message.getSkillId();
        String serviceName = message.getServiceName();

        // 使用服务名称进行校验
        String currentServiceName = getCurrentServiceName();
        if (!isServiceMatched(currentServiceName, List.of(serviceName))) {
            return;  // 与当前服务无关
        }

        scheduleTask(() -> {
            skillCache.removeSkill(skillId);
            // 原子更新双缓存，保证一致性
            synchronized (this) {
                // 增量移除单个技能
                toolCache.removeSkill(skillId);
                long adminGlobalVersion = System.currentTimeMillis();
                GlobalCacheVersion.update(adminGlobalVersion);
            }
            log.info("Skill unassigned: skillId={}", skillId);
        }, 0, 3000);
    }

    /**
     * 触发全量同步
     */
    private void triggerFullSync() {
        scheduleTask(() -> {
            log.info("Starting full skill sync...");

            // 清空本地缓存
            skillCache.clear();

            // 调用 Admin API 获取所有技能
            try {
                log.debug("Fetching full skill set from Admin API");
                // CloudSkillClient 会自动从 Admin 拉取所有技能，并返回全局时间戳
                Long globalTimestamp = cloudSkillClient.syncSkills();
                // syncSkills() 会强制要求返回全局时间戳，否则抛出异常
                localTimestamp = globalTimestamp;
                // 原子更新双缓存，保证一致性
                synchronized (this) {
                    // SkillCache已更新，刷新ToolCache
                    toolCache.refreshCache();
                    // 全局版本号递增，所有缓存自动失效
                    // 临时使用时间戳作为admin版本，后续从消息中获取真实版本
                GlobalCacheVersion.update(System.currentTimeMillis());
                }
                log.info("Full sync completed: skills={}, globalTimestamp={}", skillCache.getAll().size(), globalTimestamp);
            } catch (Exception e) {
                log.error("Full sync failed", e);
            }
        }, 0, 1000);
    }

    /**
     * 获取当前服务名称
     * 优先级：环境变量 SERVICE_NAME > Spring 配置 spring.application.name > 系统属性 service.name
     */
    private String getCurrentServiceName() {
        // 1. 从环境变量获取
        String serviceName = environment.getProperty("SERVICE_NAME");
        if (serviceName != null && !serviceName.isEmpty()) {
            return serviceName;
        }

        // 2. 从 Spring 配置获取（spring.application.name）
        String springAppName = environment.getProperty("spring.application.name");
        if (springAppName != null && !springAppName.isEmpty()) {
            return springAppName;
        }

        // 3. 从系统属性获取（service.name）
        return System.getProperty("service.name", "unknown");
    }

    /**
     * 检查服务名称是否匹配
     * @param currentServiceName 当前服务名称
     * @param targetServices 目标服务名称列表
     * @return 是否匹配
     */
    private boolean isServiceMatched(String currentServiceName, List<String> targetServices) {
        if (currentServiceName == null || targetServices == null || targetServices.isEmpty()) {
            return false;
        }

        // 精确匹配服务名称
        return targetServices.contains(currentServiceName);
    }

    /**
     * 调度任务（带随机退避）
     */
    protected void scheduleTask(Runnable task, long minDelay, long maxDelay) {
        if (scheduledExecutor == null) {
            // 直接执行
            task.run();
            return;
        }

        long delay = minDelay + ThreadLocalRandom.current().nextInt((int)(maxDelay - minDelay));
        scheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 订阅技能变更消息
     * 子类实现此方法完成订阅
     */
    public abstract void subscribe();

    /**
     * 设置本地时间戳（用于全量同步后更新版本）
     */
    public void setLocalTimestamp(long timestamp) {
        this.localTimestamp = timestamp;
        log.debug("Local timestamp updated: {}", timestamp);
    }

    /**
     * 获取本地时间戳
     */
    public long getLocalTimestamp() {
        return localTimestamp;
    }
}
