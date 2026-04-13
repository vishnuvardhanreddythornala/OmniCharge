import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, Router, NavigationEnd } from '@angular/router';
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
    <div class="min-h-screen bg-surface relative">
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
                              flex items-center justify-center shadow-[0_0_10px_rgba(20,184,166,0.3)] border border-white/10 relative overflow-hidden">
                    <svg class="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                  </div>
                  <span class="text-xl font-display font-bold text-white tracking-tight">
                    Omni<span class="text-transparent bg-clip-text bg-gradient-to-r from-omni-400 to-accent-teal">Charge</span>
                  </span>
                </a>
                <p class="text-surface-400 text-sm leading-relaxed max-w-md">
                  India's smartest recharge platform. Instant mobile recharges, automatic operator detection,
                  and secure payments — all in one place.
                </p>
              </div>
              <!-- Quick Links -->
              <div>
                <h4 class="text-sm font-semibold text-surface-300 uppercase tracking-wider mb-4">Quick Links</h4>
                <ul class="space-y-2.5">
                  <li><a routerLink="/recharge" class="text-sm text-surface-400 hover:text-white transition-colors">Recharge Now</a></li>
                  <li><a routerLink="/dashboard" class="text-sm text-surface-400 hover:text-white transition-colors">My Dashboard</a></li>
                </ul>
              </div>
              <!-- Support -->
              <div>
                <h4 class="text-sm font-semibold text-surface-300 uppercase tracking-wider mb-4">Support</h4>
                <ul class="space-y-2.5">
                  <li><a href="mailto:omnicharge.app@gmail.com" class="text-sm text-surface-400 hover:text-white transition-colors">omnicharge.app&#64;gmail.com</a></li>
                  <li><span class="text-sm text-surface-400">24×7 Customer Support</span></li>
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
  `,
  styles: []
})
export class AppComponent {
  private router = inject(Router);
  private seoService = inject(SeoService);
  
  constructor() {
    this.seoService.init();
  }

  isAdminRoute = toSignal(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map((event: any) => event.urlAfterRedirects.startsWith('/admin'))
    ),
    { initialValue: typeof window !== 'undefined' ? window.location.pathname.startsWith('/admin') : false }
  );
}
