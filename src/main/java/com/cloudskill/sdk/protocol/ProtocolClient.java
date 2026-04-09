package com.cloudskill.sdk.protocol;

import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallResult;

import java.util.Map;

/**
 * 协议客户端接口
 * 不同协议（如 HTTP）实现此接口，统一技能调用
 */
public interface ProtocolClient {

    /**
     * 获取协议名称
     * @return 协议名称，如 http
     */
    String getProtocolName();

    /**
     * 调用技能
     * @param skill 技能信息
     * @param params 调用参数
     * @return 调用结果
     */
    SkillCallResult invoke(Skill skill, Map<String, Object> params);

    /**
     * 测试连接是否可用
     * @return 是否可用
     */
    boolean testConnection();
}
