import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TeamDirectoryComponent } from './team-directory.component';
import { MemberCardComponent } from './member-card/member-card.component';

@NgModule({
  declarations: [TeamDirectoryComponent, MemberCardComponent],
  imports: [CommonModule, FormsModule],
  exports: [TeamDirectoryComponent],
})
export class TeamDirectoryModule {}
