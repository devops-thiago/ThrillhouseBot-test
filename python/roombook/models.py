"""Plain data types shared across the service."""
from dataclasses import dataclass


@dataclass(frozen=True)
class Booking:
    id: int
    room_id: int
    organizer: str
    start: int  # epoch seconds
    end: int  # epoch seconds
    status: str = "pending"


@dataclass(frozen=True)
class Room:
    id: int
    name: str
    building: str
    capacity: int
