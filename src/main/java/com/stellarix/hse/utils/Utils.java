package com.stellarix.hse.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class Utils {

    /**
     * Email of the currently authenticated HSE user, or null outside an authenticated
     * request (e.g. the public supervisor-closure endpoint). Must be called synchronously
     * — SecurityContextHolder's ThreadLocal isn't propagated into @Async methods, so this
     * has to be read in the controller/service caller and passed down as a parameter.
     */
    public static String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }
}
