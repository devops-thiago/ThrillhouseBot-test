import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { apiAuthInterceptor } from './app/api-auth.interceptor';
import { BookingBoardComponent } from './app/booking-board.component';

bootstrapApplication(BookingBoardComponent, {
  providers: [provideHttpClient(withInterceptors([apiAuthInterceptor]))],
}).catch((err) => console.error(err));
