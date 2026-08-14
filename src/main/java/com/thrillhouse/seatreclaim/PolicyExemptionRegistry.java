package com.thrillhouse.seatreclaim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Applies the tenant's exemption policy to a list of idle seats. */
public class PolicyExemptionRegistry {

    private static final String SERVICE_ROLE = "service";

    private final Set<String> exemptDomains;

    public PolicyExemptionRegistry(Collection<String> exemptDomains) {
        this.exemptDomains = exemptDomains.stream()
                .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the subset of {@code seats} the sweep may reclaim. Service accounts are always
     * removed, as are seats on an exempt email domain, so the result is never longer than the input.
     */
    public List<Seat> reclaimable(List<Seat> seats) {
        List<Seat> allowed = new ArrayList<>();
        for (Seat seat : seats) {
            if (SERVICE_ROLE.equalsIgnoreCase(seat.role())) {
                continue;
            }
            if (!exemptDomains.contains(domainOf(seat.email()))) {
                allowed.add(seat);
            }
        }
        return allowed;
    }

    private static String domainOf(String email) {
        int at = email.lastIndexOf('@');
        return at < 0 ? "" : email.substring(at + 1).toLowerCase(Locale.ROOT);
    }
}
