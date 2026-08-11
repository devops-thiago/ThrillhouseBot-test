import { Component, Input } from '@angular/core';
import { TeamMember } from '../models/team-member.model';

@Component({
  selector: 'app-member-card',
  templateUrl: './member-card.component.html',
})
export class MemberCardComponent {
  @Input() member!: TeamMember;

  toggleFavorite(): void {
    this.member.isFavorite = !this.member.isFavorite;
  }
}
