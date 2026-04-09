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

/**
 * 技能缓存
 * 使用 ConcurrentHashMap 实现，完全由admin统一管控生命周期
 * 【无本地过期逻辑】：只有admin推送的删除/禁用消息才会修改缓存，本地永远不过期
 */
@Slf4j
public class SkillCache {
    
    private final Map<Long, CacheEntry> cache;

    /**
     * 无参构造方法（推荐使用）
     */
    public SkillCache() {
        this.cache = new ConcurrentHashMap<>();
        log.info("Skill cache initialized, no local expiration, lifecycle managed by admin");
    }
    
    /**
     * 兼容旧版构造方法，参数忽略，仅保留向后兼容
     * @deprecated 使用无参构造方法
     */
    @Deprecated
    public SkillCache(long expireTime) {
        this();
        log.warn("SkillCache with expireTime parameter is deprecated, use no-arg constructor instead");
    }
    
    /**
     * 兼容旧版构造方法，参数忽略，仅保留向后兼容
     * @deprecated 使用无参构造方法
     */
    @Deprecated
    public SkillCache(long expireTime, long checkInterval) {
        this();
        log.warn("SkillCache with expireTime and checkInterval parameters is deprecated, use no-arg constructor instead");
    }
    
    /**
     * 获取技能
     * 【无本地过期判断】：技能有效性完全由admin管控，本地无条件返回
     */
    public Skill get(Long skillId) {
        CacheEntry entry = cache.get(skillId);
        return entry != null ? entry.skill : null;
    }
    
    /**
     * 存入技能（带时间戳）
     */
    public void put(Long skillId, Skill skill, Long updateTimestamp) {
        cache.put(skillId, new CacheEntry(skill, System.currentTimeMillis(), updateTimestamp));
    }
    
    /**
     * 存入技能
     */
    public void put(Long skillId, Skill skill) {
        cache.put(skillId, new CacheEntry(skill, System.currentTimeMillis()));
    }
    
    /**
     * 移除技能
     */
    public void remove(Long skillId) {
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
    public Map<Long, Skill> getAll() {
        return cache.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().skill
                ));
    }
    
    /**
     * 获取原始缓存（公共访问）
     */
    public Map<Long, CacheEntry> getCache() {
        return cache;
    }
    
    /**
     * 更新缓存条目的时间戳
     */
    public void updateTimestamp(Long skillId, Long updateTimestamp) {
        CacheEntry entry = cache.get(skillId);
        if (entry != null) {
            entry.updateTimestamp = updateTimestamp;
        }
    }
    
    /**
     * 获取缓存条目的更新时间戳
     */
    public Long getUpdateTimestamp(Long skillId) {
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
        List<Long> toRemove = new ArrayList<>();
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
    public void removeSkill(Long skillId) {
        remove(skillId);
        log.debug("移除技能缓存：skillId={}", skillId);
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
}
