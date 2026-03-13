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

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.cloudskill.sdk.config.CloudSkillProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServiceRegistryManager implements DisposableBean {
    
    private static final Logger log = LoggerFactory.getLogger(ServiceRegistryManager.class);
    
    private final CloudSkillProperties properties;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private String instanceId;
    private volatile boolean registered = false;
    
    public ServiceRegistryManager(CloudSkillProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CloudSkill-Service-Registry");
            t.setDaemon(true);
            return t;
        });
        
        log.info("ServiceRegistryManager initialized, enableServiceRegistry: {}", properties.isEnableServiceRegistry());
        
        // 自动生成实例ID
        if (!StringUtils.hasText(properties.getInstanceId())) {
            this.instanceId = UUID.randomUUID().toString().replace("-", "");
            properties.setInstanceId(this.instanceId);
        } else {
            this.instanceId = properties.getInstanceId();
        }
        
        // 自动获取IP地址
        if (!StringUtils.hasText(properties.getServiceIp())) {
            try {
                properties.setServiceIp(InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                log.warn("Failed to get local IP address, using 127.0.0.1", e);
                properties.setServiceIp("127.0.0.1");
            }
        }
        
        log.info("Service config: name={}, ip={}, port={}, version={}",
                properties.getServiceName(),
                properties.getServiceIp(),
                properties.getServicePort(),
                properties.getServiceVersion());
    }
    
    /**
     * 启动服务注册
     */
    public void start() {
        log.info("Starting service registry...");
        
        if (!properties.isEnableServiceRegistry()) {
            log.info("Service registry is disabled, skipping...");
            return;
        }
        
        if (!StringUtils.hasText(properties.getServiceName())) {
            log.warn("Service name is not configured, service registry disabled");
            return;
        }
        
        if (properties.getServicePort() == null) {
            log.warn("Service port is not configured, service registry disabled");
            return;
        }
        
        // 立即注册
        register();
        
        // 启动心跳任务
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 
                properties.getHeartbeatInterval(), 
                properties.getHeartbeatInterval(), 
                TimeUnit.SECONDS);
        
        log.info("Service registry started, service: {}:{}:{}",
                properties.getServiceName(), 
                properties.getServiceIp(), 
                properties.getServicePort());
    }
    
    /**
     * 注册服务
     */
    private void register() {
        try {
            String url = properties.getServerUrl() + "/cloud-skill/v1/registry/register";
            
            Map<String, Object> requestBody = Map.of(
                    "serviceName", properties.getServiceName(),
                    "instanceId", instanceId,
                    "ipAddress", properties.getServiceIp(),
                    "port", properties.getServicePort(),
                    "version", properties.getServiceVersion(),
                    "metadata", Map.of()
            );
            
            HttpResponse response = HttpRequest.post(url)
                    .header("X-API-Key", properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(5000)
                    .execute();
            
            if (response.isOk()) {
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    registered = true;
                    log.info("Service registered successfully, instanceId: {}", instanceId);
                } else {
                    log.error("Service registration failed: {}", result.get("message"));
                }
            } else {
                log.error("Service registration failed, status: {}, message: {}",
                        response.getStatus(), response.body());
            }
        } catch (Exception e) {
            log.error("Service registration error", e);
        }
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        if (!registered) {
            log.debug("Service not registered yet, trying to register...");
            register();
            return;
        }
        
        try {
            String url = properties.getServerUrl() + "/cloud-skill/v1/registry/heartbeat";
            
            Map<String, Object> requestBody = Map.of(
                    "instanceId", instanceId
            );
            
            HttpResponse response = HttpRequest.post(url)
                    .header("X-API-Key", properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(3000)
                    .execute();
            
            if (!response.isOk()) {
                log.warn("Heartbeat failed, status: {}", response.getStatus());
                registered = false;
            }
        } catch (Exception e) {
            log.warn("Heartbeat error", e);
            registered = false;
        }
    }
    
    /**
     * 服务下线
     */
    @Override
    public void destroy() {
        if (registered) {
            try {
                String url = properties.getServerUrl() + "/cloud-skill/v1/registry/deregister?instanceId=" + instanceId;
                
                HttpResponse response = HttpRequest.post(url)
                        .header("X-API-Key", properties.getApiKey())
                        .timeout(3000)
                        .execute();
                
                if (response.isOk()) {
                    log.info("Service deregistered successfully");
                } else {
                    log.warn("Service deregistration failed, status: {}", response.getStatus());
                }
            } catch (Exception e) {
                log.warn("Service deregistration error", e);
            }
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public boolean isRegistered() {
        return registered;
    }
    
    public String getInstanceId() {
        return instanceId;
    }
}
