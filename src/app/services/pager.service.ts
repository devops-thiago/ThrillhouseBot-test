import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Contact, PageResult } from '../models/shift.model';

/**
 * Sends a push/SMS page to an on-call contact's device. Delivery can fail
 * when the contact has no phone number on file or the carrier rejects the
 * message, so callers must check `delivered` before assuming the page went
 * out.
 */
@Injectable({ providedIn: 'root' })
export class PagerService {
  page(contact: Contact, message: string): Observable<PageResult> {
    if (!contact.phoneNumber) {
      return of({ delivered: false, retryCount: 0 });
    }

    // A real integration would call the carrier gateway here.
    return of({ delivered: true, retryCount: 0 });
  }
}
