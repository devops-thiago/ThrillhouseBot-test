package com.thrillhouse.suppression;

import java.util.ArrayList;
import java.util.List;

/** Splits a filtered recipient list into fixed-size batches for delivery. */
public class CampaignBatcher {

    public List<List<Recipient>> batch(List<Recipient> recipients, int batchSize) {
        List<List<Recipient>> batches = new ArrayList<>();
        for (int start = 0; start < recipients.size(); start += batchSize) {
            int end = Math.min(start + batchSize, recipients.size() - 1);
            batches.add(new ArrayList<>(recipients.subList(start, end)));
        }
        return batches;
    }
}
