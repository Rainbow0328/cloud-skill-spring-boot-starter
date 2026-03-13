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
package com.cloudskill.sdk.autoconfigure;

import com.cloudskill.sdk.agent.McpSkillManager;
import com.cloudskill.sdk.agent.aop.DynamicSkillsAspect;
import com.cloudskill.sdk.agent.proxy.DynamicSkillsChatModelProxy;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.registry.ServiceRegistryManager;
import com.cloudskill.sdk.task.SkillSyncTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j

@Configuration
@EnableConfigurationProperties(CloudSkillProperties.class)
@ConditionalOnProperty(prefix = "cloudskill.sdk", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(CloudSkillClient.class)
@EnableScheduling
public class CloudSkillAutoConfiguration {
    
    @Value("${server.port:8080}")
    private Integer serverPort;
    
    @Bean
    @ConditionalOnMissingBean
    public CloudSkillClient cloudSkillClient(CloudSkillProperties properties) {
        // 自动注入服务端口
        if (properties.getServicePort() == null && properties.isEnableServiceRegistry()) {
            properties.setServicePort(serverPort);
        }
        
        // 调试日志：打印配置属性
        log.info("=== CloudSkillProperties Debug ===");
        log.info("enableServiceRegistry: {}", properties.isEnableServiceRegistry());
        log.info("serviceName: {}", properties.getServiceName());
        log.info("servicePort: {}", properties.getServicePort());
        log.info("serverUrl: {}", properties.getServerUrl());
        log.info("apiKey: {}", properties.getApiKey());
        log.info("==============================");
        
        return new CloudSkillClient(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cloudskill.sdk", name = "auto-sync", havingValue = "true", matchIfMissing = true)
    public SkillSyncTask skillSyncTask(CloudSkillClient cloudSkillClient) {
        return new SkillSyncTask(cloudSkillClient);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cloudskill.sdk", name = "enable-service-registry", havingValue = "true")
    public ServiceRegistryManager serviceRegistryManager(CloudSkillProperties properties) {
        log.info("Creating ServiceRegistryManager, enableServiceRegistry: {}, serviceName: {}",
                properties.isEnableServiceRegistry(),
                properties.getServiceName());
        ServiceRegistryManager registryManager = new ServiceRegistryManager(properties);
        // 启动服务注册
        registryManager.start();
        return registryManager;
    }
    
    /**
     * 注册MCP技能管理器，提供动态技能的加载和管理
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.ai.tool.ToolCallback")
    @ConditionalOnProperty(prefix = "cloudskill.sdk", name = "enable-agent-integration", havingValue = "true", matchIfMissing = true)
    public McpSkillManager mcpSkillManager(CloudSkillClient cloudSkillClient) {
        return new McpSkillManager(cloudSkillClient);
    }

    
    /**
     * ChatModel代理，自动处理动态技能
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatModel.class)
    @ConditionalOnProperty(prefix = "cloudskill.sdk", name = "enable-agent-integration", havingValue = "true", matchIfMissing = true)
    public DynamicSkillsChatModelProxy dynamicSkillsChatModelProxy(ChatModel chatModel, CloudSkillClient cloudSkillClient, ObjectMapper objectMapper) {
        return new DynamicSkillsChatModelProxy(chatModel, cloudSkillClient, objectMapper);
    }

    /**
     * AOP相关配置，仅当AOP可用时加载
     */
    @Configuration
    @ConditionalOnClass({Aspect.class, Pointcut.class})
    @ConditionalOnProperty(prefix = "cloudskill.sdk.dynamic-skills.aop", name = "enabled", havingValue = "true", matchIfMissing = true)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    public static class AopConfiguration {
        
        /**
         * 动态技能AOP切面
         */
        @Bean
        @ConditionalOnMissingBean
        public DynamicSkillsAspect dynamicSkillsAspect(CloudSkillClient cloudSkillClient) {
            return new DynamicSkillsAspect(cloudSkillClient);
        }
    }

}
