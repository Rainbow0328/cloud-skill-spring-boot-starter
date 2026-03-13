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
package com.cloudskill.sdk.agent.context;

/**
 * 技能上下文持有者
 * 使用ThreadLocal存储当前线程的技能上下文
 */
public class SkillContextHolder {
    
    private static final ThreadLocal<SkillContext> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 获取当前上下文
     */
    public static SkillContext getContext() {
        SkillContext context = CONTEXT_HOLDER.get();
        if (context == null) {
            context = new SkillContext();
            CONTEXT_HOLDER.set(context);
        }
        return context;
    }
    
    /**
     * 设置上下文
     */
    public static void setContext(SkillContext context) {
        CONTEXT_HOLDER.set(context);
    }
    
    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
    
    /**
     * 判断是否有上下文
     */
    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }
}
