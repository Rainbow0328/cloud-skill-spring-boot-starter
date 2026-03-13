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
package com.cloudskill.sdk.agent.context;

import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;
import com.cloudskill.sdk.model.Skill;
import lombok.Data;
import java.util.List;

/**
 * 技能上下文
 * 存储当前请求的技能配置和可用技能列表
 */
@Data
public class SkillContext {
    
    /**
     * 注解配置
     */
    private EnableDynamicSkills config;
    
    /**
     * 可用技能列表
     */
    private List<Skill> skills;
    
    /**
     * 是否启用动态技能
     */
    private boolean enabled = true;
}
