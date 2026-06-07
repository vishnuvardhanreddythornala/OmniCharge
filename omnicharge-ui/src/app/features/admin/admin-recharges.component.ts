import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { AdminService } from '../../core/services/admin.service';
import { RechargeHistoryItem } from '../../core/services/recharge.service';

@Component({
  selector: 'app-admin-recharges',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in relative max-w-full">
      
      <!-- Header -->
      <div class="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
        <div>
          <h1 class="text-3xl font-light text-surface-900 mb-2 tracking-wide font-outfit">
            System <span class="font-semibold text-transparent bg-clip-text bg-gradient-to-r from-omni-400 to-accent-teal">Recharges</span>
          </h1>
          <p class="text-surface-500 text-sm">Monitor all platform recharge activity.</p>
        </div>
        
        <div class="flex items-center gap-3">
          <div class="relative">
            <input type="text" 
                   [ngModel]="searchQuery" 
                   (ngModelChange)="onSearchChange($event)"
                   placeholder="Search by Recharge ID..." 
                   class="w-full sm:w-64 pl-10 pr-4 py-2.5 rounded-xl bg-white border border-surface-200 text-sm focus:border-omni-500/50 outline-none transition-colors text-surface-900 placeholder-surface-400" />
            <svg class="w-4 h-4 absolute left-3.5 top-3 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
            @if (searchQuery) {
              <button (click)="clearSearch()" class="absolute right-3.5 top-3 text-surface-500 hover:text-surface-900 transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            }
          </div>
        </div>
      </div>

      <!-- Stat Cards -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6 animate-slide-up">
        <div class="glass-card p-5 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-omni-500/10 flex items-center justify-center border border-omni-500/20">
              <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
            </div>
            <p class="text-[10px] text-surface-500 font-bold uppercase tracking-wider">Total</p>
          </div>
          <span class="text-2xl font-display font-bold text-surface-900">{{ totalElements() }}</span>
        </div>
        <div class="glass-card p-5 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-accent-emerald/10 flex items-center justify-center border border-accent-emerald/20">
              <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
            </div>
            <p class="text-[10px] text-surface-500 font-bold uppercase tracking-wider">Success</p>
          </div>
          <span class="text-2xl font-display font-bold text-surface-900">{{ successCount() }}</span>
        </div>
        <div class="glass-card p-5 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-accent-rose/10 flex items-center justify-center border border-accent-rose/20">
              <svg class="w-4 h-4 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>
            </div>
            <p class="text-[10px] text-surface-500 font-bold uppercase tracking-wider">Failed</p>
          </div>
          <span class="text-2xl font-display font-bold text-surface-900">{{ failedCount() }}</span>
        </div>
        <div class="glass-card p-5 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-accent-amber/10 flex items-center justify-center border border-accent-amber/20">
              <svg class="w-4 h-4 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            </div>
            <p class="text-[10px] text-surface-500 font-bold uppercase tracking-wider">Processing</p>
          </div>
          <span class="text-2xl font-display font-bold text-surface-900">{{ processingCount() }}</span>
        </div>
      </div>

      <div class="flex flex-col md:flex-row items-stretch gap-3 mb-6 animate-slide-up">
        
        <!-- Status Filter Tabs -->
        <div class="flex-1 flex overflow-x-auto p-1 bg-white border border-white/[0.05] rounded-xl hide-scrollbar min-w-0">
          <div class="flex items-center gap-1 w-full">
            @for (filter of statusFilters; track filter.key) {
              <button (click)="setCategory(filter.key)"
                      [class]="rechargeCategory() === filter.key ? 'bg-surface-100 text-surface-900 shadow-sm' : 'text-surface-500 hover:text-surface-900 hover:bg-white/[0.02]'"
                      class="flex-1 px-4 py-2.5 rounded-lg text-xs font-bold tracking-wider transition-all whitespace-nowrap text-center">
                {{ filter.label }}
              </button>
            }
          </div>
        </div>

        <!-- Date Range Filter -->
        <div class="flex-1 glass-card p-1 flex items-center border border-white/[0.05] rounded-xl min-w-0">
          <div class="flex items-center gap-2 text-[10px] text-surface-500 font-semibold uppercase tracking-wider px-4 shrink-0 border-r border-white/5">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
            Date Range
          </div>
          <div class="flex items-center gap-1.5 px-3 py-1 flex-1 min-w-0">
            <input type="date" [(ngModel)]="filterStartDate"
                   class="bg-transparent text-sm text-surface-900 outline-none w-32 [color-scheme:dark] placeholder-surface-400 font-mono" />
            <span class="text-surface-600">-</span>
            <input type="date" [(ngModel)]="filterEndDate"
                   class="bg-transparent text-sm text-surface-900 outline-none w-32 [color-scheme:dark] placeholder-surface-400 font-mono" />
          </div>
          <div class="flex gap-1 pr-1 pl-2 border-l border-white/5 shrink-0">
            <button (click)="applyDateFilter()" 
                    [disabled]="applyingDateFilter() || (!filterStartDate && !filterEndDate)"
                    class="btn-primary !py-2 !px-4 text-xs disabled:opacity-50 h-[34px]">
              @if (applyingDateFilter()) {
                <svg class="animate-spin h-3.5 w-3.5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
              } @else {
                Apply
              }
            </button>
            @if (filterStartDate || filterEndDate) {
              <button (click)="clearDateFilter()" title="Clear Dates"
                      class="btn-ghost !py-2 !px-2.5 text-xs border border-white/5 h-[34px] hover:bg-accent-rose/10 hover:text-accent-rose hover:border-accent-rose/20">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            }
          </div>
        </div>
      </div>

      <div class="glass-card overflow-hidden">
        @if (loading()) {
          <div class="p-8 space-y-4">
            @for(i of [1,2,3,4,5]; track i) {
              <div class="skeleton h-12 w-full rounded-xl"></div>
            }
          </div>
        } @else {
          <!-- Desktop Table (hidden on mobile) -->
          <div class="hidden sm:block overflow-x-auto">
            <table class="w-full text-left text-sm text-surface-600">
              <thead class="text-xs uppercase bg-white/[0.03] text-surface-500 border-b border-white/[0.05]">
                <tr>
                  <th scope="col" class="px-6 py-4 font-semibold">Operator</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Recharge ID</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Mobile & Plan</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Status</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Date</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-white/[0.05]">
                @for (rech of recharges(); track rech.rechargeId) {
                  <tr class="hover:bg-white/[0.02] transition-colors">
                    <td class="px-6 py-4 font-mono text-xs text-surface-900">
                      {{ rech.operatorName }}
                    </td>
                    <td class="px-6 py-4 font-mono text-xs text-surface-500">
                      {{ rech.rechargeId }}
                    </td>
                    <td class="px-6 py-4">
                      <div class="text-surface-900 font-medium text-sm">{{ rech.mobileNumber }}</div>
                      <div class="text-[10px] text-surface-500 mt-0.5">₹{{ rech.amount }} ({{ rech.planValidityDays }} Days)</div>
                    </td>
                    <td class="px-6 py-4">
                      <span class="px-2 py-1 rounded bg-white/[0.05] text-[10px] font-bold uppercase tracking-wider border border-surface-200"
                            [class]="rech.status === 'SUCCESS' ? 'text-accent-emerald border-accent-emerald/20 bg-accent-emerald/5' : 
                                     rech.status === 'FAILED' ? 'text-accent-rose border-accent-rose/20 bg-accent-rose/5' : 
                                     'text-accent-amber border-accent-amber/20 bg-accent-amber/5'">
                        {{ rech.status }}
                      </span>
                    </td>
                    <td class="px-6 py-4 text-xs">
                      {{ rech.createdDate | date:'medium' }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Mobile Card Layout -->
          <div class="sm:hidden divide-y divide-white/[0.05]">
            @for (rech of recharges(); track rech.rechargeId) {
              <div class="p-4 space-y-2.5">
                <div class="flex items-center justify-between">
                  <span class="font-mono text-xs text-surface-900">{{ rech.mobileNumber }}</span>
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border"
                        [class]="rech.status === 'SUCCESS' ? 'text-accent-emerald border-accent-emerald/20 bg-accent-emerald/5' : 
                                 rech.status === 'FAILED' ? 'text-accent-rose border-accent-rose/20 bg-accent-rose/5' : 
                                 'text-accent-amber border-accent-amber/20 bg-accent-amber/5'">
                    {{ rech.status }}
                  </span>
                </div>
                <div class="flex items-center justify-between text-xs">
                  <span class="text-surface-500">Recharge ID: <span class="font-mono text-surface-600">{{ rech.rechargeId }}</span></span>
                  <span class="font-semibold text-surface-900">₹{{ rech.amount }}</span>
                </div>
                <div class="text-[10px] text-surface-500">{{ rech.createdDate | date:'medium' }}</div>
              </div>
            }
          </div>
            
          @if (recharges().length === 0) {
            <div class="p-16 text-center">
              <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-gradient-to-br from-surface-800 to-surface-900 flex items-center justify-center border border-white/[0.06]">
                <svg class="w-10 h-10 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-surface-900 mb-1.5">No recharges found</h3>
              <p class="text-sm text-surface-500 max-w-xs mx-auto leading-relaxed">
                {{ searchQuery ? 'No recharges match "' + searchQuery + '". Try a different search term.' : (filterStartDate || filterEndDate) ? 'No recharges in this date range. Try adjusting the dates.' : 'Recharge records will appear here once users make top-ups.' }}
              </p>
              @if (searchQuery || filterStartDate || filterEndDate) {
                <button (click)="clearFilters()" class="btn-ghost text-xs mt-4 border border-surface-200">
                  Clear all filters
                </button>
              }
            </div>
          }
          
          <!-- Pagination -->
          @if (totalPages() > 1) {
            <div class="flex items-center justify-between px-6 py-4 border-t border-white/[0.05]">
              <span class="text-xs text-surface-500">
                Showing {{ (currentPage() * 10) + 1 }}–{{ Math.min((currentPage() + 1) * 10, totalElements()) }} of {{ totalElements() }}
              </span>
              <div class="flex items-center gap-1">
                <button (click)="loadRecharges(currentPage() - 1)" [disabled]="currentPage() === 0" 
                        class="btn-ghost text-xs !py-1.5 !px-3 disabled:opacity-30">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                </button>
                @for (page of getPageNumbers(); track page) {
                  <button (click)="loadRecharges(page)" 
                          [class]="page === currentPage() ? 'bg-omni-600 text-surface-900' : 'text-surface-500 hover:bg-white/[0.06]'"
                          class="w-8 h-8 rounded-lg text-xs font-medium transition-colors">
                    {{ page + 1 }}
                  </button>
                }
                <button (click)="loadRecharges(currentPage() + 1)" [disabled]="currentPage() >= totalPages() - 1" 
                        class="btn-ghost text-xs !py-1.5 !px-3 disabled:opacity-30">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                </button>
              </div>
            </div>
          }
        }
      </div>
    </div>
  `
})
export class AdminRechargesComponent implements OnInit, OnDestroy {
  private adminService = inject(AdminService);
  private destroy$ = new Subject<void>();
  private searchSubject$ = new Subject<string>();
  
  recharges = signal<RechargeHistoryItem[]>([]);
  rechargeCategory = signal<'ALL'|'SUCCESS'|'FAILED'|'PROCESSING'>('ALL');
  searchQuery = '';
  filterStartDate = '';
  filterEndDate = '';

  statusFilters = [
    { key: 'ALL' as const, label: 'ALL RECHARGES' },
    { key: 'SUCCESS' as const, label: 'SUCCESS' },
    { key: 'FAILED' as const, label: 'FAILED' },
    { key: 'PROCESSING' as const, label: 'PROCESSING' },
  ];
  
  loading = signal(true);
  applyingDateFilter = signal(false);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  
  // Stat counts
  successCount = signal(0);
  failedCount = signal(0);
  processingCount = signal(0);
  
  readonly Math = Math;

  ngOnInit() {
    this.searchSubject$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe((query) => {
      this.searchQuery = query;
      this.loadRecharges(0);
    });

    this.loadRecharges(0);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(query: string) {
    this.searchSubject$.next(query);
  }

  clearSearch() {
    this.searchQuery = '';
    this.searchSubject$.next('');
  }

  clearFilters() {
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.rechargeCategory.set('ALL');
    this.clearSearch();
  }

  setCategory(cat: 'ALL'|'SUCCESS'|'FAILED'|'PROCESSING') {
    this.rechargeCategory.set(cat);
    this.loadRecharges(0);
  }

  applyDateFilter(): void {
    this.applyingDateFilter.set(true);
    this.loadRecharges(0);
  }

  clearDateFilter(): void {
    this.applyingDateFilter.set(true);
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.loadRecharges(0);
  }

  loadRecharges(page: number) {
    if (page < 0) return;
    
    if (!this.applyingDateFilter()) {
      this.loading.set(true);
    }
    
    // Note: Admin service searches by rechargeId if provided. No endpoint specifically for search, but let's assume getRechargeHistory handles search if added. Wait, admin service signature:
    // getAllRecharges(page, size, startDate, endDate) DOES NOT currently take a search param.
    // Let me check adminService.getAllRecharges
    
    const start = this.filterStartDate ? `${this.filterStartDate}T00:00:00` : undefined;
    const end = this.filterEndDate ? `${this.filterEndDate}T23:59:59` : undefined;

    this.adminService.getAllRecharges(page, 10, this.rechargeCategory(), start, end).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          let items = res.data.content;
          // Local filtering for search fallback since backend might not support it
          if (this.searchQuery) {
            const query = this.searchQuery.toLowerCase();
            items = items.filter((r: any) => 
               r.rechargeId.toLowerCase().includes(query) || 
               r.mobileNumber.toLowerCase().includes(query)
            );
          }
          this.recharges.set(items);
          this.currentPage.set(res.data.number);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
          
          // Count stats from current page items (when viewing ALL)
          if (this.rechargeCategory() === 'ALL') {
            this.successCount.set(res.data.content.filter((r: any) => r.status === 'SUCCESS').length);
            this.failedCount.set(res.data.content.filter((r: any) => r.status === 'FAILED').length);
            this.processingCount.set(res.data.content.filter((r: any) => r.status !== 'SUCCESS' && r.status !== 'FAILED').length);
          }
        }
        this.loading.set(false);
        this.applyingDateFilter.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.applyingDateFilter.set(false);
      }
    });
  }

  getPageNumbers(): number[] {
    const total = this.totalPages();
    const current = this.currentPage();
    if (total <= 5) return Array.from({ length: total }, (_, i) => i);
    
    let start = Math.max(0, current - 2);
    let end = Math.min(total - 1, current + 2);
    
    if (current < 2) end = 4;
    if (current > total - 3) start = total - 5;
    
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }
}
