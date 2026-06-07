import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminService, PlanResponse, AdminOperatorResponse, CreatePlanRequest } from '../../core/services/admin.service';

type StatusFilter = 'ALL' | 'ACTIVE' | 'MANUAL' | 'AUTO';

@Component({
  selector: 'app-admin-operator-plans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="animate-fade-in space-y-6">

      <!-- ═══════ BREADCRUMB ═══════ -->
      <nav class="flex items-center gap-2 text-sm">
        <a routerLink="/admin/operators" class="text-surface-500 hover:text-omni-400 transition-colors flex items-center gap-1.5 group">
          <svg class="w-4 h-4 opacity-50 group-hover:opacity-100 transition" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
          Operators
        </a>
        <svg class="w-3.5 h-3.5 text-surface-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        <span class="text-surface-900 font-semibold flex items-center gap-2">
          {{ operator()?.name || 'Loading...' }}
          @if (operator()) {
            <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider"
                  [class]="operator()?.isActive ? 'bg-accent-emerald/15 text-accent-emerald' : 'bg-surface-500/15 text-surface-500'">
              <span class="w-1.5 h-1.5 rounded-full" [class]="operator()?.isActive ? 'bg-accent-emerald animate-pulse' : 'bg-surface-500'"></span>
              {{ operator()?.isActive ? 'Active' : 'Inactive' }}
            </span>
          }
        </span>
      </nav>

      <!-- ═══════ PAGE HEADER ═══════ -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl sm:text-3xl font-display font-bold text-surface-900 tracking-tight">Plan Operations</h1>
          <p class="text-sm text-surface-500 mt-1">Manage recharge plans, pricing, and availability</p>
        </div>
        @if (operator()?.isActive) {
          <button (click)="showAddModal.set(true)"
                  class="btn-primary !py-2.5 !px-5 flex items-center gap-2 shrink-0 text-sm">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            Add Plan
          </button>
        }
      </div>

      <!-- ═══════ INACTIVE OPERATOR BANNER ═══════ -->
      @if (!operator()?.isActive && operator()) {
        <div class="rounded-xl p-4 border border-accent-amber/20 bg-accent-amber/[0.04] backdrop-blur-sm flex items-start gap-3">
          <div class="w-9 h-9 rounded-lg bg-accent-amber/15 flex items-center justify-center shrink-0 mt-0.5">
            <svg class="w-5 h-5 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
          </div>
          <div>
            <h3 class="text-sm font-semibold text-accent-amber">Operator Inactive — Plan modifications locked</h3>
            <p class="text-xs text-surface-500 mt-0.5">Reactivate the operator before managing plans. Auto-deactivated plans will auto-restore on reactivation.</p>
          </div>
        </div>
      }

      <!-- ═══════ KPI METRICS ═══════ -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Total Plans -->
        <div class="glass-card p-4 group hover:border-omni-500/20 transition-all duration-300">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-omni-500/10 flex items-center justify-center group-hover:bg-omni-500/15 transition">
              <svg class="w-4.5 h-4.5 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
            </div>
            <span class="text-[10px] font-bold text-surface-500 uppercase tracking-widest">Total</span>
          </div>
          <div class="text-2xl font-display font-bold text-surface-900">{{ plans().length }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">plans configured</div>
        </div>
        <!-- Active Plans -->
        <div class="glass-card p-4 group hover:border-accent-emerald/20 transition-all duration-300 cursor-pointer" (click)="toggleLegendFilter('ACTIVE')">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-accent-emerald/10 flex items-center justify-center group-hover:bg-accent-emerald/15 transition">
              <svg class="w-4.5 h-4.5 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            </div>
            <span class="text-[10px] font-bold uppercase tracking-widest" [class]="statusFilter() === 'ACTIVE' ? 'text-accent-emerald' : 'text-surface-500'">Active</span>
          </div>
          <div class="text-2xl font-display font-bold text-accent-emerald">{{ kpiActive() }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">visible to users</div>
        </div>
        <!-- Inactive Plans -->
        <div class="glass-card p-4 group hover:border-accent-rose/20 transition-all duration-300 cursor-pointer" (click)="toggleLegendFilter('MANUAL')">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-accent-rose/10 flex items-center justify-center group-hover:bg-accent-rose/15 transition">
              <svg class="w-4.5 h-4.5 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
            </div>
            <span class="text-[10px] font-bold uppercase tracking-widest" [class]="statusFilter() === 'MANUAL' ? 'text-accent-rose' : 'text-surface-500'">Manual</span>
          </div>
          <div class="text-2xl font-display font-bold text-accent-rose">{{ kpiManual() }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">manually deactivated</div>
        </div>
        <!-- Auto-deactivated -->
        <div class="glass-card p-4 group hover:border-accent-amber/20 transition-all duration-300 cursor-pointer" (click)="toggleLegendFilter('AUTO')">
          <div class="flex items-center justify-between mb-3">
            <div class="w-9 h-9 rounded-lg bg-accent-amber/10 flex items-center justify-center group-hover:bg-accent-amber/15 transition">
              <svg class="w-4.5 h-4.5 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            </div>
            <span class="text-[10px] font-bold uppercase tracking-widest" [class]="statusFilter() === 'AUTO' ? 'text-accent-amber' : 'text-surface-500'">Auto</span>
          </div>
          <div class="text-2xl font-display font-bold text-accent-amber">{{ kpiAuto() }}</div>
          <div class="text-[11px] text-surface-500 mt-0.5">by operator toggle</div>
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
                   placeholder="Search by plan name, data, or benefits..."
                   class="input-field !pl-10 !py-2.5 text-sm w-full" />
          </div>
          <!-- Category Toggles -->
          <div class="flex items-center gap-1.5 flex-wrap">
            <button (click)="categoryFilter.set('ALL')"
                    class="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200"
                    [class]="categoryFilter() === 'ALL'
                      ? 'bg-white/[0.1] text-surface-900 border border-surface-300'
                      : 'bg-white/[0.03] text-surface-500 border border-transparent hover:border-surface-200'">
              All
            </button>
            @for (cat of allCategories(); track cat) {
              <button (click)="categoryFilter.set(cat)"
                      class="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200"
                      [class]="categoryFilter() === cat
                        ? getCategoryActiveClass(cat)
                        : 'bg-white/[0.03] text-surface-500 border border-transparent hover:border-surface-200'">
                {{ cat }}
              </button>
            }
          </div>
          <!-- Status Dropdown -->
          <select [(ngModel)]="statusFilterValue"
                  (ngModelChange)="statusFilter.set($event)"
                  class="input-field !py-2.5 text-sm !w-auto min-w-[140px]">
            <option value="ALL" class="bg-white text-surface-900">All Status</option>
            <option value="ACTIVE" class="bg-white text-surface-900">Active</option>
            <option value="MANUAL" class="bg-white text-surface-900">Manually Off</option>
            <option value="AUTO" class="bg-white text-surface-900">Auto Off</option>
          </select>
        </div>
      </div>

      <!-- ═══════ BULK ACTIONS BAR ═══════ -->
      @if (selectedIds().size > 0 && operator()?.isActive) {
        <div class="glass-card p-3 border-omni-500/20 bg-omni-500/[0.03] flex items-center justify-between gap-4 animate-slide-up">
          <div class="flex items-center gap-3">
            <span class="text-sm text-surface-900 font-semibold">{{ selectedIds().size }} selected</span>
            <button (click)="clearSelection()" class="text-xs text-surface-500 hover:text-surface-900 transition underline">Clear</button>
          </div>
          <div class="flex items-center gap-2">
            <button (click)="bulkActivate()" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20 hover:bg-accent-emerald/20 transition flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4"/></svg>
              Activate All
            </button>
            <button (click)="bulkDeactivate()" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-accent-rose/10 text-accent-rose border border-accent-rose/20 hover:bg-accent-rose/20 transition flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              Deactivate All
            </button>
          </div>
        </div>
      }

      <!-- ═══════ DATA TABLE ═══════ -->
      <div class="glass-card overflow-hidden">
        @if (loading()) {
          <div class="p-6 space-y-3">
            @for (i of [1,2,3,4,5]; track i) {
              <div class="skeleton h-14 w-full rounded-xl"></div>
            }
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm" id="plans-table">
              <thead class="text-[11px] uppercase bg-white/[0.025] text-surface-500 border-b border-white/[0.05] tracking-wider">
                <tr>
                  <th class="pl-5 pr-2 py-3.5 w-10">
                    <input type="checkbox"
                           [checked]="allSelected()"
                           (change)="toggleSelectAll()"
                           class="w-4 h-4 rounded border-surface-300 bg-surface-50 text-omni-500 focus:ring-omni-500/30 cursor-pointer" />
                  </th>
                  <th class="px-4 py-3.5 font-semibold">Plan Name</th>
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
                  <tr class="hover:bg-white/[0.02] transition-colors group"
                      [ngClass]="{'selected-row': selectedIds().has(plan.id)}">
                    <td class="pl-5 pr-2 py-3.5">
                      <input type="checkbox"
                             [checked]="selectedIds().has(plan.id)"
                             (change)="toggleSelect(plan.id)"
                             class="w-4 h-4 rounded border-surface-300 bg-surface-50 text-omni-500 focus:ring-omni-500/30 cursor-pointer" />
                    </td>
                    <td class="px-4 py-3.5">
                      <div class="font-medium text-surface-900 text-[13px]">{{ plan.planName }}</div>
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
                      <span class="text-surface-900 font-bold text-base">₹{{ plan.price }}</span>
                    </td>
                    <td class="px-4 py-3.5">
                      <span class="text-surface-600 font-medium">{{ plan.validityDays }}d</span>
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
                      } @else if (plan.deactivatedByOperator) {
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold bg-accent-amber/10 text-accent-amber border border-accent-amber/15">
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                          Auto-off
                        </span>
                      } @else {
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold bg-accent-rose/10 text-accent-rose border border-accent-rose/15">
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
                          Manual-off
                        </span>
                      }
                    </td>
                    <td class="px-4 py-3.5">
                      <div class="text-[11px] text-surface-500">{{ plan.lastModifiedDate ? formatDate(plan.lastModifiedDate) : '—' }}</div>
                      <div class="text-[10px] text-surface-600">{{ plan.lastModifiedBy || 'system' }}</div>
                    </td>
                    <td class="px-4 py-3.5 text-right">
                      <div class="flex items-center justify-end gap-2">
                        <!-- Toggle Status Button -->
                        @if (operator()?.isActive) {
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
                        } @else {
                          <span class="text-[10px] text-surface-600 italic">locked</span>
                        }
                        <!-- Three-dot menu -->
                        <div class="relative">
                          <button (click)="toggleMenu(plan.id)"
                                  class="w-8 h-8 rounded-lg flex items-center justify-center text-surface-500 hover:text-surface-900 hover:bg-white/[0.06] transition opacity-0 group-hover:opacity-100">
                            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><circle cx="10" cy="4" r="1.5"/><circle cx="10" cy="10" r="1.5"/><circle cx="10" cy="16" r="1.5"/></svg>
                          </button>
                          @if (openMenuId() === plan.id) {
                            <div class="absolute right-0 top-10 z-30 w-40 rounded-xl border border-surface-200 bg-white/95 backdrop-blur-xl shadow-2xl py-1.5 animate-scale-in">
                              <button (click)="openEditModal(plan)" class="w-full px-4 py-2 text-left text-xs text-surface-600 hover:text-surface-900 hover:bg-white/[0.05] transition flex items-center gap-2">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                                Edit Plan
                              </button>
                              <button (click)="deletePlan(plan)" class="w-full px-4 py-2 text-left text-xs text-accent-rose hover:bg-accent-rose/5 transition flex items-center gap-2">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                                Delete Plan
                              </button>
                            </div>
                          }
                        </div>
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
                <p class="text-surface-500 font-medium">No plans match your filters</p>
                <p class="text-xs text-surface-600 mt-1">Try adjusting search or category filters</p>
              </div>
            }
          </div>
        }
      </div>

      <!-- ═══════ INTERACTIVE LEGEND ═══════ -->
      <div class="glass-card p-4">
        <div class="flex items-center gap-2 mb-3">
          <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          <span class="text-xs font-semibold text-surface-600 uppercase tracking-wider">Status Legend</span>
          <span class="text-[10px] text-surface-600 ml-1">— click to filter</span>
        </div>
        <div class="flex flex-wrap gap-3">
          <button (click)="toggleLegendFilter('ACTIVE')"
                  class="flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-medium transition-all duration-200"
                  [class]="statusFilter() === 'ACTIVE'
                    ? 'bg-accent-emerald/10 border-accent-emerald/30 text-accent-emerald'
                    : 'bg-white/[0.02] border-white/[0.06] text-surface-500 hover:border-accent-emerald/20'">
            <span class="w-2 h-2 rounded-full bg-accent-emerald"></span>
            Active — visible to users
          </button>
          <button (click)="toggleLegendFilter('MANUAL')"
                  class="flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-medium transition-all duration-200"
                  [class]="statusFilter() === 'MANUAL'
                    ? 'bg-accent-rose/10 border-accent-rose/30 text-accent-rose'
                    : 'bg-white/[0.02] border-white/[0.06] text-surface-500 hover:border-accent-rose/20'">
            <span class="w-2 h-2 rounded-full bg-accent-rose"></span>
            Manual — admin deactivated
          </button>
          <button (click)="toggleLegendFilter('AUTO')"
                  class="flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-medium transition-all duration-200"
                  [class]="statusFilter() === 'AUTO'
                    ? 'bg-accent-amber/10 border-accent-amber/30 text-accent-amber'
                    : 'bg-white/[0.02] border-white/[0.06] text-surface-500 hover:border-accent-amber/20'">
            <span class="w-2 h-2 rounded-full bg-accent-amber"></span>
            Auto — operator toggle
          </button>
        </div>
      </div>

      <!-- ═══════ EDIT PLAN MODAL ═══════ -->
      @if (showEditModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4" (click)="showEditModal.set(false)">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="relative glass-card p-6 sm:p-8 w-full max-w-lg border-omni-500/10 animate-scale-in" (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-display font-bold text-surface-900">Edit Plan</h2>
              <button (click)="showEditModal.set(false)" class="w-8 h-8 rounded-lg flex items-center justify-center text-surface-500 hover:text-surface-900 hover:bg-white/[0.05] transition">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="space-y-4">
              <div>
                <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Plan Name</label>
                <input type="text" [(ngModel)]="editForm.planName" class="input-field" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Price (₹)</label>
                  <input type="number" [(ngModel)]="editForm.price" class="input-field" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Validity (Days)</label>
                  <input type="number" [(ngModel)]="editForm.validityDays" class="input-field" />
                </div>
              </div>
              <div>
                <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Category</label>
                <select [(ngModel)]="editForm.category" class="input-field">
                  <option value="RECOMMENDED" class="bg-white text-surface-900">Recommended</option>
                  <option value="DATA" class="bg-white text-surface-900">Data</option>
                  <option value="UNLIMITED" class="bg-white text-surface-900">Unlimited</option>
                  <option value="TALKTIME" class="bg-white text-surface-900">Talktime</option>
                </select>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Data Limit</label>
                  <input type="text" [(ngModel)]="editForm.dataLimit" class="input-field" placeholder="e.g. 2GB/day" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Call Benefit</label>
                  <input type="text" [(ngModel)]="editForm.callBenefit" class="input-field" placeholder="e.g. Unlimited" />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">SMS Benefit</label>
                  <input type="text" [(ngModel)]="editForm.smsBenefit" class="input-field" placeholder="e.g. 100 SMS/day" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Extra Benefits</label>
                  <input type="text" [(ngModel)]="editForm.additionalBenefits" class="input-field" placeholder="Optional" />
                </div>
              </div>
            </div>
            <div class="flex items-center justify-end gap-3 mt-6 pt-4 border-t border-white/[0.06]">
              <button (click)="showEditModal.set(false)" class="btn-secondary text-sm !py-2.5 !px-5">Cancel</button>
              <button (click)="saveEditPlan()" [disabled]="actionLoading()" class="btn-primary text-sm !py-2.5 !px-5">
                {{ actionLoading() ? 'Saving...' : 'Save Changes' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- ═══════ ADD PLAN MODAL ═══════ -->
      @if (showAddModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4" (click)="showAddModal.set(false)">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="relative glass-card p-6 sm:p-8 w-full max-w-lg border-omni-500/10 animate-scale-in" (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-display font-bold text-surface-900">Add New Plan</h2>
              <button (click)="showAddModal.set(false)" class="w-8 h-8 rounded-lg flex items-center justify-center text-surface-500 hover:text-surface-900 hover:bg-white/[0.05] transition">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="space-y-4">
              <div>
                <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Plan Name *</label>
                <input type="text" [(ngModel)]="addForm.planName" class="input-field" placeholder="e.g. Ultra Data Pack" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Price (₹) *</label>
                  <input type="number" [(ngModel)]="addForm.price" class="input-field" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Validity (Days) *</label>
                  <input type="number" [(ngModel)]="addForm.validityDays" class="input-field" />
                </div>
              </div>
              <div>
                <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Category *</label>
                <select [(ngModel)]="addForm.category" class="input-field">
                  <option value="" class="bg-white text-surface-900">Select category</option>
                  <option value="RECOMMENDED" class="bg-white text-surface-900">Recommended</option>
                  <option value="DATA" class="bg-white text-surface-900">Data</option>
                  <option value="UNLIMITED" class="bg-white text-surface-900">Unlimited</option>
                  <option value="TALKTIME" class="bg-white text-surface-900">Talktime</option>
                </select>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Data Limit</label>
                  <input type="text" [(ngModel)]="addForm.dataLimit" class="input-field" placeholder="e.g. 2GB/day" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Call Benefit</label>
                  <input type="text" [(ngModel)]="addForm.callBenefit" class="input-field" placeholder="e.g. Unlimited" />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">SMS Benefit</label>
                  <input type="text" [(ngModel)]="addForm.smsBenefit" class="input-field" placeholder="e.g. 100 SMS/day" />
                </div>
                <div>
                  <label class="text-xs font-semibold text-surface-500 mb-1.5 block uppercase tracking-wider">Extra Benefits</label>
                  <input type="text" [(ngModel)]="addForm.additionalBenefits" class="input-field" placeholder="Optional" />
                </div>
              </div>
            </div>
            <div class="flex items-center justify-end gap-3 mt-6 pt-4 border-t border-white/[0.06]">
              <button (click)="showAddModal.set(false)" class="btn-secondary text-sm !py-2.5 !px-5">Cancel</button>
              <button (click)="saveNewPlan()" [disabled]="actionLoading() || !addForm.planName || !addForm.price || !addForm.category" class="btn-primary text-sm !py-2.5 !px-5">
                {{ actionLoading() ? 'Creating...' : 'Create Plan' }}
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
    @keyframes slideUp {
      from { transform: translateY(8px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
    .animate-slide-up { animation: slideUp 0.25s ease-out forwards; }
    @keyframes scaleIn {
      from { transform: scale(0.95); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }
    .animate-scale-in { animation: scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards; }

    /* Checkbox styling */
    input[type="checkbox"] {
      appearance: none;
      -webkit-appearance: none;
      width: 16px; height: 16px;
      border-radius: 4px;
      border: 1.5px solid rgba(255,255,255,0.15);
      background: rgba(255,255,255,0.04);
      cursor: pointer;
      position: relative;
      transition: all 0.15s ease;
    }
    input[type="checkbox"]:checked {
      background: var(--color-omni-500, #8b5cf6);
      border-color: var(--color-omni-500, #8b5cf6);
    }
    input[type="checkbox"]:checked::after {
      content: '';
      position: absolute;
      left: 4px; top: 1px;
      width: 5px; height: 9px;
      border: solid white;
      border-width: 0 2px 2px 0;
      transform: rotate(45deg);
    }
    .selected-row { background: rgba(139, 92, 246, 0.03); }
  `]
})
export class AdminOperatorPlansComponent implements OnInit {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  operatorId = signal<number | null>(null);
  operator = signal<AdminOperatorResponse | null>(null);
  plans = signal<(PlanResponse & { lastModifiedDate?: string; lastModifiedBy?: string })[]>([]);
  loading = signal(true);
  actionLoading = signal(false);

  // Filters
  searchQuery = '';
  categoryFilter = signal<string>('ALL');
  statusFilter = signal<StatusFilter>('ALL');
  statusFilterValue: StatusFilter = 'ALL';

  // Multi-select
  selectedIds = signal<Set<number>>(new Set());

  // Menus / Modals
  openMenuId = signal<number | null>(null);
  showEditModal = signal(false);
  showAddModal = signal(false);

  // Edit form
  editForm: any = {};
  editPlanId: number | null = null;

  // Add form
  addForm: any = {
    planName: '', price: 0, validityDays: 28, category: '',
    dataLimit: '', callBenefit: '', smsBenefit: '', additionalBenefits: ''
  };

  // Toast
  toastVisible = signal(false);
  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  private toastTimer: any;

  // KPIs
  kpiActive = computed(() => this.plans().filter(p => p.isActive).length);
  kpiManual = computed(() => this.plans().filter(p => !p.isActive && !p.deactivatedByOperator).length);
  kpiAuto = computed(() => this.plans().filter(p => !p.isActive && p.deactivatedByOperator).length);

  // Categories
  allCategories = computed(() => {
    const cats = new Set(this.plans().map(p => p.category));
    return Array.from(cats).sort();
  });

  // Filtered plans
  filteredPlans = computed(() => {
    let result = this.plans();

    // Status filter
    const sf = this.statusFilter();
    if (sf === 'ACTIVE') result = result.filter(p => p.isActive);
    else if (sf === 'MANUAL') result = result.filter(p => !p.isActive && !p.deactivatedByOperator);
    else if (sf === 'AUTO') result = result.filter(p => !p.isActive && p.deactivatedByOperator);

    // Category filter
    const cf = this.categoryFilter();
    if (cf !== 'ALL') result = result.filter(p => p.category === cf);

    // Search
    const q = this.searchQuery?.toLowerCase().trim();
    if (q) {
      result = result.filter(p =>
        p.planName.toLowerCase().includes(q) ||
        (p.dataLimit && p.dataLimit.toLowerCase().includes(q)) ||
        (p.callBenefit && p.callBenefit.toLowerCase().includes(q)) ||
        (p.additionalBenefits && p.additionalBenefits.toLowerCase().includes(q))
      );
    }

    return result;
  });

  allSelected = computed(() => {
    const fp = this.filteredPlans();
    return fp.length > 0 && fp.every(p => this.selectedIds().has(p.id));
  });

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.operatorId.set(+id);
      this.loadOperatorAndPlans();
    }
    // Close menu on outside click
    document.addEventListener('click', () => this.openMenuId.set(null));
  }

  loadOperatorAndPlans() {
    const id = this.operatorId();
    if (!id) return;
    this.loading.set(true);

    this.adminService.getAllOperators().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const op = res.data.find(o => o.id === id);
          if (op) this.operator.set(op);
        }
      }
    });

    this.adminService.getOperatorPlans(id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.plans.set(res.data);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // === Filtering ===
  toggleLegendFilter(filter: StatusFilter) {
    if (this.statusFilter() === filter) {
      this.statusFilter.set('ALL');
      this.statusFilterValue = 'ALL';
    } else {
      this.statusFilter.set(filter);
      this.statusFilterValue = filter;
    }
  }

  // === Selection ===
  toggleSelect(id: number) {
    const next = new Set(this.selectedIds());
    if (next.has(id)) next.delete(id);
    else next.add(id);
    this.selectedIds.set(next);
  }

  toggleSelectAll() {
    if (this.allSelected()) {
      this.selectedIds.set(new Set());
    } else {
      const ids = new Set(this.filteredPlans().map(p => p.id));
      this.selectedIds.set(ids);
    }
  }

  clearSelection() {
    this.selectedIds.set(new Set());
  }

  // === Actions ===
  togglePlanStatus(plan: PlanResponse) {
    if (!this.operator()?.isActive) {
      this.showToast('Cannot modify plans while operator is inactive', 'error');
      return;
    }
    this.actionLoading.set(true);
    const action = plan.isActive
      ? this.adminService.deactivatePlan(plan.id)
      : this.adminService.activatePlan(plan.id);

    action.subscribe({
      next: (res) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.showToast(`Plan "${plan.planName}" ${plan.isActive ? 'deactivated' : 'activated'}`, 'success');
          this.loadOperatorAndPlans();
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

  bulkActivate() {
    const ids = Array.from(this.selectedIds());
    let completed = 0;
    ids.forEach(id => {
      this.adminService.activatePlan(id).subscribe({
        next: () => {
          completed++;
          if (completed === ids.length) {
            this.showToast(`${ids.length} plans activated`, 'success');
            this.clearSelection();
            this.loadOperatorAndPlans();
          }
        },
        error: () => { completed++; }
      });
    });
  }

  bulkDeactivate() {
    const ids = Array.from(this.selectedIds());
    let completed = 0;
    ids.forEach(id => {
      this.adminService.deactivatePlan(id).subscribe({
        next: () => {
          completed++;
          if (completed === ids.length) {
            this.showToast(`${ids.length} plans deactivated`, 'success');
            this.clearSelection();
            this.loadOperatorAndPlans();
          }
        },
        error: () => { completed++; }
      });
    });
  }

  deletePlan(plan: PlanResponse) {
    this.openMenuId.set(null);
    if (!confirm(`Delete "${plan.planName}"? This action cannot be undone.`)) return;

    this.adminService.deletePlan(plan.id).subscribe({
      next: (res) => {
        if (res.success) {
          this.showToast(`Plan "${plan.planName}" deleted`, 'success');
          this.loadOperatorAndPlans();
        }
      },
      error: (err) => this.showToast(err?.error?.message || 'Failed to delete plan.', 'error')
    });
  }

  // === Menus ===
  toggleMenu(planId: number) {
    event?.stopPropagation();
    this.openMenuId.set(this.openMenuId() === planId ? null : planId);
  }

  // === Edit Modal ===
  openEditModal(plan: PlanResponse) {
    this.openMenuId.set(null);
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

  saveEditPlan() {
    if (!this.editPlanId) return;
    this.actionLoading.set(true);

    this.adminService.updatePlan(this.editPlanId, this.editForm as CreatePlanRequest).subscribe({
      next: (res) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.showToast('Plan updated successfully!', 'success');
          this.showEditModal.set(false);
          this.loadOperatorAndPlans();
        } else {
          this.showToast(res.message || 'Update failed.', 'error');
        }
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.showToast(err?.error?.message || 'Update failed.', 'error');
      }
    });
  }

  // === Add Plan ===
  saveNewPlan() {
    const id = this.operatorId();
    if (!id) return;
    this.actionLoading.set(true);

    this.adminService.createPlan(id, this.addForm as CreatePlanRequest).subscribe({
      next: (res) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.showToast('Plan created successfully!', 'success');
          this.showAddModal.set(false);
          this.addForm = { planName: '', price: 0, validityDays: 28, category: '', dataLimit: '', callBenefit: '', smsBenefit: '', additionalBenefits: '' };
          this.loadOperatorAndPlans();
        }
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.showToast(err?.error?.message || 'Failed to create plan.', 'error');
      }
    });
  }

  // === Helpers ===
  formatDate(dateStr: string): string {
    try {
      const d = new Date(dateStr);
      return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true });
    } catch { return dateStr; }
  }

  getCategoryBadge(category: string): string {
    switch (category) {
      case 'RECOMMENDED': return 'bg-omni-500/15 text-omni-400 border border-omni-500/20';
      case 'DATA': return 'bg-sky-500/15 text-sky-400 border border-sky-500/20';
      case 'UNLIMITED': return 'bg-accent-emerald/15 text-accent-emerald border border-accent-emerald/20';
      case 'TALKTIME': return 'bg-violet-500/15 text-violet-400 border border-violet-500/20';
      default: return 'bg-white/[0.05] text-surface-600 border border-surface-200';
    }
  }

  getCategoryActiveClass(cat: string): string {
    switch (cat) {
      case 'RECOMMENDED': return 'bg-omni-500/15 text-omni-400 border border-omni-500/30';
      case 'DATA': return 'bg-sky-500/15 text-sky-400 border border-sky-500/30';
      case 'UNLIMITED': return 'bg-accent-emerald/15 text-accent-emerald border border-accent-emerald/30';
      case 'TALKTIME': return 'bg-violet-500/15 text-violet-400 border border-violet-500/30';
      default: return 'bg-white/[0.1] text-surface-900 border border-surface-300';
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
