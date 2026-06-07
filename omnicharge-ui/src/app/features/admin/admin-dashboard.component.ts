import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, PaymentStatsResponse, RechargeStatsResponse, UserProfileResponse } from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="animate-fade-in">

      <!-- Header Row -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-display font-bold text-surface-900">Dashboard Overview</h1>
          <p class="text-sm text-surface-500 mt-1">Real-time platform metrics &amp; system health</p>
        </div>
        <button
          id="rebuild-cache-btn"
          (click)="rebuildCache()"
          [disabled]="rebuildingCache()"
          class="group relative flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all duration-300 border overflow-hidden"
          [class]="rebuildingCache()
            ? 'bg-white/[0.03] border-surface-200 text-surface-500 cursor-not-allowed'
            : 'bg-gradient-to-r from-accent-amber/10 to-accent-amber/5 border-accent-amber/25 text-accent-amber hover:border-accent-amber/50 hover:shadow-[0_0_20px_rgba(245,158,11,0.1)]'"
        >
          <!-- Animated spinning gears icon -->
          <svg class="w-4 h-4 transition-transform duration-500" [class.animate-spin]="rebuildingCache()" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          {{ rebuildingCache() ? 'Rebuilding...' : 'Rebuild System Cache' }}
        </button>
      </div>

      <!-- Toast Notification -->
      @if (toastVisible()) {
        <div class="fixed top-20 right-6 z-[100] animate-slide-in-right">
          <div class="flex items-center gap-3 px-5 py-3.5 rounded-xl border shadow-2xl backdrop-blur-xl"
               [class]="toastType() === 'success'
                 ? 'bg-accent-emerald/10 border-accent-emerald/30 text-accent-emerald shadow-accent-emerald/10'
                 : 'bg-accent-rose/10 border-accent-rose/30 text-accent-rose shadow-accent-rose/10'">
            @if (toastType() === 'success') {
              <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            } @else {
              <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            }
            <span class="text-sm font-medium">{{ toastMessage() }}</span>
            <button (click)="toastVisible.set(false)" class="ml-2 opacity-60 hover:opacity-100 transition-opacity">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
          </div>
        </div>
      }

      <!-- Skeleton Loading -->
      @if (loading()) {
        <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          @for (i of [1,2,3,4,5,6,7,8]; track i) {
            <div class="glass-card p-6 h-32 skeleton rounded-2xl border-none"></div>
          }
        </div>
      } @else {

        <!-- Payment Stats Row -->
        <div class="mb-4">
          <h2 class="text-xs uppercase tracking-widest text-surface-500 font-semibold mb-3 flex items-center gap-2">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z"/></svg>
            Payment Metrics
          </h2>
        </div>
        <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <!-- Total Revenue -->
          <div class="glass-card p-6 border-accent-emerald/20 shadow-[0_0_15px_rgba(16,185,129,0.05)] relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-accent-emerald/10 rounded-full blur-xl group-hover:bg-accent-emerald/20 transition-all duration-500"></div>
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-emerald/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Total Revenue</span>
              </div>
              <div class="text-3xl font-display font-bold text-accent-emerald">₹{{ formatNumber(paymentStats()?.totalRevenue) }}</div>
              <div class="text-[10px] text-surface-500 mt-1 font-medium">Successful payments · 30 days</div>
            </div>
          </div>

          <!-- Total Transactions -->
          <div class="glass-card p-6 border-omni-500/20 shadow-[0_0_15px_rgba(168,85,247,0.05)] relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-omni-500/10 rounded-full blur-xl group-hover:bg-omni-400/20 transition-all duration-500"></div>
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-omni-500/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Transactions</span>
              </div>
              <div class="text-3xl font-display font-bold text-surface-900">{{ paymentStats()?.totalTransactions || 0 }}</div>
              <div class="text-[10px] text-surface-500 mt-1 font-medium">
                <span class="text-accent-emerald">{{ paymentStats()?.successfulTransactions || 0 }} passed</span> ·
                <span class="text-accent-rose">{{ paymentStats()?.failedTransactions || 0 }} failed</span>
              </div>
            </div>
          </div>

          <!-- Success Rate -->
          <div class="glass-card p-6 relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-accent-teal/10 rounded-full blur-xl group-hover:bg-accent-teal/20 transition-all duration-500"></div>
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-teal/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-accent-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Success Rate</span>
              </div>
              <div class="text-3xl font-display font-bold text-accent-teal">{{ getSuccessRate() }}%</div>
              <div class="mt-2 w-full h-1.5 bg-white/[0.05] rounded-full overflow-hidden">
                <div class="h-full bg-gradient-to-r from-accent-teal to-accent-emerald rounded-full transition-all duration-1000"
                     [style.width.%]="getSuccessRate()"></div>
              </div>
            </div>
          </div>

          <!-- Today's Revenue -->
          <div class="glass-card p-6 relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-accent-amber/10 rounded-full blur-xl group-hover:bg-accent-amber/20 transition-all duration-500"></div>
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-amber/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Today</span>
              </div>
              <div class="text-3xl font-display font-bold text-accent-amber">₹{{ formatNumber(paymentStats()?.todayRevenue) }}</div>
              <div class="text-[10px] text-surface-500 mt-1 font-medium">{{ paymentStats()?.todayTransactions || 0 }} transactions today</div>
            </div>
          </div>
        </div>

        <!-- Recharge Stats Row -->
        <div class="mb-4">
          <h2 class="text-xs uppercase tracking-widest text-surface-500 font-semibold mb-3 flex items-center gap-2">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
            Recharge Metrics
          </h2>
        </div>
        <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <!-- Total Recharges -->
          <div class="glass-card p-6 border-sky-500/20 shadow-[0_0_15px_rgba(14,165,233,0.05)] relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-sky-500/10 rounded-full blur-xl group-hover:bg-sky-400/20 transition-all duration-500"></div>
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-sky-500/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-sky-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Total Recharges</span>
              </div>
              <div class="text-3xl font-display font-bold text-surface-900">{{ rechargeStats()?.totalRecharges || 0 }}</div>
              <div class="text-[10px] text-surface-500 mt-1 font-medium">All-time recharge requests</div>
            </div>
          </div>

          <!-- Recharge Volume -->
          <div class="glass-card p-6 relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-violet-500/10 rounded-full blur-xl group-hover:bg-violet-400/20 transition-all duration-500"></div>
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-violet-500/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-violet-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Recharge Volume</span>
              </div>
              <div class="text-3xl font-display font-bold text-violet-400">₹{{ formatNumber(rechargeStats()?.totalAmount) }}</div>
              <div class="text-[10px] text-surface-500 mt-1 font-medium">Total recharge value processed</div>
            </div>
          </div>

          <!-- Recharges Successful -->
          <div class="glass-card p-6 relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-emerald/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Successful</span>
              </div>
              <div class="text-3xl font-display font-bold text-accent-emerald">{{ rechargeStats()?.successCount || 0 }}</div>
              <div class="text-[10px] text-surface-500 mt-1 font-medium">Completed recharges</div>
            </div>
          </div>

          <!-- Recharges Failed -->
          <div class="glass-card p-6 relative overflow-hidden group hover:-translate-y-0.5 transition-all duration-300">
            <div class="relative">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-lg bg-accent-rose/10 flex items-center justify-center">
                  <svg class="w-4 h-4 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                </div>
                <span class="text-surface-500 text-xs font-semibold uppercase tracking-wider">Failed</span>
              </div>
              <div class="text-3xl font-display font-bold text-accent-rose">{{ rechargeStats()?.failedCount || 0 }}</div>
              <div class="text-[10px] text-surface-500 mt-1 font-medium">Failed recharge attempts</div>
            </div>
          </div>
        </div>

        <!-- Additional Insights Row -->
        <div class="grid lg:grid-cols-2 gap-4">

          <!-- Avg Transaction & Pending -->
          <div class="glass-card p-6 border border-white/[0.05]">
            <h3 class="text-sm font-semibold text-surface-900 mb-4 flex items-center gap-2">
              <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>
              Transaction Breakdown
            </h3>
            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <span class="text-sm text-surface-500">Avg. Transaction</span>
                <span class="text-sm font-semibold text-surface-900">₹{{ formatNumber(paymentStats()?.averageTransactionAmount) }}</span>
              </div>
              <div class="w-full h-px bg-white/[0.05]"></div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-surface-500">Pending Transactions</span>
                <span class="text-sm font-semibold text-accent-amber">{{ paymentStats()?.pendingTransactions || 0 }}</span>
              </div>
              <div class="w-full h-px bg-white/[0.05]"></div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-surface-500">Success Amount</span>
                <span class="text-sm font-semibold text-accent-emerald">₹{{ formatNumber(paymentStats()?.successAmount) }}</span>
              </div>
              <div class="w-full h-px bg-white/[0.05]"></div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-surface-500">Failed Amount</span>
                <span class="text-sm font-semibold text-accent-rose">₹{{ formatNumber(paymentStats()?.failedAmount) }}</span>
              </div>
            </div>
          </div>

          <!-- Top Users -->
          <div class="glass-card p-6 border border-white/[0.05]">
            <h3 class="text-sm font-semibold text-surface-900 mb-4 flex items-center gap-2">
              <svg class="w-4 h-4 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/></svg>
              Top Users by Spend
            </h3>
            @if (paymentStats()?.topUsers && paymentStats()!.topUsers.length > 0) {
              <div class="space-y-3">
                @for (user of paymentStats()!.topUsers.slice(0, 5); track user.userId; let i = $index) {
                  <div class="flex items-center gap-3">
                    <div class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold"
                         [class]="i === 0 ? 'bg-accent-amber/20 text-accent-amber' : i === 1 ? 'bg-surface-400/20 text-surface-600' : 'bg-amber-900/20 text-amber-700'">
                      {{ i + 1 }}
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="text-sm text-surface-600 font-medium">{{ getUserName(user.userId) }}</div>
                      <div class="text-[10px] text-surface-500">{{ user.transactionCount }} transactions</div>
                    </div>
                    <div class="text-sm font-semibold text-surface-900">₹{{ formatNumber(user.totalSpent) }}</div>
                  </div>
                }
              </div>
            } @else {
              <div class="text-sm text-surface-500 text-center py-6">No user data available yet.</div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes slideInRight {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    .animate-slide-in-right {
      animation: slideInRight 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
  `]
})
export class AdminDashboardComponent implements OnInit {
  private adminService = inject(AdminService);

  paymentStats = signal<PaymentStatsResponse | null>(null);
  rechargeStats = signal<RechargeStatsResponse | null>(null);
  userNameMap = signal<Map<number, string>>(new Map());
  loading = signal(true);
  rebuildingCache = signal(false);

  // Toast state
  toastVisible = signal(false);
  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  private toastTimer: any;

  ngOnInit() {
    this.loadStats();
  }

  private loadStats() {
    let completed = 0;
    const checkDone = () => { if (++completed >= 3) this.loading.set(false); };

    this.adminService.getPaymentStats(30).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.paymentStats.set(res.data);
        }
        checkDone();
      },
      error: () => checkDone()
    });

    this.adminService.getRechargeStats().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.rechargeStats.set(res.data);
        }
        checkDone();
      },
      error: () => checkDone()
    });

    // Fetch user names for the Top Users widget
    this.adminService.getAllUsers(0, 100).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const map = new Map<number, string>();
          res.data.content.forEach(u => map.set(u.id, u.fullName));
          this.userNameMap.set(map);
        }
        checkDone();
      },
      error: () => checkDone()
    });
  }

  rebuildCache() {
    this.rebuildingCache.set(true);
    this.adminService.rebuildCache().subscribe({
      next: (res) => {
        this.rebuildingCache.set(false);
        if (res.success) {
          this.showToast('Redis cache rebuilt successfully!', 'success');
        } else {
          this.showToast(res.message || 'Cache rebuild failed.', 'error');
        }
      },
      error: (err) => {
        this.rebuildingCache.set(false);
        this.showToast('Failed to rebuild cache. Please try again.', 'error');
      }
    });
  }

  getSuccessRate(): number {
    const stats = this.paymentStats();
    if (!stats || !stats.totalTransactions) return 0;
    return Math.round((stats.successfulTransactions / stats.totalTransactions) * 100);
  }

  formatNumber(value: number | undefined | null): string {
    if (value === undefined || value === null) return '0';
    return new Intl.NumberFormat('en-IN').format(value);
  }

  getUserName(userId: number): string {
    const formattedId = 'USR-' + ('00000' + userId).slice(-5);
    return this.userNameMap().get(userId) || `User (${formattedId})`;
  }

  private showToast(message: string, type: 'success' | 'error') {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage.set(message);
    this.toastType.set(type);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), 4000);
  }
}
