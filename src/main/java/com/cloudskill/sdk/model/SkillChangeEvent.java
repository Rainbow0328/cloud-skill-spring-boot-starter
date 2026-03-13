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
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillChangeEvent {
    public enum ChangeType {
        CREATE, UPDATE, DELETE, ENABLE, DISABLE
    }
    
    private String skillId;
    private ChangeType changeType;
    private Skill skill;
    private LocalDateTime timestamp;
    private String version;

    // Getter methods
    public String getSkillId() { return skillId; }
    public ChangeType getChangeType() { return changeType; }
    public Skill getSkill() { return skill; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getVersion() { return version; }

    // Setter methods
    public void setSkillId(String skillId) { this.skillId = skillId; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setVersion(String version) { this.version = version; }
}
