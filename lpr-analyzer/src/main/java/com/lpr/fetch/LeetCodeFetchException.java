package com.lpr.fetch;

/**
 * Thrown when the unofficial LeetCode GraphQL endpoint can't be reached,
 * returns an error, or returns no data for a given username.
 * Callers should catch this and offer manual entry as a fallback.
 */
public class LeetCodeFetchException extends Exception {
    public LeetCodeFetchException(String message) {
        super(message);
    }

    public LeetCodeFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
