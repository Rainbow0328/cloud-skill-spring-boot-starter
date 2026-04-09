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
 * limitations under the License.
 */
package com.cloudskill.sdk.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.cloudskill.sdk.agent.ToolCache;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.model.Skill;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 技能缓存后台刷新任务
 * 定期检查 Admin 端技能的时间戳，发现更新时异步刷新本地缓存
 */
public class SkillCacheRefresher {

    private static final Logger log = LoggerFactory.getLogger(SkillCacheRefresher.class);

    private final SkillCache skillCache;
    private final CloudSkillClient cloudSkillClient;
    private final CloudSkillProperties properties;
    private final ToolCache toolCache;
    private ObjectMapper objectMapper;

    /**
     * 构造方法
     */
    public SkillCacheRefresher(SkillCache skillCache,
                               CloudSkillClient cloudSkillClient,
                               CloudSkillProperties properties,
                               ToolCache toolCache) {
        this.skillCache = skillCache;
        this.cloudSkillClient = cloudSkillClient;
        this.properties = properties;
        this.toolCache = toolCache;
        // 初始化 ObjectMapper 以支持 Java 8 日期时间类型
        this.objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        objectMapper.registerModule(javaTimeModule);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 后台定时检查缓存更新
     * 默认每 5 分钟检查一次（可配置）
     */
    @Scheduled(fixedRateString = "#{T(java.lang.Long).parseLong('${cloudskill.sdk.cache-check-interval:300000}')}")
    public void refreshCache() {
        if (!properties.isEnableLocalCache()) {
            log.trace("Local cache is disabled, skip refresh");
            return;
        }

        log.debug("Starting skill cache refresh check");

        try {
            // 获取所有本地缓存的技能 ID
            Map<Long, SkillCache.CacheEntry> cacheMap = skillCache.getCache();

            if (cacheMap.isEmpty()) {
                log.trace("Local cache is empty, skip refresh");
                return;
            }

            int checkedCount = 0;
            int updatedCount = 0;

            // 遍历本地缓存，检查时间戳
            for (Map.Entry<Long, SkillCache.CacheEntry> entry : cacheMap.entrySet()) {
                Long skillId = entry.getKey();
                SkillCache.CacheEntry cacheEntry = entry.getValue();

                // 检查技能是否过期
                if (isExpired(cacheEntry)) {
                    log.debug("Skill cache expired, removing: skillId={}", skillId);
                    skillCache.remove(skillId);
                    continue;
                }

                // 检查 Admin 端的时间戳
                Long serverTimestamp = getSkillTimestampFromServer(skillId);
                if (serverTimestamp == null) {
                    // 服务端不存在，可能已被删除
                    log.debug("Skill not found on server, may be deleted: skillId={}", skillId);
                    skillCache.remove(skillId);
                    continue;
                }

                checkedCount++;

                // 比较时间戳
                Long localTimestamp = cacheEntry.updateTimestamp;
                if (localTimestamp == null || serverTimestamp > localTimestamp) {
                    // 发现更新，刷新本地缓存
                    log.info("Skill updated, refreshing local cache: skillId={}, serverTimestamp={}, localTimestamp={}",
                            skillId, serverTimestamp, localTimestamp);
                    refreshSkill(skillId);
                    updatedCount++;
                }
            }

            // SkillCache 更新后，刷新 ToolCache
            if (updatedCount > 0) {
                toolCache.refreshCache();
            }

            log.info("Cache refresh check completed, checked: {}, updated: {}", checkedCount, updatedCount);

        } catch (Exception e) {
            log.error("Error during cache refresh check", e);
        }
    }

    /**
     * 刷新单个技能
     */
    private void refreshSkill(Long skillId) {
        try {
            // 从服务端获取最新技能信息
            String url = properties.getServerUrl() + "/cloud-skill/v1/sdk/skills/" + skillId;

            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .header("X-IP-Address", properties.getServiceIp())
                    .timeout(properties.getCallTimeout())
                    .execute();

            if (response.isOk()) {
                Skill skill = objectMapper.readValue(response.body(), Skill.class);

                // 提取时间戳（从响应头或技能元数据中）
                Long timestamp = extractTimestamp(response, skill);

                // 更新本地缓存（带时间戳）
                skillCache.put(skillId, skill, timestamp);

                log.debug("Skill cache refreshed: skillId={}", skillId);
            } else {
                log.warn("Failed to refresh skill from server: skillId={}, status={}", skillId, response.getStatus());
            }

        } catch (Exception e) {
            log.warn("Failed to refresh skill cache: skillId={}", skillId, e);
        }
    }

    /**
     * 从服务端获取技能时间戳
     */
    private Long getSkillTimestampFromServer(Long skillId) {
        try {
            String url = properties.getServerUrl() + "/cloud-skill/v1/sdk/skills/" + skillId + "/timestamp";

            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", properties.getApiKey())
                    .header("X-Service-Name", properties.getServiceName())
                    .timeout(5000) // 短超时
                    .execute();

            if (response.isOk()) {
                // 响应格式：{"timestamp": 1234567890}
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                return ((Number) result.get("timestamp")).longValue();
            }
            return null;
        } catch (Exception e) {
            log.trace("Failed to get skill timestamp from server: skillId={}", skillId, e);
            return null;
        }
    }

    /**
     * 提取时间戳
     */
    private Long extractTimestamp(HttpResponse response, Skill skill) {
        // 优先从响应头获取
        String timestampHeader = response.header("X-Skill-Timestamp");
        if (timestampHeader != null) {
            try {
                return Long.parseLong(timestampHeader);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }

        // 否则使用当前时间
        return System.currentTimeMillis();
    }

    /**
     * 检查缓存条目是否过期
     */
    private boolean isExpired(SkillCache.CacheEntry entry) {
        long now = System.currentTimeMillis();
        long expireTime = properties.getCacheExpireTime() * 1000; // 转换为毫秒
        return now - entry.timestamp > expireTime;
    }
}
