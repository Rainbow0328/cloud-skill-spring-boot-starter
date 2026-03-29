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

import com.cloudskill.sdk.model.Skill;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 技能缓存
 * 使用 ConcurrentHashMap 实现，支持过期清理
 * 新增：后台定时检查更新（基于时间戳）
 */
@Slf4j
public class SkillCache {
    
    private final long expireTime; // 过期时间，单位秒
    private final long checkInterval; // 后台检查间隔，单位毫秒
    private final Map<String, CacheEntry> cache;
    private final ScheduledExecutorService scheduler;
    
    public SkillCache(long expireTime) {
        this(expireTime, 300000); // 默认 5 分钟检查一次
    }
    
    public SkillCache(long expireTime, long checkInterval) {
        this.expireTime = expireTime;
        this.checkInterval = checkInterval;
        this.cache = new ConcurrentHashMap<>();
        
        // 启动定时清理线程
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("cloud-skill-cache-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        
        // 每分钟清理一次过期缓存
        this.scheduler.scheduleAtFixedRate(this::cleanExpired, 1, 1, TimeUnit.MINUTES);
        
        log.info("Skill cache initialized, expireTime: {}s, checkInterval: {}ms", expireTime, checkInterval);
    }
    
    /**
     * 获取技能
     */
    public Skill get(String skillId) {
        CacheEntry entry = cache.get(skillId);
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() - entry.timestamp > expireTime * 1000) {
            cache.remove(skillId);
            return null;
        }
        
        return entry.skill;
    }
    
    /**
     * 存入技能（带时间戳）
     */
    public void put(String skillId, Skill skill, Long updateTimestamp) {
        cache.put(skillId, new CacheEntry(skill, System.currentTimeMillis(), updateTimestamp));
    }
    
    /**
     * 存入技能
     */
    public void put(String skillId, Skill skill) {
        cache.put(skillId, new CacheEntry(skill, System.currentTimeMillis()));
    }
    
    /**
     * 移除技能
     */
    public void remove(String skillId) {
        cache.remove(skillId);
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 缓存是否为空
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }
    
    /**
     * 获取所有缓存的技能
     */
    public Map<String, Skill> getAll() {
        return cache.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().skill
                ));
    }
    
    /**
     * 获取原始缓存（公共访问）
     */
    public Map<String, CacheEntry> getCache() {
        return cache;
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanExpired() {
        try {
            long now = System.currentTimeMillis();
            int beforeSize = cache.size();
            
            cache.entrySet().removeIf(entry -> 
                    now - entry.getValue().timestamp > expireTime * 1000);
            
            int afterSize = cache.size();
            if (beforeSize != afterSize) {
                log.debug("Cleaned {} expired skill cache entries, remaining: {}", 
                        beforeSize - afterSize, afterSize);
            }
        } catch (Exception e) {
            log.warn("Failed to clean expired cache", e);
        }
    }
    
    /**
     * 关闭缓存
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 缓存条目（公共访问）
     */
    public static class CacheEntry {
        private final Skill skill;
        public final long timestamp; // 本地缓存时间
        public Long updateTimestamp; // Redis 中的更新时间戳
        
        public CacheEntry(Skill skill, long timestamp) {
            this.skill = skill;
            this.timestamp = timestamp;
        }
        
        public CacheEntry(Skill skill, long timestamp, Long updateTimestamp) {
            this.skill = skill;
            this.timestamp = timestamp;
            this.updateTimestamp = updateTimestamp;
        }
    }
    
    /**
     * 更新缓存条目的时间戳
     */
    public void updateTimestamp(String skillId, Long updateTimestamp) {
        CacheEntry entry = cache.get(skillId);
        if (entry != null) {
            entry.updateTimestamp = updateTimestamp;
        }
    }
    
    /**
     * 获取缓存条目的更新时间戳
     */
    public Long getUpdateTimestamp(String skillId) {
        CacheEntry entry = cache.get(skillId);
        return entry != null ? entry.updateTimestamp : null;
    }
    
    // ==================== Provider 相关方法 ====================
    
    /**
     * Provider 状态缓存
     */
    private final Map<String, String> providerStatusCache = new ConcurrentHashMap<>();
    
    /**
     * 设置 Provider 状态
     */
    public void setProviderStatus(String providerId, String status) {
        providerStatusCache.put(providerId, status);
        log.info("设置 Provider 状态：providerId={}, status={}", providerId, status);
    }
    
    /**
     * 获取 Provider 状态
     */
    public String getProviderStatus(String providerId) {
        return providerStatusCache.get(providerId);
    }
    
    /**
     * 移除 Provider 状态
     */
    public void removeProviderStatus(String providerId) {
        providerStatusCache.remove(providerId);
        log.info("移除 Provider 状态：providerId={}", providerId);
    }
    
    /**
     * 按 Provider ID 移除技能缓存
     */
    public void evictSkillsByProvider(String providerId) {
        List<String> toRemove = new ArrayList<>();
        cache.forEach((skillId, entry) -> {
            if (providerId.equals(entry.skill.getProviderId())) {
                toRemove.add(skillId);
            }
        });
        
        toRemove.forEach(cache::remove);
        log.info("按 Provider 移除技能缓存：providerId={}, 移除数量={}", providerId, toRemove.size());
    }
    
    /**
     * 存入技能（便捷方法）
     */
    public void putSkill(Skill skill) {
        if (skill != null && skill.getId() != null) {
            put(skill.getId(), skill);
            log.debug("存入技能缓存：skillId={}", skill.getId());
        }
    }
    
    /**
     * 移除技能（便捷方法）
     */
    public void removeSkill(String skillId) {
        remove(skillId);
        log.debug("移除技能缓存：skillId={}", skillId);
    }
}
