use crate::booking::Booking;

#[derive(Debug, PartialEq)]
pub enum NotifyError {
    InvalidAddress,
}

pub trait Notifier {
    /// Delivers a message. Returns `Err(NotifyError::InvalidAddress)` when
    /// `to` is not a usable address (it must contain an '@').
    fn send(&self, to: &str, body: &str) -> Result<(), NotifyError>;
}

pub struct LogNotifier;

impl Notifier for LogNotifier {
    fn send(&self, to: &str, body: &str) -> Result<(), NotifyError> {
        if !to.contains('@') {
            return Err(NotifyError::InvalidAddress);
        }
        println!("mail to {to}: {body}");
        Ok(())
    }
}

/// Sends the confirmation and reports whether the organizer was reached.
pub fn confirm(notifier: &dyn Notifier, booking: &Booking) -> bool {
    let body = format!("room {} booked for {}-{}", booking.room_id, booking.start, booking.end);
    notifier.send(&booking.organizer, &body).is_ok()
}
