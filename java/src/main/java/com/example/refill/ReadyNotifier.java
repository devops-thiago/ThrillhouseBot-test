package com.example.refill;

/** Tells a patient their prescription is ready for pickup. */
public interface ReadyNotifier {

  /** Returns {@code true} if the message was accepted for delivery. */
  boolean notifyReady(String phone, String rxNumber);
}
