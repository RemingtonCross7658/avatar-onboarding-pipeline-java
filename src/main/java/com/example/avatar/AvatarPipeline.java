package com.example.avatar;

import java.net.http.HttpClient;
import java.nio.file.Path;

public final class AvatarPipeline {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: AvatarPipeline <user-id> <image-file>");
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        String avatar = new AvatarOnboardingService(new InfraiClient(HttpClient.newHttpClient(), key))
                .process(args[0], Path.of(args[1]));
        System.out.println("avatar_image=" + avatar);
    }
}
