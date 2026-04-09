package com.cloudskill.sdk.protocol.http;

import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallResult;
import com.cloudskill.sdk.protocol.ProtocolClient;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * HTTP 协议客户端：根据技能的 {@code requestSchema} 中参数位置（path/query/body）构造真实请求。
 */
@Slf4j
public class HttpProtocolClient implements ProtocolClient {

    private final CloudSkillProperties properties;
    private final ObjectMapper objectMapper;

    public HttpProtocolClient(CloudSkillProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getProtocolName() {
        return "http";
    }

    @Override
    public SkillCallResult invoke(Skill skill, Map<String, Object> params) {
        String method = skill.getHttpMethod();
        if (method == null || method.isBlank()) {
            method = "POST";
        }
        Integer timeout = skill.getTimeout();
        if (timeout == null) {
            timeout = properties.getCallTimeout();
        }

        try {
            HttpSkillRequestPlanner.Plan plan = HttpSkillRequestPlanner.plan(skill, params, objectMapper);
            String url = plan.resolvedUrl() + HttpSkillRequestPlanner.buildQueryString(plan.queryStringParams());

            HttpRequest request = createRequest(method, url);

            if (plan.headerParams() != null) {
                plan.headerParams().forEach(request::header);
            }

            String contentType = skill.getContentType() != null ? skill.getContentType() : "application/json";
            if (plan.sendJsonBody() && plan.bodyPayload() != null) {
                request.header("Content-Type", contentType);
                request.body(objectMapper.writeValueAsString(plan.bodyPayload()));
            }

            request.timeout(timeout);

            HttpResponse response = request.execute();

            if (response.isOk()) {
                String body = response.body();
                log.debug("HTTP 调用成功: url={}, status={}", url, response.getStatus());
                SkillCallResult result = new SkillCallResult();
                result.setSuccess(true);
                result.setData(body);
                result.setCode(response.getStatus());
                result.setMessage("success");
                result.setTimestamp(LocalDateTime.now());
                return result;
            }
            String body = response.body();
            log.warn("HTTP 调用失败: url={}, status={}, body={}", url, response.getStatus(), body);
            SkillCallResult result = new SkillCallResult();
            result.setSuccess(false);
            result.setCode(response.getStatus());
            result.setMessage("HTTP " + response.getStatus() + ": " + body);
            result.setTimestamp(LocalDateTime.now());
            return result;
        } catch (Exception e) {
            log.error("HTTP 调用异常: skill={}", skill.getId(), e);
            SkillCallResult result = new SkillCallResult();
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            result.setTimestamp(LocalDateTime.now());
            return result;
        }
    }

    private static HttpRequest createRequest(String method, String url) {
        if (method == null || method.isBlank()) {
            return HttpRequest.post(url);
        }
        String m = method.trim().toUpperCase();
        return switch (m) {
            case "GET" -> HttpRequest.get(url);
            case "PUT" -> HttpRequest.put(url);
            case "DELETE" -> HttpRequest.delete(url);
            case "PATCH" -> HttpRequest.patch(url);
            case "HEAD" -> HttpRequest.head(url);
            default -> HttpRequest.post(url);
        };
    }

    @Override
    public boolean testConnection() {
        return true;
    }
}
