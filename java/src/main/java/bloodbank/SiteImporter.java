package bloodbank;

import java.util.List;

/** Pulls a site's inventory feed into the tracker. */
public class SiteImporter {

    private final InventoryApi api;
    private final ExpiryTracker tracker;

    public SiteImporter(InventoryApi api, ExpiryTracker tracker) {
        this.api = api;
        this.tracker = tracker;
    }

    /** Imports every unit the site reports, following the cursor to the last page. */
    public int importSite(String site) {
        InventoryApi.Page page = api.fetchPage(site, null);
        List<BloodUnit> units = page.items();
        int added = 0;
        for (BloodUnit u : units) {
            if (tracker.register(u)) {
                added++;
            }
        }
        return added;
    }
}
