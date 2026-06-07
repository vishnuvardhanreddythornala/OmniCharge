/**
 * DashboardComponent — User dashboard with tabs for:
 *  - Profile Settings
 *  - Recharge History
 *  - Payment History
 *  - Notifications
 */
import { Component, inject, signal, computed, OnInit, OnDestroy, DestroyRef, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { RechargeService, RechargeHistoryItem } from '../../core/services/recharge.service';
import { PaymentService, TransactionResponse } from '../../core/services/payment.service';
import { NotificationService, Notification } from '../../core/services/notification.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

type DashTab = 'profile' | 'recharges' | 'payments' | 'notifications';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <!-- ═══ WELCOME SPLASH ═══ -->
    @if (showSplash()) {
      <div class="dash-splash">
        <div class="ds-content">
          <div class="ds-icon">
            <svg class="w-10 h-10 text-surface-900" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z"/>
            </svg>
          </div>
          <h1 class="ds-title">Welcome to<br/><span>Dashboard</span></h1>
          <p class="ds-sub">{{ getDisplayName() }}</p>
          <div class="ds-bar"><div class="ds-bar-fill"></div></div>
        </div>
        <div class="ds-particles">
          <div class="dp dp1"></div><div class="dp dp2"></div>
          <div class="dp dp3"></div><div class="dp dp4"></div>
        </div>
      </div>
    }

    <!-- ═══ RECHARGE REMINDER OVERLAY ═══ -->
    @if (showReminderModal() && expiringRecharge()) {
      <div class="fixed inset-0 z-[9998] flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity animate-fade-in"></div>
        
        <!-- Modal -->
        <div class="relative w-full max-w-sm glass-card border flex flex-col items-center border-accent-rose/30 shadow-[0_0_30px_rgba(244,63,94,0.15)] rounded-3xl p-6 sm:p-8 animate-scale-in">
          
          <div class="w-16 h-16 rounded-full bg-gradient-to-br from-accent-rose to-orange-500 mb-4 flex items-center justify-center shadow-glow shadow-accent-rose/20 text-surface-900">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
          </div>
          
          <h2 class="text-2xl font-bold font-display text-surface-900 mb-1">Recharge Reminder</h2>
          <p class="text-sm font-medium text-accent-rose mb-1">
            {{ reminderTimeLeft() }}
          </p>
          <p class="text-xs font-semibold text-surface-500 mb-6">
            Due On: {{ formatDate(expiringRecharge()?.planExpiryDate) }}
          </p>
          
          <div class="w-full bg-white/50 rounded-2xl p-4 border border-white/5 mb-6">
            <div class="text-center font-bold text-surface-900 mb-1">{{ getDisplayName() }}</div>
            <div class="text-center text-sm text-surface-500 mb-3">{{ expiringRecharge()?.mobileNumber }} • {{ expiringRecharge()?.operatorName || 'Last Recharge' }}</div>
            <div class="text-center text-3xl font-bold font-mono tracking-tight text-surface-900">₹{{ expiringRecharge()?.amount }}</div>
          </div>
          
          <div class="w-full space-y-3">
            <a routerLink="/recharge" [queryParams]="{ mobile: expiringRecharge()?.mobileNumber, op: expiringRecharge()?.operatorName }" 
               class="btn-primary w-full !py-3.5 flex justify-center text-sm">
              Select Plan &amp; Recharge
            </a>
            <button (click)="showReminderModal.set(false)" class="w-full py-3 text-sm font-semibold text-surface-500 hover:text-surface-900 transition-colors">
              Skip Reminder
            </button>
          </div>
        </div>
      </div>
    }

    <div class="section-container py-8 sm:py-12 page-enter">

      <!-- Welcome Banner -->
      <div class="glass-card p-6 sm:p-8 mb-8 relative overflow-hidden animate-slide-up">
        <div class="absolute inset-0 bg-gradient-to-br from-omni-600/10 to-accent-teal/5 pointer-events-none"></div>
        <div class="relative flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-omni-500 to-accent-teal
                        flex items-center justify-center text-surface-900 shadow-glow">
              @if (hasRealName()) {
                <span class="text-xl font-bold">{{ authService.userInitials() }}</span>
              } @else {
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"/></svg>
              }
            </div>
            <div>
              <h1 class="text-xl font-display font-bold">
                Welcome, {{ getDisplayName() }}
              </h1>
            <div class="flex items-center gap-2 mt-0.5">
                @if (authService.currentUser()?.mobileNumber) {
                  <p class="text-sm text-surface-500">{{ authService.currentUser()?.mobileNumber }}</p>
                  @if (authService.currentUser()?.email) {
                    <span class="text-surface-600">•</span>
                  }
                }
                @if (authService.currentUser()?.email) {
                  <p class="text-sm text-surface-500">{{ authService.currentUser()?.email }}</p>
                }
              </div>
            </div>
          </div>
          <a routerLink="/recharge" class="btn-primary !py-2.5 !px-5 text-sm flex items-center gap-2 shrink-0">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
            New Recharge
          </a>
        </div>
      </div>

      <!-- Tab Navigation -->
      <div role="tablist" aria-label="Dashboard sections" class="flex gap-2 overflow-x-auto pb-2 mb-6 scrollbar-none animate-slide-up" style="animation-delay: 0.1s">
        @for (tab of tabs; track tab.key) {
          <button (click)="activeTab.set(tab.key)"
                  role="tab"
                  [attr.aria-selected]="activeTab() === tab.key"
                  [class]="activeTab() === tab.key ? 'tab-item-active' : 'tab-item'"
                  class="flex items-center gap-2 whitespace-nowrap">
            <span [innerHTML]="tab.icon"></span>
            {{ tab.label }}
            @if (tab.key === 'notifications' && notificationService.unreadCount() > 0) {
              <span class="w-5 h-5 rounded-full bg-accent-rose text-surface-900 text-[10px] font-bold
                           flex items-center justify-center">
                {{ notificationService.unreadCount() }}
              </span>
            }
          </button>
        }
      </div>

      <!-- ═══════════ TAB: PROFILE ═══════════ -->
      @if (activeTab() === 'profile') {
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 animate-fade-in">
          
          <!-- Left Column: Settings -->
          <div class="glass-card p-6 sm:p-8">
            <h2 class="text-lg font-display font-semibold mb-6">Profile Settings</h2>

            @if (profileMsg()) {
              <div class="mb-5 p-3 rounded-xl animate-scale-in"
                   [class]="profileMsgError() ? 'bg-accent-rose/10 border border-accent-rose/20' : 'bg-accent-emerald/10 border border-accent-emerald/20'">
                <p class="text-sm" [class]="profileMsgError() ? 'text-accent-rose' : 'text-accent-emerald'">{{ profileMsg() }}</p>
              </div>
            }

            <div class="form-group mb-4">
              <label>Full Name</label>
              <input type="text" [(ngModel)]="profileName" class="input-field"
                     placeholder="Your name" />
            </div>
            <div class="form-group mb-4">
              <label>Mobile Number</label>
              @if (authService.currentUser()?.mobileNumber) {
                <div class="flex items-center gap-3 animate-fade-in">
                  <div class="relative flex-1">
                    <input type="tel" [value]="authService.currentUser()?.mobileNumber || ''" class="input-field !bg-surface-50 cursor-not-allowed text-surface-500" disabled />
                  </div>
                  <span class="text-xs text-accent-emerald font-semibold whitespace-nowrap">✓ Verified</span>
                </div>
              } @else {
                <div class="space-y-4 animate-fade-in">
                  <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                    <div class="relative flex-1">
                      <div class="absolute left-4 top-1/2 -translate-y-1/2 text-surface-500 text-sm">+91</div>
                      <input type="tel" [(ngModel)]="newMobile" placeholder="Enter mobile number" class="input-field !pl-14" 
                             [disabled]="mobileOtpSent()" />
                    </div>
                    @if (!mobileOtpSent()) {
                      <button type="button" (click)="onSendMobileVerification()" [disabled]="!newMobile || mobileVerifying()"
                              class="btn-primary !py-2.5 !px-4 text-xs whitespace-nowrap flex items-center justify-center gap-2 min-w-[120px]">
                        @if (mobileVerifying()) {
                          <span class="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block"></span>
                        }
                        <span>{{ mobileVerifying() ? 'Sending...' : 'Verify Mobile' }}</span>
                      </button>
                    } @else {
                      <button type="button" (click)="mobileOtpSent.set(false)" class="btn-ghost !py-2.5 !px-4 text-xs whitespace-nowrap border border-surface-200 hover:border-white/30">
                        Change Number
                      </button>
                    }
                  </div>
                  
                  <!-- Mobile OTP Modal Overlay -->
                  @if (mobileOtpSent()) {
                    <div class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
                      <!-- Backdrop -->
                      <div class="absolute inset-0 bg-black/60 backdrop-blur-md transition-opacity animate-fade-in" (click)="mobileOtpSent.set(false)"></div>
                      
                      <!-- Modal Content -->
                      <div class="relative w-full max-w-sm glass-card border border-surface-200 shadow-2xl rounded-3xl p-6 sm:p-8 animate-scale-in overflow-hidden backdrop-blur-2xl">
                        <div class="absolute inset-0 bg-gradient-to-br from-omni-500/10 to-accent-teal/5 pointer-events-none"></div>
                        
                        <div class="w-12 h-12 mx-auto rounded-full bg-accent-emerald/10 text-accent-emerald flex items-center justify-center mb-4 border border-accent-emerald/20 shadow-inner">
                          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"/></svg>
                        </div>
                        
                        <h3 class="text-xl font-display font-bold text-center text-surface-900 mb-2 relative z-10">Verify Mobile</h3>
                        <p class="text-xs text-surface-500 mb-6 text-center relative z-10">Enter 6-digit OTP sent to <span class="text-surface-900 font-semibold">+91 {{ newMobile }}</span></p>
                        
                        <div class="flex justify-center gap-2 sm:gap-3 mb-6 relative z-10">
                          @for (i of [0,1,2,3,4,5]; track i) {
                            <input type="text" inputmode="numeric" [id]="'mobile-otp-' + i"
                                   [value]="mobileOtpDigits()[i]"
                                   (input)="onMobileOtpInput($event, i)"
                                   (keydown)="onMobileOtpKeydown($event, i)"
                                   (paste)="onMobileOtpPaste($event)"
                                   maxlength="1"
                                   class="w-10 h-12 sm:w-11 sm:h-14 rounded-xl bg-white/60 border border-surface-200 text-center text-lg sm:text-xl font-bold font-mono text-surface-900 transition-all outline-none focus:border-omni-500/60 focus:shadow-[0_0_15px_rgba(99,102,241,0.2)]"
                                   [class.border-omni-500]="mobileOtpDigits()[i]"
                                   placeholder="·" />
                          }
                        </div>
                        
                        <button type="button" (click)="onVerifyMobile()" [disabled]="getMobileOtpString().length !== 6 || mobileVerifying()"
                                class="btn-primary w-full !py-3 text-sm relative z-10">
                          {{ mobileVerifying() ? 'Verifying Code...' : 'Confirm OTP' }}
                        </button>
                      </div>
                    </div>
                  }
                </div>
              }
            </div>
            <!-- Email Section -->
            <div class="form-group mb-4">
              <div class="flex items-center gap-4 mb-2">
                <label class="!mb-0">Email</label>
                @if (authService.currentUser()?.email && !isEditingEmail()) {
                  <button type="button" (click)="isEditingEmail.set(true); newEmail = authService.currentUser()?.email || ''" 
                          class="text-xs font-medium text-omni-400 hover:text-omni-300 transition-colors flex items-center gap-1.5 focus:outline-none">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
                    Edit
                  </button>
                }
              </div>
              @if (authService.currentUser()?.email && !isEditingEmail()) {
                <div class="flex items-center gap-3 animate-fade-in">
                  <input type="email" [value]="authService.currentUser()?.email" disabled
                         class="input-field !bg-surface-50 !text-surface-500 cursor-not-allowed flex-1" />
                  <span class="text-xs text-accent-emerald font-semibold whitespace-nowrap">✓ Verified</span>
                </div>
              } @else {
                <div class="space-y-4 animate-fade-in">
                  <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                    <input type="email" [(ngModel)]="newEmail" placeholder="Add your email address" class="input-field flex-1" 
                           [disabled]="emailOtpSent()" />
                    @if (!emailOtpSent()) {
                      <button type="button" (click)="onSendEmailVerification()" [disabled]="!newEmail || emailVerifying() || newEmail === authService.currentUser()?.email"
                              class="btn-primary !py-2.5 !px-4 text-xs whitespace-nowrap flex items-center justify-center gap-2 min-w-[120px]">
                        @if (emailVerifying()) {
                          <span class="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block"></span>
                        }
                        <span>{{ emailVerifying() ? 'Sending...' : (authService.currentUser()?.email ? 'Verify New Email' : 'Verify Email') }}</span>
                      </button>
                      @if (authService.currentUser()?.email) {
                        <button type="button" (click)="isEditingEmail.set(false)" class="btn-ghost !py-2.5 !px-4 text-xs whitespace-nowrap border border-surface-200 hover:border-white/30 text-surface-500">Cancel</button>
                      }
                    } @else {
                      <button type="button" (click)="emailOtpSent.set(false)" class="btn-ghost !py-2.5 !px-4 text-xs whitespace-nowrap border border-surface-200 hover:border-white/30">
                        Change Email
                      </button>
                    }
                  </div>
                  
                  <!-- Email OTP Modal Overlay -->
                  @if (emailOtpSent()) {
                    <div class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
                      <!-- Backdrop -->
                      <div class="absolute inset-0 bg-black/60 backdrop-blur-md transition-opacity animate-fade-in" (click)="emailOtpSent.set(false)"></div>
                      
                      <!-- Modal Content -->
                      <div class="relative w-full max-w-sm glass-card border border-surface-200 shadow-2xl rounded-3xl p-6 sm:p-8 animate-scale-in overflow-hidden backdrop-blur-2xl">
                        <div class="absolute inset-0 bg-gradient-to-br from-omni-500/10 to-accent-teal/5 pointer-events-none"></div>
                        
                        <div class="w-12 h-12 mx-auto rounded-full bg-omni-500/10 text-omni-400 flex items-center justify-center mb-4 border border-omni-500/20 shadow-inner">
                          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>
                        </div>
                        
                        <h3 class="text-xl font-display font-bold text-center text-surface-900 mb-2 relative z-10">Verify Email</h3>
                        <p class="text-xs text-surface-500 mb-6 text-center relative z-10">Enter 6-digit OTP sent to <span class="text-surface-900 font-semibold">{{ newEmail }}</span></p>
                        
                        <div class="flex justify-center gap-2 sm:gap-3 mb-6 relative z-10">
                          @for (i of [0,1,2,3,4,5]; track i) {
                            <input type="text" inputmode="numeric" [id]="'email-otp-' + i"
                                   [value]="emailOtpDigits()[i]"
                                   (input)="onEmailOtpInput($event, i)"
                                   (keydown)="onEmailOtpKeydown($event, i)"
                                   (paste)="onEmailOtpPaste($event)"
                                   maxlength="1"
                                   class="w-10 h-12 sm:w-11 sm:h-14 rounded-xl bg-white/60 border border-surface-200 text-center text-lg sm:text-xl font-bold font-mono text-surface-900 transition-all outline-none focus:border-omni-500/60 focus:shadow-[0_0_15px_rgba(99,102,241,0.2)]"
                                   [class.border-omni-500]="emailOtpDigits()[i]"
                                   placeholder="·" />
                          }
                        </div>
                        
                        <button type="button" (click)="onVerifyEmail()" [disabled]="getEmailOtpString().length !== 6 || emailVerifying()"
                                class="btn-primary w-full !py-3 text-sm relative z-10">
                          {{ emailVerifying() ? 'Verifying Code...' : 'Confirm OTP' }}
                        </button>
                      </div>
                    </div>
                  }
                </div>
              }
            </div>

            <button (click)="onUpdateProfile()" [disabled]="profileSaving()"
                    class="btn-primary !py-3 mb-8">
              {{ profileSaving() ? 'Saving...' : 'Update Profile' }}
            </button>

          </div>

          <!-- Right Column: Security Status Overview Card -->
          <div class="space-y-6 animate-slide-up" style="animation-delay: 0.1s">
            
            <!-- Account Security Card -->
            <div class="glass-card p-6 border-l-4 border-l-omni-500 relative overflow-hidden">
              <h3 class="text-lg font-display font-semibold mb-4 flex items-center gap-2">
                <svg class="w-5 h-5 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
                Account Security
              </h3>
              
              <div class="space-y-4 relative z-10">
                <div class="flex items-center justify-between p-4 rounded-xl bg-surface-50 border border-surface-200">
                  <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center bg-accent-emerald/10 border border-accent-emerald/20">
                      <svg class="w-5 h-5 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"/></svg>
                    </div>
                    <div>
                      <p class="text-sm font-medium text-surface-900">Mobile Status</p>
                      <p class="text-xs text-surface-500">Used for Quick Logins</p>
                    </div>
                  </div>
                  @if (authService.currentUser()?.mobileNumber) {
                    <span class="badge-success">Verified</span>
                  } @else {
                    <span class="badge-failed">Missing</span>
                  }
                </div>

                <div class="flex items-center justify-between p-4 rounded-xl bg-surface-50 border border-surface-200">
                  <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center bg-omni-500/10 border border-omni-500/20">
                      <svg class="w-5 h-5 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>
                    </div>
                    <div>
                      <p class="text-sm font-medium text-surface-900">Email Status</p>
                      <p class="text-xs text-surface-500">Used for Receipts</p>
                    </div>
                  </div>
                  @if (authService.currentUser()?.email) {
                    <span class="badge-success">Verified</span>
                  } @else {
                    <span class="badge-failed">Missing</span>
                  }
                </div>
              </div>
            </div>

            <!-- Quick Actions -->
            <div class="glass-card p-6 border border-white/5 relative overflow-hidden group">
              <div class="absolute inset-0 bg-gradient-to-br from-white/[0.01] to-white/[0.03] opacity-0 group-hover:opacity-100 transition-opacity"></div>
              <h3 class="text-base font-semibold text-surface-900 mb-4 flex items-center gap-2 relative z-10">
                <svg class="w-4 h-4 text-accent-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
                Quick Actions
              </h3>
              <div class="grid grid-cols-2 gap-3 relative z-10">
                <button (click)="goToRecharge()" class="p-3 text-left rounded-xl bg-surface-50 border border-surface-200 hover:bg-surface-50 hover:border-surface-200 transition-all group/btn cursor-pointer">
                  <div class="w-8 h-8 rounded-full bg-accent-emerald/10 text-accent-emerald flex items-center justify-center mb-2 group-hover/btn:scale-110 transition-transform">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
                  </div>
                  <p class="text-sm font-medium text-surface-900">New Recharge</p>
                  <p class="text-[10px] text-surface-500">Recharge now</p>
                </button>
                <button (click)="activeTab.set('notifications')" class="p-3 text-left rounded-xl bg-surface-50 border border-surface-200 hover:bg-surface-50 hover:border-surface-200 transition-all group/btn cursor-pointer">
                  <div class="w-8 h-8 rounded-full bg-amber-500/10 text-amber-400 flex items-center justify-center mb-2 group-hover/btn:scale-110 transition-transform">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
                  </div>
                  <p class="text-sm font-medium text-surface-900">Notifications</p>
                  <p class="text-[10px] text-surface-500">View alerts</p>
                </button>
              </div>
            </div>

          </div>

        </div>
      }

      <!-- ═══════════ TAB: RECHARGES (MY PACKS) ═══════════ -->
      @if (activeTab() === 'recharges') {
        <div class="animate-fade-in">
          
          <!-- Header -->
          <div class="mb-6 animate-slide-up">
            <h2 class="text-xl font-display font-bold text-surface-900 mb-2">My Packs</h2>
            <p class="text-sm text-surface-500">Your active, processing, expired & failed recharge packs</p>
          </div>

          <!-- Filters -->
          <div class="flex gap-2 mb-6 overflow-x-auto scrollbar-none animate-slide-up" style="animation-delay: 0.1s">
            <button (click)="packCategory.set('ALL')" [class]="packCategory()==='ALL' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">ALL PACKS</button>
            <button (click)="packCategory.set('ACTIVE')" [class]="packCategory()==='ACTIVE' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
              ACTIVE
            </button>
            <button (click)="packCategory.set('PROCESSING')" [class]="packCategory()==='PROCESSING' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
              <span class="w-2 h-2 rounded-full border-2 border-current rounded-full animate-spin border-t-transparent inline-block"></span>
              PROCESSING
            </button>
            <button (click)="packCategory.set('EXPIRED')" [class]="packCategory()==='EXPIRED' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
              EXPIRED
            </button>
            <button (click)="packCategory.set('FAILED')" [class]="packCategory()==='FAILED' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>
              FAILED
            </button>
          </div>

          <!-- Date Range Filter -->
          <div class="glass-card p-4 mb-6 animate-slide-up flex flex-col sm:flex-row items-stretch sm:items-center gap-3 border border-surface-200" style="animation-delay: 0.15s">
            <div class="flex items-center gap-2 text-xs text-surface-500 font-semibold uppercase tracking-wider shrink-0">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
              Filter by Date
            </div>
            <div class="flex flex-1 flex-col sm:flex-row items-stretch sm:items-center gap-3">
              <div class="flex items-center gap-2 flex-1">
                <label class="text-[10px] text-surface-500 uppercase tracking-wide font-bold shrink-0">From</label>
                <input type="date" [(ngModel)]="rechStartDate"
                       class="flex-1 min-w-0 px-3 py-2 rounded-lg bg-white/60 border border-surface-200 text-sm text-surface-900 outline-none transition-all focus:border-omni-500/50 [color-scheme:dark]" />
              </div>
              <div class="flex items-center gap-2 flex-1">
                <label class="text-[10px] text-surface-500 uppercase tracking-wide font-bold shrink-0">To</label>
                <input type="date" [(ngModel)]="rechEndDate"
                       class="flex-1 min-w-0 px-3 py-2 rounded-lg bg-white/60 border border-surface-200 text-sm text-surface-900 outline-none transition-all focus:border-omni-500/50 [color-scheme:dark]" />
              </div>
              <div class="flex gap-2 shrink-0">
                <button (click)="applyRechDateFilter()" 
                        [disabled]="applyingRechDateFilter()"
                        class="btn-primary !py-2 !px-4 text-xs flex items-center justify-center gap-1.5 min-w-[80px] disabled:opacity-50">
                  @if (applyingRechDateFilter()) {
                    <svg class="animate-spin h-3.5 w-3.5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                  } @else {
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"/></svg>
                    Apply
                  }
                </button>
                @if (rechStartDate || rechEndDate) {
                  <button (click)="clearRechDateFilter()" 
                          class="btn-ghost !py-2 !px-3 text-xs border border-surface-200 hover:border-accent-rose/30 hover:text-accent-rose transition-all">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  </button>
                }
              </div>
            </div>
          </div>

          <!-- Stat Cards -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6 animate-slide-up" style="animation-delay: 0.2s">
            <div class="glass-card p-6 flex flex-col justify-between border border-surface-200">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-emerald/10 flex items-center justify-center border border-accent-emerald/20">
                  <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                </div>
                <p class="text-xs text-surface-500 font-bold uppercase tracking-wider">Active Packs</p>
              </div>
              <span class="text-3xl font-display font-bold px-1">{{ stats().active }}</span>
            </div>
            
            <div class="glass-card p-6 flex flex-col justify-between border border-surface-200">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-amber/10 flex items-center justify-center border border-accent-amber/20">
                  <svg class="w-4 h-4 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" /></svg>
                </div>
                <p class="text-xs text-surface-500 font-bold uppercase tracking-wider">Processing</p>
              </div>
              <span class="text-3xl font-display font-bold px-1">{{ stats().processing }}</span>
            </div>

            <div class="glass-card p-6 flex flex-col justify-between border border-surface-200">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-surface-500/10 flex items-center justify-center border border-surface-500/20">
                  <svg class="w-4 h-4 text-surface-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                </div>
                <p class="text-xs text-surface-500 font-bold uppercase tracking-wider">Expired</p>
              </div>
              <span class="text-3xl font-display font-bold px-1">{{ stats().expired }}</span>
            </div>

            <div class="glass-card p-6 flex flex-col justify-between border border-surface-200">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-rose/10 flex items-center justify-center border border-accent-rose/20">
                  <svg class="w-4 h-4 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>
                </div>
                <p class="text-xs text-surface-500 font-bold uppercase tracking-wider">Failed</p>
              </div>
              <span class="text-3xl font-display font-bold px-1">{{ stats().failed }}</span>
            </div>
          </div>

          <!-- TABLE -->
          @if (rechLoading()) {
            <div class="glass-card overflow-hidden">
              <div class="p-5 space-y-4">
                @for (i of [1,2,3]; track i) {
                  <div class="skeleton h-12 w-full rounded-xl"></div>
                }
              </div>
            </div>
          } @else if (filteredRecharges().length > 0) {
            <div class="glass-card overflow-hidden bg-surface-50 animate-slide-up" style="animation-delay: 0.3s">
              
              <!-- Grid Header -->
              <div class="hidden lg:grid grid-cols-12 gap-2 px-6 py-4 border-b border-surface-200 bg-surface-50 text-[10px] font-bold text-surface-500 uppercase tracking-wider">
                <div class="col-span-1">Date</div>
                <div class="col-span-2">Mobile</div>
                <div class="col-span-1">Operator</div>
                <div class="col-span-2">Plan</div>
                <div class="col-span-1">Amount</div>
                <div class="col-span-1">Valid Till</div>
                <div class="col-span-1">Days Left</div>
                <div class="col-span-1">Status</div>
                <div class="col-span-2 text-right"></div>
              </div>

              <div class="divide-y divide-surface-200">
                @for (r of filteredRecharges(); track r.rechargeId) {
                  <div class="px-6 py-4 hover:bg-surface-50 transition-colors group">
                    <div class="flex flex-col lg:grid lg:grid-cols-12 gap-3 lg:gap-2 lg:items-center">
                      <div class="col-span-1 text-xs font-semibold text-surface-600">
                        {{ r.createdDate | date:'dd MMM yyyy, h:mm a' }}
                      </div>
                      
                      <div class="col-span-2 text-sm font-bold tracking-wider font-mono text-surface-900">
                        {{ r.mobileNumber }}
                      </div>
                      
                      <div class="col-span-1 text-xs text-surface-600">
                        {{ r.operatorName }}
                      </div>
                      
                      <div class="col-span-2 text-xs text-surface-600 truncate" [title]="r.planName">
                        {{ r.planName }}
                      </div>
                      
                      <div class="col-span-1 text-sm font-bold text-surface-900">
                        ₹{{ r.amount }}
                      </div>
                      
                      <div class="col-span-1 text-[11px] font-semibold" [class]="getPackStatus(r).type === 'expired' || getPackStatus(r).type === 'failed' ? 'text-surface-500' : 'text-surface-600'">
                        {{ getExactExpiryDate(r) && getPackStatus(r).type !== 'failed' ? (getExactExpiryDate(r) | date:'dd MMM yyyy') : (getPackStatus(r).type === 'expired' ? 'Expired' : 'N/A') }}
                      </div>
                      
                      <div class="col-span-1 text-xs font-semibold">
                        @if (getExactExpiryDate(r) && getPackStatus(r).type === 'active') {
                          <span [class]="getDaysLeft(r) <= 5 ? 'text-accent-rose' : 'text-surface-600'">
                            {{ getTimeLeft(r) }}
                          </span>
                        } @else {
                          <span class="text-surface-500">{{ getPackStatus(r).type === 'expired' ? 'Expired' : (getPackStatus(r).type === 'failed' ? 'Failed' : '-') }}</span>
                        }
                      </div>

                      <div class="col-span-1">
                        <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border bg-surface-50"
                             [class]="getPackStatus(r).type === 'active' ? 'border-accent-emerald/20 text-accent-emerald' : 
                                      getPackStatus(r).type === 'expired' ? 'border-surface-600/30 text-surface-500' : 
                                      getPackStatus(r).type === 'failed' ? 'border-accent-rose/20 text-accent-rose' :
                                      'border-accent-amber/20 text-accent-amber'">
                          @if (getPackStatus(r).type === 'active') {
                            <span class="w-1.5 h-1.5 rounded-full bg-accent-emerald"></span>
                          } @else if (getPackStatus(r).type === 'expired') {
                            <span class="w-1.5 h-1.5 rounded-full bg-surface-500"></span>
                          } @else if (getPackStatus(r).type === 'failed') {
                            <span class="w-1.5 h-1.5 rounded-full bg-accent-rose"></span>
                          } @else {
                            <span class="w-1.5 h-1.5 rounded-full bg-accent-amber"></span>
                          }
                          <span class="text-[9px] font-extrabold uppercase tracking-widest">{{ getPackStatus(r).label }}</span>
                        </div>
                      </div>

                      <div class="col-span-2 flex justify-end">
                        @if (getPackStatus(r).type !== 'active') {
                          <a [routerLink]="['/recharge']" [queryParams]="{mobile: r.mobileNumber}" 
                             class="btn-secondary !py-1.5 !px-4 text-xs !bg-transparent border border-surface-300 text-surface-900 hover:bg-surface-100 hover:border-white/40 transition-all font-semibold">
                            Recharge Now
                          </a>
                        }
                      </div>
                    </div>
                  </div>
                }
              </div>

              <!-- Pagination -->
              @if (rechTotalPages() > 1 && packCategory() === 'ALL') {
                <div class="flex justify-between items-center px-6 py-4 border-t border-surface-200 bg-surface-50">
                  <span class="text-xs text-surface-500 font-medium">Page {{ rechPage() + 1 }} of {{ rechTotalPages() }}</span>
                  <div class="flex gap-2">
                    <button (click)="loadRecharges(rechPage() - 1)" [disabled]="rechPage() === 0" class="btn-ghost text-xs group">
                      <svg class="w-4 h-4 inline mr-1 group-disabled:opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                    </button>
                    <button (click)="loadRecharges(rechPage() + 1)" [disabled]="rechPage() >= rechTotalPages() - 1" class="btn-ghost text-xs group">
                      <svg class="w-4 h-4 inline ml-1 group-disabled:opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                    </button>
                  </div>
                </div>
              }
            </div>
          } @else {
            <div class="glass-card p-16 text-center border-dashed border-2 border-surface-200">
              <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-gradient-to-br from-surface-800 to-surface-900 flex items-center justify-center border border-surface-200">
                <svg class="w-10 h-10 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"/>
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-surface-900 mb-2">No {{ packCategory() !== 'ALL' ? packCategory().toLowerCase() : '' }} recharges found</h3>
              <p class="text-sm text-surface-500 max-w-sm mx-auto leading-relaxed mb-6">
                {{ (rechStartDate || rechEndDate) ? 'We could not find any recharges in this date range. Try clearing your filters to see all history.' : 'You have not made any recharges yet. Recharge your mobile now to enjoy uninterrupted services.' }}
              </p>
              @if (rechStartDate || rechEndDate) {
                <button (click)="clearRechDateFilter()" class="btn-ghost text-xs border border-surface-200">
                  Clear date filters
                </button>
              } @else {
                <a routerLink="/recharge" class="btn-primary text-sm !px-6">Make Your First Recharge</a>
              }
            </div>
          }
        </div>
      }

      <!-- ═══════════ TAB: PAYMENTS ═══════════ -->
      @if (activeTab() === 'payments') {
        <div class="animate-fade-in">
          @if (payLoading()) {
            <div class="glass-card overflow-hidden">
              <div class="p-5 space-y-4">
                @for (i of [1,2,3]; track i) {
                  <div class="skeleton h-12 w-full rounded-xl"></div>
                }
              </div>
            </div>
          } @else if (payments().length > 0) {
            <!-- Filter Pills -->
            <div class="flex items-center gap-2 mb-6 overflow-x-auto scrollbar-none animate-slide-up">
              <button (click)="paymentCategory.set('ALL')"
                      [class]="paymentCategory() === 'ALL' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold' : 'tab-item !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2'">
                ALL TRANSACTIONS
              </button>
              <button (click)="paymentCategory.set('SUCCESS')"
                      [class]="paymentCategory() === 'SUCCESS' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
                <svg class="w-3.5 h-3.5 text-accent-emerald" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
                SUCCESS
              </button>
              <button (click)="paymentCategory.set('FAILED')"
                      [class]="paymentCategory() === 'FAILED' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
                <svg class="w-3.5 h-3.5 text-accent-rose" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>
                FAILED
              </button>
            </div>

            <!-- Date Range Filter -->
            <div class="glass-card p-4 mb-6 animate-slide-up flex flex-col sm:flex-row items-stretch sm:items-center gap-3 border border-surface-200">
              <div class="flex items-center gap-2 text-xs text-surface-500 font-semibold uppercase tracking-wider shrink-0">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                Filter by Date
              </div>
              <div class="flex flex-1 flex-col sm:flex-row items-stretch sm:items-center gap-3">
                <div class="flex items-center gap-2 flex-1">
                  <label class="text-[10px] text-surface-500 uppercase tracking-wide font-bold shrink-0">From</label>
                  <input type="date" [(ngModel)]="payStartDate"
                         class="flex-1 min-w-0 px-3 py-2 rounded-lg bg-white/60 border border-surface-200 text-sm text-surface-900 outline-none transition-all focus:border-omni-500/50 [color-scheme:dark]" />
                </div>
                <div class="flex items-center gap-2 flex-1">
                  <label class="text-[10px] text-surface-500 uppercase tracking-wide font-bold shrink-0">To</label>
                  <input type="date" [(ngModel)]="payEndDate"
                         class="flex-1 min-w-0 px-3 py-2 rounded-lg bg-white/60 border border-surface-200 text-sm text-surface-900 outline-none transition-all focus:border-omni-500/50 [color-scheme:dark]" />
                </div>
                <div class="flex gap-2 shrink-0">
                  <button (click)="applyPayDateFilter()" 
                          [disabled]="applyingPayDateFilter()"
                          class="btn-primary !py-2 !px-4 text-xs flex items-center justify-center gap-1.5 min-w-[80px] disabled:opacity-50">
                    @if (applyingPayDateFilter()) {
                      <svg class="animate-spin h-3.5 w-3.5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                    } @else {
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"/></svg>
                      Apply
                    }
                  </button>
                  @if (payStartDate || payEndDate) {
                    <button (click)="clearPayDateFilter()" 
                            class="btn-ghost !py-2 !px-3 text-xs border border-surface-200 hover:border-accent-rose/30 hover:text-accent-rose transition-all">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                    </button>
                  }
                </div>
              </div>
            </div>

            <div class="glass-card overflow-hidden">
              <!-- Table Header -->
              <div class="hidden sm:grid grid-cols-12 gap-4 px-6 py-4 border-b border-surface-200 bg-surface-50 text-xs font-semibold text-surface-500 uppercase tracking-wider">
                <div class="col-span-3">Transaction ID</div>
                <div class="col-span-3">Date / Time</div>
                <div class="col-span-2 text-right">Amount</div>
                <div class="col-span-4 text-right">Status</div>
              </div>
              
              <div class="divide-y divide-surface-200">
                @for (p of filteredPayments(); track p.transactionId) {
                  <div class="p-4 sm:px-6 hover:bg-surface-50 transition-colors">
                    <div class="flex flex-col sm:grid sm:grid-cols-12 gap-2 sm:gap-4 items-start sm:items-center">
                      
                      <!-- Transaction ID -->
                      <div class="col-span-3 w-full flex justify-between sm:block">
                        <span class="sm:hidden font-medium text-surface-500">ID</span>
                        <div>
                          <div class="text-xs font-mono text-omni-300 font-medium tracking-wide">{{ p.transactionId }}</div>
                          <div class="text-[10px] text-surface-500 uppercase mt-0.5">{{ p.paymentMethod }}</div>
                        </div>
                      </div>
                      
                      <!-- Date / Time -->
                      <div class="col-span-3 text-xs text-surface-600 w-full flex justify-between sm:block">
                        <span class="sm:hidden font-medium text-surface-500">Date</span>
                        {{ p.createdDate | date:'mediumDate' }} • <span class="text-surface-500">{{ p.createdDate | date:'shortTime' }}</span>
                      </div>
                      
                      <!-- Amount -->
                      <div class="col-span-2 sm:text-right w-full flex justify-between sm:block">
                        <span class="sm:hidden font-medium text-surface-500">Amount</span>
                        <span class="text-sm font-bold text-surface-900">₹{{ p.amount }}</span>
                      </div>
                      
                      <!-- Status -->
                      <div class="col-span-4 sm:text-right w-full flex justify-between sm:block">
                        <span class="sm:hidden font-medium text-surface-500">Status</span>
                        <div class="flex flex-col sm:items-end gap-1">
                          <span class="px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wide uppercase inline-block"
                                [class]="p.status === 'SUCCESS' ? 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/15' : 
                                         p.status === 'FAILED' ? 'bg-accent-rose/10 text-accent-rose border border-accent-rose/15' : 
                                         'bg-accent-amber/10 text-accent-amber border border-accent-amber/15'">
                            {{ p.status }}
                          </span>
                          @if (p.failureReason && p.status === 'FAILED') {
                            <div class="text-[10px] text-accent-rose mt-1 w-full sm:text-right truncate max-w-xs" [title]="p.failureReason">
                              {{ p.failureReason }}
                            </div>
                          }
                        </div>
                      </div>
                      
                    </div>
                  </div>
                }
              </div>

              <!-- Pagination -->
              @if (payTotalPages() > 1) {
                <div class="flex items-center justify-between px-6 py-4 border-t border-surface-200">
                  <span class="text-xs text-surface-500">
                    Showing {{ (payPage() * 10) + 1 }}–{{ mathMin((payPage() + 1) * 10, payTotalElements()) }} of {{ payTotalElements() }}
                  </span>
                  <div class="flex items-center gap-1">
                    <button (click)="loadPayments(payPage() - 1)" [disabled]="payPage() === 0" 
                            class="btn-ghost text-xs !py-1.5 !px-3 disabled:opacity-30">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                    </button>
                    @for (pg of getPayPageNumbers(); track pg) {
                      <button (click)="loadPayments(pg)" 
                              [class]="pg === payPage() ? 'bg-omni-600 text-surface-900' : 'text-surface-500 hover:bg-surface-50'"
                              class="w-8 h-8 rounded-lg text-xs font-medium transition-colors">
                        {{ pg + 1 }}
                      </button>
                    }
                    <button (click)="loadPayments(payPage() + 1)" [disabled]="payPage() >= payTotalPages() - 1" 
                            class="btn-ghost text-xs !py-1.5 !px-3 disabled:opacity-30">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                    </button>
                  </div>
                </div>
              }
            </div>
          } @else {
            <div class="glass-card p-16 text-center">
              <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-gradient-to-br from-surface-800 to-surface-900 flex items-center justify-center border border-surface-200">
                <svg class="w-10 h-10 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3.75 3v11.25A2.25 2.25 0 006 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0119.5 16.5h-2.25m-9 0h9l-3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z"/>
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-surface-900 mb-2">No payment transactions found</h3>
              <p class="text-sm text-surface-500 max-w-sm mx-auto leading-relaxed">
                {{ (payStartDate || payEndDate) ? 'We could not find any payments in this date range. Try clearing your filters to see all history.' : 'Your payment history is currently empty.' }}
              </p>
              @if (payStartDate || payEndDate) {
                <button (click)="clearPayDateFilter()" class="btn-ghost text-xs mt-6 border border-surface-200">
                  Clear date filters
                </button>
              }
            </div>
          }
        </div>
      }

      <!-- ═══════════ TAB: NOTIFICATIONS ═══════════ -->
      @if (activeTab() === 'notifications') {
        <div class="animate-fade-in">
          @if (notifications().length > 0 || notifTotalElements() > 0) {
            <!-- Filter Pills -->
            <div class="flex items-center justify-between mb-6">
              <div class="flex items-center gap-2 overflow-x-auto scrollbar-none animate-slide-up">
                <button (click)="notificationCategory.set('ALL')"
                        [class]="notificationCategory() === 'ALL' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
                  ALL NOTIFICATIONS
                </button>
                <button (click)="notificationCategory.set('UNREAD')"
                        [class]="notificationCategory() === 'UNREAD' ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold flex items-center gap-2' : 'tab-item !px-5 !py-2.5 flex items-center gap-2 rounded-full text-xs font-semibold'">
                  @if (notificationService.unreadCount() > 0) {
                    <span class="w-1.5 h-1.5 rounded-full bg-omni-500 animate-[pulse_2s_ease-in-out_infinite]"></span>
                  }
                  UNREAD
                </button>
              </div>
              <span class="text-xs text-surface-500">{{ notifTotalElements() }} total</span>
            </div>

            <div class="space-y-3">
              @for (n of filteredNotifications(); track n.id) {
                <div class="glass-card group p-5 cursor-pointer border-l-4 transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_8px_24px_-4px_rgba(0,0,0,0.3)] hover:shadow-omni-500/10 hover:bg-surface-50"
                     [class.border-l-omni-500]="!n.isRead"
                     [class.border-l-transparent]="n.isRead"
                     (click)="markRead(n)">
                  <div class="flex items-start gap-4">
                    <div class="w-10 h-10 rounded-full flex items-center justify-center shrink-0 transition-all duration-500 group-hover:scale-110 group-hover:shadow-[0_0_15px_inherit]"
                         [class]="getNotifStyle(n).iconClass">
                      @if (getNotifStyle(n).type === 'success') {
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                      } @else if (getNotifStyle(n).type === 'error') {
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                      } @else {
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
                      }
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between gap-3 mb-1.5">
                        <div class="flex items-center gap-2 min-w-0">
                          <p class="text-sm font-bold tracking-wide truncate" [class]="n.isRead ? 'text-surface-600' : 'text-surface-900'">{{ n.subject || 'OmniCharge Alert' }}</p>
                          <span class="px-2 py-0.5 rounded-md text-[9px] font-bold uppercase tracking-wider shrink-0"
                                [class]="getNotifStyle(n).type === 'success' ? 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20' :
                                         getNotifStyle(n).type === 'error' ? 'bg-accent-rose/10 text-accent-rose border border-accent-rose/20' :
                                         'bg-omni-500/10 text-omni-300 border border-omni-500/20'">
                            {{ getNotifStyle(n).badge }}
                          </span>
                        </div>
                        <span class="text-[11px] text-surface-500 font-medium shrink-0 whitespace-nowrap">{{ n.createdDate | date:'M/d/yy, h:mm a' }}</span>
                      </div>
                      <p class="text-xs text-surface-500 leading-relaxed mb-2">{{ n.message }}</p>
                      <div class="flex items-center gap-3">
                        <span class="flex items-center gap-1.5">
                          <span class="w-1.5 h-1.5 rounded-full" [class]="n.isRead ? 'bg-surface-600' : 'bg-accent-emerald'"></span>
                          <span class="text-[10px] font-semibold uppercase tracking-wider" [class]="n.isRead ? 'text-surface-500' : 'text-accent-emerald'">{{ n.isRead ? 'READ' : 'NEW' }}</span>
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              }
            </div>

            <!-- Pagination -->
            @if (notifTotalPages() > 1) {
              <div class="flex items-center justify-between mt-6 px-2">
                <span class="text-xs text-surface-500">
                  Page {{ notifPage() + 1 }} of {{ notifTotalPages() }}
                </span>
                <div class="flex items-center gap-1">
                  <button (click)="loadNotifications(notifPage() - 1)" [disabled]="notifPage() === 0" 
                          class="btn-ghost text-xs !py-1.5 !px-3 disabled:opacity-30">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                  </button>
                  @for (pg of getNotifPageNumbers(); track pg) {
                    <button (click)="loadNotifications(pg)" 
                            [class]="pg === notifPage() ? 'bg-omni-600 text-surface-900' : 'text-surface-500 hover:bg-surface-50'"
                            class="w-8 h-8 rounded-lg text-xs font-medium transition-colors">
                      {{ pg + 1 }}
                    </button>
                  }
                  <button (click)="loadNotifications(notifPage() + 1)" [disabled]="notifPage() >= notifTotalPages() - 1" 
                          class="btn-ghost text-xs !py-1.5 !px-3 disabled:opacity-30">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                  </button>
                </div>
              </div>
            }
          } @else {
            <div class="glass-card p-12 text-center">
              <p class="text-surface-500">No notifications yet.</p>
            </div>
          }
        </div>
      }

      <!-- ═══════════ FAQ SECTION ═══════════ -->
      <div class="mt-12 max-w-3xl mx-auto animate-slide-up" style="animation-delay: 0.3s">
        <h3 class="text-lg font-display font-semibold mb-6 flex items-center justify-center gap-2">
          <span>Frequently Asked Questions</span>
        </h3>
        <div class="space-y-4">
          @for (faq of faqs; track faq.q; let i = $index) {
            <div class="glass-card transition-all duration-300 group overflow-hidden"
                 [style.border-color]="faqOpen() === i ? 'rgba(99,102,241,0.4)' : ''"
                 [style.background]="faqOpen() === i ? 'rgba(255,255,255,0.04)' : ''"
                 [style.box-shadow]="faqOpen() === i ? '0 0 20px rgba(99,102,241,0.1)' : ''">
              <button (click)="toggleFaq(i)" class="w-full px-6 py-5 flex items-center justify-between text-left hover:bg-surface-50 transition-colors focus:outline-none rounded-2xl">
                <span class="font-medium text-[15px] transition-colors duration-300" [class.text-surface-900]="faqOpen() === i" [class.text-surface-600]="faqOpen() !== i">{{ faq.q }}</span>
                <span class="transform transition-transform duration-300 flex items-center justify-center w-8 h-8 rounded-full border border-surface-200" 
                      [class.rotate-180]="faqOpen() === i" [class.bg-omni-500]="faqOpen() === i" [class.text-surface-900]="faqOpen() === i" [class.text-surface-500]="faqOpen() !== i">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7"/></svg>
                </span>
              </button>
              <div class="px-6 text-sm text-surface-500 transition-all duration-300 ease-in-out leading-relaxed" 
                   [class.max-h-0]="faqOpen() !== i" [class.max-h-40]="faqOpen() === i" [class.opacity-0]="faqOpen() !== i" [class.py-0]="faqOpen() !== i" [class.pb-5]="faqOpen() === i" [class.-mt-2]="faqOpen() === i"
                   style="overflow: hidden;">
                {{ faq.a }}
              </div>
            </div>
          }
        </div>
      </div>

    </div>
  `,
  styles: [`
    .scrollbar-none::-webkit-scrollbar { display: none; }
    .scrollbar-none { -ms-overflow-style: none; scrollbar-width: none; }

    .dash-splash {
      position: fixed; inset: 0; z-index: 9999;
      display: flex; align-items: center; justify-content: center;
      background: radial-gradient(ellipse at center, #ffffff 0%, #f9fafb 60%, #f3f4f6 100%);
      animation: dsFade 0.5s ease 1.8s forwards;
    }
    .ds-content { text-align: center; animation: dsIn 0.7s cubic-bezier(0.16,1,0.3,1) 0.15s both; }
    .ds-icon {
      width: 72px; height: 72px; margin: 0 auto 20px; border-radius: 20px;
      background: linear-gradient(135deg, #C65D3B, #e06f5c);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 0 50px rgba(198,93,59,0.3), 0 0 100px rgba(224,111,92,0.15);
      animation: dsGlow 2s ease-in-out infinite; color: white;
    }
    .ds-title { font-family: 'Outfit', sans-serif; font-size: 24px; font-weight: 300; color: #6B7280; line-height: 1.3; margin-bottom: 6px; }
    .ds-title span { display: block; font-size: 36px; font-weight: 800; background: linear-gradient(135deg, #C65D3B, #A94E32); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
    .ds-sub { font-size: 13px; color: #9CA3AF; font-weight: 500; margin-bottom: 28px; }
    .ds-bar { width: 160px; height: 3px; margin: 0 auto; background: rgba(0,0,0,0.06); border-radius: 99px; overflow: hidden; }
    .ds-bar-fill { height: 100%; width: 0; background: linear-gradient(90deg, #C65D3B, #A94E32); border-radius: 99px; animation: dsBarFill 1.6s ease 0.3s forwards; }
    .ds-particles { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
    .dp { position: absolute; border-radius: 50%; opacity: 0; animation: dpFloat 2.5s ease forwards; }
    .dp1 { width: 5px; height: 5px; background: #C65D3B; top: 35%; left: 20%; animation-delay: 0.2s; }
    .dp2 { width: 4px; height: 4px; background: #e06f5c; top: 55%; left: 75%; animation-delay: 0.5s; }
    .dp3 { width: 6px; height: 6px; background: #eb8f81; top: 25%; left: 65%; animation-delay: 0.8s; }
    .dp4 { width: 3px; height: 3px; background: #A94E32; top: 70%; left: 30%; animation-delay: 0.4s; }
    @keyframes dsFade { to { opacity: 0; pointer-events: none; } }
    @keyframes dsIn { from { opacity: 0; transform: translateY(25px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
    @keyframes dsGlow { 0%,100% { box-shadow: 0 0 50px rgba(198,93,59,0.3); } 50% { box-shadow: 0 0 70px rgba(198,93,59,0.5), 0 0 120px rgba(224,111,92,0.25); } }
    @keyframes dsBarFill { to { width: 100%; } }
    @keyframes dpFloat { 0% { opacity: 0; transform: translateY(0) scale(0); } 30% { opacity: 0.5; transform: translateY(-15px) scale(1); } 100% { opacity: 0; transform: translateY(-60px) scale(0.5); } }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly authService = inject(AuthService);
  readonly rechargeService = inject(RechargeService);
  readonly paymentService = inject(PaymentService);
  readonly notificationService = inject(NotificationService);
  readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  activeTab = signal<DashTab>('recharges');
  showSplash = signal(true);

  // Profile
  profileName = '';
  profileMobile = '';
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  profileMsg = signal('');
  profileMsgError = signal(false);
  profileSaving = signal(false);

  // Mobile verification
  mobileEditMode = signal(false);
  mobileOtpSent = signal(false);
  mobileVerifying = signal(false);
  newMobile = '';
  mobileOtpDigits = signal<string[]>(['', '', '', '', '', '']);

  // Email verification
  emailEditMode = signal(false);
  isEditingEmail = signal(false);
  emailOtpSent = signal(false);
  emailVerifying = signal(false);
  newEmail = '';
  emailOtpDigits = signal<string[]>(['', '', '', '', '', '']);

  // Date Formatting Helper
  parseDateLocally(dateInput: any): Date | null {
    if (!dateInput) return null;
    try {
      let d: Date;
      if (Array.isArray(dateInput)) {
        if (dateInput.length >= 6) {
          d = new Date(dateInput[0], dateInput[1] - 1, dateInput[2], dateInput[3], dateInput[4], dateInput[5]);
        } else if (dateInput.length >= 3) {
          d = new Date(dateInput[0], dateInput[1] - 1, dateInput[2]);
        } else return null;
      } else {
        d = new Date(dateInput);
      }
      if (isNaN(d.getTime())) return null;
      return d;
    } catch { return null; }
  }

  formatDate(dateInput: any): string {
    const d = this.parseDateLocally(dateInput);
    if (!d) return '—';
    return d.toLocaleString('en-IN', { month: 'short', day: '2-digit', year: 'numeric' });
  }

  // Recharges
  recharges = signal<RechargeHistoryItem[]>([]);
  rechLoading = signal(false);
  applyingRechDateFilter = signal(false);
  rechPage = signal(0);
  rechTotalPages = signal(0);

  // Recharge Reminder
  showReminderModal = signal(false);
  expiringRecharge = signal<RechargeHistoryItem | null>(null);
  reminderTimeLeft = signal('');

  // FAQs
  faqOpen = signal<number | null>(null);
  faqs = [
    { q: 'How do I recharge a different number?', a: 'You can navigate to the "New Recharge" page from the dashboard header. Enter the new mobile number and select the respective operator to proceed.' },
    { q: 'What happens if my payment fails?', a: 'If a payment fails but cash is deducted, the amount will be automatically refunded to your original payment method within 3-5 business days.' },
    { q: 'How do I download an invoice?', a: 'Currently, you receive an automated email receipt for successful payments. We are bringing PDF invoice downloads directly to the dashboard very soon!' },
    { q: 'Can I change my registered email?', a: 'Yes, you can navigate to the Profile Tab here within the dashboard and click "Change Email". You will need to verify the new email via OTP.' }
  ];

  toggleFaq(index: number) {
    this.faqOpen.set(this.faqOpen() === index ? null : index);
  }

  goToRecharge(): void {
    this.router.navigate(['/recharge']);
  }

  // Date filters for Recharges tab
  rechStartDate = '';
  rechEndDate = '';

  // My Packs Logic
  packCategory = signal<'ALL'|'ACTIVE'|'PROCESSING'|'EXPIRED'|'FAILED'>('ALL');

  filteredRecharges = computed(() => {
    const cat = this.packCategory();
    if (cat === 'ALL') return this.recharges();
    return this.recharges().filter(r => this.getPackStatus(r).type === cat.toLowerCase());
  });

  stats = computed(() => {
    let active = 0, processing = 0, expired = 0, failed = 0;
    for (const r of this.recharges()) {
      const type = this.getPackStatus(r).type;
      if (type === 'active') active++;
      if (type === 'processing') processing++;
      if (type === 'expired') expired++;
      if (type === 'failed') failed++;
    }
    return { active, processing, expired, failed };
  });

  getExactExpiryDate(r: RechargeHistoryItem): Date | null {
    if (r.createdDate && r.planValidityDays != null) {
      const created = this.parseDateLocally(r.createdDate);
      if (created) {
        return new Date(created.getTime() + r.planValidityDays * 24 * 60 * 60 * 1000);
      }
    }
    if (r.planExpiryDate) {
      return this.parseDateLocally(r.planExpiryDate);
    }
    return null;
  }

  getDaysLeft(r: RechargeHistoryItem): number {
    const expiry = this.getExactExpiryDate(r);
    if (!expiry) return -1;
    const now = new Date();
    return Math.ceil((expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  }

  getTimeLeft(r: RechargeHistoryItem): string {
    const expiry = this.getExactExpiryDate(r);
    if (!expiry) return '-';
    const now = new Date();
    const diffMs = expiry.getTime() - now.getTime();

    if (diffMs <= 0) return 'Expired';

    const totalMins = Math.floor(diffMs / (1000 * 60));
    const totalHours = Math.floor(diffMs / (1000 * 60 * 60));
    const totalDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (totalHours < 1) {
      return `${totalMins} Min${totalMins !== 1 ? 's' : ''}`;
    }
    if (totalHours < 24) {
      return `${totalHours} Hour${totalHours !== 1 ? 's' : ''}`;
    }
    return `${totalDays} Day${totalDays !== 1 ? 's' : ''}`;
  }

  getPackStatus(r: RechargeHistoryItem): { label: string, type: 'active'|'expired'|'processing'|'failed' } {
    if (r.status === 'SUCCESS') {
      const expiry = this.getExactExpiryDate(r);
      if (expiry && expiry.getTime() > new Date().getTime()) {
        return { label: 'ACTIVE', type: 'active' };
      } else {
        return { label: 'EXPIRED', type: 'expired' };
      }
    } else if (r.status === 'FAILED') {
      return { label: 'FAILED', type: 'failed' };
    } else {
      const expiry = this.getExactExpiryDate(r);
      if (expiry && expiry.getTime() <= new Date().getTime()) {
        return { label: 'EXPIRED', type: 'expired' };
      }
      return { label: 'PROCESSING', type: 'processing' };
    }
  }

  // Payments
  payments = signal<TransactionResponse[]>([]);
  payLoading = signal(false);
  applyingPayDateFilter = signal(false);
  payPage = signal(0);
  payTotalPages = signal(0);
  payTotalElements = signal(0);
  paymentCategory = signal<'ALL'|'SUCCESS'|'FAILED'>('ALL');

  // Date filters for Payments tab
  payStartDate = '';
  payEndDate = '';

  filteredPayments = computed(() => {
    const cat = this.paymentCategory();
    if (cat === 'ALL') return this.payments();
    return this.payments().filter(p => p.status === cat);
  });

  // Notifications
  notifications = signal<Notification[]>([]);
  notifPage = signal(0);
  notifTotalPages = signal(0);
  notifTotalElements = signal(0);
  notificationCategory = signal<'ALL'|'UNREAD'>('ALL');

  mathMin = Math.min;

  filteredNotifications = computed(() => {
    const cat = this.notificationCategory();
    if (cat === 'ALL') return this.notifications();
    return this.notifications().filter(n => !n.isRead);
  });

  tabs = [
    { key: 'recharges' as DashTab, label: 'Recharges', icon: '⚡' },
    { key: 'payments' as DashTab, label: 'Payments', icon: '💳' },
    { key: 'notifications' as DashTab, label: 'Notifications', icon: '🔔' },
    { key: 'profile' as DashTab, label: 'Profile', icon: '👤' },
  ];

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.profileName = user.fullName;
      this.profileMobile = user.mobileNumber;
    }
    
    this.route.queryParams.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(params => {
      if (params['tab']) {
        this.activeTab.set(params['tab'] as DashTab);
        this.showSplash.set(false); // skip splash if deep-linked from bell
      }
    });

    // Auto-dismiss splash
    setTimeout(() => this.showSplash.set(false), 2300);

    this.loadRecharges(0);
    this.loadPayments(0);
    this.loadNotifications(0);
    this.notificationService.fetchUnreadCount();
    this.notificationService.startPolling();

    // Auto-refresh notification list when new notifications arrive
    this._notifRefreshInterval = setInterval(() => {
      if (this.notificationService.countChanged()) {
        this.notificationService.acknowledgeCountChange();
        this.loadNotifications(this.notifPage());
      }
    }, 2000);
  }

  private _notifRefreshInterval: any;

  loadRecharges(page: number): void {
    if (page < 0) return;
    if (!this.applyingRechDateFilter()) {
      this.rechLoading.set(true);
    }
    const startDate = this.rechStartDate ? `${this.rechStartDate}T00:00:00` : undefined;
    const endDate = this.rechEndDate ? `${this.rechEndDate}T23:59:59` : undefined;
    this.rechargeService.getRechargeHistory(page, 10, startDate, endDate).subscribe({
      next: res => {
        if (res.success && res.data) {
          const uniqueItems = new Map<string, RechargeHistoryItem>();
          res.data.content.forEach((item: RechargeHistoryItem) => {
            const existing = uniqueItems.get(item.rechargeId);
            if (!existing) {
              uniqueItems.set(item.rechargeId, item);
            } else if (item.status === 'SUCCESS' || item.status === 'FAILED') {
              uniqueItems.set(item.rechargeId, item);
            }
          });
          
          this.recharges.set(Array.from(uniqueItems.values()));
          this.rechPage.set(res.data.number);
          this.rechTotalPages.set(res.data.totalPages);
          
          if (page === 0) {
            this.checkRechargeReminder();
          }
        }
        this.rechLoading.set(false);
        this.applyingRechDateFilter.set(false);
      },
      error: () => {
        this.rechLoading.set(false);
        this.applyingRechDateFilter.set(false);
      }
    });
  }

  checkRechargeReminder(): void {
    // Only check once per session ideally, but for demo we check on load
    const activeRecharges = this.recharges()
      .filter(r => r.status === 'SUCCESS' && this.getExactExpiryDate(r) !== null)
      .sort((a, b) => {
        const da = this.parseDateLocally(a.createdDate)?.getTime() || 0;
        const db = this.parseDateLocally(b.createdDate)?.getTime() || 0;
        return db - da;
      });
      
    if (activeRecharges.length > 0) {
      const latest = activeRecharges[0];
      const expiry = this.getExactExpiryDate(latest);
      if (expiry) {
        const now = new Date();
        const diffMs = expiry.getTime() - now.getTime();
        
        // Show if expiration is within 3 days (3 * 24 * 60 * 60 * 1000 ms) and plan is not already expired
        if (diffMs > 0 && diffMs <= 3 * 24 * 60 * 60 * 1000) {
          this.expiringRecharge.set(latest);
          
          let timeText = '';
          const totalHours = Math.floor(diffMs / (1000 * 60 * 60));
          const totalDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
          
          if (totalHours < 48) {
            if (totalHours === 0) {
               timeText = `Expires in less than an hour`;
            } else {
               timeText = `Due in ${totalHours} hour${totalHours > 1 ? 's' : ''}`;
            }
          } else {
            timeText = `Due in ${totalDays} days`;
          }
          
          this.reminderTimeLeft.set(timeText);
          this.showReminderModal.set(true);
        }
      }
    }
  }

  loadPayments(page: number = 0): void {
    if (page < 0) return;
    if (!this.applyingPayDateFilter()) {
      this.payLoading.set(true);
    }
    const filters: { startDate?: string; endDate?: string } = {};
    if (this.payStartDate) filters.startDate = `${this.payStartDate}T00:00:00`;
    if (this.payEndDate) filters.endDate = `${this.payEndDate}T23:59:59`;
    this.paymentService.getPaymentHistory(page, 10, Object.keys(filters).length > 0 ? filters : undefined).subscribe({
      next: res => {
        if (res.success && res.data) {
          // De-duplicate by rechargeId, keeping SUCCESS/FAILED over PENDING
          const uniqueItems = new Map<string, TransactionResponse>();
          res.data.content.forEach((item: TransactionResponse) => {
            const existing = uniqueItems.get(item.rechargeId);
            if (!existing) {
              uniqueItems.set(item.rechargeId, item);
            } else if (item.status === 'SUCCESS' || (item.status === 'FAILED' && existing.status === 'PENDING')) {
              uniqueItems.set(item.rechargeId, item);
            }
          });
          
          this.payments.set(Array.from(uniqueItems.values()));
          this.payPage.set(res.data.number);
          this.payTotalPages.set(res.data.totalPages);
          this.payTotalElements.set(res.data.totalElements);
        }
        this.payLoading.set(false);
        this.applyingPayDateFilter.set(false);
      },
      error: () => {
        this.payLoading.set(false);
        this.applyingPayDateFilter.set(false);
      }
    });
  }

  /** Apply date filter to recharges and reload from page 0 */
  applyRechDateFilter(): void {
    this.applyingRechDateFilter.set(true);
    this.loadRecharges(0);
  }

  /** Clear date filter on recharges and reload from page 0 */
  clearRechDateFilter(): void {
    this.applyingRechDateFilter.set(true);
    this.rechStartDate = '';
    this.rechEndDate = '';
    this.loadRecharges(0);
  }

  /** Apply date filter to payments and reload from page 0 */
  applyPayDateFilter(): void {
    this.applyingPayDateFilter.set(true);
    this.loadPayments(0);
  }

  /** Clear date filter on payments and reload from page 0 */
  clearPayDateFilter(): void {
    this.applyingPayDateFilter.set(true);
    this.payStartDate = '';
    this.payEndDate = '';
    this.loadPayments(0);
  }

  loadNotifications(page: number = 0): void {
    if (page < 0) return;
    this.notificationService.getNotifications(page, 10).subscribe({
      next: res => {
        if (res.success && res.data) {
          // Filter out low-value noise and silently mark them as read!
          const filtered = res.data.content.filter(n => {
            const msg = (n.message || '').toLowerCase();
            const subject = (n.subject || '').toLowerCase();
            const isNoise = msg.includes('email sent') || subject.includes('email sent') ||
                            msg.includes('confirmation email') || subject.includes('confirmation email');
            
            if (isNoise && !n.isRead) {
               // Silently mark background noise as read to prevent unread ghost badges
               this.notificationService.markAsRead(n.id).subscribe();
            }
            
            return !isNoise; 
          });
          this.notifications.set(filtered);
          this.notifPage.set(res.data.number);
          this.notifTotalPages.set(res.data.totalPages);
          this.notifTotalElements.set(filtered.length);
        }
      }
    });
  }

  getPayPageNumbers(): number[] {
    const total = this.payTotalPages();
    const current = this.payPage();
    const delta = 2;
    const range: number[] = [];
    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  getNotifPageNumbers(): number[] {
    const total = this.notifTotalPages();
    const current = this.notifPage();
    const delta = 2;
    const range: number[] = [];
    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  /** Derive notification style/badge/type from content */
  getNotifStyle(n: Notification): { type: 'success' | 'error' | 'info'; badge: string; iconClass: string } {
    const combined = ((n.subject || '') + ' ' + n.message).toLowerCase();
    if (combined.includes('success')) {
      const badge = combined.includes('payment') ? 'PAYMENT_SUCCESS' :
                    combined.includes('recharge') ? 'RECHARGE_SUCCESS' : 'SUCCESS';
      return { type: 'success', badge, iconClass: 'bg-accent-emerald/10 text-accent-emerald group-hover:shadow-accent-emerald/40' };
    }
    if (combined.includes('fail') || combined.includes('reject') || combined.includes('expired')) {
      const badge = combined.includes('payment') ? 'PAYMENT_FAILED' :
                    combined.includes('recharge') ? 'RECHARGE_FAILED' : 'FAILED';
      return { type: 'error', badge, iconClass: 'bg-accent-rose/10 text-accent-rose group-hover:shadow-accent-rose/40' };
    }
    return { type: 'info', badge: 'INFO', iconClass: 'bg-omni-500/20 text-omni-300 group-hover:shadow-omni-500/40' };
  }

  onUpdateProfile(): void {
    if (!this.profileName) {
      this.profileMsg.set('Please enter your full name.');
      this.profileMsgError.set(true);
      return;
    }

    this.profileSaving.set(true);
    this.profileMsg.set('');

    // Step 1: Always update fullName via profile endpoint
    this.authService.updateProfile({
      fullName: this.profileName
    }).subscribe({
      next: () => {
        this.profileMsg.set('Profile updated successfully.');
        this.profileMsgError.set(false);
        this.profileSaving.set(false);
        
        // Auto-dismiss the success message after 4 seconds
        setTimeout(() => {
          if (!this.profileMsgError()) {
            this.profileMsg.set('');
          }
        }, 4000);
      },
      error: err => {
        this.profileMsg.set(err.error?.message || 'Failed to update profile.');
        this.profileMsgError.set(true);
        this.profileSaving.set(false);
      }
    });
  }

  getDisplayName(): string {
    const user = this.authService.currentUser();
    if (!user) return 'User';
    const name = user.fullName;
    if (!name || name.startsWith('User ')) {
      return 'User';
    }
    return name;
  }

  hasRealName(): boolean {
    const name = this.authService.currentUser()?.fullName;
    return !!name && !name.startsWith('User ');
  }

  onSendEmailVerification(): void {
    if (!this.newEmail) return;
    this.emailVerifying.set(true);
    this.profileMsg.set('');

    const token = this.authService.getAccessToken();
    if (!token) return;

    this.http.post<any>(
      `${this.authService['API']}/email/send-verification?email=${encodeURIComponent(this.newEmail)}`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    ).subscribe({
      next: () => {
        this.emailOtpSent.set(true);
        this.emailVerifying.set(false);
        this.profileMsg.set('Verification OTP sent to ' + this.newEmail);
        this.profileMsgError.set(false);
      },
      error: err => {
        this.emailVerifying.set(false);
        this.profileMsg.set(err.error?.message || 'Failed to send email verification.');
        this.profileMsgError.set(true);
      }
    });
  }

  // --- Mobile OTP Handlers ---
  onSendMobileVerification(): void {
    if (!this.newMobile) return;
    this.mobileVerifying.set(true);
    this.authService.sendMobileOtp(`+91${this.newMobile}`).subscribe({
      next: () => {
        this.mobileVerifying.set(false);
        this.mobileOtpSent.set(true);
        this.profileMsg.set(`OTP sent to +91 ${this.newMobile}`);
        this.profileMsgError.set(false);
      },
      error: err => {
        this.mobileVerifying.set(false);
        this.profileMsg.set(err.error?.message || 'Failed to send OTP.');
        this.profileMsgError.set(true);
      }
    });
  }

  onMobileOtpInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/[^0-9]/g, '');
    const digits = [...this.mobileOtpDigits()];
    digits[index] = value;
    this.mobileOtpDigits.set(digits);
    if (value && index < 5) setTimeout(() => document.getElementById(`mobile-otp-${index + 1}`)?.focus());
  }

  onMobileOtpKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.mobileOtpDigits()[index] && index > 0) {
      setTimeout(() => {
        const prevInput = document.getElementById(`mobile-otp-${index - 1}`);
        if (prevInput) {
          prevInput.focus();
          const digits = [...this.mobileOtpDigits()];
          digits[index - 1] = '';
          this.mobileOtpDigits.set(digits);
        }
      });
    }
  }

  onMobileOtpPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = (event.clipboardData?.getData('text') || '').replace(/[^0-9]/g, '').slice(0, 6);
    if (pasted) {
      const digits = [...this.mobileOtpDigits()];
      for (let i = 0; i < pasted.length; i++) digits[i] = pasted[i];
      this.mobileOtpDigits.set(digits);
      setTimeout(() => document.getElementById(`mobile-otp-${Math.min(pasted.length, 5)}`)?.focus());
    }
  }

  getMobileOtpString(): string {
    return this.mobileOtpDigits().join('');
  }

  onVerifyMobile(): void {
    const otp = this.getMobileOtpString();
    if (otp.length !== 6) return;
    this.mobileVerifying.set(true);
    this.authService.verifyMobileOtp(`+91${this.newMobile}`, otp).subscribe({
      next: () => {
        this.mobileVerifying.set(false);
        this.mobileEditMode.set(false);
        this.mobileOtpSent.set(false);
        this.mobileOtpDigits.set(['', '', '', '', '', '']);
        this.profileMsg.set('Mobile number linked and verified successfully!');
        this.profileMsgError.set(false);
        this.authService.loadProfile();
      },
      error: err => {
        this.mobileVerifying.set(false);
        this.profileMsg.set(err.error?.message || 'Verification failed.');
        this.profileMsgError.set(true);
      }
    });
  }

  // --- Email OTP Handlers ---
  onEmailOtpInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/[^0-9]/g, ''); // Ensure only numbers
    
    // Update signal correctly
    const digits = [...this.emailOtpDigits()];
    digits[index] = value;
    this.emailOtpDigits.set(digits);
    
    // Auto-advance focus
    if (value && index < 5) {
      setTimeout(() => {
        const nextInput = document.getElementById(`email-otp-${index + 1}`);
        if (nextInput) nextInput.focus();
      });
    }
  }

  onEmailOtpKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.emailOtpDigits()[index] && index > 0) {
      setTimeout(() => {
        const prevInput = document.getElementById(`email-otp-${index - 1}`);
        if (prevInput) {
          prevInput.focus();
          // Also clear the previous digit visually
          const digits = [...this.emailOtpDigits()];
          digits[index - 1] = '';
          this.emailOtpDigits.set(digits);
        }
      });
    }
  }

  onEmailOtpPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pastedData = event.clipboardData?.getData('text') || '';
    const digitsOnly = pastedData.replace(/[^0-9]/g, '').slice(0, 6);
    
    if (digitsOnly) {
      const newDigits = [...this.emailOtpDigits()];
      for (let i = 0; i < digitsOnly.length; i++) {
        newDigits[i] = digitsOnly[i];
      }
      this.emailOtpDigits.set(newDigits);
      
      // Focus the next empty box or the last box
      const nextIndex = Math.min(digitsOnly.length, 5);
      setTimeout(() => {
        const nextInput = document.getElementById(`email-otp-${nextIndex}`);
        if (nextInput) nextInput.focus();
      });
    }
  }

  getEmailOtpString(): string {
    return this.emailOtpDigits().join('');
  }

  onVerifyEmail(): void {
    const otp = this.getEmailOtpString();
    if (otp.length !== 6) return;
    this.emailVerifying.set(true);

    const token = this.authService.getAccessToken();
    if (!token) return;

    this.http.post<any>(
      `${this.authService['API']}/email/verify?otp=${otp}`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    ).subscribe({
      next: () => {
        this.emailVerifying.set(false);
        this.emailEditMode.set(false);
        this.emailOtpSent.set(false);
        this.emailOtpDigits.set(['', '', '', '', '', '']);
        this.newEmail = '';
        this.profileMsg.set('Email verified and linked successfully!');
        this.profileMsgError.set(false);
        this.authService.loadProfile();
      },
      error: err => {
        this.emailVerifying.set(false);
        this.profileMsg.set(err.error?.message || 'Email verification failed.');
        this.profileMsgError.set(true);
      }
    });
  }

  markRead(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe();
      // Update local state
      this.notifications.update(list =>
        list.map(n => n.id === notification.id ? { ...n, isRead: true } : n)
      );
    }
  }
  ngOnDestroy(): void {
    this.notificationService.stopPolling();
    if (this._notifRefreshInterval) clearInterval(this._notifRefreshInterval);
  }
}
