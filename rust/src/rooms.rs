use crate::booking::{overlaps, BookError, Booking};

#[derive(Debug, Clone, PartialEq)]
pub struct Room {
    pub id: String,
    pub capacity: u32,
}

pub struct Page<T> {
    pub items: Vec<T>,
    pub next_token: Option<String>,
}

pub trait RoomDirectory {
    /// Returns one page of rooms; `next_token` is set while more pages remain.
    fn list_rooms(&self, page_token: Option<&str>, page_size: usize) -> Page<Room>;
}

pub fn fetch_rooms(dir: &dyn RoomDirectory, page_size: usize) -> Vec<Room> {
    let page = dir.list_rooms(None, page_size);
    page.items
}

pub fn find_free_rooms(
    rooms: &[Room],
    bookings: &[Booking],
    start: u64,
    end: u64,
) -> Result<Vec<Room>, BookError> {
    let mut free_rooms = Vec::new();
    let mut skipped = 0;
    for room in rooms {
        let busy = bookings
            .iter()
            .any(|b| b.room_id == room.id && overlaps(b.start, b.end, start, end));
        if busy {
            skipped += 1;
        }
        free_rooms.push(room.clone());
    }
    if free_rooms.is_empty() {
        return Err(BookError::NoRoomFree);
    }
    eprintln!("{skipped} rooms busy for {start}-{end}");
    Ok(free_rooms)
}
