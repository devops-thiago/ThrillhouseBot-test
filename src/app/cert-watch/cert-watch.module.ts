import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CertWatchComponent } from './cert-watch.component';
import { CertItemComponent } from './cert-item/cert-item.component';

@NgModule({
  declarations: [CertWatchComponent, CertItemComponent],
  imports: [CommonModule],
  exports: [CertWatchComponent],
})
export class CertWatchModule {}
