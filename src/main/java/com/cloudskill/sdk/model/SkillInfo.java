package com.cloudskill.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 技能信息（精简版，用于消息传输）
 * 忽略未知字段以支持版本兼容性
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillInfo {
    
    private String skillId;
    private String providerId;
    
    public SkillInfo() {}
    
    public SkillInfo(String skillId, String providerId) {
        this.skillId = skillId;
        this.providerId = providerId;
    }
    
    public String getSkillId() {
        return skillId;
    }
    
    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
