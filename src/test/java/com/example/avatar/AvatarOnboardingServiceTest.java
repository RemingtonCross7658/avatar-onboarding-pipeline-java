package com.example.avatar;

public final class AvatarOnboardingServiceTest {
    public static void main(String[] args) {
        check(AvatarOnboardingService.shouldReplace("old-avatar", "new-avatar"));
        check(!AvatarOnboardingService.shouldReplace("same-avatar", "same-avatar"));
        check(!AvatarOnboardingService.shouldReplace("old-avatar", ""));
        System.out.println("Avatar replacement decision tests passed");
    }

    private static void check(boolean result) {
        if (!result) throw new AssertionError("avatar replacement decision was incorrect");
    }
}
