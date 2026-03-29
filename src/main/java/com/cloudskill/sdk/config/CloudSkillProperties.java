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
package com.cloudskill.sdk.config;

import com.cloudskill.sdk.agent.DynamicSkillsMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.skill")
public class CloudSkillProperties {
    
    /**
     * MCP服务端地址
     */
    private String serverUrl = "http://localhost:8766";
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * 是否自动同步技能
     */
    private boolean autoSync = true;
    
    /**
     * 技能同步间隔（秒）
     */
    private int syncInterval = 30;
    
    /**
     * 是否启用WebSocket实时推送
     */
    private boolean enableWebSocket = true;
    
    /**
     * WebSocket重连间隔（秒）
     */
    private int reconnectInterval = 5;
    
    /**
     * 调用超时时间（毫秒）
     */
    private int callTimeout = 30000;
    
    /**
     * 调用重试次数
     */
    private int retryCount = 3;
    
    /**
     * 是否启用本地缓存
     */
    private boolean enableLocalCache = true;
    
    /**
     * 本地缓存过期时间（秒）
     */
    private long cacheExpireTime = 3600;
    
    /**
     * 后台检查缓存更新间隔（毫秒）
     * 默认：300000 (5 分钟)
     */
    private long cacheCheckInterval = 300000;
    
    /**
     * 是否启用服务自动注册
     */
    private boolean enableServiceRegistry = true;
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 服务版本
     */
    private String serviceVersion = "1.0.0";
    
    /**
     * 服务IP地址（自动获取）
     */
    private String serviceIp;
    
    /**
     * 服务端口
     */
    private Integer servicePort;
    
    /**
     * 心跳间隔（秒）
     */
    private int heartbeatInterval = 30;
    
    /**
     * 实例ID（自动生成）
     */
    private String instanceId;
    
    /**
     * 是否启用Agent集成，自动将技能注入到Spring AI Agent中
     */
    private boolean enableAgentIntegration = true;
    
    /**
     * 参数校验配置
     */
    private Validation validation = new Validation();
    
    /**
     * 动态技能配置
     */
    private DynamicSkills dynamicSkills = new DynamicSkills();
    
    /**
     * Spring AI Alibaba 特定配置
     */
    private Alibaba alibaba = new Alibaba();
    
    /**
     * 参数校验配置类
     */
    public static class Validation {
        /**
         * 是否启用请求参数校验
         */
        private boolean enableRequestValidation = true;
        
        /**
         * 是否启用响应结果校验
         */
        private boolean enableResponseValidation = true;
        
        /**
         * 校验失败时是否阻止调用（false时仅记录警告）
         */
        private boolean failOnError = true;
        
        // Getter and Setter
        public boolean isEnableRequestValidation() { return enableRequestValidation; }
        public void setEnableRequestValidation(boolean enableRequestValidation) { this.enableRequestValidation = enableRequestValidation; }
        public boolean isEnableResponseValidation() { return enableResponseValidation; }
        public void setEnableResponseValidation(boolean enableResponseValidation) { this.enableResponseValidation = enableResponseValidation; }
        public boolean isFailOnError() { return failOnError; }
        public void setFailOnError(boolean failOnError) { this.failOnError = failOnError; }
    }
    
    /**
     * 是否启用调试模式，输出详细日志
     */
    private boolean debug = false;
    
    public boolean isDebug() {
        return debug;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public static class DynamicSkills {
        /**
         * 动态技能模式：OFF/GLOBAL/ANNOTATION
         */
        private DynamicSkillsMode mode = DynamicSkillsMode.GLOBAL;
        
        /**
         * Advisor执行顺序
         */
        private int order = Integer.MAX_VALUE - 100;

        // Getter and Setter for DynamicSkills
        public DynamicSkillsMode getMode() { return mode; }
        public void setMode(DynamicSkillsMode mode) { this.mode = mode; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }

    // Getter methods
    public String getServerUrl() { return serverUrl; }
    public String getApiKey() { return apiKey; }
    public boolean isAutoSync() { return autoSync; }
    public int getSyncInterval() { return syncInterval; }
    public boolean isEnableWebSocket() { return enableWebSocket; }
    public int getReconnectInterval() { return reconnectInterval; }
    public int getCallTimeout() { return callTimeout; }
    public int getRetryCount() { return retryCount; }
    public boolean isEnableLocalCache() { return enableLocalCache; }
    public long getCacheExpireTime() { return cacheExpireTime; }
    public long getCacheCheckInterval() { return cacheCheckInterval; }
    public boolean isEnableServiceRegistry() { return enableServiceRegistry; }
    public String getServiceName() { return serviceName; }
    public String getServiceVersion() { return serviceVersion; }
    public String getServiceIp() { return serviceIp; }
    public Integer getServicePort() { return servicePort; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public String getInstanceId() { return instanceId; }
    public boolean isEnableAgentIntegration() { return enableAgentIntegration; }
    public Validation getValidation() { return validation; }
    public DynamicSkills getDynamicSkills() { return dynamicSkills; }
    public Alibaba getAlibaba() { return alibaba; }

    // Setter methods
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setAutoSync(boolean autoSync) { this.autoSync = autoSync; }
    public void setSyncInterval(int syncInterval) { this.syncInterval = syncInterval; }
    public void setEnableWebSocket(boolean enableWebSocket) { this.enableWebSocket = enableWebSocket; }
    public void setReconnectInterval(int reconnectInterval) { this.reconnectInterval = reconnectInterval; }
    public void setCallTimeout(int callTimeout) { this.callTimeout = callTimeout; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public void setEnableLocalCache(boolean enableLocalCache) { this.enableLocalCache = enableLocalCache; }
    public void setCacheExpireTime(long cacheExpireTime) { this.cacheExpireTime = cacheExpireTime; }
    public void setCacheCheckInterval(long cacheCheckInterval) { this.cacheCheckInterval = cacheCheckInterval; }
    public void setEnableServiceRegistry(boolean enableServiceRegistry) { this.enableServiceRegistry = enableServiceRegistry; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }
    public void setServiceIp(String serviceIp) { this.serviceIp = serviceIp; }
    public void setServicePort(Integer servicePort) { this.servicePort = servicePort; }
    public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public void setEnableAgentIntegration(boolean enableAgentIntegration) { this.enableAgentIntegration = enableAgentIntegration; }
    public void setValidation(Validation validation) { this.validation = validation; }
    public void setDynamicSkills(DynamicSkills dynamicSkills) { this.dynamicSkills = dynamicSkills; }
    public void setAlibaba(Alibaba alibaba) { this.alibaba = alibaba; }
    
    /**
     * Spring AI Alibaba 特定配置类
     */
    public static class Alibaba {
        /**
         * 是否启用 Spring AI Alibaba Agent 支持
         */
        private boolean enableAgentSupport = true;
        
        /**
         * 是否自动注入技能到 ReactAgent
         */
        private boolean autoInjectSkills = true;
        
        // Getter and Setter
        public boolean isEnableAgentSupport() { return enableAgentSupport; }
        public void setEnableAgentSupport(boolean enableAgentSupport) { this.enableAgentSupport = enableAgentSupport; }
        public boolean isAutoInjectSkills() { return autoInjectSkills; }
        public void setAutoInjectSkills(boolean autoInjectSkills) { this.autoInjectSkills = autoInjectSkills; }
    }
}
