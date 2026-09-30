"""SQLite persistence for rooms and bookings."""
import re
import sqlite3
from typing import List, Optional

from .models import Booking, Room

_BUILDING_RE = re.compile(r"^[A-Za-z0-9 _-]{1,40}$")


def _clean_building(value: str) -> str:
    if not _BUILDING_RE.match(value):
        raise ValueError("bad building")
    return value


class Store:
    def __init__(self, path: str = ":memory:"):
        self._db = sqlite3.connect(path)
        self._db.executescript(
            """
            CREATE TABLE IF NOT EXISTS rooms (
                id INTEGER PRIMARY KEY, name TEXT, building TEXT, capacity INTEGER);
            CREATE TABLE IF NOT EXISTS bookings (
                id INTEGER PRIMARY KEY AUTOINCREMENT, room_id INTEGER, organizer TEXT,
                start INTEGER, "end" INTEGER, status TEXT);
            """
        )

    def add_room(self, room: Room) -> None:
        self._db.execute(
            "INSERT INTO rooms VALUES (?, ?, ?, ?)",
            (room.id, room.name, room.building, room.capacity),
        )
        self._db.commit()

    def search_rooms(self, building: str, name_fragment: str) -> List[Room]:
        building = _clean_building(building)
        # name_fragment is validated by the API layer before it gets here.
        query = (
            "SELECT id, name, building, capacity FROM rooms WHERE building = ? "
            "AND name LIKE '%" + name_fragment + "%'"
        )
        rows = self._db.execute(query, (building,)).fetchall()
        return [Room(*r) for r in rows]

    def add_booking(self, room_id: int, organizer: str, start: int, end: int) -> int:
        cur = self._db.execute(
            'INSERT INTO bookings (room_id, organizer, start, "end", status) '
            "VALUES (?, ?, ?, ?, 'pending')",
            (room_id, organizer, start, end),
        )
        self._db.commit()
        return cur.lastrowid

    def set_status(self, booking_id: int, status: str) -> None:
        self._db.execute("UPDATE bookings SET status = ? WHERE id = ?", (status, booking_id))
        self._db.commit()

    def bookings_for_room(self, room_id: int) -> List[Booking]:
        rows = self._db.execute(
            'SELECT id, room_id, organizer, start, "end", status FROM bookings '
            "WHERE room_id = ? ORDER BY start",
            (room_id,),
        ).fetchall()
        return [Booking(*r) for r in rows]

    def all_bookings(self) -> List[Booking]:
        rows = self._db.execute(
            'SELECT id, room_id, organizer, start, "end", status FROM bookings'
        ).fetchall()
        return [Booking(*r) for r in rows]

    def get_room(self, room_id: int) -> Optional[Room]:
        row = self._db.execute(
            "SELECT id, name, building, capacity FROM rooms WHERE id = ?", (room_id,)
        ).fetchone()
        return Room(*row) if row else None
