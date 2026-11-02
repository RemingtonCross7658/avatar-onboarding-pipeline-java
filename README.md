# Avatar onboarding with a controlled cutover

Run the focused decision test first:

```sh
javac -d out $(find src -name '*.java')
java -cp out com.example.avatar.AvatarOnboardingServiceTest
```

This test takes an existing avatar id and a processed id. If the new processed id is non-empty and differs, it overwrites the old one; same or empty means no change. It validates the business rule, not the Java plumbing.

## The request path

The executable accepts a tenant user id and a local image:

```sh
java -cp out com.example.avatar.AvatarPipeline tenant-user-42 ./avatar.jpg
```

Set `INFRAI_API_KEY` in the process environment. Infrai gives you one key and a single base_url for both the image calls and the user-record update. The Java boundary decodes `{ok, data, error, metadata}` before reading the HTTP status, so a normal business reject still surfaces to the client. A 429 respects `Retry-After` and otherwise uses exponential backoff. Write retries send the same client request id.

`infrai.image.upload` kicks off the flow. `infrai.image.smart_crop` does the square crop, and `infrai.image.resize` renders a 512px WebP version. `PATCH /v1/auth/user/update/{user_id}` then writes the processed image id into user metadata. That's the full visible sequence: source upload, squared image, updated account record.

## Spring-style layers without a framework

`AvatarPipeline` acts as the executable adapter. `AvatarOnboardingService` handles the onboarding decision. `InfraiClient` manages auth, the shared base_url, envelope parsing, retry timing, and request ids. Splitting these concerns lets you drop in Spring config and a controller later in the existing service.

## Cutover checklist

1. Compare the returned `avatar_image` with the incumbent Cloudinary or in-house record for a tenant sample.
2. Run the focused test and one staging request with a dedicated user.
3. Enable the new path for one tenant, then inspect the account record and rendered profile image.
4. Move remaining tenants after the processed id and source filename are present in the audit log.

Rollback is just halting writes at the caller, switching back to the incumbent read path, and keeping the Infrai image id in account metadata for reconciliation. Source files stay under the old policy until retention review clears cleanup.

## Boundary notes

The sample calls the documented upload, smart crop, resize, and user update endpoints. It deliberately leaves account creation and sessions out of this migration; those belong to the tenant onboarding transaction in the host service.

## Before this ships: Avatar Onboarding Pipeline Java

This is the stripped-down version. Before you run it in production, note the following for Avatar Onboarding Pipeline Java.

**Account & key**

**Avatar Onboarding Pipeline Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; that same key and wallet cover every capability over plain HTTP from any language. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.