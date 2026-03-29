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
 * limitations under the the License.
 */
package com.cloudskill.sdk.listener;

import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.model.SkillChangeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 基于 Redis Pub/Sub 的技能变更监听器
 * 继承 {@link AbstractSkillChangeListener} 统一处理技能变更消息
 *
 * @author Cloud Skill Team
 * @version 1.0.0
 */
@Component
public class RedisSkillChangeListener extends AbstractSkillChangeListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisSkillChangeListener.class);

    private static final String CHANNEL_NAME = "cloud.skill:channel:changes";

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    /**
     * 构造器注入
     */
    public RedisSkillChangeListener(StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper,
                                      SkillCache skillCache,
                                      RedisMessageListenerContainer redisMessageListenerContainer,
                                      ScheduledExecutorService scheduledExecutor,
                                      CloudSkillClient cloudSkillClient,
                                      Environment environment) {
        super(objectMapper, skillCache, cloudSkillClient, environment, scheduledExecutor);
        this.redisTemplate = redisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            SkillChangeMessage changeMessage = objectMapper.readValue(body, SkillChangeMessage.class);

            // 调用父类模板方法处理消息
            handleMessage(changeMessage);

        } catch (Exception e) {
            log.error("Redis 处理技能变更消息失败", e);
        }
    }

    @Override
    public void subscribe() {
        if (redisMessageListenerContainer != null) {
            redisMessageListenerContainer.addMessageListener(this, new PatternTopic(CHANNEL_NAME));
            log.info("已订阅 Redis 技能变更频道：{}", CHANNEL_NAME);
        } else {
            log.warn("未配置 RedisMessageListenerContainer，无法订阅技能变更频道");
        }
    }
}
