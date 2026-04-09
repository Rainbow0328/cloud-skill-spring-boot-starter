/*
 * Copyright 2024 Cloud Skill Team
 */
package com.cloudskill.sdk.protocol.http;

import com.cloudskill.sdk.model.Skill;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据技能的 {@code requestSchema}（各属性上的 {@code in} / {@code x-in}：path、query、body）
 * 将模型给出的扁平参数 Map 拆成：路径替换、查询串、JSON body，并生成最终 URL。
 */
public final class HttpSkillRequestPlanner {

    private static final Logger log = LoggerFactory.getLogger(HttpSkillRequestPlanner.class);
    private static final Pattern PATH_TEMPLATE = Pattern.compile("\\{([^{}/]+)}");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private HttpSkillRequestPlanner() {
    }

    public record Plan(String resolvedUrl, Map<String, String> queryStringParams,
                       Map<String, String> headerParams, Object bodyPayload, boolean sendJsonBody) {
    }

    /**
     * @param params 模型 / 调用方给出的参数（一般为扁平 Map）
     */
    public static Plan plan(Skill skill, Map<String, Object> params, ObjectMapper objectMapper) {
        String endpoint = skill.getEndpoint() != null ? skill.getEndpoint() : "";
        String method = skill.getHttpMethod() != null ? skill.getHttpMethod() : "POST";
        String schemaJson = skill.getRequestSchema();

        if (schemaJson == null || schemaJson.isBlank()) {
            return legacyPlan(endpoint, method, params);
        }
        try {
            Map<String, Object> schema = objectMapper.readValue(schemaJson, MAP_TYPE);
            Object propsObj = schema.get("properties");
            if (!(propsObj instanceof Map<?, ?>)) {
                return legacyPlan(endpoint, method, params);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) propsObj;

            if (properties.isEmpty()) {
                return legacyPlan(endpoint, method, params);
            }

            Map<String, Object> pathVals = new LinkedHashMap<>();
            Map<String, String> queryVals = new LinkedHashMap<>();
            Map<String, String> headerVals = new LinkedHashMap<>();
            Map<String, Object> bodyVals = new LinkedHashMap<>();

            if (params != null) {
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    String key = e.getKey();
                    Object val = e.getValue();
                    Object propDef = properties.get(key);
                    String loc = null;
                    if (propDef instanceof Map<?, ?> pd) {
                        Object in = ((Map<?, ?>) pd).get("in");
                        if (in == null) {
                            in = ((Map<?, ?>) pd).get("x-in");
                        }
                        if (in != null) {
                            loc = in.toString().trim().toLowerCase();
                        }
                    }
                    if (loc == null) {
                        loc = inferDefaultLocation(method);
                    }
                    switch (loc) {
                        case "path" -> pathVals.put(key, val);
                        case "query" -> queryVals.put(key, formatQueryValue(val, objectMapper));
                        case "body", "form" -> bodyVals.put(key, val);
                        case "header" -> headerVals.put(key, formatQueryValue(val, objectMapper));
                        default -> {
                            if ("get".equalsIgnoreCase(method)) {
                                queryVals.put(key, formatQueryValue(val, objectMapper));
                            } else {
                                bodyVals.put(key, val);
                            }
                        }
                    }
                }
            }

            String url = applyPathTemplate(endpoint, pathVals);
            boolean sendBody = !bodyVals.isEmpty();
            Object bodyPayload = singleBodyWrapperIfNeeded(bodyVals, properties, params);
            if (bodyPayload instanceof Map<?, ?> m && !m.isEmpty()) {
                sendBody = true;
            }
            return new Plan(url, queryVals, headerVals, bodyPayload, sendBody);
        } catch (Exception ex) {
            log.warn("解析 requestSchema 失败，使用兼容模式: {}", ex.getMessage());
            return legacyPlan(endpoint, method, params);
        }
    }

    private static Object singleBodyWrapperIfNeeded(Map<String, Object> bodyVals,
                                                    Map<String, Object> properties,
                                                    Map<String, Object> originalParams) {
        if (bodyVals.size() == 1) {
            Map.Entry<String, Object> only = bodyVals.entrySet().iterator().next();
            Object def = properties.get(only.getKey());
            if (def instanceof Map<?, ?> pd) {
                boolean looksLikeObject = "object".equals(String.valueOf(((Map<?, ?>) pd).get("type")))
                        && ((Map<?, ?>) pd).containsKey("properties");
                if (looksLikeObject && originalParams != null) {
                    Object v = originalParams.get(only.getKey());
                    if (v instanceof Map<?, ?>) {
                        return v;
                    }
                }
            }
        }
        return bodyVals.isEmpty() ? null : bodyVals;
    }

    private static String inferDefaultLocation(String method) {
        if (method == null || "get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method)) {
            return "query";
        }
        return "body";
    }

    static Plan legacyPlan(String endpoint, String method, Map<String, Object> params) {
        String url = endpoint;
        Map<String, String> query = new LinkedHashMap<>();
        Map<String, Object> body = new LinkedHashMap<>();
        if (params != null) {
            List<String> pathKeys = extractPathKeys(endpoint);
            Map<String, Object> pathVals = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (pathKeys.contains(e.getKey())) {
                    pathVals.put(e.getKey(), e.getValue());
                }
            }
            url = applyPathTemplate(url, pathVals);
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (pathVals.containsKey(e.getKey())) {
                    continue;
                }
                if ("get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method)) {
                    query.put(e.getKey(), formatQueryValue(e.getValue(), new ObjectMapper()));
                } else {
                    body.put(e.getKey(), e.getValue());
                }
            }
        }
        Object bodyPayload = body.isEmpty() ? null : body;
        return new Plan(url, query, Map.of(), bodyPayload, bodyPayload != null && !body.isEmpty());
    }

    private static List<String> extractPathKeys(String endpoint) {
        List<String> keys = new ArrayList<>();
        Matcher m = PATH_TEMPLATE.matcher(endpoint);
        while (m.find()) {
            keys.add(m.group(1));
        }
        return keys;
    }

    static String applyPathTemplate(String endpoint, Map<String, Object> pathVals) {
        if (endpoint == null || pathVals == null || pathVals.isEmpty()) {
            return endpoint;
        }
        String out = endpoint;
        for (Map.Entry<String, Object> e : pathVals.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            String placeholder = "{" + e.getKey() + "}";
            out = out.replace(placeholder, urlEncodePathSegment(String.valueOf(e.getValue())));
        }
        return out;
    }

    private static String urlEncodePathSegment(String raw) {
        try {
            return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return raw;
        }
    }

    private static String formatQueryValue(Object v, ObjectMapper objectMapper) {
        if (v == null) {
            return "";
        }
        if (v instanceof String s) {
            return s;
        }
        if (v instanceof Map || v instanceof Iterable) {
            try {
                return objectMapper.writeValueAsString(v);
            } catch (Exception e) {
                return String.valueOf(v);
            }
        }
        return String.valueOf(v);
    }

    /**
     * 拼 queryString（带 ? 前缀，若无参数则返回空串）。
     */
    public static String buildQueryString(Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        for (Map.Entry<String, String> e : query.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            sb.append(urlEncodeQueryName(e.getKey()))
                    .append('=')
                    .append(urlEncodeQueryValue(e.getValue()))
                    .append('&');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static String urlEncodeQueryName(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String urlEncodeQueryValue(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
