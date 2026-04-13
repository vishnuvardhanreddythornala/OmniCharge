/**
 * AuthInterceptor — Functional HTTP Interceptor (Angular 17+)
 *
 * Rules:
 *  1. Public API paths (auth, operators, plans) → NO token injection.
 *  2. All other requests → Attach `Authorization: Bearer <jwt>` header.
 *  3. On 401 response → Attempt silent refresh with request queuing.
 *     - Only ONE refresh request is fired at a time.
 *     - All concurrent 401s are queued and replayed with the new token.
 *     - If refresh fails → all queued requests are rejected and user is logged out.
 */
import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Paths that never receive an Authorization header */
const PUBLIC_PATHS = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/google',
  '/api/auth/refresh-token',
  '/api/auth/forgot-password',
  '/api/auth/verify-otp',
  '/api/auth/reset-password',
  '/api/operators/',
  '/api/operators/detect',
  '/api/operators/active',
  '/api/plans/',
];

function isPublicUrl(url: string): boolean {
  return PUBLIC_PATHS.some(path => url.includes(path));
}

/** Module-level state for refresh token queuing */
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Skip token injection for public endpoints
  if (isPublicUrl(req.url)) {
    return next(req);
  }

  const token = authService.getAccessToken();

  // If no token and trying to hit a protected route, pass through
  if (!token) {
    return next(req);
  }

  // Clone request and attach Bearer token
  const authedReq = addTokenToRequest(req, token);

  return next(authedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return handle401Error(req, next, authService, router);
      }
      return throwError(() => error);
    })
  );
};

/**
 * Handles 401 errors with concurrent request queuing.
 * Only the first 401 triggers a refresh; subsequent 401s queue up.
 */
function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router
): Observable<any> {
  if (!isRefreshing) {
    // First 401 — initiate refresh
    isRefreshing = true;
    refreshTokenSubject.next(null); // Signal that refresh is in progress

    return authService.refreshToken().pipe(
      switchMap(response => {
        isRefreshing = false;

        if (response.success && response.data) {
          const newToken = response.data.accessToken;
          refreshTokenSubject.next(newToken); // Unblock queued requests
          return next(addTokenToRequest(req, newToken));
        }

        // Refresh didn't return valid data — force logout
        refreshTokenSubject.next(null);
        forceLogout(authService, router);
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }),
      catchError(refreshError => {
        // Refresh failed — force logout and reject all queued
        isRefreshing = false;
        refreshTokenSubject.next(null);
        forceLogout(authService, router);
        return throwError(() => refreshError);
      })
    );
  } else {
    // Refresh already in progress — queue this request
    return refreshTokenSubject.pipe(
      filter(token => token !== null), // Wait until the refresh completes
      take(1), // Only take the first emission (the new token)
      switchMap(newToken => next(addTokenToRequest(req, newToken!)))
    );
  }
}

/** Helper to clone a request with Authorization header */
function addTokenToRequest(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}

/** Force logout — navigate to login with return URL */
function forceLogout(authService: AuthService, router: Router): void {
  authService.logout();
  router.navigate(['/login'], {
    queryParams: { returnUrl: router.url }
  });
}
