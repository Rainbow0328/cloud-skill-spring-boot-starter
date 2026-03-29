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
package com.cloudskill.sdk.spi;

import com.cloudskill.sdk.model.Skill;
import org.springframework.core.Ordered;
import org.springframework.ai.tool.ToolCallback;

/**
 * 技能转换器扩展点
 * 自定义Skill到ToolCallback的转换逻辑
 */
public interface SkillConverter extends Ordered {
    
    /**
     * 是否支持该技能的转换
     * @param skill 技能信息
     * @return true支持，false不支持
     */
    boolean supports(Skill skill);
    
    /**
     * 转换技能为ToolCallback
     * @param skill 技能信息
     * @return ToolCallback实例
     */
    ToolCallback convert(Skill skill, com.cloudskill.sdk.core.CloudSkillClient client);
    
    /**
     * 优先级，数值越小优先级越高
     * @return 优先级值
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
