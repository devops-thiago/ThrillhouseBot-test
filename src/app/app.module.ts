import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import { OnCallDashboardComponent } from './components/on-call-dashboard/on-call-dashboard.component';
import { HandoffNoteComponent } from './components/handoff-note/handoff-note.component';

@NgModule({
  declarations: [AppComponent, OnCallDashboardComponent, HandoffNoteComponent],
  imports: [BrowserModule, FormsModule, HttpClientModule],
  bootstrap: [AppComponent],
})
export class AppModule {}
