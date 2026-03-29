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
package com.cloudskill.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 技能参数定义
 * 忽略未知字段以支持版本兼容性
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameter {
    private String name;
    private String type;
    private String description;
    private boolean required;
    private Object defaultValue;
    
    public Parameter() {
    }
    
    public Parameter(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }
    
    public Parameter(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }
    
    // Getter and Setter methods
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
}
