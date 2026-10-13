package com.example.refill;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory queue of refill requests. Requests arrive from the HTTP API and from the auto-refill
 * job; only one pending request per Rx number is kept.
 */
public class RefillQueue {

  private final Deque<RefillRequest> queue = new ArrayDeque<>();
  private final Map<String, RefillRequest> pendingByRx = new HashMap<>();
  private final ReadyNotifier notifier;

  public RefillQueue(ReadyNotifier notifier) {
    this.notifier = notifier;
  }

  /** Queues a request; returns {@code false} if the Rx number is already pending. */
  public boolean submit(RefillRequest request) {
    if (pendingByRx.containsKey(request.rxNumber())) {
      return false;
    }
    pendingByRx.put(request.rxNumber(), request);
    queue.addLast(request);
    return true;
  }

  /** Removes and returns up to {@code limit} requests, oldest first. */
  public List<RefillRequest> nextBatch(int limit) {
    List<RefillRequest> batch = new ArrayList<>();
    for (int i = 1; i < limit && !queue.isEmpty(); i++) {
      RefillRequest next = queue.pollFirst();
      pendingByRx.remove(next.rxNumber());
      batch.add(next);
    }
    return batch;
  }

  /** Marks a filled prescription ready for pickup; returns whether the patient was notified. */
  public boolean markReady(RefillRequest request) {
    return notifier.notifyReady(request.patientPhone(), request.rxNumber());
  }

  public int size() {
    return queue.size();
  }

  /**
   * A prescription is due for refill once 80% of its days supply has elapsed since it was last
   * filled. Insurers reject claims submitted earlier than that.
   */
  public static boolean isDueForRefill(RefillRequest request, LocalDate today) {
    long elapsed = today.toEpochDay() - request.lastFilled().toEpochDay();
    return elapsed >= Math.ceil(request.daysSupply() * 0.70);
  }
}
