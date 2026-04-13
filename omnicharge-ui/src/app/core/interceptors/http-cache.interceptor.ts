/**
 * HTTP Cache Interceptor — Caches GET responses for static-ish endpoints.
 *
 * Strategy:
 *  - Caches GET requests to public plan/operator endpoints for 5 minutes (TTL).
 *  - Requests with 'Cache-Control: no-cache' header bypass the cache.
 *  - Cache is automatically invalidated after TTL expiry.
 *  - Only caches successful (2xx) responses.
 */
import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of, tap } from 'rxjs';

interface CacheEntry {
  response: HttpResponse<unknown>;
  expiry: number;
}

const cache = new Map<string, CacheEntry>();
const TTL_MS = 5 * 60 * 1000; // 5 minutes

/** Paths eligible for caching (public, rarely changing data) */
const CACHEABLE_PATTERNS = [
  '/api/operators/active',
  '/api/plans/search',
];

function isCacheable(url: string): boolean {
  return CACHEABLE_PATTERNS.some(pattern => url.includes(pattern));
}

export const httpCacheInterceptor: HttpInterceptorFn = (req, next) => {
  // Only cache GET requests
  if (req.method !== 'GET') {
    return next(req);
  }

  // Skip if caller explicitly wants fresh data
  if (req.headers.has('Cache-Control') && req.headers.get('Cache-Control') === 'no-cache') {
    cache.delete(req.urlWithParams);
    return next(req);
  }

  // Only cache whitelisted endpoints
  if (!isCacheable(req.url)) {
    return next(req);
  }

  const cacheKey = req.urlWithParams;
  const cached = cache.get(cacheKey);

  // Return cached response if still valid
  if (cached && cached.expiry > Date.now()) {
    return of(cached.response.clone());
  }

  // Otherwise, make the request and cache the response
  return next(req).pipe(
    tap(event => {
      if (event instanceof HttpResponse && event.ok) {
        cache.set(cacheKey, {
          response: event.clone(),
          expiry: Date.now() + TTL_MS,
        });
      }
    })
  );
};

/** Utility to manually clear the HTTP cache (e.g., after admin mutations) */
export function clearHttpCache(): void {
  cache.clear();
}
