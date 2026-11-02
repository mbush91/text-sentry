package org.textsentry.app;

/** No purchase redirect, beta bypass or sandbox receipt alone can activate managed setup. */
final class SubscriptionOnboarding {
    private SubscriptionOnboarding() { }

    static boolean shouldRetrieveKey(boolean setupContinuation, boolean alreadyFetching,
                                     AccountStatusClient.Status account, boolean sandboxBuild) {
        return setupContinuation && !alreadyFetching && account != null
                && account.managedAccess && account.subscriptionValidForBuild(sandboxBuild);
    }
}
