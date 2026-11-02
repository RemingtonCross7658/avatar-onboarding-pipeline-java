package com.example.avatar;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import javax.net.ssl.*;

public final class AvatarBinaryTest {
    public static void main(String[] args) throws Exception {
        byte[] binary = {(byte) 0x89, 'P', 'N', 'G', 13, 10, (byte) 0xff, 0};
        Path source = Files.createTempFile(Path.of("."), "avatar-test-", ".png");
        try {
            Files.write(source, binary);
            HttpClient fake = new FakeClient(binary);
            String result = new AvatarOnboardingService(new InfraiClient(fake, "test"))
                    .process("local-user", source);
            if (!"image-3".equals(result)) throw new AssertionError(result);
        } finally { Files.delete(source); }
        System.out.println("Binary upload and response regression tests passed");
    }

    private static final class FakeClient extends HttpClient {
        private final byte[] binary;
        private int calls;
        FakeClient(byte[] binary) { this.binary = binary; }
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            calls++;
            CompletableFuture<String> body = new CompletableFuture<>();
            request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
                final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
                public void onNext(ByteBuffer buffer) {
                    byte[] data = new byte[buffer.remaining()]; buffer.get(data); bytes.writeBytes(data);
                }
                public void onError(Throwable error) { body.completeExceptionally(error); }
                public void onComplete() { body.complete(bytes.toString(java.nio.charset.StandardCharsets.UTF_8)); }
            });
            Map<?, ?> payload = Json.object(body.join());
            if (calls == 1 && !Arrays.equals(binary, Base64.getDecoder().decode((String) payload.get("file")))) {
                throw new AssertionError("Binary upload was corrupted");
            }
            if (calls == 2 && !"image-1".equals(payload.get("image"))) throw new AssertionError();
            if (calls == 3 && !"image-2".equals(payload.get("image"))) throw new AssertionError();
            String json = "{\"ok\" : true, \"data\" : {\"id\" : \"image-" + calls + "\"}}";
            return new HttpResponse<T>() {
                public int statusCode() { return 200; }
                public HttpRequest request() { return request; }
                public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
                public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a,b) -> true); }
                @SuppressWarnings("unchecked") public T body() { return (T) json; }
                public Optional<SSLSession> sslSession() { return Optional.empty(); }
                public URI uri() { return request.uri(); }
                public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
            };
        }
        public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        public Optional<Duration> connectTimeout() { return Optional.empty(); }
        public Redirect followRedirects() { return Redirect.NEVER; }
        public Optional<ProxySelector> proxy() { return Optional.empty(); }
        public SSLContext sslContext() { throw new UnsupportedOperationException(); }
        public SSLParameters sslParameters() { return new SSLParameters(); }
        public Optional<Authenticator> authenticator() { return Optional.empty(); }
        public Version version() { return Version.HTTP_1_1; }
        public Optional<Executor> executor() { return Optional.empty(); }
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) { throw new UnsupportedOperationException(); }
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { throw new UnsupportedOperationException(); }
    }
}
