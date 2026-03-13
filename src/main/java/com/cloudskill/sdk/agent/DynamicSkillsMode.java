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

/**
 * 动态技能模式枚举
 */
public enum DynamicSkillsMode {
    /**
     * 关闭动态技能功能
     */
    OFF,
    
    /**
     * 全局模式，所有ChatModel调用自动注入动态技能
     */
    GLOBAL,
    
    /**
     * 注解模式，仅对标注了@EnableDynamicSkills的类/方法生效
     */
    ANNOTATION
}
