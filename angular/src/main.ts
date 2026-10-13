import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { apiAuthInterceptor } from './app/api-auth.interceptor';
import { RecallBoardComponent } from './app/recall-board.component';

bootstrapApplication(RecallBoardComponent, {
  providers: [provideHttpClient(withInterceptors([apiAuthInterceptor]))],
}).catch((err) => console.error(err));
