/**
 * App Configuration — Angular 17+ standalone bootstrap.
 * Provides HttpClient with interceptor chain and router.
 *
 * Interceptor order:
 *  1. httpCacheInterceptor — Returns cached GET responses (avoids duplicate requests)
 *  2. authInterceptor      — Injects Bearer token + handles 401 refresh
 *  3. errorInterceptor     — Catches 403/404/500 and routes to error pages
 */
import { ApplicationConfig, ErrorHandler } from '@angular/core';
import { provideRouter, withViewTransitions, withPreloading, PreloadAllModules, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { httpCacheInterceptor } from './core/interceptors/http-cache.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { GlobalErrorHandler } from './core/services/global-error-handler.service';
import { AdminPreloadStrategy } from './core/strategies/admin-preload.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    provideRouter(
      routes, 
      withViewTransitions(),
      withPreloading(AdminPreloadStrategy),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' })
    ),
    provideHttpClient(withInterceptors([
      httpCacheInterceptor,
      authInterceptor,
      errorInterceptor,
    ])),
  ]
};
