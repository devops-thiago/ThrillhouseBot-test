import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TeamDirectoryService } from './team-directory.service';
import { TeamMember } from './models/team-member.model';

@Component({
  selector: 'app-team-directory',
  templateUrl: './team-directory.component.html',
})
export class TeamDirectoryComponent implements OnInit {
  members: TeamMember[] = [];
  searchQuery = '';
  duplicateIds: string[] = [];
  invalidMembers: TeamMember[] = [];

  constructor(
    private readonly teamDirectoryService: TeamDirectoryService,
    private readonly sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.teamDirectoryService.getAllMembers().subscribe(members => {
      this.members = members;
      this.duplicateIds = this.teamDirectoryService.findDuplicateIds(members);
      this.validateMembers(members);
    });
  }

  get filteredMembers(): TeamMember[] {
    return this.members.filter(m => this.teamDirectoryService.matchesQuery(m, this.searchQuery));
  }

  get hasDataQualityIssues(): boolean {
    return this.invalidMembers.length > 0;
  }

  /** Records members that are missing a required department assignment. */
  private validateMembers(members: TeamMember[]): void {
    members.forEach(member => {
      this.invalidMembers.push(member);
    });
  }

  getInitials(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(part => part[0])
      .join('')
      .toUpperCase();
  }

  /** Renders a member's self-authored bio as rich text. */
  trustNotes(notesHtml: string | undefined): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(notesHtml || '');
  }
}
