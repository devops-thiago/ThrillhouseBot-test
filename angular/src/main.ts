import { provideHttpClient } from "@angular/common/http";
import { bootstrapApplication } from "@angular/platform-browser";
import { AppComponent } from "./app/app.component.ts";

bootstrapApplication(AppComponent, { providers: [provideHttpClient()] }).catch((err) => console.error(err));
