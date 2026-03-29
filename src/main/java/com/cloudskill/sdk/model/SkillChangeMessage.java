package com.cloudskill.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 技能变更消息
 * 用于 Redis Pub/Sub 广播通知
 * 忽略未知字段以支持版本兼容性
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillChangeMessage {
    
    /**
     * 操作类型
     */
    private OperationType operationType;
    
    /**
     * 本次消息时间戳（新版本）
     */
    private long timestamp;
    
    /**
     * 上一次时间戳（旧版本，用于连续性校验）
     */
    private long previousTimestamp;
    
    /**
     * 涉及的技能列表
     */
    private List<SkillInfo> skills;
    
    /**
     * 目标服务 ID 列表（分配操作时使用）
     */
    private List<String> targetServices;
    
    /**
     * API 范围列表（预上线时使用）
     */
    private List<String> scopes;
    
    /**
     * 是否全量更新
     */
    private Boolean fullReload;
    
    /**
     * Provider ID（Provider 相关操作使用）
     */
    private String providerId;
    
    /**
     * 完整 Skill 对象（技能变更使用）
     */
    private Skill skill;
    
    /**
     * 分配给的服务列表（技能变更使用）
     */
    private List<String> assignedServices;
    
    /**
     * 技能 ID（取消分配使用）
     */
    private String skillId;
    
    /**
     * 服务名称（取消分配使用）
     */
    private String serviceName;
    
    // Getters and Setters
    public OperationType getOperationType() {
        return operationType;
    }
    
    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getPreviousTimestamp() {
        return previousTimestamp;
    }
    
    public void setPreviousTimestamp(long previousTimestamp) {
        this.previousTimestamp = previousTimestamp;
    }
    
    public List<SkillInfo> getSkills() {
        return skills;
    }
    
    public void setSkills(List<SkillInfo> skills) {
        this.skills = skills;
    }
    
    public List<String> getTargetServices() {
        return targetServices;
    }
    
    public void setTargetServices(List<String> targetServices) {
        this.targetServices = targetServices;
    }
    
    public List<String> getScopes() {
        return scopes;
    }
    
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
    
    public Boolean getFullReload() {
        return fullReload;
    }
    
    public void setFullReload(Boolean fullReload) {
        this.fullReload = fullReload;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public Skill getSkill() {
        return skill;
    }
    
    public void setSkill(Skill skill) {
        this.skill = skill;
    }
    
    public List<String> getAssignedServices() {
        return assignedServices;
    }
    
    public void setAssignedServices(List<String> assignedServices) {
        this.assignedServices = assignedServices;
    }
    
    public String getSkillId() {
        return skillId;
    }
    
    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
