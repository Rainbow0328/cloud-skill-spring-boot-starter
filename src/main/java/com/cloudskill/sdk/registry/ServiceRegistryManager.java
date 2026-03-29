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
package com.cloudskill.sdk.registry;

import com.cloudskill.sdk.config.CloudSkillProperties;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务注册管理器
 * 将当前 consumer 服务注册到 admin 平台
 */
@Slf4j
public class ServiceRegistryManager implements DisposableBean {
    
    private final CloudSkillProperties properties;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;
    private boolean started = false;
    
    public ServiceRegistryManager(CloudSkillProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("cloud-skill-registry-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * 启动服务注册
     */
    public void start() {
        if (!properties.isEnableServiceRegistry()) {
            log.info("Service registry disabled, skipping registration");
            return;
        }
        
        if (started) {
            log.warn("Service registry already started");
            return;
        }
        
        // 检查 serviceName 必须配置
        if (properties.getServiceName() == null || properties.getServiceName().isEmpty()) {
            throw new IllegalStateException(
                "[CloudSkill] service-name is required when service-registry is enabled. " +
                "Please configure cloud.skill.service-name (usually set to ${spring.application.name})"
            );
        }
        
        // 检查 servicePort 必须配置
        if (properties.getServicePort() == null) {
            throw new IllegalStateException(
                "[CloudSkill] service-port is required when service-registry is enabled. " +
                "Please configure cloud.skill.service-port (usually set to ${server.port})"
            );
        }
        
        // 自动补全配置
        autoCompleteConfig();
        
        log.info("Starting service registry: serviceName={}, serviceVersion={}, heartbeatInterval={}",
                properties.getServiceName(),
                properties.getServiceVersion(),
                properties.getHeartbeatInterval());
        
        scheduler.scheduleAtFixedRate(this::register, 0, properties.getHeartbeatInterval(), TimeUnit.SECONDS);
        started = true;
        log.info("Service registry started");
    }
    
    /**
     * 执行注册
     */
    private void register() {
        try {
            String url = properties.getServerUrl() + "/cloud-skill/v1/registry/heartbeat";
            
            Map<String, Object> body = new HashMap<>();
            body.put("serviceName", properties.getServiceName());
            body.put("serviceVersion", properties.getServiceVersion());
            body.put("serviceIp", properties.getServiceIp());
            body.put("servicePort", properties.getServicePort());
            body.put("instanceId", properties.getInstanceId());
            body.put("heartbeatInterval", properties.getHeartbeatInterval());
            body.put("enabled", true);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", properties.getApiKey())
                    .body(jsonBody)
                    .timeout(10000)
                    .execute();
            
            if (response.isOk()) {
                log.debug("Heartbeat sent successfully: serviceName={}, instanceId={}",
                        properties.getServiceName(), properties.getInstanceId());
            } else {
                log.warn("Heartbeat failed: status={}, body={}",
                        response.getStatus(), response.body());
            }
        } catch (Exception e) {
            log.warn("Failed to send heartbeat: {}", e.getMessage());
        }
    }
    
    /**
     * 自动补全配置
     */
    private void autoCompleteConfig() {
        // 自动生成 instanceId
        if (properties.getInstanceId() == null || properties.getInstanceId().isEmpty()) {
            properties.setInstanceId(UUID.randomUUID().toString());
        }
        
        // 自动获取 IP
        if (properties.getServiceIp() == null || properties.getServiceIp().isEmpty()) {
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                properties.setServiceIp(localhost.getHostAddress());
            } catch (Exception e) {
                log.warn("Failed to get local IP address: {}", e.getMessage());
                properties.setServiceIp("127.0.0.1");
            }
        }
    }
    
    @Override
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("Service registry stopped");
        }
    }
}
