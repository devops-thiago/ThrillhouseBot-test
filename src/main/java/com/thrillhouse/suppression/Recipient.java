package com.thrillhouse.suppression;

/** A single recipient targeted by a campaign send. */
public record Recipient(String email, String campaignId) {

    /** Domain portion of the email address, or "" if there is no '@'. */
    public String domain() {
        int at = email.indexOf('@');
        return at < 0 ? "" : email.substring(at + 1);
    }
}
