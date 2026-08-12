package com.commons.security.context;

/**
 * UserSessionKeyResolver derives a stable per-user key from the authenticated
 * caller. Agent services use it to key conversation memory and prepared-action
 * state, so the same user always maps to the same session bucket while two
 * different users can never share one.
 */
public class UserSessionKeyResolver {

    private final AuthenticatedCallerProvider callerProvider;

    public UserSessionKeyResolver(AuthenticatedCallerProvider callerProvider) {
        this.callerProvider = callerProvider;
    }

    /**
     * @return the JWT subject of the current caller, used as the session key
     * @throws IllegalStateException when no authenticated caller exists
     */
    public String resolve() {
        return callerProvider.currentCaller().subject();
    }
}
