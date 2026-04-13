/**
 * Error Interceptor — Global HTTP error handler.
 *
 * Strategy:
 *  - 401 errors are handled by the auth interceptor (token refresh) — skipped here.
 *  - 403/404 on non-API page navigations → route to error pages.
 *  - 5xx errors → show toast notification instead of hard redirect.
 *    This prevents transient backend hiccups (e.g., notification polling)
 *    from disrupting the user's current workflow.
 */
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 is handled by auth interceptor — skip here
      if (error.status === 401) {
        return throwError(() => error);
      }

      switch (error.status) {
        case 400:
          // Intercept Spring validation errors format
          if (error.error && error.error.errors) {
            const validationErrors = Object.values(error.error.errors).join(' | ');
            error.error.message = `Validation failed: ${validationErrors}`;
          }
          break;
        case 403:
          // Only redirect for page-level 403 failures,
          // not for API 403s (those are handled by component error handlers, e.g. profile-incomplete)
          if (!req.url.includes('/api/')) {
            router.navigate(['/error/403']);
          }
          break;
        case 404:
          // Only redirect for page-level navigation failures,
          // not for API 404s (those are handled by component error handlers)
          if (!req.url.includes('/api/')) {
            router.navigate(['/error/404']);
          }
          break;
        case 500:
        case 502:
        case 503:
        case 504:
          // Show a toast instead of navigating away — prevents workflow disruption
          toast.error('Something went wrong on our server. Please try again in a moment.');
          break;
        case 0:
          // Server down or network error
          toast.warning('The server is temporarily unreachable. Please try again later.');
          break;
      }

      return throwError(() => error);
    })
  );
};
