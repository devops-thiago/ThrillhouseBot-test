package tech.roombook;

import java.util.List;

/** Remote directory of bookable rooms. Results are served in pages. */
public interface RoomDirectory {

    /** One page of rooms; {@code hasMore} is true while further pages exist. */
    record Page(List<Room> rooms, int page, boolean hasMore) {
    }

    /** Returns the requested zero-based page. Callers must keep asking until hasMore is false. */
    Page listRooms(int page);
}
