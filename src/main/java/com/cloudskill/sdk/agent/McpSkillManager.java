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
import com.cloudskill.sdk.model.SkillChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP技能管理器，提供动态技能的加载和管理
 */
public class McpSkillManager {
    
    private static final Logger log = LoggerFactory.getLogger(McpSkillManager.class);
    private final CloudSkillClient cloudSkillClient;
    private final List<ToolCallback> skillTools = new ArrayList<>();
    
    /**
     * 构造方法
     */
    public McpSkillManager(CloudSkillClient cloudSkillClient) {
        this.cloudSkillClient = cloudSkillClient;
        refreshSkillTools();
        
        // 注册技能变更监听器，实时更新技能列表
        cloudSkillClient.registerSkillChangeListener(event -> {
            log.info("Received skill change event: {} - {}, refreshing skill tools", 
                    event.getSkillId(), event.getChangeType());
            refreshSkillTools();
        });
    }
    
    /**
     * 刷新技能工具列表
     */
    public void refreshSkillTools() {
        skillTools.clear();
        List<Skill> skills = cloudSkillClient.getAllSkills();
        for (Skill skill : skills) {
            // 将Skill转换为ToolCallback
            ToolCallback toolCallback = convertSkillToToolCallback(skill);
            if (toolCallback != null) {
                skillTools.add(toolCallback);
            }
        }
    }
    
    /**
     * 将Skill转换为ToolCallback
     */
    private ToolCallback convertSkillToToolCallback(Skill skill) {
        // 创建一个动态函数，调用远程的MCP技能
        Function<Map<String, Object>, Object> skillFunction = params -> {
            // 调用远程技能服务
            return cloudSkillClient.invokeSkill(skill.getId(), params);
        };
        
        return FunctionToolCallback.builder(skill.getName(), skillFunction)
                .description(skill.getDescription())
                .inputType(Map.class)
                .build();
    }
    
    /**
     * 获取当前已加载的技能工具列表
     */
    public List<ToolCallback> getSkillTools() {
        return new ArrayList<>(skillTools);
    }
}
