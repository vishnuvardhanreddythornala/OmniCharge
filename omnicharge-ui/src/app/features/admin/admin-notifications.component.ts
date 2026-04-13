import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService, NotificationResponse } from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-notifications',
  standalone: true,
  imports: [CommonModule],
  providers: [DatePipe],
  template: `
    <div class="animate-fade-in">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-display font-bold text-white">System Notifications</h1>
        <div class="text-sm text-surface-400">
          Total: <span class="text-white font-semibold">{{ totalElements() }}</span>
        </div>
      </div>

      <!-- Tab Navigation -->
      <div class="flex gap-2 overflow-x-auto pb-2 mb-6 scrollbar-none animate-slide-up">
        @for (tab of tabs; track tab.key) {
          <button (click)="setTab(tab.key)"
                  [class]="activeTab() === tab.key ? 'tab-item-active' : 'tab-item'"
                  class="flex items-center gap-2 whitespace-nowrap">
            {{ tab.label }}
          </button>
        }
      </div>

      <div class="glass-card overflow-hidden">
        @if (loading()) {
          <div class="p-8 space-y-4">
            @for(i of [1,2,3,4,5,6,7,8]; track i) {
              <div class="skeleton h-20 w-full rounded-xl"></div>
            }
          </div>
        } @else {
          <div class="divide-y divide-white/[0.05]">
            @for (notification of notifications(); track notification.id) {
              <div class="p-6 hover:bg-white/[0.02] transition-colors">
                <div class="flex items-start gap-4">
                  <!-- Icon based on category -->
                  <div class="flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center"
                       [class]="getCategoryIconClass(notification.category)">
                    <span class="text-lg" [innerHTML]="getCategoryIcon(notification.category)"></span>
                  </div>

                  <!-- Content -->
                  <div class="flex-1 min-w-0">
                    <div class="flex items-start justify-between gap-4 mb-2">
                      <div class="flex-1">
                        <div class="flex items-center gap-2 mb-1">
                          <h3 class="text-sm font-semibold text-white">{{ notification.subject }}</h3>
                          <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider"
                                [class]="getCategoryBadgeClass(notification.category)">
                            {{ notification.category }}
                          </span>
                        </div>
                        <p class="text-sm text-surface-300 line-clamp-2">{{ notification.message }}</p>
                      </div>
                      <div class="flex-shrink-0 text-right">
                        <div class="text-xs text-surface-400 mb-1">{{ notification.createdDate | date:'short' }}</div>
                        <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold"
                              [class]="getStatusBadgeClass(notification.status)">
                          <span class="w-1.5 h-1.5 rounded-full" [class]="getStatusDotClass(notification.status)"></span>
                          {{ notification.status }}
                        </span>
                      </div>
                    </div>

                    <!-- Metadata -->
                    <div class="flex items-center gap-4 mt-3 text-xs text-surface-500">
                      <span class="flex items-center gap-1">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                        </svg>
                        User ID: {{ notification.userId }}
                      </span>
                      <span class="flex items-center gap-1">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
                        </svg>
                        Type: {{ notification.type }}
                      </span>
                      @if (notification.referenceId) {
                        <span class="flex items-center gap-1 font-mono">
                          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
                          </svg>
                          Ref: {{ notification.referenceId }}
                        </span>
                      }
                    </div>
                  </div>
                </div>
              </div>
            }
          </div>
          
          @if (notifications().length === 0) {
            <div class="p-12 text-center">
              <svg class="w-16 h-16 mx-auto mb-4 text-surface-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
              </svg>
              <p class="text-surface-400">No notifications found.</p>
            </div>
          }
          
          <!-- Pagination -->
          @if (totalPages() > 1) {
            <div class="flex items-center justify-between px-6 py-4 border-t border-white/[0.05]">
              <span class="text-xs text-surface-400">
                Showing {{ (currentPage() * pageSize()) + 1 }} - {{ Math.min((currentPage() + 1) * pageSize(), totalElements()) }} of {{ totalElements() }}
              </span>
              <div class="flex gap-2">
                <button (click)="loadNotifications(currentPage() - 1)" 
                        [disabled]="currentPage() === 0" 
                        class="btn-ghost text-xs !py-1 !px-3 disabled:opacity-30">
                  Prev
                </button>
                <div class="flex items-center gap-1">
                  @for (page of getPageNumbers(); track page) {
                    <button (click)="loadNotifications(page)" 
                            [class]="page === currentPage() ? 'bg-omni-600 text-white' : 'text-surface-400 hover:bg-white/[0.06]'"
                            class="w-8 h-8 rounded-lg text-xs font-medium transition-colors">
                      {{ page + 1 }}
                    </button>
                  }
                </div>
                <button (click)="loadNotifications(currentPage() + 1)" 
                        [disabled]="currentPage() >= totalPages() - 1" 
                        class="btn-ghost text-xs !py-1 !px-3 disabled:opacity-30">
                  Next
                </button>
              </div>
            </div>
          }
        }
      </div>
    </div>
  `
})
export class AdminNotificationsComponent implements OnInit {
  private adminService = inject(AdminService);
  
  notifications = signal<NotificationResponse[]>([]);
  
  tabs: { key: 'ALL' | 'USER' | 'SYSTEM'; label: string }[] = [
    { key: 'ALL', label: 'All Messages' },
    { key: 'USER', label: 'User Messages' },
    { key: 'SYSTEM', label: 'System Messages' }
  ];
  activeTab = signal<'ALL' | 'USER' | 'SYSTEM'>('ALL');
  
  loading = signal(true);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  pageSize = signal(10);
  
  readonly Math = Math;

  ngOnInit() {
    this.loadNotifications(0);
  }

  setTab(tab: 'ALL' | 'USER' | 'SYSTEM') {
    this.activeTab.set(tab);
    this.loadNotifications(0);
  }

  loadNotifications(page: number) {
    if (page < 0) return;
    this.loading.set(true);
    this.adminService.getAllNotifications(page, this.pageSize(), this.activeTab()).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.notifications.set(res.data.content);
          this.currentPage.set(res.data.number);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
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

  getCategoryIconClass(category: string): string {
    if (category.includes('SUCCESS')) return 'bg-accent-emerald/15 text-accent-emerald border border-accent-emerald/20';
    if (category.includes('FAILED')) return 'bg-accent-rose/15 text-accent-rose border border-accent-rose/20';
    if (category.includes('PLAN')) return 'bg-accent-amber/15 text-accent-amber border border-accent-amber/20';
    return 'bg-white/[0.05] text-surface-400 border border-white/10';
  }

  getCategoryIcon(category: string): string {
    if (category.includes('SUCCESS') || category.includes('PAYMENT')) return '💰';
    if (category.includes('PLAN')) return '⚡';
    return '🔔';
  }

  getCategoryBadgeClass(category: string): string {
    return this.getCategoryIconClass(category); // Re-use the same mapping for badges
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'SENT': return 'bg-accent-emerald/15 text-accent-emerald';
      case 'FAILED': return 'bg-accent-rose/15 text-accent-rose';
      case 'PENDING': return 'bg-accent-amber/15 text-accent-amber';
      default: return 'bg-white/[0.05] text-surface-400';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'SENT': return 'bg-accent-emerald';
      case 'FAILED': return 'bg-accent-rose';
      case 'PENDING': return 'bg-accent-amber';
      default: return 'bg-surface-500';
    }
  }
}
