import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, Router, NavigationEnd, NavigationStart, NavigationCancel, NavigationError } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { SeoService } from './core/services/seo.service';

import { ConfirmDialogComponent } from './shared/components/confirm-dialog/confirm-dialog.component';
import { ToastComponent } from './shared/components/toast/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, NavbarComponent, ConfirmDialogComponent, ToastComponent],
  template: `
    <!-- ═══════════ WELCOME SPLASH (shown during initial route load) ═══════════ -->
    @if (isInitialLoad()) {
      <div class="welcome-splash" id="welcome-splash">
        <div class="welcome-content">
          <!-- Logo -->
          <div class="welcome-logo">
            <div class="welcome-icon">
              <svg class="w-8 h-8 text-surface-900" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
          </div>

          <!-- Brand Name -->
          <h1 class="welcome-title">
            Omni<span class="welcome-title-accent">Charge</span>
          </h1>

          <!-- Tagline -->
          <p class="welcome-tagline">Instant Mobile Recharges</p>

          <!-- Loading Bar -->
          <div class="welcome-loader-track">
            <div class="welcome-loader-bar"></div>
          </div>
        </div>
      </div>
    }

    <!-- ═══════════ MAIN APP (hidden during initial load) ═══════════ -->
    @if (!isInitialLoad()) {
      <div class="min-h-screen bg-surface relative page-enter">
        <app-confirm-dialog />
        <app-toast />
        <!-- Background ambient glow effects -->
        <div class="fixed inset-0 pointer-events-none overflow-hidden z-0">
          <div class="absolute -top-40 -right-40 w-96 h-96 bg-omni-600/10 rounded-full blur-[128px] animate-pulse-slow"></div>
          <div class="absolute top-1/3 -left-20 w-72 h-72 bg-omni-800/10 rounded-full blur-[100px] animate-pulse-slow" style="animation-delay: 1s"></div>
          <div class="absolute bottom-0 right-1/4 w-80 h-80 bg-accent-teal/5 rounded-full blur-[120px] animate-pulse-slow" style="animation-delay: 2s"></div>
        </div>

        @if (!isAdminRoute()) {
          <!-- Navigation -->
          <app-navbar class="relative z-50" />
        }

        <!-- Main Content -->
        <main class="relative z-20">
          <router-outlet />
        </main>

        @if (!isAdminRoute()) {
          <!-- Footer -->
          <footer class="relative z-10 border-t border-white/[0.06] mt-20">
            <div class="section-container py-12">
              <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
                <!-- Brand -->
                <div class="md:col-span-2">
                  <a routerLink="/" class="flex items-center gap-2.5 group mb-3 w-fit">
                    <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-omni-500 via-omni-400 to-accent-teal
                                flex items-center justify-center shadow-[0_0_10px_rgba(20,184,166,0.3)] border border-surface-200 relative overflow-hidden">
                      <svg class="w-4 h-4 text-surface-900" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
                      </svg>
                    </div>
                    <span class="text-xl font-display font-bold text-surface-900 tracking-tight">
                      Omni<span class="text-transparent bg-clip-text bg-gradient-to-r from-omni-400 to-accent-teal">Charge</span>
                    </span>
                  </a>
                  <p class="text-surface-500 text-sm leading-relaxed max-w-md">
                    India's smartest recharge platform. Instant mobile recharges, automatic operator detection,
                    and secure payments — all in one place.
                  </p>
                </div>
                <!-- Quick Links -->
                <div>
                  <h4 class="text-sm font-semibold text-surface-600 uppercase tracking-wider mb-4">Quick Links</h4>
                  <ul class="space-y-2.5">
                    <li><a routerLink="/recharge" class="text-sm text-surface-500 hover:text-surface-900 transition-colors">Recharge Now</a></li>
                    <li><a routerLink="/dashboard" class="text-sm text-surface-500 hover:text-surface-900 transition-colors">My Dashboard</a></li>
                  </ul>
                </div>
                <!-- Support -->
                <div>
                  <h4 class="text-sm font-semibold text-surface-600 uppercase tracking-wider mb-4">Support</h4>
                  <ul class="space-y-2.5">
                    <li><a href="mailto:omnicharge.app@gmail.com" class="text-sm text-surface-500 hover:text-surface-900 transition-colors">omnicharge.app&#64;gmail.com</a></li>
                    <li><span class="text-sm text-surface-500">24×7 Customer Support</span></li>
                  </ul>
                </div>
              </div>
              <div class="mt-10 pt-6 border-t border-white/[0.06] flex flex-col sm:flex-row justify-between items-center gap-4">
                <p class="text-xs text-surface-500">&copy; 2026 OmniCharge. All rights reserved.</p>
                <div class="flex items-center gap-4">
                  <span class="text-xs text-surface-600">Terms of Service</span>
                  <span class="text-xs text-surface-600">•</span>
                  <span class="text-xs text-surface-600">Privacy Policy</span>
                  <span class="text-xs text-surface-600">•</span>
                  <span class="text-xs text-surface-600">Powered by Razorpay</span>
                </div>
              </div>
            </div>
          </footer>
        }
      </div>
    }
  `,
  styles: [`
    .welcome-splash {
      position: fixed;
      inset: 0;
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #0b0f1a;
      animation: splashFadeIn 0.3s ease-out;
    }

    .welcome-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
    }

    .welcome-logo {
      animation: logoFloat 2s ease-in-out infinite;
    }

    .welcome-icon {
      width: 72px;
      height: 72px;
      border-radius: 20px;
      background: linear-gradient(135deg, #6366f1, #818cf8, #14b8a6);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 60px rgba(99, 102, 241, 0.4), 0 0 120px rgba(20, 184, 166, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .welcome-title {
      font-family: 'Outfit', sans-serif;
      font-size: 2.5rem;
      font-weight: 800;
      color: white;
      letter-spacing: -0.02em;
      margin-top: 16px;
    }

    .welcome-title-accent {
      background: linear-gradient(to right, #818cf8, #14b8a6);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .welcome-tagline {
      font-family: 'Inter', sans-serif;
      font-size: 0.95rem;
      color: rgba(255, 255, 255, 0.4);
      margin-top: 2px;
      letter-spacing: 0.05em;
    }

    .welcome-loader-track {
      width: 180px;
      height: 3px;
      background: rgba(255, 255, 255, 0.06);
      border-radius: 4px;
      overflow: hidden;
      margin-top: 28px;
    }

    .welcome-loader-bar {
      width: 40%;
      height: 100%;
      background: linear-gradient(90deg, #6366f1, #14b8a6);
      border-radius: 4px;
      animation: loaderSlide 1.5s ease-in-out infinite;
    }

    @keyframes splashFadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes logoFloat {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-8px); }
    }

    @keyframes loaderSlide {
      0% { transform: translateX(-100%); }
      50% { transform: translateX(250%); }
      100% { transform: translateX(-100%); }
    }
  `]
})
export class AppComponent {
  private router = inject(Router);
  private seoService = inject(SeoService);

  /** True during the very first route load (before ANY NavigationEnd fires) */
  isInitialLoad = signal(true);

  constructor() {
    this.seoService.init();

    // Listen for the first NavigationEnd to dismiss the splash screen
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError) {
        if (this.isInitialLoad()) {
          // Small delay for a smooth transition
          setTimeout(() => this.isInitialLoad.set(false), 200);
        }
      }
    });
  }

  isAdminRoute = toSignal(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map((event: any) => event.urlAfterRedirects.startsWith('/admin'))
    ),
    { initialValue: typeof window !== 'undefined' ? window.location.pathname.startsWith('/admin') : false }
  );
}

