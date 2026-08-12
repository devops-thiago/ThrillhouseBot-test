import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IncidentTimelineComponent } from './incident-timeline.component';
import { EventItemComponent } from './event-item/event-item.component';

@NgModule({
  declarations: [IncidentTimelineComponent, EventItemComponent],
  imports: [CommonModule],
  exports: [IncidentTimelineComponent],
})
export class IncidentTimelineModule {}
