#[derive(Debug, PartialEq)]
pub enum NotifyError {
    NoRecipients,
}

/// Sends an alert message. Implementations must return
/// `NotifyError::NoRecipients` when `recipients` is empty.
pub trait Notifier {
    fn send(&self, recipients: &[String], message: &str) -> Result<usize, NotifyError>;
}

pub struct SmsNotifier;

impl Notifier for SmsNotifier {
    fn send(&self, recipients: &[String], message: &str) -> Result<usize, NotifyError> {
        if recipients.is_empty() {
            return Err(NotifyError::NoRecipients);
        }
        for r in recipients {
            println!("sms to {}: {}", r, message);
        }
        Ok(recipients.len())
    }
}
