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
package com.cloudskill.sdk.agent;

import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.Skill;
import org.springframework.ai.tool.ToolCallback;

/**
 * Skill到ToolCallback的转换器SPI
 * 用户可以自定义实现来支持特殊的Skill格式
 */
public interface SkillConverter {
    
    /**
     * 是否支持转换该技能
     */
    boolean supports(Skill skill);
    
    /**
     * 将Skill转换为ToolCallback
     */
    ToolCallback convert(Skill skill, CloudSkillClient client);
}
