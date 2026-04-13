/**
 * Error Pages — Standalone components for 403, 404, 500 errors.
 * Premium design with animations and clear CTAs.
 */
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-error-403',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-[calc(100vh-4rem)] flex items-center justify-center px-4">
      <div class="text-center animate-slide-up max-w-md">
        <div class="text-8xl font-display font-black bg-gradient-to-r from-accent-rose to-orange-400 bg-clip-text text-transparent mb-4">403</div>
        <h1 class="text-2xl font-bold text-white mb-3">Access Denied</h1>
        <p class="text-surface-400 mb-8 text-sm leading-relaxed">
          You don't have permission to access this page. If you believe this is an error, please contact the system administrator.
        </p>
        <div class="flex items-center justify-center gap-3">
          <a (click)="goHome()" class="btn-primary !py-2.5 !px-6 text-sm cursor-pointer">Go Home</a>
          <a routerLink="/dashboard" class="btn-secondary !py-2.5 !px-6 text-sm">Dashboard</a>
        </div>
      </div>
    </div>
  `
})
export class Error403Component {
  private auth = inject(AuthService);
  private router = inject(Router);

  goHome() {
    if (this.auth.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
    } else if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/']);
    }
  }
}

@Component({
  selector: 'app-error-404',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-[calc(100vh-4rem)] flex items-center justify-center px-4">
      <div class="text-center animate-slide-up max-w-md">
        <div class="text-8xl font-display font-black bg-gradient-to-r from-omni-400 to-accent-teal bg-clip-text text-transparent mb-4">404</div>
        <h1 class="text-2xl font-bold text-white mb-3">Page Not Found</h1>
        <p class="text-surface-400 mb-8 text-sm leading-relaxed">
          The page you're looking for doesn't exist or has been moved. Let's get you back on track.
        </p>
        <div class="flex items-center justify-center gap-3">
          <a (click)="goHome()" class="btn-primary !py-2.5 !px-6 text-sm cursor-pointer">Go Home</a>
          <a routerLink="/recharge" class="btn-secondary !py-2.5 !px-6 text-sm">Recharge Now</a>
        </div>
      </div>
    </div>
  `
})
export class Error404Component {
  private auth = inject(AuthService);
  private router = inject(Router);

  goHome() {
    if (this.auth.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
    } else if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/']);
    }
  }
}

@Component({
  selector: 'app-error-500',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-[calc(100vh-4rem)] flex items-center justify-center px-4">
      <div class="text-center animate-slide-up max-w-md">
        <div class="text-8xl font-display font-black bg-gradient-to-r from-orange-400 to-accent-rose bg-clip-text text-transparent mb-4">500</div>
        <h1 class="text-2xl font-bold text-white mb-3">Server Error</h1>
        <p class="text-surface-400 mb-8 text-sm leading-relaxed">
          Something went wrong on our end. Our team has been notified. Please try again in a moment.
        </p>
        <div class="flex items-center justify-center gap-3">
          <button (click)="tryAgain()" class="btn-primary !py-2.5 !px-6 text-sm">Try Again</button>
          <a (click)="goHome()" class="btn-secondary !py-2.5 !px-6 text-sm cursor-pointer">Go Home</a>
        </div>
      </div>
    </div>
  `
})
export class Error500Component {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  tryAgain() {
    const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
    this.router.navigateByUrl(returnUrl);
  }

  goHome() {
    if (this.auth.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
    } else if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/']);
    }
  }
}
