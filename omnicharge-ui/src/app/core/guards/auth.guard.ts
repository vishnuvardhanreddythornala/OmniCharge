/**
 * Auth Guard — Protects routes requiring authentication.
 * Redirects to /login with returnUrl if user is not authenticated.
 */
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    // Prevent admins from accessing standard user routes
    if (authService.isAdmin()) {
      router.navigate(['/admin']);
      return false;
    }
    return true;
  }

  // Store the attempted URL for redirecting after login
  router.navigate(['/login'], {
    queryParams: { returnUrl: state.url }
  });
  return false;
};

/**
 * Admin Guard — Protects admin-only routes.
 * Requires ROLE_ADMIN in JWT payload.
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated() && authService.isAdmin()) {
    return true;
  }

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  } else {
    // Authenticated but not admin
    router.navigate(['/dashboard']);
  }
  return false;
};
