import { Component, Inject, OnDestroy, OnInit } from '@angular/core';

import { APP_CONFIG, AppConfig } from '../../app-config.token';
import { PostmortemApiService } from '../../services/postmortem-api.service';
import { TimelineLinkService } from '../../services/timeline-link.service';
import {
  ActionItem,
  BoardRow,
  Postmortem,
  Reviewer,
  SignoffRequest,
} from '../../models/postmortem.model';
import { buildRows, filterBySeverity } from '../../utils/board-row.util';
import { alreadySignedOff, blockersFor } from '../../utils/signoff-readiness.util';

@Component({
  selector: 'app-review-board',
  templateUrl: './review-board.component.html',
})
export class ReviewBoardComponent implements OnInit, OnDestroy {
  rows: BoardRow[] = [];
  severityFilter = 'all';
  selected: Postmortem | null = null;
  reviewer: Reviewer | null = null;
  loadFailed = false;
  submitting = false;
  signoffError: string | null = null;

  private refreshTimer: number | undefined;

  constructor(
    private readonly api: PostmortemApiService,
    private readonly links: TimelineLinkService,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  ngOnInit(): void {
    this.api.fetchCurrentReviewer().subscribe((reviewer) => (this.reviewer = reviewer));
    this.refresh();
    this.refreshTimer = window.setInterval(() => this.refresh(), this.config.refreshMs);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer !== undefined) {
      window.clearInterval(this.refreshTimer);
    }
  }

  refresh(): void {
    this.api.fetchBoard().subscribe({
      next: (page) => {
        this.loadFailed = false;
        this.rows = buildRows(page.items, Date.now(), this.config.signoffsRequiredForSev1);

        // A postmortem that left the board while the reviewer was reading it
        // should not stay open in the detail pane.
        if (this.selected !== null && !this.rows.some((row) => row.postmortem.id === this.selected?.id)) {
          this.selected = null;
        }
      },
      error: () => (this.loadFailed = true),
    });
  }

  get visibleRows(): BoardRow[] {
    return filterBySeverity(this.rows, this.severityFilter);
  }

  get boardSummary(): string {
    return `${this.visibleRows.length} postmortems awaiting review`;
  }

  /**
   * Loads the full record. The board listing carries neither the narrative nor
   * the action items, so opening a row is a second request.
   */
  open(row: BoardRow): void {
    this.signoffError = null;
    this.api.fetchPostmortem(row.postmortem.id).subscribe({
      next: (postmortem) => (this.selected = postmortem),
      error: () => (this.signoffError = 'That postmortem could not be opened.'),
    });
  }

  get selectedBlockers(): string[] {
    return this.selected === null ? [] : blockersFor(this.selected);
  }

  get selectedAlreadySignedOff(): boolean {
    if (this.selected === null || this.reviewer === null) {
      return false;
    }

    return alreadySignedOff(this.selected, this.reviewer.id);
  }

  recordSignoff(request: SignoffRequest): void {
    const postmortem = this.selected;
    if (postmortem === null) {
      return;
    }

    this.submitting = true;
    this.api.recordSignoff(postmortem.id, request).subscribe({
      next: (signoff) => {
        this.submitting = false;
        this.signoffError = null;
        postmortem.signoffs = [...postmortem.signoffs, signoff];
        this.refresh();
      },
      error: () => {
        this.submitting = false;
        this.signoffError = 'The sign-off was not recorded. Try again in a moment.';
      },
    });
  }

  timelineUrl(postmortem: Postmortem): string | null {
    return this.links.timelineUrlFor(postmortem.incidentId);
  }

  trackerUrl(item: ActionItem): string | null {
    return this.links.trackerUrlFor(item.trackerKey);
  }
}
