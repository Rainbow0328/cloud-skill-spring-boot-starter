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
package com.cloudskill.sdk.task;

import com.cloudskill.sdk.core.CloudSkillClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class SkillSyncTask {
    
    private static final Logger log = LoggerFactory.getLogger(SkillSyncTask.class);
    private final CloudSkillClient cloudSkillClient;
    
    /**
     * 构造方法
     */
    public SkillSyncTask(CloudSkillClient cloudSkillClient) {
        this.cloudSkillClient = cloudSkillClient;
    }
    
    @Scheduled(fixedDelayString = "${cloudskill.sdk.sync-interval:30}000")
    public void syncSkills() {
        if (cloudSkillClient.getProperties().isAutoSync()) {
            log.debug("Starting scheduled skill sync");
            cloudSkillClient.syncSkillUpdates();
        }
    }
}
