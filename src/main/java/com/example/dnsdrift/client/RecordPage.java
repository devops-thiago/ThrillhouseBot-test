package com.example.dnsdrift.client;

import com.example.dnsdrift.model.DnsRecord;
import java.util.List;

/** One page of results from the registrar's record-listing endpoint. {@code nextCursor} is null on the last page. */
public record RecordPage(List<DnsRecord> records, String nextCursor) {

    public boolean hasMore() {
        return nextCursor != null;
    }
}
