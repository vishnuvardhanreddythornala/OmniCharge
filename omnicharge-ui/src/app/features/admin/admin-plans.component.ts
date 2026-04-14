/**
 * AdminPlansComponent — Top-level plan management across all operators.
 * 
 * This provides a unified view of ALL plans across all operators,
 * allowing admins to search, filter, edit, and manage plans without
 * navigating into individual operators first.
 */
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminService, PlanResponse, AdminOperatorResponse, CreatePlanRequest } from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-plans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="animate-fade-in space-y-6">

      <!-- ═══════ PAGE HEADER ═══════ -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl sm:text-3xl font-display font-bold text-white tracking-tight">All Plans</h1>
          <p class="text-sm text-surface-400 mt-1">Manage recharge plans across all operators</p>
        </div>
        <button (click)="openAddModal()" 
                class="btn-primary flex items-center justify-center gap-2 !py-2.5 !px-5 whitespace-nowrap">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
          Add Plan
        </button>
      </div>

      <!-- ═══════ KPI METRICS ═══════ -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="glass-card p-4 group hover:border-omni-500/20 transition-all duration-300">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-omni-500/10 flex items-center justify-center group-hover:bg-omni-500/15 transition">
              <svg class="w-4.5 h-4.5 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
            </div>
            <span class="text-[10px] font-bold text-surface-500 uppercase tracking-widest">Total</span>
          </div>
          <div class="text-2xl font-display font-bold text-white">{{ totalElements() }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">Total plan records matching filters</div>
        </div>
        <div class="glass-card p-4 group hover:border-accent-emerald/20 transition-all duration-300">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-accent-emerald/10 flex items-center justify-center group-hover:bg-accent-emerald/15 transition">
              <svg class="w-4.5 h-4.5 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            </div>
            <span class="text-[10px] font-bold text-surface-500 uppercase tracking-widest">Active</span>
          </div>
          <div class="text-2xl font-display font-bold text-accent-emerald">{{ kpiActive() }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">visible to users</div>
        </div>
        <div class="glass-card p-4 group hover:border-accent-rose/20 transition-all duration-300">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-accent-rose/10 flex items-center justify-center group-hover:bg-accent-rose/15 transition">
              <svg class="w-4.5 h-4.5 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
            </div>
            <span class="text-[10px] font-bold text-surface-500 uppercase tracking-widest">Inactive</span>
          </div>
          <div class="text-2xl font-display font-bold text-accent-rose">{{ kpiInactive() }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">deactivated</div>
        </div>
        <div class="glass-card p-4 group hover:border-accent-amber/20 transition-all duration-300">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-accent-amber/10 flex items-center justify-center group-hover:bg-accent-amber/15 transition">
              <svg class="w-4.5 h-4.5 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5"/></svg>
            </div>
            <span class="text-[10px] font-bold text-surface-500 uppercase tracking-widest">Operators</span>
          </div>
          <div class="text-2xl font-display font-bold text-accent-amber">{{ operators().length }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">with plans</div>
        </div>
      </div>

      <!-- ═══════ FILTER BAR ═══════ -->
      <div class="glass-card p-4">
        <div class="flex flex-col lg:flex-row lg:items-center gap-3">
          <!-- Search -->
          <div class="relative flex-1">
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
            <input type="text"
                   [(ngModel)]="searchQuery"
                   (ngModelChange)="onFilterChange()"
                   placeholder="Search by plan name, data, benefits..."
                   class="input-field !pl-10 !py-2.5 text-sm w-full" />
          </div>
          <!-- Operator Filter -->
          <select [(ngModel)]="operatorFilterValue"
                  (ngModelChange)="operatorFilter.set($event); onFilterChange()"
                  class="input-field !py-2.5 text-sm !w-auto min-w-[160px]">
            <option value="ALL" class="bg-surface-900 text-white">All Operators</option>
            @for (op of operators(); track op.id) {
              <option [value]="op.id" class="bg-surface-900 text-white">{{ op.name }}</option>
            }
          </select>
          <!-- Status Filter -->
          <select [(ngModel)]="statusFilterValue"
                  (ngModelChange)="statusFilter.set($event); onFilterChange()"
                  class="input-field !py-2.5 text-sm !w-auto min-w-[130px]">
            <option value="ALL" class="bg-surface-900 text-white">All Status</option>
            <option value="ACTIVE" class="bg-surface-900 text-white">Active</option>
            <option value="INACTIVE" class="bg-surface-900 text-white">Inactive</option>
          </select>
          <!-- Category Filter -->
          <select [(ngModel)]="categoryFilterValue"
                  (ngModelChange)="categoryFilter.set($event); onFilterChange()"
                  class="input-field !py-2.5 text-sm !w-auto min-w-[140px]">
            <option value="ALL" class="bg-surface-900 text-white">All Categories</option>
            @for (cat of allCategories(); track cat) {
              <option [value]="cat" class="bg-surface-900 text-white">{{ cat }}</option>
            }
          </select>
        </div>
      </div>

      <!-- ═══════ DATA TABLE ═══════ -->
      <div class="glass-card overflow-hidden">
        @if (loading()) {
          <div class="p-6 space-y-3">
            @for (i of [1,2,3,4,5,6]; track i) {
              <div class="skeleton h-14 w-full rounded-xl"></div>
            }
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
              <thead class="text-[11px] uppercase bg-white/[0.025] text-surface-500 border-b border-white/[0.05] tracking-wider">
                <tr>
                  <th class="px-5 py-3.5 font-semibold">Plan Name</th>
                  <th class="px-4 py-3.5 font-semibold">Operator</th>
                  <th class="px-4 py-3.5 font-semibold">Price</th>
                  <th class="px-4 py-3.5 font-semibold">Validity</th>
                  <th class="px-4 py-3.5 font-semibold">Category</th>
                  <th class="px-4 py-3.5 font-semibold">Status</th>
                  <th class="px-4 py-3.5 font-semibold">Last Updated</th>
                  <th class="px-4 py-3.5 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-white/[0.04]">
                @for (plan of filteredPlans(); track plan.id) {
                  <tr class="hover:bg-white/[0.02] transition-colors group">
                    <td class="px-5 py-3.5">
                      <div class="font-medium text-white text-[13px]">{{ plan.planName }}</div>
                      <div class="text-[11px] text-surface-500 mt-0.5 flex items-center gap-2">
                        @if (plan.dataLimit) {
                          <span class="flex items-center gap-1">
                            <svg class="w-3 h-3 text-sky-400/60" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z"/></svg>
                            {{ plan.dataLimit }}
                          </span>
                        }
                        @if (plan.callBenefit) {
                          <span>· {{ plan.callBenefit }}</span>
                        }
                      </div>
                    </td>
                    <td class="px-4 py-3.5">
                      <a [routerLink]="['/admin/operators', plan.operatorId, 'plans']"
                         class="text-xs font-semibold text-omni-400 hover:text-omni-300 transition hover:underline underline-offset-2">
                        {{ plan.operatorName }}
                      </a>
                    </td>
                    <td class="px-4 py-3.5">
                      <span class="text-white font-bold text-base">₹{{ plan.price }}</span>
                    </td>
                    <td class="px-4 py-3.5">
                      <span class="text-surface-300 font-medium">{{ plan.validityDays }}d</span>
                    </td>
                    <td class="px-4 py-3.5">
                      <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider" [class]="getCategoryBadge(plan.category)">
                        {{ plan.category }}
                      </span>
                    </td>
                    <td class="px-4 py-3.5">
                      @if (plan.isActive) {
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/15">
                          <span class="w-1.5 h-1.5 rounded-full bg-accent-emerald animate-pulse"></span>
                          Active
                        </span>
                      } @else {
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold bg-accent-rose/10 text-accent-rose border border-accent-rose/15">
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
                          Inactive
                        </span>
                      }
                    </td>
                    <td class="px-4 py-3.5">
                      <div class="text-[11px] text-surface-400">{{ plan.lastModifiedDate ? formatDate(plan.lastModifiedDate) : '—' }}</div>
                    </td>
                    <td class="px-4 py-3.5 text-right">
                      <div class="flex items-center justify-end gap-2">
                        <button (click)="togglePlanStatus(plan)"
                                [disabled]="actionLoading()"
                                class="px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all duration-200 flex items-center gap-1.5"
                                [class]="plan.isActive
                                  ? 'border-accent-rose/25 text-accent-rose hover:bg-accent-rose/10'
                                  : 'border-accent-emerald/25 text-accent-emerald hover:bg-accent-emerald/10'">
                          @if (plan.isActive) {
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                            Deactivate
                          } @else {
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                            Activate
                          }
                        </button>
                        <!-- Edit Button -->
                        <button (click)="openEditModal(plan)"
                                class="px-3 py-1.5 rounded-lg text-xs font-semibold border border-white/10 text-surface-300 hover:text-white hover:bg-white/[0.06] transition-all duration-200 flex items-center gap-1.5 opacity-0 group-hover:opacity-100">
                          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                          Edit
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            @if (filteredPlans().length === 0) {
              <div class="p-16 text-center">
                <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-white/[0.03] flex items-center justify-center">
                  <svg class="w-8 h-8 text-surface-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                </div>
                <p class="text-surface-400 font-medium">No plans match your filters</p>
                <p class="text-xs text-surface-600 mt-1">Try adjusting search, operator, or category filters</p>
              </div>
            }

            <!-- ═══════ PAGINATION FOOTER ═══════ -->
            @if (totalPages() > 1) {
              <div class="flex items-center justify-between px-6 py-4 border-t border-white/[0.05] bg-white/[0.01]">
                <div class="text-xs text-surface-400 font-medium">
                  Showing page <span class="text-white">{{ currentPage() + 1 }}</span> of <span class="text-white">{{ totalPages() }}</span>
                </div>
                <div class="flex gap-2">
                  <button (click)="prevPage()"
                          [disabled]="currentPage() === 0"
                          class="px-3 py-1.5 rounded-md text-xs font-semibold bg-white/[0.05] hover:bg-white/[0.1] text-surface-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
                    Previous
                  </button>
                  <button (click)="nextPage()"
                          [disabled]="currentPage() >= totalPages() - 1"
                          class="px-3 py-1.5 rounded-md text-xs font-semibold bg-white/[0.05] hover:bg-white/[0.1] text-surface-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
                    Next
                  </button>
                </div>
              </div>
            }
          </div>
        }
      </div>

      <!-- ═══════ EDIT PLAN MODAL ═══════ -->
      @if (showEditModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4" (click)="showEditModal.set(false)">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="relative glass-card p-6 sm:p-8 w-full max-w-lg border-omni-500/10 animate-scale-in" (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-display font-bold text-white">{{ editPlanId ? 'Edit Plan' : 'Add New Plan' }}</h2>
              <button (click)="showEditModal.set(false)" class="w-8 h-8 rounded-lg flex items-center justify-center text-surface-400 hover:text-white hover:bg-white/[0.05] transition">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="space-y-4">
              <div>
                <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Plan Name</label>
                <input type="text" [(ngModel)]="editForm.planName" class="input-field" />
              </div>
              @if (!editPlanId) {
                <div>
                  <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Operator</label>
                  <select [(ngModel)]="editForm.operatorId" class="input-field">
                    @for (op of operators(); track op.id) {
                      <option [value]="op.id" class="bg-surface-900 text-white">{{ op.name }}</option>
                    }
                  </select>
                </div>
              }
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Price (₹)</label>
                  <input type="number" [(ngModel)]="editForm.price" class="input-field" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Validity (Days)</label>
                  <input type="number" [(ngModel)]="editForm.validityDays" class="input-field" />
                </div>
              </div>
              <div>
                <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Category</label>
                <select [(ngModel)]="editForm.category" class="input-field">
                  <option value="RECOMMENDED" class="bg-surface-900 text-white">Recommended</option>
                  <option value="DATA" class="bg-surface-900 text-white">Data</option>
                  <option value="UNLIMITED" class="bg-surface-900 text-white">Unlimited</option>
                  <option value="TALKTIME" class="bg-surface-900 text-white">Talktime</option>
                </select>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Data Limit</label>
                  <input type="text" [(ngModel)]="editForm.dataLimit" class="input-field" placeholder="e.g. 2GB/day" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Call Benefit</label>
                  <input type="text" [(ngModel)]="editForm.callBenefit" class="input-field" placeholder="e.g. Unlimited" />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">SMS Benefit</label>
                  <input type="text" [(ngModel)]="editForm.smsBenefit" class="input-field" placeholder="e.g. 100 SMS/day" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-400 mb-1.5 block uppercase tracking-wider">Extra Benefits</label>
                  <input type="text" [(ngModel)]="editForm.additionalBenefits" class="input-field" placeholder="Optional" />
                </div>
              </div>
            </div>
            <div class="flex items-center justify-end gap-3 mt-6 pt-4 border-t border-white/[0.06]">
              <button (click)="showEditModal.set(false)" class="btn-secondary text-sm !py-2.5 !px-5">Cancel</button>
              <button (click)="savePlan()" [disabled]="actionLoading()" class="btn-primary text-sm !py-2.5 !px-5">
                {{ actionLoading() ? 'Saving...' : 'Save Changes' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- ═══════ TOAST ═══════ -->
      @if (toastVisible()) {
        <div class="fixed top-20 right-6 z-[100] animate-slide-in-right">
          <div class="flex items-center gap-3 px-5 py-3.5 rounded-xl border shadow-2xl backdrop-blur-xl"
               [class]="toastType() === 'success'
                 ? 'bg-accent-emerald/10 border-accent-emerald/30 text-accent-emerald'
                 : 'bg-accent-rose/10 border-accent-rose/30 text-accent-rose'">
            @if (toastType() === 'success') {
              <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            } @else {
              <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            }
            <span class="text-sm font-medium">{{ toastMessage() }}</span>
            <button (click)="toastVisible.set(false)" class="ml-2 opacity-60 hover:opacity-100">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
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
    .animate-slide-in-right { animation: slideInRight 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
    @keyframes scaleIn {
      from { transform: scale(0.95); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }
    .animate-scale-in { animation: scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
  `]
})
export class AdminPlansComponent implements OnInit {
  private adminService = inject(AdminService);

  operators = signal<AdminOperatorResponse[]>([]);
  allPlans = signal<(PlanResponse & { lastModifiedDate?: string; lastModifiedBy?: string; deactivatedByOperator?: boolean })[]>([]);
  loading = signal(true);
  actionLoading = signal(false);

  // Pagination State
  currentPage = signal(0);
  pageSize = signal(10);
  totalElements = signal(0);
  totalPages = signal(0);

  // Filters
  searchQuery = '';
  operatorFilter = signal<string>('ALL');
  operatorFilterValue = 'ALL';
  statusFilter = signal<string>('ALL');
  statusFilterValue = 'ALL';
  categoryFilter = signal<string>('ALL');
  categoryFilterValue = 'ALL';

  // Edit Modal
  showEditModal = signal(false);
  editForm: any = {};
  editPlanId: number | null = null;

  // Toast
  toastVisible = signal(false);
  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  private toastTimer: any;

  // KPIs Based on Current Page
  kpiActive = computed(() => this.allPlans().filter(p => p.isActive).length);
  kpiInactive = computed(() => this.allPlans().filter(p => !p.isActive).length);

  // Categories
  allCategories = computed(() => {
    const cats = new Set(this.allPlans().map(p => p.category));
    return Array.from(cats).sort();
  });

  // We now fetch paginated plans, so filteredPlans is just allPlans
  filteredPlans = computed(() => this.allPlans());

  ngOnInit() {
    this.loadAllPlans();
  }

  onFilterChange() {
    this.currentPage.set(0);
    this.loadAllPlans();
  }

  loadAllPlans() {
    this.loading.set(true);

    // Ensure operators list is loaded (for the edit modal and filter dropdown)
    if (this.operators().length === 0) {
      this.adminService.getAllOperators().subscribe(res => {
        if (res.success && res.data) {
          this.operators.set(res.data);
        }
      });
    }

    // Call paginated search endpoint
    const opFilter = this.operatorFilter() !== 'ALL' ? parseInt(this.operatorFilter(), 10) : undefined;
    const catFilter = this.categoryFilter() !== 'ALL' ? this.categoryFilter() : undefined;
    const statFilter = this.statusFilter() !== 'ALL' ? this.statusFilter() : undefined;

    this.adminService.searchAllPlans(
      this.currentPage(),
      this.pageSize(),
      opFilter,
      catFilter,
      statFilter,
      this.searchQuery
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.allPlans.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
        } else {
          this.allPlans.set([]);
          this.totalElements.set(0);
          this.totalPages.set(0);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.allPlans.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
      }
    });
  }

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update(p => p + 1);
      this.loadAllPlans();
    }
  }

  prevPage() {
    if (this.currentPage() > 0) {
      this.currentPage.update(p => p - 1);
      this.loadAllPlans();
    }
  }

  togglePlanStatus(plan: PlanResponse) {
    this.actionLoading.set(true);
    const action = plan.isActive
      ? this.adminService.deactivatePlan(plan.id)
      : this.adminService.activatePlan(plan.id);

    action.subscribe({
      next: (res) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.showToast(`Plan "${plan.planName}" ${plan.isActive ? 'deactivated' : 'activated'}`, 'success');
          this.loadAllPlans();
        } else {
          this.showToast(res.message || 'Failed to update plan.', 'error');
        }
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.showToast(err?.error?.message || 'Failed to update plan.', 'error');
      }
    });
  }

  openAddModal() {
    this.editPlanId = null;
    this.editForm = {
      planName: '',
      price: null,
      validityDays: null,
      category: 'RECOMMENDED',
      dataLimit: '',
      callBenefit: '',
      smsBenefit: '',
      additionalBenefits: '',
      operatorId: null
    };
    this.showEditModal.set(true);
  }

  openEditModal(plan: PlanResponse) {
    this.editPlanId = plan.id;
    this.editForm = {
      planName: plan.planName,
      price: plan.price,
      validityDays: plan.validityDays,
      category: plan.category,
      dataLimit: plan.dataLimit || '',
      callBenefit: plan.callBenefit || '',
      smsBenefit: plan.smsBenefit || '',
      additionalBenefits: plan.additionalBenefits || ''
    };
    this.showEditModal.set(true);
  }

  savePlan() {
    this.actionLoading.set(true);
    const { operatorId, ...planRequestDto } = this.editForm;

    if (this.editPlanId) {
      // Update
      this.adminService.updatePlan(this.editPlanId, planRequestDto as CreatePlanRequest).subscribe({
        next: (res) => {
          this.actionLoading.set(false);
          if (res.success) {
            this.showToast('Plan updated successfully!', 'success');
            this.showEditModal.set(false);
            this.loadAllPlans();
          } else {
            this.showToast(res.message || 'Update failed.', 'error');
          }
        },
        error: (err) => {
          this.actionLoading.set(false);
          this.showToast(err?.error?.message || 'Update failed.', 'error');
        }
      });
    } else {
      // Create
      if (!this.editForm.operatorId) {
        this.actionLoading.set(false);
        this.showToast('Please select an Operator', 'error');
        return;
      }
      this.adminService.createPlan(this.editForm.operatorId, planRequestDto as CreatePlanRequest).subscribe({
        next: (res) => {
          this.actionLoading.set(false);
          if (res.success) {
            this.showToast('Plan created successfully!', 'success');
            this.showEditModal.set(false);
            this.loadAllPlans();
          } else {
            this.showToast(res.message || 'Creation failed.', 'error');
          }
        },
        error: (err) => {
          this.actionLoading.set(false);
          this.showToast(err?.error?.message || 'Creation failed.', 'error');
        }
      });
    }
  }

  // === Helpers ===
  // === Helpers ===
  formatDate(dateInput: any): string {
    if (!dateInput) return '—';
    try {
      let d: Date;
      if (Array.isArray(dateInput)) {
        if (dateInput.length >= 6) {
          d = new Date(dateInput[0], dateInput[1] - 1, dateInput[2], dateInput[3], dateInput[4], dateInput[5]);
        } else if (dateInput.length >= 3) {
          d = new Date(dateInput[0], dateInput[1] - 1, dateInput[2]);
        } else {
          return '—';
        }
      } else {
        d = new Date(dateInput);
      }
      if (isNaN(d.getTime())) return '—';
      return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true });
    } catch { return '—'; }
  }

  getCategoryBadge(category: string): string {
    switch (category) {
      case 'RECOMMENDED': return 'bg-omni-500/15 text-omni-400 border border-omni-500/20';
      case 'DATA': return 'bg-sky-500/15 text-sky-400 border border-sky-500/20';
      case 'UNLIMITED': return 'bg-accent-emerald/15 text-accent-emerald border border-accent-emerald/20';
      case 'TALKTIME': return 'bg-violet-500/15 text-violet-400 border border-violet-500/20';
      default: return 'bg-white/[0.05] text-surface-300 border border-white/10';
    }
  }

  private showToast(message: string, type: 'success' | 'error') {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage.set(message);
    this.toastType.set(type);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), 4000);
  }
}
