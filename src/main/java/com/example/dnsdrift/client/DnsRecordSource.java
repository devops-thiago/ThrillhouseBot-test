package com.example.dnsdrift.client;

import com.example.dnsdrift.model.DnsRecord;
import java.io.IOException;
import java.util.List;

/** Source of the DNS records currently published for a domain. */
public interface DnsRecordSource {

    /**
     * Returns every DNS record currently published for {@code domain}. Implementations
     * must return an empty list (never {@code null}) when the domain has no records.
     */
    List<DnsRecord> fetchAllRecords(String domain) throws IOException, InterruptedException;
}
