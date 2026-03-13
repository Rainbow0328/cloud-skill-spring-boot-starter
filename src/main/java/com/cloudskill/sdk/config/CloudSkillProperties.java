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

@ConfigurationProperties(prefix = "cloudskill.sdk")
public class CloudSkillProperties {
    
    /**
     * MCP服务端地址
     */
    private String serverUrl = "http://localhost:8080";
    
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
     * 是否启用服务自动注册
     */
    private boolean enableServiceRegistry = false;
    
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
     * 动态技能配置
     */
    private DynamicSkills dynamicSkills = new DynamicSkills();
    
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
    public boolean isEnableServiceRegistry() { return enableServiceRegistry; }
    public String getServiceName() { return serviceName; }
    public String getServiceVersion() { return serviceVersion; }
    public String getServiceIp() { return serviceIp; }
    public Integer getServicePort() { return servicePort; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public String getInstanceId() { return instanceId; }
    public boolean isEnableAgentIntegration() { return enableAgentIntegration; }
    public DynamicSkills getDynamicSkills() { return dynamicSkills; }

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
    public void setEnableServiceRegistry(boolean enableServiceRegistry) { this.enableServiceRegistry = enableServiceRegistry; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }
    public void setServiceIp(String serviceIp) { this.serviceIp = serviceIp; }
    public void setServicePort(Integer servicePort) { this.servicePort = servicePort; }
    public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public void setEnableAgentIntegration(boolean enableAgentIntegration) { this.enableAgentIntegration = enableAgentIntegration; }
    public void setDynamicSkills(DynamicSkills dynamicSkills) { this.dynamicSkills = dynamicSkills; }
}
