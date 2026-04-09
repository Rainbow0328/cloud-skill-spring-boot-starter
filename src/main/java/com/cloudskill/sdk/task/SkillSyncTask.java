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

import com.cloudskill.sdk.agent.ToolCache;
import com.cloudskill.sdk.cache.GlobalCacheVersion;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;

public class SkillSyncTask {

    private static final Logger log = LoggerFactory.getLogger(SkillSyncTask.class);
    private final CloudSkillClient cloudSkillClient;
    private final SkillCache skillCache;
    private final ToolCache toolCache;

    /**
     * 构造方法
     */
    public SkillSyncTask(CloudSkillClient cloudSkillClient, SkillCache skillCache, ToolCache toolCache) {
        this.cloudSkillClient = cloudSkillClient;
        this.skillCache = skillCache;
        this.toolCache = toolCache;
    }

    /**
     * 启动时全量同步
     */
    @PostConstruct
    public void initSync() {
        try {
            log.info("启动时开始全量同步技能...");
            Long adminGlobalVersion = cloudSkillClient.syncSkills();

            // 同步刷新ToolCache
            toolCache.refreshCache();

            // 更新全局版本号
            if (adminGlobalVersion != null && adminGlobalVersion > 0) {
                GlobalCacheVersion.update(adminGlobalVersion);
            }

            log.info("启动全量同步完成，当前全局版本：{}，SkillCache数量：{}",
                    GlobalCacheVersion.getCurrentVersion(),
                    skillCache.size());
        } catch (Exception e) {
            log.error("启动全量同步失败，将依赖Redis消息兜底同步", e);
        }
    }

    @Scheduled(fixedDelayString = "${cloudskill.sdk.sync-interval:30}000")
    public void syncSkills() {
        try {
            log.debug("Starting scheduled skill sync");
            Long adminGlobalVersion = cloudSkillClient.syncSkills();

            // 定时同步后刷新ToolCache
            toolCache.refreshCache();

            // 更新全局版本号
            if (adminGlobalVersion != null && adminGlobalVersion > 0) {
                GlobalCacheVersion.update(adminGlobalVersion);
            }

            log.debug("Scheduled skill sync completed, globalTimestamp: {}, SkillCache数量：{}",
                    adminGlobalVersion,
                    skillCache.size());
        } catch (Exception e) {
            log.error("Scheduled skill sync failed", e);
        }
    }
}
