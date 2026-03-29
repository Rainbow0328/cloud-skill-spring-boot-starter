package com.cloudskill.sdk.model;

/**
 * 操作类型枚举
 */
public enum OperationType {
    
    /**
     * 技能信息更新
     */
    INFO_UPDATE,
    
    /**
     * 技能分配
     */
    ASSIGN,
    
    /**
     * 技能取消分配
     */
    UNASSIGN,
    
    /**
     * 技能下线（全量/增量清理）
     */
    SKILL_OFFLINE,
    
    /**
     * 按 Provider 批量删除技能
     */
    SKILL_DELETE_BY_PROVIDER,
    
    /**
     * Provider 全量更新
     */
    PROVIDER_FULL_RELOAD,
    
    /**
     * Provider 增量更新
     */
    PROVIDER_PRE_ONLINE,
    
    /**
     * Provider 下线（心跳超时）
     */
    PROVIDER_OFFLINE,
    
    /**
     * Provider 恢复上线
     */
    PROVIDER_RECOVERY,
    
    /**
     * 技能创建
     */
    CREATE,
    
    /**
     * 技能删除
     */
    DELETE
}
