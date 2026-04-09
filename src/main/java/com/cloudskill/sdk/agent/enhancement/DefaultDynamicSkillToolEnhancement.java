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
package com.cloudskill.sdk.agent.enhancement;

import com.cloudskill.sdk.agent.CloudSkillToolCallbackProvider;
import com.cloudskill.sdk.config.CloudSkillProperties;

/**
 * 默认的动态工具增强实现；扩展点请继承 {@link AbstractDynamicSkillToolEnhancement} 并注册为 Spring Bean（可用 {@code @Primary} 覆盖本默认 Bean）.
 */
public class DefaultDynamicSkillToolEnhancement extends AbstractDynamicSkillToolEnhancement {

    public DefaultDynamicSkillToolEnhancement(CloudSkillToolCallbackProvider toolCallbackProvider,
                                              CloudSkillProperties properties) {
        super(toolCallbackProvider, properties);
    }
}
