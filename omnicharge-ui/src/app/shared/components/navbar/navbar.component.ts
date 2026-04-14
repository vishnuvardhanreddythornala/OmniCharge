/**
 * NavbarComponent — Premium glassmorphic navigation bar.
 * Shows auth-aware state: Login/Register vs. User menu & notification bell.
 */
import { Component, inject, HostListener, signal, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="fixed top-0 left-0 right-0 z-50 transition-all duration-300"
         [class]="scrolled() ? 'bg-surface-900/80 backdrop-blur-xl border-b border-white/[0.06] shadow-lg' : 'bg-transparent'">
      <div class="w-full max-w-full px-4 sm:px-8">
        <div class="flex items-center justify-between h-16">

          <!-- Brand Logo -->
          <a routerLink="/" class="flex items-center gap-2.5 group">
            <img src="assets/omnicharge%20logo.png" alt="OmniCharge Logo" class="w-10 h-10 rounded-full object-cover border-[1.5px] border-white/10 shadow-[0_0_15px_rgba(20,184,166,0.3)] transform group-hover:scale-105 transition-transform duration-300 group-hover:shadow-[0_0_20px_rgba(20,184,166,0.5)]" />
          </a>

          <!-- Desktop Nav Links -->
          <div class="hidden md:flex items-center gap-1">
            @if (!authService.isAdmin()) {
              <a routerLink="/" routerLinkActive="text-white bg-white/[0.06]" [routerLinkActiveOptions]="{exact: true}"
                 class="btn-ghost text-sm">Home</a>
              <a routerLink="/recharge" routerLinkActive="text-white bg-white/[0.06]"
                 class="btn-ghost text-sm">Recharge</a>
              @if (authService.isAuthenticated()) {
                <a routerLink="/dashboard" routerLinkActive="text-white bg-white/[0.06]"
                   class="btn-ghost text-sm">Dashboard</a>
              }
            }
          </div>

          <!-- Right Actions -->
          <div class="flex items-center gap-3">
            @if (authService.isAuthenticated()) {
              <!-- Notification Bell -->
              <button (click)="toggleNotifications()" class="relative p-2 rounded-lg hover:bg-white/[0.06] transition-colors">
                <svg class="w-5 h-5 text-surface-400 hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"/>
                </svg>
                @if (notificationService.unreadCount() > 0) {
                  <span class="absolute -top-0.5 -right-0.5 w-5 h-5 bg-accent-rose text-white text-[10px] font-bold
                               rounded-full flex items-center justify-center animate-scale-in">
                    {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
                  </span>
                }
              </button>

              <!-- User Avatar Menu -->
              <div class="relative">
                <button (click)="toggleMenu()" class="flex items-center gap-2 p-1.5 rounded-xl hover:bg-white/[0.06] transition-colors">
                  <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-omni-500 to-accent-teal
                              flex items-center justify-center text-white text-xs font-bold">
                    {{ authService.userInitials() }}
                  </div>
                  <svg class="w-4 h-4 text-surface-400 hidden sm:block transition-transform duration-200"
                       [class.rotate-180]="menuOpen()" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                  </svg>
                </button>

                <!-- Dropdown -->
                @if (menuOpen()) {
                  <div class="absolute right-0 mt-2 w-56 glass-card p-2 animate-slide-down origin-top-right">
                    <div class="px-3 py-2 mb-1">
                      <p class="text-sm font-medium text-white truncate">{{ authService.currentUser()?.fullName }}</p>
                      @if (authService.currentUser()?.email) {
                        <p class="text-xs text-surface-400 truncate">{{ authService.currentUser()?.email }}</p>
                      } @else {
                        <p class="text-xs text-surface-400 truncate">{{ authService.currentUser()?.mobileNumber }}</p>
                      }
                    </div>
                    <div class="border-t border-white/[0.06] my-1"></div>
                      <a *ngIf="authService.isAdmin()" routerLink="/admin/profile" 
                         (click)="menuOpen.set(false)"
                         class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-surface-300
                                hover:bg-white/[0.06] hover:text-white transition-colors cursor-pointer">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"/>
                        </svg>
                        Settings
                      </a>
                      <button *ngIf="!authService.isAdmin()" (click)="menuOpen.set(false); navigateToProfile()"
                         class="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-surface-300
                                hover:bg-white/[0.06] hover:text-white transition-colors cursor-pointer text-left">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"/>
                        </svg>
                        Profile
                      </button>
                    <button (click)="onLogout()"
                       class="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-accent-rose
                              hover:bg-rose-500/10 transition-colors text-left">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"/>
                      </svg>
                      Sign Out
                    </button>
                  </div>
                }
              </div>
            } @else {
              <!-- Auth Buttons (Unauthenticated) -->
              <a routerLink="/login" class="btn-primary text-sm !py-2 !px-4">Sign In</a>
            }

            <!-- Mobile Hamburger -->
            <button (click)="mobileMenuOpen.set(!mobileMenuOpen())"
                    class="md:hidden p-2 rounded-lg hover:bg-white/[0.06] transition-colors">
              <svg class="w-5 h-5 text-surface-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                @if (mobileMenuOpen()) {
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                } @else {
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
                }
              </svg>
            </button>
          </div>
        </div>

        <!-- Mobile Menu Dropdown -->
        @if (mobileMenuOpen()) {
          <div class="md:hidden pb-4 animate-slide-down">
            <div class="glass-card p-3 mt-2 space-y-1">
              @if (!authService.isAdmin()) {
                <a routerLink="/" (click)="mobileMenuOpen.set(false)"
                   class="block px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-white/[0.06] hover:text-white transition">Home</a>
                <a routerLink="/recharge" (click)="mobileMenuOpen.set(false)"
                   class="block px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-white/[0.06] hover:text-white transition">Recharge</a>
              }
              @if (authService.isAuthenticated()) {
                @if (authService.isAdmin()) {
                  <a routerLink="/admin" (click)="mobileMenuOpen.set(false)"
                     class="block px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-white/[0.06] hover:text-white transition">Admin Panel</a>
                  <a routerLink="/admin/profile" (click)="mobileMenuOpen.set(false)"
                     class="block px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-white/[0.06] hover:text-white transition">Settings</a>
                } @else {
                  <a routerLink="/dashboard" (click)="mobileMenuOpen.set(false)"
                     class="block px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-white/[0.06] hover:text-white transition">Dashboard</a>
                }
              } @else {
                <a routerLink="/login" (click)="mobileMenuOpen.set(false)"
                   class="block px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-white/[0.06] hover:text-white transition">Sign In</a>
              }
            </div>
          </div>
        }
      </div>
    </nav>

    <!-- Global Mobile Number Soft Prompt Banner -->
    @if (authService.isAuthenticated() && !authService.isProfileComplete() && !authService.isAdmin()) {
      <div class="bg-omni-500/20 border-b border-omni-400/30 backdrop-blur-md fixed top-16 left-0 right-0 z-40 animate-slide-down">
        <div class="section-container py-2.5 flex flex-col sm:flex-row items-center justify-between gap-3">
          <p class="text-sm text-white/90">
            <span class="font-semibold text-omni-300">Action Required:</span> Please link your mobile number to enable recharges and payments.
          </p>
          <button (click)="navigateToProfile()"
             class="btn-primary !py-1.5 !px-4 text-xs whitespace-nowrap">
            Link Mobile Number
          </button>
        </div>
      </div>
      <!-- Extra Spacer for Banner -->
      <div class="h-10"></div>
    }

    <!-- ═══ LOGOUT CONFIRMATION MODAL ═══ -->
    @if (showLogoutModal()) {
      <div class="fixed inset-0 z-[10000] flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity animate-fade-in" (click)="cancelLogout()"></div>
        
        <!-- Modal -->
        <div class="relative w-full max-w-sm glass-card border flex flex-col items-center border-white/10 shadow-2xl rounded-3xl p-6 sm:p-8 animate-scale-in text-center">
          <div class="w-16 h-16 rounded-full bg-gradient-to-br from-surface-800 to-surface-700 mb-4 flex items-center justify-center shadow-inner border border-white/5 text-accent-rose">
            <svg class="w-8 h-8 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"></path>
            </svg>
          </div>
          <h2 class="text-xl font-bold font-display text-white mb-2">Sign Out</h2>
          <p class="text-sm font-medium text-surface-400 mb-8">
            Are you sure you want to sign out safely from OmniCharge?
          </p>
          
          <div class="w-full flex gap-3">
            <button (click)="cancelLogout()" class="flex-1 py-3 text-sm font-semibold text-surface-300 hover:text-white bg-surface-800 hover:bg-surface-700 border border-white/5 rounded-xl transition-colors">
              Cancel
            </button>
            <button (click)="confirmLogout()" class="flex-1 py-3 text-sm font-semibold text-white bg-accent-rose hover:bg-rose-600 rounded-xl transition-colors shadow-lg shadow-accent-rose/20">
              Yes, Sign Out
            </button>
          </div>
        </div>
      </div>
    }

    <!-- Spacer so content doesn't hide behind fixed navbar -->
    <div class="h-16"></div>
  `,
  styles: []
})
export class NavbarComponent {
  readonly authService = inject(AuthService);
  readonly notificationService = inject(NotificationService);
  readonly router = inject(Router);

  constructor(private elementRef: ElementRef) {}

  scrolled = signal(false);
  menuOpen = signal(false);
  mobileMenuOpen = signal(false);
  showLogoutModal = signal(false);

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!this.elementRef.nativeElement.contains(target)) {
      this.menuOpen.set(false);
      this.mobileMenuOpen.set(false);
    }
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 20);
  }

  toggleMenu(): void {
    this.menuOpen.update(v => !v);
  }

  toggleNotifications(): void {
    if (this.authService.isAdmin() && this.router.url.includes('/admin')) {
      this.router.navigate(['/admin/notifications']);
    } else {
      this.router.navigate(['/dashboard'], { queryParams: { tab: 'notifications' } });
    }
    this.mobileMenuOpen.set(false);
  }

  isOnAdminPage(): boolean {
    return this.router.url.startsWith('/admin');
  }

  onLogout(): void {
    this.menuOpen.set(false);
    this.mobileMenuOpen.set(false);
    this.showLogoutModal.set(true);
  }

  confirmLogout(): void {
    this.showLogoutModal.set(false);
    this.authService.logout();
  }

  cancelLogout(): void {
    this.showLogoutModal.set(false);
  }

  navigateToProfile(): void {
    // Force navigation even when already on /dashboard by using a unique timestamp
    this.router.navigate(['/dashboard'], { queryParams: { tab: 'profile', _t: Date.now() } });
  }
}
