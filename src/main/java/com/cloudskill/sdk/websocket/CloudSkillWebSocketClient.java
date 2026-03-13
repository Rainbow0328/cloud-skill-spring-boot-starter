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

package com.cloudskill.sdk.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.SkillChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudSkillWebSocketClient extends WebSocketClient {
    
    private static final Logger log = LoggerFactory.getLogger(CloudSkillWebSocketClient.class);
    private final CloudSkillClient cloudSkillClient;
    private final ObjectMapper objectMapper;
    private final Timer reconnectTimer;
    private final int reconnectInterval;
    private volatile boolean isRunning = true;
    
    public CloudSkillWebSocketClient(String serverUri, CloudSkillClient cloudSkillClient) {
        super(URI.create(serverUri));
        this.cloudSkillClient = cloudSkillClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        this.reconnectInterval = cloudSkillClient.getProperties().getReconnectInterval() * 1000;
        this.reconnectTimer = new Timer("CloudSkill-WebSocket-Reconnect", true);
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("WebSocket connection opened to Cloud Skill platform, server: {}", getURI());
    }
    
    @Override
    public void onMessage(String message) {
        log.debug("Received WebSocket message: {}", message);
        try {
            // 先判断消息类型
            if (message.contains("\"type\":\"CONNECT_SUCCESS\"")) {
                log.info("WebSocket connection confirmed: {}", message);
                return;
            }
            
            // 只处理技能变更事件
            if (message.contains("skillId") && message.contains("changeType")) {
                SkillChangeEvent event = objectMapper.readValue(message, SkillChangeEvent.class);
                if (event.getSkillId() != null) {
                    cloudSkillClient.handleSkillChangeEvent(event);
                }
            }
        } catch (Exception e) {
            log.debug("Ignoring non-skill-change message: {}", message);
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("WebSocket connection closed: code={}, reason={}, remote={}", code, reason, remote);
        scheduleReconnect();
    }
    
    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error occurred", ex);
    }
    
    private void scheduleReconnect() {
        if (!isRunning) {
            return;
        }
        
        log.info("Scheduling WebSocket reconnection in {} seconds", reconnectInterval / 1000);
        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isRunning && !isOpen()) {
                    try {
                        log.info("Attempting to reconnect to MCP server...");
                        reconnect();
                    } catch (Exception e) {
                        log.error("Reconnection attempt failed", e);
                        scheduleReconnect();
                    }
                }
            }
        }, reconnectInterval);
    }
    
    @Override
    public void close() {
        isRunning = false;
        reconnectTimer.cancel();
        super.close();
    }
}
