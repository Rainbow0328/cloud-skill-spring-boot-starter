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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.Function;

/**
 * 默认的Skill转换器实现
 */
public class DefaultSkillConverter implements SkillConverter {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultSkillConverter.class);
    
    @Override
    public boolean supports(Skill skill) {
        // 默认支持所有技能
        return true;
    }
    
    @Override
    public ToolCallback convert(Skill skill, CloudSkillClient client) {
        Function<java.util.Map<String, Object>, Object> function = params -> {
            log.info("执行动态技能调用: [{}] {}", skill.getId(), skill.getName());
            log.debug("调用参数: {}", params);
            return client.invokeSkill(skill.getId(), params).getData();
        };
        
        // 构建完整的技能描述，包含参数说明，帮助模型理解如何调用
        StringBuilder description = new StringBuilder(skill.getDescription());
        
        // 添加参数说明
        if (skill.getParameters() != null && !skill.getParameters().isEmpty()) {
            description.append(" 参数：");
            skill.getParameters().forEach(param -> {
                description.append(param.get("name")).append("(").append(param.get("type")).append(")：")
                        .append(param.get("description")).append("，");
            });
            // 移除最后一个逗号
            if (description.length() > 0) {
                description.setLength(description.length() - 1);
            }
        }
        
        return FunctionToolCallback.builder(
                        skill.getName(),
                        function
                )
                .description(description.toString())
                .inputType(java.util.Map.class)
                .build();
    }
}
