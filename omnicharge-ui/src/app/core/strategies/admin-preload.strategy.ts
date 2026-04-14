import { Injectable, inject } from '@angular/core';
import { PreloadingStrategy, Route } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AdminPreloadStrategy implements PreloadingStrategy {
  private authService = inject(AuthService);

  preload(route: Route, fn: () => Observable<any>): Observable<any> {
    // Check if route has our custom preloadAdmin flag
    if (route.data?.['preloadAdmin']) {
      // Only preload if the current user is an admin
      const user = this.authService.currentUser();
      if (user && user.role === 'ROLE_ADMIN') {
        return fn();
      }
    }
    // Check if route has standard preload flag (for other non-auth routes)
    if (route.data?.['preload']) {
       return fn();
    }
    return of(null);
  }
}
