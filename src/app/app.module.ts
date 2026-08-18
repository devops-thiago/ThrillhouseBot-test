import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import { ReviewBoardComponent } from './components/review-board/review-board.component';
import { SignoffFormComponent } from './components/signoff-form/signoff-form.component';

@NgModule({
  declarations: [AppComponent, ReviewBoardComponent, SignoffFormComponent],
  imports: [BrowserModule, FormsModule, HttpClientModule],
  bootstrap: [AppComponent],
})
export class AppModule {}
