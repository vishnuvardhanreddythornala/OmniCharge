/**
 * LandingComponent — Hero section with Quick Recharge widget,
 * feature highlights, and a "How it works" section.
 * Fully public — no auth required.
 */
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OperatorService } from '../../core/services/operator.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="page-enter">

      <!-- ═══════════ HERO SECTION ═══════════ -->
      <section class="relative overflow-hidden pt-12 pb-20 sm:pt-20 sm:pb-32">
        <!-- Mesh gradient background -->
        <div class="absolute inset-0 bg-mesh pointer-events-none"></div>

        <div class="section-container relative">
          <div class="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">

            <!-- Left: Copy -->
            <div class="animate-slide-up">
              <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full
                          bg-omni-50 border border-omni-200 mb-6 shadow-sm">
                <span class="w-2 h-2 rounded-full bg-accent-emerald animate-pulse"></span>
                <span class="text-xs font-medium text-omni-600 tracking-wide">Instant Recharge • Live Now</span>
              </div>

              <h1 class="text-4xl sm:text-5xl lg:text-6xl font-display font-bold leading-[1.1] mb-6">
                Recharge in
                <span class="text-gradient"> seconds,</span><br>
                not minutes.
              </h1>

              <p class="text-lg text-surface-500 leading-relaxed max-w-lg mb-8">
                Auto-detect your operator, browse the best plans, and pay securely with Razorpay.
                No account needed to explore — sign in only when you're ready to pay.
              </p>

              <div class="flex flex-wrap gap-3">
                <a routerLink="/recharge" class="btn-primary text-base !py-3.5 !px-8 flex items-center gap-2">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
                  </svg>
                  Recharge Now
                </a>
                <a routerLink="/recharge" class="btn-secondary text-base !py-3.5 !px-8">
                  Browse Plans
                </a>
              </div>

              <!-- Trust badges -->
              <div class="flex items-center gap-6 mt-10">
                <div class="flex items-center gap-2">
                  <div class="w-8 h-8 rounded-lg bg-accent-emerald/10 flex items-center justify-center">
                    <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                    </svg>
                  </div>
                  <span class="text-xs text-surface-500">Razorpay Secured</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="w-8 h-8 rounded-lg bg-omni-500/10 flex items-center justify-center">
                    <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                  </div>
                  <span class="text-xs text-surface-500">Instant Activation</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="w-8 h-8 rounded-lg bg-accent-teal/10 flex items-center justify-center">
                    <svg class="w-4 h-4 text-accent-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
                    </svg>
                  </div>
                  <span class="text-xs text-surface-500">All Operators</span>
                </div>
              </div>
            </div>

            <!-- Right: Quick Recharge Card -->
            <div class="animate-slide-up" style="animation-delay: 0.15s">
              <div class="glass-card p-6 sm:p-8 relative overflow-hidden">
                <!-- Card shine overlay -->
                <div class="absolute inset-0 bg-card-shine pointer-events-none"></div>

                <div class="relative">
                  <h2 class="text-lg font-display font-semibold mb-1">Quick Recharge</h2>
                  <p class="text-sm text-surface-500 mb-6">Enter a mobile number to get started</p>

                  <!-- Mobile Number Input -->
                  <div class="relative mb-4">
                    <div class="absolute left-4 top-1/2 -translate-y-1/2 flex items-center gap-2 pointer-events-none">
                      <span class="text-surface-500 text-sm font-medium">+91</span>
                      <div class="w-px h-5 bg-surface-100"></div>
                    </div>
                    <input type="tel"
                           [(ngModel)]="mobileNumber"
                           (input)="onMobileInput()"
                           (keydown.enter)="goToRecharge()"
                           maxlength="10"
                           placeholder="Enter mobile number"
                           class="input-field-lg !text-left !pl-[72px] !tracking-wider"
                           id="quick-recharge-input" />
                  </div>

                  <!-- Operator Detection Status -->
                  @if (mobileNumber.length === 10 && operatorService.isDetecting()) {
                    <div class="flex items-center gap-3 p-3 rounded-xl bg-white/[0.03] mb-4 animate-fade-in">
                      <div class="w-8 h-8 rounded-lg bg-omni-500/20 flex items-center justify-center">
                        <div class="w-4 h-4 border-2 border-omni-400 border-t-transparent rounded-full animate-spin"></div>
                      </div>
                      <span class="text-sm text-surface-600">Detecting operator...</span>
                    </div>
                  }

                  @if (mobileNumber.length === 10 && operatorService.selectedOperator(); as op) {
                    <div class="flex items-center justify-between p-3 rounded-xl bg-accent-emerald/5 border border-accent-emerald/20 mb-4 animate-scale-in">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-lg bg-accent-emerald/20 flex items-center justify-center">
                          <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                          </svg>
                        </div>
                        <div>
                          <p class="text-sm font-medium text-surface-900">{{ op.operatorName }}</p>
                          <p class="text-xs text-surface-500">{{ op.type }} • {{ operatorService.isManualOverride() ? 'Selected' : 'Auto-detected' }}</p>
                        </div>
                      </div>
                      <button (click)="toggleOperatorDropdown()" class="text-xs font-semibold text-omni-400 hover:text-omni-300 transition-colors px-2 py-1">
                        Change
                      </button>
                    </div>
                  } @else if (mobileNumber.length === 10 && operatorService.detectionFailed() && !operatorService.isDetecting()) {
                    <div class="flex items-center justify-between p-3 rounded-xl bg-accent-rose/5 border border-accent-rose/20 mb-4 animate-scale-in">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-lg bg-accent-rose/20 flex items-center justify-center">
                          <svg class="w-4 h-4 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                          </svg>
                        </div>
                        <div>
                          <p class="text-sm font-medium text-surface-900">Detection Failed</p>
                          <p class="text-[10px] text-surface-500">Please select your operator</p>
                        </div>
                      </div>
                      <button (click)="toggleOperatorDropdown()" class="text-xs font-semibold text-omni-400 hover:text-omni-300 transition-colors px-2 py-1">
                        Select
                      </button>
                    </div>
                  }

                  <!-- Manual Operator Selection Dropdown -->
                  @if (showOperatorDropdown()) {
                    <div class="mb-5 animate-slide-down">
                      <p class="text-xs font-medium text-surface-500 mb-2 px-1">Select your operator</p>
                      <div class="grid grid-cols-2 gap-2">
                        @for (op of operatorService.operators(); track op.id) {
                          <button (click)="selectManualOperator(op)"
                                  class="flex items-center gap-2.5 p-2.5 rounded-xl border transition-all duration-300"
                                  [class]="operatorService.selectedOperator()?.operatorId === op.id ? 'bg-omni-500/10 border-omni-500 shadow-glow' : 'glass-card border-white/[0.05] hover:bg-white/[0.08]'">
                            <span class="text-xs font-bold font-display tracking-wide text-surface-900">{{ op.name }}</span>
                          </button>
                        }
                      </div>
                    </div>
                  }

                  <!-- CTA Button -->
                  <button (click)="goToRecharge()"
                          [disabled]="mobileNumber.length < 10"
                          class="btn-primary w-full text-base !py-3.5 flex items-center justify-center gap-2">
                    @if (operatorService.selectedOperator()) {
                      <span>View Plans</span>
                    } @else {
                      <span>Continue</span>
                    }
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ═══════════ HOW IT WORKS ═══════════ -->
      <section class="py-20">
        <div class="section-container">
          <div class="text-center mb-14">
            <h2 class="text-3xl sm:text-4xl font-display font-bold mb-4">
              How it <span class="text-gradient">works</span>
            </h2>
            <p class="text-surface-500 max-w-lg mx-auto">Three simple steps to recharge any mobile number instantly.</p>
          </div>

          <div class="grid md:grid-cols-3 gap-6">
            @for (step of steps; track step.num) {
              <div class="glass-card-hover p-6 text-center group" style="animation-delay: {{step.num * 0.1}}s">
                <div class="w-14 h-14 mx-auto mb-5 rounded-2xl flex items-center justify-center
                            bg-gradient-to-br {{step.gradient}} shadow-lg
                            group-hover:scale-110 transition-transform duration-300">
                  <span class="text-2xl">{{ step.icon }}</span>
                </div>
                <div class="text-xs font-semibold text-omni-400 uppercase tracking-widest mb-2">Step {{ step.num }}</div>
                <h3 class="text-lg font-semibold mb-2">{{ step.title }}</h3>
                <p class="text-sm text-surface-500 leading-relaxed">{{ step.desc }}</p>
              </div>
            }
          </div>
        </div>
      </section>

      <!-- ═══════════ FEATURES ═══════════ -->
      <section class="py-20">
        <div class="section-container">
          <div class="text-center mb-14">
            <h2 class="text-3xl sm:text-4xl font-display font-bold mb-4">
              Built for <span class="text-gradient-warm">modern recharges</span>
            </h2>
            <p class="text-surface-500 max-w-lg mx-auto">Every feature you need in a recharge platform, nothing you don't.</p>
          </div>

          <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
            @for (feature of features; track feature.title) {
              <div class="glass-card-hover p-5 group">
                <div class="flex items-start gap-4">
                  <div class="w-10 h-10 shrink-0 rounded-xl flex items-center justify-center
                              {{feature.bg}} transition-transform duration-300 group-hover:scale-110">
                    <span class="text-lg">{{ feature.icon }}</span>
                  </div>
                  <div>
                    <h3 class="text-sm font-semibold mb-1">{{ feature.title }}</h3>
                    <p class="text-xs text-surface-500 leading-relaxed">{{ feature.desc }}</p>
                  </div>
                </div>
              </div>
            }
          </div>
        </div>
      </section>

      <!-- ═══════════ CTA SECTION ═══════════ -->
      <section class="py-20">
        <div class="section-container">
          <div class="glass-card relative overflow-hidden p-8 sm:p-12 text-center">
            <div class="absolute inset-0 bg-gradient-to-br from-omni-600/20 to-accent-teal/10 pointer-events-none"></div>
            <div class="relative">
              <h2 class="text-3xl sm:text-4xl font-display font-bold mb-4">
                Ready to recharge?
              </h2>
              <p class="text-surface-600 mb-8 max-w-md mx-auto">
                Join thousands of users who trust OmniCharge for instant, secure mobile recharges.
              </p>
              <a routerLink="/recharge" class="btn-primary text-base !py-3.5 !px-10 inline-flex items-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
                Start Recharge
              </a>
            </div>
          </div>
        </div>
      </section>

    </div>
  `,
  styles: []
})
export class LandingComponent implements OnInit {
  readonly operatorService = inject(OperatorService);
  private router = inject(Router);

  mobileNumber = '';
  showOperatorDropdown = signal(false);

  steps = [
    { num: 1, icon: '📱', title: 'Enter Number', desc: 'Type any mobile number. We auto-detect the operator instantly using Numverify.', gradient: 'from-omni-600/30 to-omni-500/10' },
    { num: 2, icon: '📋', title: 'Choose a Plan', desc: 'Browse curated plans by category — Popular, Data, Unlimited, Talktime.', gradient: 'from-accent-teal/30 to-accent-teal/10' },
    { num: 3, icon: '✅', title: 'Pay & Done', desc: 'Pay securely with Razorpay. Your recharge is activated within seconds.', gradient: 'from-accent-emerald/30 to-accent-emerald/10' },
  ];

  features = [
    { icon: '🔍', title: 'Auto Operator Detection', desc: 'Just enter a number — we identify the operator instantly.', bg: 'bg-omni-500/10' },
    { icon: '⚡', title: 'Instant Activation', desc: 'Recharges go live within seconds of payment.', bg: 'bg-accent-amber/10' },
    { icon: '🔒', title: 'Razorpay Secured', desc: 'Enterprise-grade payment security with Razorpay.', bg: 'bg-accent-emerald/10' },
    { icon: '📊', title: 'Transaction History', desc: 'Track every recharge and payment in your dashboard.', bg: 'bg-accent-sky/10' },
    { icon: '🔔', title: 'Smart Notifications', desc: 'Get notified about recharge status and receipts.', bg: 'bg-accent-rose/10' },
    { icon: '🌐', title: 'All Operators', desc: 'Supports Jio, Airtel, Vi, BSNL, and more.', bg: 'bg-accent-teal/10' },
  ];

  ngOnInit(): void {
    this.operatorService.loadActiveOperators();
  }

  onMobileInput(): void {
    // Remove non-digits
    this.mobileNumber = this.mobileNumber.replace(/\D/g, '');
    this.showOperatorDropdown.set(false); // Hide manual selection on new input
    
    // Auto-detect when 10 digits entered
    if (this.mobileNumber.length === 10) {
      this.operatorService.detectOperator(this.mobileNumber).subscribe();
    } else {
      this.operatorService.clearSelection();
    }
  }

  toggleOperatorDropdown(): void {
    this.showOperatorDropdown.update((v: boolean) => !v);
    if (this.showOperatorDropdown() && this.operatorService.operators().length === 0) {
      this.operatorService.loadActiveOperators();
    }
  }

  selectManualOperator(operator: any): void {
    this.operatorService.setManualOperator(operator);
    this.showOperatorDropdown.set(false);
  }

  goToRecharge(): void {
    if (this.mobileNumber.length === 10) {
      const queryParams: any = { mobile: this.mobileNumber };
      if (this.operatorService.selectedOperator()) {
         queryParams.operatorId = this.operatorService.selectedOperator()?.operatorId;
      }
      this.router.navigate(['/recharge'], { queryParams });
    }
  }
}
