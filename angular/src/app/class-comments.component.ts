import { Component, Input, OnChanges, SecurityContext } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { ClassScheduleService } from './class-schedule.service';
import { ClassComment } from './models';

interface RenderedComment {
  id: string;
  author: string;
  html: SafeHtml;
}

const MENTION = /@([a-z0-9_]{2,30})/gi;

/** Turns @handles in a member comment into profile links. */
function linkMentions(text: string): string {
  return text.replace(MENTION, (_m, handle: string) => `<a href="/members/${handle}">@${handle}</a>`);
}

@Component({
  selector: 'gym-class-comments',
  standalone: true,
  template: `
    <ul class="comments">
      @for (c of rendered; track c.id) {
        <li>
          <strong>{{ c.author }}</strong>
          <div class="comment-body" [innerHTML]="c.html"></div>
        </li>
      }
    </ul>
  `,
})
export class ClassCommentsComponent implements OnChanges {
  @Input({ required: true }) classId!: string;

  rendered: RenderedComment[] = [];

  constructor(
    private readonly schedule: ClassScheduleService,
    private readonly sanitizer: DomSanitizer,
  ) {}

  async ngOnChanges(): Promise<void> {
    const comments = await this.schedule.listComments(this.classId);
    this.rendered = comments.map((c) => this.render(c));
  }

  private render(comment: ClassComment): RenderedComment {
    const author = this.sanitizer.sanitize(SecurityContext.HTML, comment.authorName) ?? '';
    const memberText = comment.body;
    const withMentions = linkMentions(memberText);
    const paragraphs = withMentions.split('\n').join('<br>');
    return {
      id: comment.id,
      author,
      html: this.sanitizer.bypassSecurityTrustHtml(paragraphs),
    };
  }
}
