import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, UserProfileResponse } from '../../core/services/admin.service';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="animate-fade-in">
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 class="text-2xl font-display font-bold text-white">User Management</h1>
          <p class="text-sm text-surface-400 mt-1">Manage platform users and security</p>
        </div>

        <!-- Search Bar -->
        <div class="relative w-full sm:w-80">
          <svg class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          <input type="text"
                 [(ngModel)]="searchQuery"
                 (ngModelChange)="onSearchChange($event)"
                 placeholder="Search by name, email, or mobile..."
                 class="w-full pl-10 pr-10 py-2.5 rounded-xl bg-surface-900/60 border border-white/[0.08] text-sm text-white placeholder-surface-500 outline-none transition-all focus:border-omni-500/50 focus:shadow-[0_0_12px_rgba(99,102,241,0.1)]" />
          @if (searchQuery) {
            <button (click)="clearSearch()" class="absolute right-3 top-1/2 -translate-y-1/2 text-surface-500 hover:text-white transition-colors">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          }
        </div>
      </div>

      <!-- Stat Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6 animate-slide-up">
        
        <div class="glass-card p-6 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-omni-500/10 flex items-center justify-center border border-omni-500/20">
              <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <p class="text-xs text-surface-400 font-bold uppercase tracking-wider">Total Users</p>
          </div>
          <span class="text-3xl font-display font-bold px-1 text-white">{{ totalUsersCount() }}</span>
        </div>

        <div class="glass-card p-6 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-accent-emerald/10 flex items-center justify-center border border-accent-emerald/20">
              <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
            </div>
            <p class="text-xs text-surface-400 font-bold uppercase tracking-wider">Active Users</p>
          </div>
          <span class="text-3xl font-display font-bold px-1 text-white">{{ activeUsersCount() }}</span>
        </div>

        <div class="glass-card p-6 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-accent-rose/10 flex items-center justify-center border border-accent-rose/20">
              <svg class="w-4 h-4 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>
            </div>
            <p class="text-xs text-surface-400 font-bold uppercase tracking-wider">Suspended Users</p>
          </div>
          <span class="text-3xl font-display font-bold px-1 text-white">{{ suspendedUsersCount() }}</span>
        </div>

      </div>

      <!-- Status Filter Tabs -->
      <div class="flex items-center gap-2 mb-6 overflow-x-auto scrollbar-none animate-slide-up">
        @for (filter of statusFilters; track filter.key) {
          <button (click)="setStatusFilter(filter.key)"
                  [class]="activeStatus() === filter.key ? 'tab-item-active !px-5 !py-2.5 rounded-full text-xs font-semibold' : 'tab-item !px-5 !py-2.5 rounded-full text-xs font-semibold'">
            {{ filter.label }}
          </button>
        }
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
            <table class="w-full text-left text-sm text-surface-300">
              <thead class="text-xs uppercase bg-white/[0.03] text-surface-400 border-b border-white/[0.05]">
                <tr>
                  <th scope="col" class="px-6 py-4 font-semibold">User ID</th>
                  <th scope="col" class="px-6 py-4 font-semibold">User</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Contact</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Role</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Status</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Joined On</th>
                  <th scope="col" class="px-6 py-4 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-white/[0.05]">
                @for (user of users(); track user.id) {
                  <tr class="hover:bg-white/[0.02] transition-colors">
                    <td class="px-6 py-4 font-mono text-xs text-white">
                      USR-{{ ('00000' + user.id).slice(-5) }}
                    </td>
                    <td class="px-6 py-4">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-full bg-gradient-to-br from-omni-600 to-accent-teal flex items-center justify-center text-white text-xs font-bold">
                          {{ user.fullName.charAt(0).toUpperCase() }}
                        </div>
                        <div class="font-medium text-white">{{ user.fullName }}</div>
                      </div>
                    </td>
                    <td class="px-6 py-4">
                      <div class="text-surface-200">{{ user.mobileNumber || '—' }}</div>
                      <div class="text-xs text-surface-500">{{ user.email }}</div>
                    </td>
                    <td class="px-6 py-4">
                      <span class="px-2 py-1 rounded bg-white/[0.05] text-xs font-mono border border-white/10"
                            [class.text-omni-400]="user.role === 'ROLE_ADMIN'">
                        {{ user.role.replace('ROLE_', '') }}
                      </span>
                    </td>
                    <td class="px-6 py-4">
                      <span class="flex items-center gap-1.5 text-xs font-medium"
                            [class]="user.isActive ? 'text-accent-emerald' : 'text-surface-500'">
                        <span class="w-1.5 h-1.5 rounded-full" [class]="user.isActive ? 'bg-accent-emerald' : 'bg-surface-500'"></span>
                        {{ user.isActive ? 'Active' : 'Suspended' }}
                      </span>
                    </td>
                    <td class="px-6 py-4 text-xs text-surface-400">
                      {{ user.createdDate | date:'mediumDate' }}
                    </td>
                    <td class="px-6 py-4 text-right">
                      <button *ngIf="user.role !== 'ROLE_ADMIN'" 
                              (click)="toggleStatus(user)"
                              [disabled]="togglingUser() === user.id"
                              class="text-xs px-3 py-1.5 rounded-md border transition-colors disabled:opacity-50"
                              [class]="user.isActive ? 'border-accent-rose/30 text-accent-rose hover:bg-accent-rose/10' : 'border-accent-emerald/30 text-accent-emerald hover:bg-accent-emerald/10'">
                        @if (togglingUser() === user.id) {
                           <svg class="animate-spin h-3.5 w-3.5 inline" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                             <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                             <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                           </svg>
                        } @else {
                           {{ user.isActive ? 'Suspend' : 'Activate' }}
                        }
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Mobile Card Layout (shown only on mobile) -->
          <div class="sm:hidden divide-y divide-white/[0.05]">
            @for (user of users(); track user.id) {
              <div class="p-4 space-y-3">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <div class="w-8 h-8 rounded-full bg-gradient-to-br from-omni-600 to-accent-teal flex items-center justify-center text-white text-xs font-bold">
                      {{ user.fullName.charAt(0).toUpperCase() }}
                    </div>
                    <div>
                      <div class="font-medium text-white text-sm">{{ user.fullName }}</div>
                      <div class="font-mono text-[10px] text-surface-400">USR-{{ ('00000' + user.id).slice(-5) }}</div>
                    </div>
                  </div>
                  <span class="px-2 py-0.5 rounded bg-white/[0.05] text-[10px] font-mono border border-white/10"
                        [class.text-omni-400]="user.role === 'ROLE_ADMIN'">
                    {{ user.role.replace('ROLE_', '') }}
                  </span>
                </div>
                
                <div class="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <div class="text-surface-500 text-[10px] uppercase">Contact</div>
                    <div class="text-surface-200">{{ user.mobileNumber || '—' }}</div>
                    <div class="text-surface-400 truncate">{{ user.email }}</div>
                  </div>
                  <div class="text-right">
                    <div class="text-surface-500 text-[10px] uppercase">Status</div>
                    <span class="inline-flex items-center gap-1 font-medium"
                          [class]="user.isActive ? 'text-accent-emerald' : 'text-surface-500'">
                      <span class="w-1.5 h-1.5 rounded-full" [class]="user.isActive ? 'bg-accent-emerald' : 'bg-surface-500'"></span>
                      {{ user.isActive ? 'Active' : 'Suspended' }}
                    </span>
                    <div class="text-surface-500 text-[10px] mt-0.5">{{ user.createdDate | date:'shortDate' }}</div>
                  </div>
                </div>

                @if (user.role !== 'ROLE_ADMIN') {
                  <div class="pt-2 border-t border-white/[0.05]">
                    <button (click)="toggleStatus(user)"
                            [disabled]="togglingUser() === user.id"
                            class="w-full py-2 rounded-lg text-xs font-medium border transition-colors disabled:opacity-50 flex justify-center items-center h-8"
                            [class]="user.isActive ? 'border-accent-rose/30 text-accent-rose hover:bg-accent-rose/10' : 'border-accent-emerald/30 text-accent-emerald hover:bg-accent-emerald/10'">
                      @if (togglingUser() === user.id) {
                        <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                      } @else {
                        {{ user.isActive ? 'Suspend User' : 'Activate User' }}
                      }
                    </button>
                  </div>
                }
              </div>
            }
            
            @if (users().length === 0) {
              <div class="p-16 text-center">
                <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-gradient-to-br from-surface-800 to-surface-900 flex items-center justify-center border border-white/[0.06]">
                  <svg class="w-10 h-10 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
                  </svg>
                </div>
                <h3 class="text-lg font-semibold text-white mb-1.5">No users found</h3>
                <p class="text-sm text-surface-400 max-w-xs mx-auto leading-relaxed">
                  {{ searchQuery ? 'No users match "' + searchQuery + '". Try a different search term.' : 'User records will appear here once registrations begin.' }}
                </p>
                @if (searchQuery) {
                  <button (click)="clearSearch()" class="btn-ghost text-xs mt-4 border border-white/10">
                    Clear search
                  </button>
                }
              </div>
            }
          </div>
          
          <!-- Pagination -->
          @if (totalPages() > 1) {
            <div class="flex items-center justify-between px-6 py-4 border-t border-white/[0.05]">
              <span class="text-xs text-surface-400">
                Showing {{ (currentPage() * 10) + 1 }}–{{ Math.min((currentPage() + 1) * 10, totalElements()) }} of {{ totalElements() }}
              </span>
              <div class="flex items-center gap-1">
                <button (click)="loadUsers(currentPage() - 1)" [disabled]="currentPage() === 0" 
                        class="btn-ghost text-xs !py-1.5 !px-3 disabled:opacity-30">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                </button>
                @for (page of getPageNumbers(); track page) {
                  <button (click)="loadUsers(page)" 
                          [class]="page === currentPage() ? 'bg-omni-600 text-white' : 'text-surface-400 hover:bg-white/[0.06]'"
                          class="w-8 h-8 rounded-lg text-xs font-medium transition-colors">
                    {{ page + 1 }}
                  </button>
                }
                <button (click)="loadUsers(currentPage() + 1)" [disabled]="currentPage() >= totalPages() - 1" 
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
export class AdminUsersComponent implements OnInit, OnDestroy {
  private adminService = inject(AdminService);
  private destroy$ = new Subject<void>();
  private searchSubject$ = new Subject<string>();
  
  users = signal<UserProfileResponse[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  searchQuery = '';
  
  statusFilters: { key: 'ALL' | 'ACTIVE' | 'SUSPENDED', label: string }[] = [
    { key: 'ALL', label: 'All Users' },
    { key: 'ACTIVE', label: 'Active' },
    { key: 'SUSPENDED', label: 'Suspended' }
  ];
  activeStatus = signal<'ALL' | 'ACTIVE' | 'SUSPENDED'>('ALL');
  
  totalUsersCount = signal(0);
  activeUsersCount = signal(0);
  suspendedUsersCount = signal(0);
  
  togglingUser = signal<number | null>(null);

  readonly Math = Math;

  ngOnInit() {
    // Debounced search - waits 400ms after user stops typing
    this.searchSubject$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => {
      this.searchQuery = query;
      this.loadUsers(0);
    });

    this.fetchUserStats();
    this.loadUsers(0);
  }

  fetchUserStats() {
    this.adminService.getAllUsers(0, 1, undefined, 'ACTIVE').subscribe(res => {
      if (res.success && res.data) {
        this.activeUsersCount.set(res.data.totalElements);
        this.updateTotalCount();
      }
    });
    this.adminService.getAllUsers(0, 1, undefined, 'SUSPENDED').subscribe(res => {
      if (res.success && res.data) {
        this.suspendedUsersCount.set(res.data.totalElements);
        this.updateTotalCount();
      }
    });
  }

  private updateTotalCount() {
    this.totalUsersCount.set(this.activeUsersCount() + this.suspendedUsersCount());
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

  setStatusFilter(status: 'ALL' | 'ACTIVE' | 'SUSPENDED') {
    this.activeStatus.set(status);
    this.loadUsers(0);
  }

  loadUsers(page: number) {
    if (page < 0) return;
    this.loading.set(true);
    this.adminService.getAllUsers(page, 10, this.searchQuery || undefined, this.activeStatus()).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const data = res.data as any;
          this.users.set(data.content || []);
          this.currentPage.set(data.number ?? data.page ?? data.pageable?.pageNumber ?? page);
          this.totalPages.set(data.totalPages ?? 0);
          this.totalElements.set(data.totalElements ?? 0);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getPageNumbers(): number[] {
    const total = this.totalPages();
    const current = this.currentPage();
    const delta = 2;
    const range: number[] = [];
    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  toggleStatus(user: UserProfileResponse) {
    this.togglingUser.set(user.id);
    const newStatus = !user.isActive;
    this.adminService.toggleUserStatus(user.id, newStatus).subscribe({
      next: (res) => {
        if (res.success) {
          this.users.update(list => list.map(u => u.id === user.id ? { ...u, isActive: newStatus } : u));
          this.fetchUserStats(); // Update stats instantly
        }
        this.togglingUser.set(null);
      },
      error: () => this.togglingUser.set(null)
    });
  }
}
