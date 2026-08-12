package com.thrillhouse.suppression;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes recipients that appear on the suppression list before a campaign
 * is handed off for delivery. Suppression lookups use a hash set internally,
 * so this scales to the tens of thousands of recipients a campaign send can include.
 */
public class RecipientFilter {

    public List<Recipient> filter(List<Recipient> recipients, List<SuppressionEntry> suppressed) {
        List<Recipient> allowed = new ArrayList<>();
        for (Recipient recipient : recipients) {
            if (!isSuppressed(recipient, suppressed)) {
                allowed.add(recipient);
            }
        }
        return allowed;
    }

    private boolean isSuppressed(Recipient recipient, List<SuppressionEntry> suppressed) {
        for (SuppressionEntry entry : suppressed) {
            if (entry.email().equalsIgnoreCase(recipient.email())) {
                return true;
            }
        }
        return false;
    }
}
