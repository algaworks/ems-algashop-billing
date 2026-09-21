package com.algaworks.algashop.billing.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.type.TypeFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class KafkaConsumerTypeIdIdentifier {

    private final JavaType genericJsonType = TypeFactory.createDefaultInstance().constructType(ObjectNode.class);
    private Map<String, JavaType> javaTypeMap = new HashMap<>();

    private static KafkaConsumerTypeIdIdentifier instance;

    public KafkaConsumerTypeIdIdentifier(
            @Value("${spring.kafka.properties.spring.json.type.mapping:}")
            String typeMappings) {
        javaTypeMap = createMappings(typeMappings);
        instance = this;
    }

    public static JavaType readType(byte[] data, Headers headers) {
        return instance.readTypeByHeader(headers);
    }

    public JavaType readTypeByHeader(Headers headers) {
        if (headers == null) {
            return genericJsonType;
        }

        Header typeId = headers.lastHeader("__TypeId__");

        if (typeId == null) {
            return genericJsonType;
        }

        String typeIdString = new String(typeId.value());
        JavaType javaType = javaTypeMap.get(typeIdString);

        if (javaType == null) {
            log.warn("No class found for __TypeId__={}, applying generic.", typeIdString);
            return genericJsonType;
        }

        return javaType;
    }

    protected Map<String, JavaType> createMappings(String mappings) {
        Map<String, JavaType> mappingsMap = new HashMap<>();
        String[] array = StringUtils.commaDelimitedListToStringArray(mappings);
        for (String entry : array) {
            String[] split = entry.split(":");
            Assert.isTrue(split.length == 2, "Each comma-delimited mapping entry must have exactly one ':'");
            try {
                String typeId = split[0].trim();
                Class<?> javaClass = ClassUtils.forName(split[1].trim(), ClassUtils.getDefaultClassLoader());
                JavaType javaType = TypeFactory.createDefaultInstance().constructType(javaClass);
                mappingsMap.put(typeId, javaType);
            } catch (ClassNotFoundException | LinkageError e) {
                throw new IllegalArgumentException("Failed to load: " + split[1] + " for " + split[0], e);
            }
        }
        return mappingsMap;
    }
}
