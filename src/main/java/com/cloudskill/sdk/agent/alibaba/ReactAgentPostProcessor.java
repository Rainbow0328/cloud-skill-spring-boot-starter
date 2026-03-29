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
package com.cloudskill.sdk.agent.alibaba;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.cloudskill.sdk.agent.McpSkillManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * ReactAgent Bean 后处理器
 * 自动为所有 ReactAgent Bean 注入动态技能
 * 用户只需要正常定义 ReactAgent Bean，不需要手动注入
 * 零侵入自动增强
 */
@Component
@ConditionalOnClass(ReactAgent.class)
public class ReactAgentPostProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(ReactAgentPostProcessor.class);
    
    private final SpringAiAlibabaAgentAdapter agentAdapter;
    private final McpSkillManager mcpSkillManager;
    
    public ReactAgentPostProcessor(SpringAiAlibabaAgentAdapter agentAdapter, McpSkillManager mcpSkillManager) {
        this.agentAdapter = agentAdapter;
        this.mcpSkillManager = mcpSkillManager;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ReactAgent) {
            ReactAgent agent = (ReactAgent) bean;
            log.info("检测到 ReactAgent Bean: {}, 自动注入动态技能");
            return agentAdapter.injectSkills(agent);
        }
        return bean;
    }
}
