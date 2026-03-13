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
package com.cloudskill.sdk.core;

import com.cloudskill.sdk.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillCache {
    
    private static final Logger log = LoggerFactory.getLogger(SkillCache.class);
    private final Map<String, Skill> cache = new ConcurrentHashMap<>();
    
    /**
     * 获取缓存
     */
    public Map<String, Skill> getCache() {
        return cache;
    }
    
    private final Map<String, Long> cacheTimes = new ConcurrentHashMap<>();
    private final long expireTime; // 过期时间，秒
    
    public SkillCache(long expireTime) {
        this.expireTime = expireTime;
    }
    
    public void put(String skillId, Skill skill) {
        cache.put(skillId, skill);
        cacheTimes.put(skillId, System.currentTimeMillis());
    }
    
    public Skill get(String skillId) {
        Long cacheTime = cacheTimes.get(skillId);
        if (cacheTime == null) {
            return null;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() - cacheTime > expireTime * 1000) {
            cache.remove(skillId);
            cacheTimes.remove(skillId);
            return null;
        }
        
        return cache.get(skillId);
    }
    
    public void remove(String skillId) {
        cache.remove(skillId);
        cacheTimes.remove(skillId);
    }
    
    public List<Skill> getAll() {
        List<Skill> skills = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        cache.forEach((id, skill) -> {
            Long cacheTime = cacheTimes.get(id);
            if (cacheTime != null && now - cacheTime <= expireTime * 1000) {
                skills.add(skill);
            }
        });
        
        return skills;
    }
    
    public boolean isEmpty() {
        return cache.isEmpty();
    }
    
    public void clear() {
        cache.clear();
        cacheTimes.clear();
    }
    
    public int size() {
        return cache.size();
    }
    
    public boolean containsKey(String skillId) {
        return cache.containsKey(skillId) && 
               cacheTimes.containsKey(skillId) && 
               System.currentTimeMillis() - cacheTimes.get(skillId) <= expireTime * 1000;
    }
}
