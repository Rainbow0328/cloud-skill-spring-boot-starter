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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 技能调用统计
 */
public class SkillCallMetrics {
    
    private final Map<String, AtomicLong> callCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> successCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> totalTime = new ConcurrentHashMap<>();
    
    /**
     * 记录技能调用
     * @param skillId 技能ID
     * @param time 调用耗时（毫秒）
     * @param success 是否成功
     */
    public void recordCall(String skillId, long time, boolean success) {
        callCount.computeIfAbsent(skillId, k -> new AtomicLong()).incrementAndGet();
        totalTime.computeIfAbsent(skillId, k -> new AtomicLong()).addAndGet(time);
        if (success) {
            successCount.computeIfAbsent(skillId, k -> new AtomicLong()).incrementAndGet();
        } else {
            failCount.computeIfAbsent(skillId, k -> new AtomicLong()).incrementAndGet();
        }
    }
    
    /**
     * 获取技能的统计数据
     * @param skillId 技能ID
     * @return 统计数据
     */
    public Map<String, Object> getMetrics(String skillId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("callCount", getCount(callCount, skillId));
        metrics.put("successCount", getCount(successCount, skillId));
        metrics.put("failCount", getCount(failCount, skillId));
        metrics.put("totalTime", getCount(totalTime, skillId));
        
        long calls = getCount(callCount, skillId);
        if (calls > 0) {
            metrics.put("avgTime", getCount(totalTime, skillId) / (double) calls);
            metrics.put("successRate", getCount(successCount, skillId) / (double) calls * 100);
        } else {
            metrics.put("avgTime", 0.0);
            metrics.put("successRate", 0.0);
        }
        
        return metrics;
    }
    
    /**
     * 获取所有技能的统计数据
     * @return 所有技能的统计数据
     */
    public Map<String, Map<String, Object>> getAllMetrics() {
        Map<String, Map<String, Object>> allMetrics = new HashMap<>();
        for (String skillId : callCount.keySet()) {
            allMetrics.put(skillId, getMetrics(skillId));
        }
        return allMetrics;
    }
    
    /**
     * 重置指定技能的统计数据
     * @param skillId 技能ID
     */
    public void reset(String skillId) {
        callCount.remove(skillId);
        successCount.remove(skillId);
        failCount.remove(skillId);
        totalTime.remove(skillId);
    }
    
    /**
     * 重置所有统计数据
     */
    public void resetAll() {
        callCount.clear();
        successCount.clear();
        failCount.clear();
        totalTime.clear();
    }
    
    private long getCount(Map<String, AtomicLong> map, String key) {
        AtomicLong count = map.get(key);
        return count != null ? count.get() : 0L;
    }
}
