package com.example.avatar;

import java.lang.reflect.Method;
import java.util.Map;

public final class JsonTest {
    public static void main(String[] args) throws Exception {
        String response = "{\n \"metadata\": {\"id\": \"wrong\"}, \"ok\" : true,"
                + " \"data\" : {\"id\" : \"image-\\u0031\\\"\\\\end\","
                + " \"other\": [null, false, -1.25e2]}}";
        Map<?, ?> parsed = Json.object(response);
        if (!Boolean.TRUE.equals(parsed.get("ok"))) throw new AssertionError("Whitespace in ok");
        Method value = AvatarOnboardingService.class.getDeclaredMethod("value", String.class, String.class);
        value.setAccessible(true);
        if (!"image-1\"\\end".equals(value.invoke(null, response, "id"))) {
            throw new AssertionError("Must read and unescape data.id only");
        }
        if (!Boolean.FALSE.equals(Json.object("{\"ok\":false,\"data\":{\"ok\":true}}").get("ok"))) {
            throw new AssertionError("Must use envelope ok");
        }
        System.out.println("JSON response regression tests passed");
    }
}
