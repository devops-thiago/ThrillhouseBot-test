import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DeliveryLogComponent } from './delivery-log.component';

@NgModule({
  declarations: [DeliveryLogComponent],
  imports: [CommonModule],
  exports: [DeliveryLogComponent],
})
export class WebhooksModule {}
