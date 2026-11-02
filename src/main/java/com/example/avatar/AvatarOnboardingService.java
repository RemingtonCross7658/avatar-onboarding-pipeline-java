package com.example.avatar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.Base64;
import java.util.Map;

public final class AvatarOnboardingService {
    // Canonical capability marker: infrai.image.smart_crop
    private final InfraiClient infrai;

    public AvatarOnboardingService(InfraiClient infrai) {
        this.infrai = infrai;
    }

    public String process(String userId, Path source) throws IOException, InterruptedException {
        String requestId = UUID.randomUUID().toString();
        String uploaded = infrai.post("/image/upload", "{\"file\":\"" + Base64.getEncoder().encodeToString(Files.readAllBytes(source))
                + "\",\"filename\":\"" + escape(source.getFileName().toString()) + "\"}", requestId);
        String image = value(uploaded, "id");
        String cropped = infrai.post("/image/smart_crop", InfraiClient.json(
                "image", quote(image), "aspect", "1"), requestId);
        String optimized = infrai.post("/image/resize", InfraiClient.json(
                "image", quote(value(cropped, "id")), "width", "512", "height", "512",
                "fit", quote("cover"), "enlarge", "false", "format", quote("webp"), "store", "true"), requestId);
        String avatarId = value(optimized, "id");
        infrai.patch("/auth/user/update/" + userId, InfraiClient.json(
                "user_id", quote(userId),
                "metadata", "{\"avatar_image\":" + quote(avatarId) + "}"), requestId);
        return avatarId;
    }

    static boolean shouldReplace(String currentAvatarId, String processedAvatarId) {
        return processedAvatarId != null && !processedAvatarId.isBlank()
                && !processedAvatarId.equals(currentAvatarId);
    }

    private static String value(String json, String key) {
        Object data = Json.object(json).get("data");
        if (data instanceof Map) {
            Object value = ((Map<?, ?>) data).get(key);
            if (value instanceof String && !((String) value).isBlank()) return (String) value;
        }
        throw new IllegalArgumentException("Missing " + key + " in Infrai data");
    }

    private static String quote(String value) { return "\"" + escape(value) + "\""; }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
