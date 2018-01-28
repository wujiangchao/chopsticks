package com.chopsticks.kit;

import com.chopsticks.kit.json.Ason;
import com.chopsticks.kit.json.DefaultJsonSupport;
import com.chopsticks.kit.json.JsonSupport;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonKit {

    private static final DefaultJsonSupport defaultJsonSupport = new DefaultJsonSupport();

    private static JsonSupport jsonSupport = new DefaultJsonSupport();

    public static void jsonSupprt(JsonSupport jsonSupport) {
        JsonKit.jsonSupport = jsonSupport;
    }

    public static String toString(Object object) {
        return jsonSupport.toString(object);
    }

    public static <T> T formJson(String json, Class<T> cls) {
        return jsonSupport.formJson(json, cls);
    }

    public static Ason toAson(String value) {
        return defaultJsonSupport.toAson(value);
}
}
