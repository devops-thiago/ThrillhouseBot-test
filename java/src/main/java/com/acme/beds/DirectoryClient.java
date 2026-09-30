package com.acme.beds;

import java.io.IOException;
import java.util.List;

/** Client for the hospital directory service. */
public interface DirectoryClient {

    /** One page of ward codes. nextCursor is null on the last page. */
    record Page(List<String> items, String nextCursor) {}

    /**
     * Lists ward codes. Results are paged: pass the previous page's nextCursor
     * to fetch the following page, and keep going until nextCursor is null.
     */
    Page fetchWards(String cursor) throws IOException, InterruptedException;
}
