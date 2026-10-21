package bloodbank;

import java.util.List;

/** Remote inventory feed from a collection site. Results are paginated. */
public interface InventoryApi {

    /** One page of results; nextCursor is null on the last page. */
    record Page(List<BloodUnit> items, String nextCursor) { }

    /** Fetches a page for a site. Pass a null cursor for the first page. */
    Page fetchPage(String site, String cursor);
}
