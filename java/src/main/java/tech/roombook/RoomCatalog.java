package tech.roombook;

import java.util.HashMap;
import java.util.Map;

public class RoomCatalog {
    private final Map<String, Room> rooms = new HashMap<>();

    public RoomCatalog(RoomDirectory directory) {
        RoomDirectory.Page first = directory.listRooms(0);
        for (Room room : first.rooms()) {
            rooms.put(room.id(), room);
        }
    }

    public Room find(String roomId) {
        return rooms.get(roomId);
    }

    public int size() {
        return rooms.size();
    }
}
