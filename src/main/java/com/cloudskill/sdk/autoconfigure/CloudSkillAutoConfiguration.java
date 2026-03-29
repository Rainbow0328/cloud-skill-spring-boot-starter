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
package com.cloudskill.sdk.autoconfigure;

import com.cloudskill.sdk.agent.McpSkillManager;
import com.cloudskill.sdk.agent.alibaba.SpringAiAlibabaAgentAdapter;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.spi.SkillConverter;
import com.cloudskill.sdk.spi.SkillExecutionHook;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.core.metrics.SkillMetrics;
import com.cloudskill.sdk.listener.RedisSkillChangeListener;
import com.cloudskill.sdk.registry.ServiceRegistryManager;
import com.cloudskill.sdk.task.SkillCacheRefresher;
import com.cloudskill.sdk.task.SkillSyncTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@EnableConfigurationProperties(CloudSkillProperties.class)
@ConditionalOnProperty(prefix = "cloud.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(CloudSkillClient.class)
@EnableScheduling
public class CloudSkillAutoConfiguration {
    
    private Integer serverPort;
    
    @jakarta.annotation.Resource
    private Environment environment;
    
    @PostConstruct
    public void init() {
        if (serverPort == null) {
            String portStr = environment.getProperty("server.port");
            if (portStr != null) {
                serverPort = Integer.parseInt(portStr);
            }
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    public SkillMetrics skillMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new SkillMetrics(meterRegistryProvider.getIfAvailable());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CloudSkillClient cloudSkillClient(CloudSkillProperties properties, 
                                            SkillCache skillCache,
                                            Environment environment) {
        // 自动注入服务端口
        if (properties.getServicePort() == null && properties.isEnableServiceRegistry()) {
            properties.setServicePort(serverPort);
        }
        
        // 如果没有配置 serviceName，默认使用 spring.application.name
        if (properties.getServiceName() == null || properties.getServiceName().isEmpty()) {
            String appName = environment.getProperty("spring.application.name", "unknown-application");
            properties.setServiceName(appName);
            log.info("Auto-set serviceName from spring.application.name: {}", appName);
        }
        
        // 调试日志：打印配置属性
        log.info("=== CloudSkillProperties Debug ===");
        log.info("enableServiceRegistry: {}", properties.isEnableServiceRegistry());
        log.info("serviceName: {}", properties.getServiceName());
        log.info("servicePort: {}", properties.getServicePort());
        log.info("serverUrl: {}", properties.getServerUrl());
        log.info("apiKey: {}", properties.getApiKey() != null ? "[masked]" : "null");
        log.info("==============================");
        
        return new CloudSkillClient(properties, skillCache);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cloud.skill", name = "enable-local-cache", havingValue = "true", matchIfMissing = true)
    public SkillCache skillCache(CloudSkillProperties properties) {
        return new SkillCache(
            properties.getCacheExpireTime(),
            properties.getCacheCheckInterval()
        );
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cloud.skill", name = "auto-sync", havingValue = "true", matchIfMissing = true)
    public SkillSyncTask skillSyncTask(CloudSkillClient cloudSkillClient) {
        return new SkillSyncTask(cloudSkillClient);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cloud.skill", name = "enable-local-cache", havingValue = "true", matchIfMissing = true)
    public SkillCacheRefresher skillCacheRefresher(SkillCache skillCache, 
                                                    CloudSkillClient cloudSkillClient,
                                                    CloudSkillProperties properties) {
        return new SkillCacheRefresher(skillCache, cloudSkillClient, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ServiceRegistryManager serviceRegistryManager(CloudSkillProperties properties) {
        log.info("Creating ServiceRegistryManager, serviceName: {}",
                properties.getServiceName());
        ServiceRegistryManager registryManager = new ServiceRegistryManager(properties);
        if (properties.isEnableServiceRegistry()) {
            registryManager.start();
        } else {
            log.info("Service registry disabled by configuration");
        }
        return registryManager;
    }
    
    /**
     * 配置 Redis 消息监听器容器（仅在应用未定义时创建）
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        log.info("RedisMessageListenerContainer configured");
        return container;
    }
    
    /**
     * 配置技能变更监听器（仅在应用未定义时创建）
     * 使用 Redis 实现
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cloud.skill", name = "enable-listener", havingValue = "true", matchIfMissing = true)
    public RedisSkillChangeListener skillChangeListener(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SkillCache skillCache,
            RedisMessageListenerContainer redisMessageListenerContainer,
            CloudSkillClient cloudSkillClient,
            Environment environment) {
        
        log.info("Configuring RedisSkillChangeListener for real-time skill change notification");
        
        // 创建定时任务调度器
        java.util.concurrent.ScheduledExecutorService scheduler = 
            java.util.concurrent.Executors.newScheduledThreadPool(2);
        
        // 创建监听器
        RedisSkillChangeListener listener = new RedisSkillChangeListener(
            redisTemplate,
            objectMapper,
            skillCache,
            redisMessageListenerContainer,
            scheduler,
            cloudSkillClient,
            environment
        );
        
        // 订阅频道
        listener.subscribe();
        
        return listener;
    }
    
    /**
     * 配置 ObjectMapper（仅在应用未定义时创建）
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    /**
     * 注册 MCP 技能管理器，提供动态技能的加载和管理
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ToolCallback.class)
    @ConditionalOnProperty(prefix = "cloud.skill", name = "enable-agent-integration", havingValue = "true", matchIfMissing = true)
    public McpSkillManager mcpSkillManager(CloudSkillClient cloudSkillClient) {
        log.info("Initializing McpSkillManager for dynamic skill management");
        return new McpSkillManager(cloudSkillClient);
    }
    

    
    /**
     * Spring AI Alibaba Agent 适配器，支持 ReactAgent 动态技能注入
     */
    @Bean
    @ConditionalOnClass(name = "com.alibaba.cloud.ai.graph.agent.ReactAgent")
    @ConditionalOnProperty(prefix = "cloud.skill.alibaba", name = "enable-agent-support", havingValue = "true", matchIfMissing = true)
    public SpringAiAlibabaAgentAdapter springAiAlibabaAgentAdapter(
            CloudSkillClient cloudSkillClient,
            CloudSkillProperties properties,
            ObjectProvider<List<SkillConverter>> convertersProvider,
            ObjectProvider<List<SkillExecutionHook>> hooksProvider) {
        
        log.info("Initializing SpringAiAlibabaAgentAdapter for ReactAgent support");
        return new SpringAiAlibabaAgentAdapter(
                cloudSkillClient,
                properties,
                convertersProvider,
                hooksProvider
        );
    }
}
