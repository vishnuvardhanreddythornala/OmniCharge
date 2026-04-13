import { ErrorHandler, Injectable, NgZone, Injector } from '@angular/core';
import { ToastService } from './toast.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  // Use Injector because ErrorHandler is instantiated before providers are fully resolved
  constructor(private injector: Injector, private zone: NgZone) {}

  handleError(error: any): void {
    // 1. Extract error details
    const message = error.message || error.toString();
    const stack = error.stack || '';

    // 2. Format for Sentry/LogRocket in the future
    /* 
      SENTRY INTEGRATION HOOK (When ready to implement):
      if (environment.production) {
        Sentry.captureException(error);
      }
    */

    // 3. Log to console for debugging (always good practice)
    console.error(' [Global Error Handler] Caught Exception:', error);

    // 4. Show user-friendly toast notification
    this.zone.run(() => {
      try {
        const toast = this.injector.get(ToastService);

        // Ignore chunk load errors (common on flaky networks) — silently reload
        if (message.includes('Loading chunk') || message.includes('ChunkLoadError')) {
          console.warn('Chunk load error (likely network blip). Reloading...');
          toast.warning('Connection interrupted. Reloading page...');
          setTimeout(() => window.location.reload(), 1500);
          return;
        }

        // Ignore Angular router navigation cancellation (normal behavior)
        if (message.includes('NavigationCancel') || message.includes('NG04002')) {
          return;
        }

        // Ignore HTTP errors — these are handled by the error interceptor or local component subscribes
        if (error?.status || error?.name === 'HttpErrorResponse' || message.includes('Http failure')) {
          return;
        }

        // Show generic error for all other uncaught exceptions
        toast.error('An unexpected error occurred. Please try again in a moment.');
      } catch {
        // If the toast service itself fails, fall back to console
        console.error('Could not display toast for error:', message);
      }
    });
  }
}
