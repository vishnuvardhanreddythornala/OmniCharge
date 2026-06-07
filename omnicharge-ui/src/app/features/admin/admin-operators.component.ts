import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminService, AdminOperatorResponse, CreateOperatorRequest, CreatePlanRequest } from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-operators',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="animate-fade-in">

      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-display font-bold text-surface-900">Operators</h1>
          <p class="text-sm text-surface-500 mt-1">Manage telecom operators &amp; their plans</p>
        </div>
        <button id="create-operator-btn" (click)="showCreateOperator.set(true)"
                class="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold bg-gradient-to-r from-omni-600 to-omni-500 text-surface-900 hover:from-omni-500 hover:to-omni-400 transition-all duration-300 shadow-lg shadow-omni-500/20 hover:shadow-omni-500/30 hover:-translate-y-0.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
          Create Operator
        </button>
      </div>

      <!-- Stat Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6 animate-slide-up">
        <div class="glass-card p-5 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-omni-500/10 flex items-center justify-center border border-omni-500/20">
              <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
            </div>
            <p class="text-[10px] text-surface-500 font-bold uppercase tracking-wider">Total Operators</p>
          </div>
          <span class="text-3xl font-display font-bold text-surface-900">{{ operators().length }}</span>
        </div>
        <div class="glass-card p-5 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-accent-emerald/10 flex items-center justify-center border border-accent-emerald/20">
              <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
            </div>
            <p class="text-[10px] text-surface-500 font-bold uppercase tracking-wider">Active</p>
          </div>
          <span class="text-3xl font-display font-bold text-surface-900">{{ activeOperatorCount() }}</span>
        </div>
        <div class="glass-card p-5 flex flex-col justify-between border border-white/[0.05]">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-lg bg-surface-100/50 flex items-center justify-center border border-white/[0.08]">
              <svg class="w-4 h-4 text-surface-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
            </div>
            <p class="text-[10px] text-surface-500 font-bold uppercase tracking-wider">Inactive</p>
          </div>
          <span class="text-3xl font-display font-bold text-surface-900">{{ inactiveOperatorCount() }}</span>
        </div>
      </div>

      <!-- Toast -->
      @if (toastVisible()) {
        <div class="fixed top-20 right-6 z-[100] animate-slide-in-right">
          <div class="flex items-center gap-3 px-5 py-3.5 rounded-xl border shadow-2xl backdrop-blur-xl"
               [class]="toastType() === 'success'
                 ? 'bg-accent-emerald/10 border-accent-emerald/30 text-accent-emerald'
                 : 'bg-accent-rose/10 border-accent-rose/30 text-accent-rose'">
            @if (toastType() === 'success') {
              <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            } @else {
              <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            }
            <span class="text-sm font-medium">{{ toastMessage() }}</span>
            <button (click)="toastVisible.set(false)" class="ml-2 opacity-60 hover:opacity-100"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg></button>
          </div>
        </div>
      }

      <!-- Operators Table -->
      <div class="glass-card overflow-hidden">
        @if (loading()) {
          <div class="p-8 space-y-4">
            @for (i of [1,2,3,4,5]; track i) {
              <div class="skeleton h-14 w-full rounded-xl"></div>
            }
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm text-surface-600">
              <thead class="text-xs uppercase bg-white/[0.03] text-surface-500 border-b border-white/[0.05]">
                <tr>
                  <th scope="col" class="px-6 py-4 font-semibold">Operator</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Code</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Category</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Plans</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Status</th>
                  <th scope="col" class="px-6 py-4 font-semibold">Last Updated</th>
                  <th scope="col" class="px-6 py-4 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-white/[0.05]">
                @for (op of operators(); track op.id) {
                  <tr class="hover:bg-white/[0.02] transition-colors">
                    <td class="px-6 py-4">
                      <div class="flex items-center gap-3">
                        <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-omni-600/30 to-accent-teal/30 border border-surface-200 flex items-center justify-center text-surface-900 text-xs font-bold uppercase">
                          {{ op.code ? op.code.substring(0, 2) : 'OP' }}
                        </div>
                        <div class="font-medium text-surface-900">{{ op.name || 'Unnamed Operator' }}</div>
                      </div>
                    </td>
                    <td class="px-6 py-4">
                      <span class="px-2 py-1 rounded bg-white/[0.05] text-xs font-mono border border-surface-200 text-surface-600">{{ op.code || 'N/A' }}</span>
                    </td>
                    <td class="px-6 py-4">
                      @if (op.category) {
                        <span class="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider"
                              [class]="getCategoryClass(op.category)">
                          {{ op.category }}
                        </span>
                      }
                    </td>
                    <td class="px-6 py-4">
                      <span class="text-sm font-semibold text-surface-900">{{ op.planCount || 0 }}</span>
                      <span class="text-surface-500 text-xs ml-1">plans</span>
                    </td>
                    <td class="px-6 py-4">
                      <span class="flex items-center gap-1.5 text-xs font-medium"
                            [class]="op.isActive ? 'text-accent-emerald' : 'text-surface-500'">
                        <span class="w-1.5 h-1.5 rounded-full" [class]="op.isActive ? 'bg-accent-emerald' : 'bg-surface-500'"></span>
                        {{ op.isActive ? 'Active' : 'Inactive' }}
                      </span>
                    </td>
                    <td class="px-6 py-4">
                      <div class="flex flex-col">
                        @if (op.lastModifiedDate) {
                          <span class="text-xs text-surface-800 font-medium">{{ formatDate(op.lastModifiedDate) }}</span>
                        } @else {
                          <span class="text-xs text-surface-500 font-mono tracking-wider">—</span>
                        }
                      </div>
                    </td>
                    <td class="px-6 py-4 text-right">
                      <div class="flex items-center justify-end gap-2">
                        <button (click)="toggleOperatorStatus(op)"
                                class="inline-flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border transition-colors font-medium"
                                [class]="op.isActive 
                                  ? 'border-accent-rose/30 text-accent-rose hover:bg-accent-rose/10' 
                                  : 'border-accent-emerald/30 text-accent-emerald hover:bg-accent-emerald/10'">
                          @if (op.isActive) {
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
                            Deactivate
                          } @else {
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                            Activate
                          }
                        </button>
                        <button (click)="openEditOperator(op)"
                                class="inline-flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border border-omni-500/30 text-omni-400 hover:bg-omni-500/10 transition-colors font-medium">
                          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                          Edit
                        </button>
                        <button (click)="confirmDeleteOperator(op)"
                                class="inline-flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border border-accent-rose/30 text-accent-rose hover:bg-accent-rose/10 transition-colors font-medium">
                          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            @if (operators().length === 0) {
              <div class="p-12 text-center">
                <svg class="w-12 h-12 text-surface-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
                <p class="text-surface-500 font-medium">No operators found.</p>
                <p class="text-surface-500 text-sm mt-1">Create your first operator to get started.</p>
              </div>
            }
          </div>
        }
      </div>

      <!-- ═══ Create Operator Modal ═══ -->
      @if (showCreateOperator()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4" (click)="showCreateOperator.set(false)">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="relative w-full max-w-lg glass-card p-0 border border-white/[0.08] shadow-2xl animate-scale-in" (click)="$event.stopPropagation()">
            <!-- Modal Header -->
            <div class="flex items-center justify-between px-6 py-5 border-b border-white/[0.05]">
              <h2 class="text-lg font-display font-bold text-surface-900">Create New Operator</h2>
              <button (click)="showCreateOperator.set(false)" class="text-surface-500 hover:text-surface-900 transition-colors">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <!-- Modal Body -->
            <div class="p-6 space-y-5">
              <div>
                <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Operator Name *</label>
                <input type="text" [(ngModel)]="newOperator.name" placeholder="e.g. Jio"
                       class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Code *</label>
                  <input type="text" [(ngModel)]="newOperator.code" placeholder="e.g. JIO"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Category *</label>
                  <select [(ngModel)]="newOperator.category"
                          class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all appearance-none cursor-pointer">
                    <option value="" disabled class="bg-omni-950">Select Category</option>
                    @for (cat of operatorCategories; track cat) {
                      <option [value]="cat" class="bg-omni-950">{{ cat }}</option>
                    }
                  </select>
                </div>
              </div>
              <div>
                <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Logo URL (optional)</label>
                <input type="text" [(ngModel)]="newOperator.logoUrl" placeholder="https://..."
                       class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
              </div>
            </div>
            <!-- Modal Footer -->
            <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/[0.05]">
              <button (click)="showCreateOperator.set(false)" class="px-4 py-2.5 rounded-xl text-sm font-medium text-surface-500 hover:text-surface-900 hover:bg-white/[0.04] transition-all">Cancel</button>
              <button (click)="createOperator()" [disabled]="creatingOperator()"
                      class="px-5 py-2.5 rounded-xl text-sm font-semibold bg-gradient-to-r from-omni-600 to-omni-500 text-surface-900 hover:from-omni-500 hover:to-omni-400 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
                {{ creatingOperator() ? 'Creating...' : 'Create Operator' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- ═══ Edit Operator Modal ═══ -->
      @if (showEditOperator()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4" (click)="showEditOperator.set(false)">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="relative w-full max-w-lg glass-card p-0 border border-white/[0.08] shadow-2xl animate-scale-in" (click)="$event.stopPropagation()">
            <!-- Modal Header -->
            <div class="flex items-center justify-between px-6 py-5 border-b border-white/[0.05]">
              <h2 class="text-lg font-display font-bold text-surface-900">Edit Operator</h2>
              <button (click)="showEditOperator.set(false)" class="text-surface-500 hover:text-surface-900 transition-colors">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <!-- Modal Body -->
            <div class="p-6 space-y-5">
              <div>
                <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Operator Name *</label>
                <input type="text" [(ngModel)]="editOperator.name" placeholder="e.g. Jio"
                       class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Code *</label>
                  <input type="text" [(ngModel)]="editOperator.code" placeholder="e.g. JIO"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Category *</label>
                  <select [(ngModel)]="editOperator.category"
                          class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all appearance-none cursor-pointer">
                    <option value="" disabled class="bg-omni-950">Select Category</option>
                    @for (cat of operatorCategories; track cat) {
                      <option [value]="cat" class="bg-omni-950">{{ cat }}</option>
                    }
                  </select>
                </div>
              </div>
              <div>
                <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Logo URL (optional)</label>
                <input type="text" [(ngModel)]="editOperator.logoUrl" placeholder="https://..."
                       class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
              </div>
            </div>
            <!-- Modal Footer -->
            <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/[0.05]">
              <button (click)="showEditOperator.set(false)" class="px-4 py-2.5 rounded-xl text-sm font-medium text-surface-500 hover:text-surface-900 hover:bg-white/[0.04] transition-all">Cancel</button>
              <button (click)="updateOperator()" [disabled]="updatingOperator()"
                      class="px-5 py-2.5 rounded-xl text-sm font-semibold bg-gradient-to-r from-omni-600 to-omni-500 text-surface-900 hover:from-omni-500 hover:to-omni-400 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
                {{ updatingOperator() ? 'Updating...' : 'Update Operator' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- ═══ Delete Confirmation Modal ═══ -->
      @if (showDeleteConfirm()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4" (click)="showDeleteConfirm.set(false)">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="relative w-full max-w-md glass-card p-0 border border-accent-rose/20 shadow-2xl animate-scale-in" (click)="$event.stopPropagation()">
            <!-- Modal Header -->
            <div class="flex items-center gap-3 px-6 py-5 border-b border-white/[0.05]">
              <div class="w-10 h-10 rounded-xl bg-accent-rose/10 border border-accent-rose/20 flex items-center justify-center">
                <svg class="w-5 h-5 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
              </div>
              <div>
                <h2 class="text-lg font-display font-bold text-surface-900">Delete Operator</h2>
                <p class="text-xs text-surface-500 mt-0.5">This action cannot be undone</p>
              </div>
            </div>
            <!-- Modal Body -->
            <div class="p-6">
              <p class="text-sm text-surface-600">
                Are you sure you want to delete <span class="font-semibold text-surface-900">{{ operatorToDelete()?.name }}</span>? 
                This will also remove all associated plans.
              </p>
            </div>
            <!-- Modal Footer -->
            <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/[0.05]">
              <button (click)="showDeleteConfirm.set(false)" class="px-4 py-2.5 rounded-xl text-sm font-medium text-surface-500 hover:text-surface-900 hover:bg-white/[0.04] transition-all">Cancel</button>
              <button (click)="deleteOperator()" [disabled]="deletingOperator()"
                      class="px-5 py-2.5 rounded-xl text-sm font-semibold bg-accent-rose text-surface-900 hover:bg-accent-rose/90 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
                {{ deletingOperator() ? 'Deleting...' : 'Delete Operator' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- ═══ Add Plan Modal ═══ -->
      @if (showAddPlan()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4" (click)="showAddPlan.set(false)">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="relative w-full max-w-2xl glass-card p-0 border border-white/[0.08] shadow-2xl animate-scale-in max-h-[90vh] overflow-y-auto" (click)="$event.stopPropagation()">
            <!-- Modal Header -->
            <div class="flex items-center justify-between px-6 py-5 border-b border-white/[0.05] sticky top-0 bg-omni-950/95 backdrop-blur-xl z-10">
              <div>
                <h2 class="text-lg font-display font-bold text-surface-900">Add Plan</h2>
                <p class="text-xs text-surface-500 mt-0.5">for <span class="text-omni-400 font-semibold">{{ selectedOperator()?.name }}</span></p>
              </div>
              <button (click)="showAddPlan.set(false)" class="text-surface-500 hover:text-surface-900 transition-colors">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <!-- Modal Body -->
            <div class="p-6 space-y-5">
              <div class="grid grid-cols-2 gap-4">
                <div class="col-span-2">
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Plan Name *</label>
                  <input type="text" [(ngModel)]="newPlan.planName" placeholder="e.g. Unlimited Data Pack"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Price (₹) *</label>
                  <input type="number" [(ngModel)]="newPlan.price" placeholder="299"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Validity (Days) *</label>
                  <input type="number" [(ngModel)]="newPlan.validityDays" placeholder="28"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Data Limit</label>
                  <input type="text" [(ngModel)]="newPlan.dataLimit" placeholder="e.g. 2GB/day"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Call Benefit</label>
                  <input type="text" [(ngModel)]="newPlan.callBenefit" placeholder="e.g. Unlimited"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">SMS Benefit</label>
                  <input type="text" [(ngModel)]="newPlan.smsBenefit" placeholder="e.g. 100 SMS/day"
                         class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all"/>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Category *</label>
                  <select [(ngModel)]="newPlan.category"
                          class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all appearance-none cursor-pointer">
                    <option value="" disabled class="bg-omni-950">Select Category</option>
                    @for (cat of planCategories; track cat) {
                      <option [value]="cat" class="bg-omni-950">{{ cat }}</option>
                    }
                  </select>
                </div>
                <div class="col-span-2">
                  <label class="block text-xs font-semibold text-surface-500 uppercase tracking-wider mb-2">Additional Benefits</label>
                  <textarea [(ngModel)]="newPlan.additionalBenefits" placeholder="e.g. Free access to JioTV, JioCinema..."
                            rows="3"
                            class="w-full px-4 py-3 rounded-xl bg-white/[0.04] border border-surface-200 text-surface-900 placeholder-surface-400 text-sm focus:outline-none focus:border-omni-500/50 focus:ring-1 focus:ring-omni-500/30 transition-all resize-none"></textarea>
                </div>
              </div>
            </div>
            <!-- Modal Footer -->
            <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/[0.05] sticky bottom-0 bg-omni-950/95 backdrop-blur-xl">
              <button (click)="showAddPlan.set(false)" class="px-4 py-2.5 rounded-xl text-sm font-medium text-surface-500 hover:text-surface-900 hover:bg-white/[0.04] transition-all">Cancel</button>
              <button (click)="createPlan()" [disabled]="creatingPlan()"
                      class="px-5 py-2.5 rounded-xl text-sm font-semibold bg-gradient-to-r from-accent-teal to-accent-emerald text-surface-900 hover:shadow-lg hover:shadow-accent-teal/20 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
                {{ creatingPlan() ? 'Creating...' : 'Add Plan' }}
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes scaleIn {
      from { transform: scale(0.95); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }
    .animate-scale-in {
      animation: scaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
    @keyframes slideInRight {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    .animate-slide-in-right {
      animation: slideInRight 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
  `]
})
export class AdminOperatorsComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  operators = signal<AdminOperatorResponse[]>([]);
  loading = signal(true);

  // Stat counts
  activeOperatorCount = signal(0);
  inactiveOperatorCount = signal(0);

  // Create Operator
  showCreateOperator = signal(false);
  creatingOperator = signal(false);
  newOperator: CreateOperatorRequest = { name: '', code: '', category: '', logoUrl: '' };
  operatorCategories = ['PREPAID', 'POSTPAID', 'DTH', 'ELECTRICITY', 'GAS', 'WATER'];

  // Edit Operator
  showEditOperator = signal(false);
  updatingOperator = signal(false);
  editOperator: CreateOperatorRequest & { id?: number } = { name: '', code: '', category: '', logoUrl: '' };

  // Delete Operator
  showDeleteConfirm = signal(false);
  deletingOperator = signal(false);
  operatorToDelete = signal<AdminOperatorResponse | null>(null);

  // Add Plan
  showAddPlan = signal(false);
  creatingPlan = signal(false);
  selectedOperator = signal<AdminOperatorResponse | null>(null);
  newPlan: CreatePlanRequest = {
    planName: '', price: 0, validityDays: 0, dataLimit: '',
    callBenefit: '', smsBenefit: '', additionalBenefits: '', category: ''
  };
  planCategories = ['RECOMMENDED', 'DATA', 'UNLIMITED', 'TALKTIME'];

  // Toast
  toastVisible = signal(false);
  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  private toastTimer: any;

  ngOnInit() {
    this.loadOperators();
  }

  loadOperators() {
    this.loading.set(true);
    this.adminService.getAllOperators().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.operators.set(res.data);
          this.activeOperatorCount.set(res.data.filter((o: any) => o.isActive).length);
          this.inactiveOperatorCount.set(res.data.filter((o: any) => !o.isActive).length);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  createOperator() {
    if (!this.newOperator.name || !this.newOperator.code || !this.newOperator.category) {
      this.showToast('Please fill in all required fields.', 'error');
      return;
    }
    this.creatingOperator.set(true);
    this.adminService.createOperator(this.newOperator).subscribe({
      next: (res) => {
        this.creatingOperator.set(false);
        if (res.success) {
          this.showCreateOperator.set(false);
          this.newOperator = { name: '', code: '', category: '', logoUrl: '' };
          this.showToast('Operator created successfully!', 'success');
          this.loadOperators();
        } else {
          this.showToast(res.message || 'Failed to create operator.', 'error');
        }
      },
      error: (err) => {
        this.creatingOperator.set(false);
        this.showToast(err?.error?.message || 'Failed to create operator.', 'error');
      }
    });
  }

  openEditOperator(op: AdminOperatorResponse) {
    this.editOperator = {
      id: op.id,
      name: op.name,
      code: op.code,
      category: op.category,
      logoUrl: op.logoUrl || ''
    };
    this.showEditOperator.set(true);
  }

  updateOperator() {
    if (!this.editOperator.id || !this.editOperator.name || !this.editOperator.code || !this.editOperator.category) {
      this.showToast('Please fill in all required fields.', 'error');
      return;
    }
    this.updatingOperator.set(true);
    const { id, ...request } = this.editOperator;
    this.adminService.updateOperator(id!, request).subscribe({
      next: (res) => {
        this.updatingOperator.set(false);
        if (res.success) {
          this.showEditOperator.set(false);
          this.showToast('Operator updated successfully!', 'success');
          this.loadOperators();
        } else {
          this.showToast(res.message || 'Failed to update operator.', 'error');
        }
      },
      error: (err) => {
        this.updatingOperator.set(false);
        this.showToast(err?.error?.message || 'Failed to update operator.', 'error');
      }
    });
  }

  confirmDeleteOperator(op: AdminOperatorResponse) {
    this.operatorToDelete.set(op);
    this.showDeleteConfirm.set(true);
  }

  deleteOperator() {
    const op = this.operatorToDelete();
    if (!op) return;
    this.deletingOperator.set(true);
    this.adminService.deleteOperator(op.id).subscribe({
      next: (res) => {
        this.deletingOperator.set(false);
        if (res.success) {
          this.showDeleteConfirm.set(false);
          this.showToast(`${op.name} deleted successfully!`, 'success');
          this.loadOperators();
        } else {
          this.showToast(res.message || 'Failed to delete operator.', 'error');
        }
      },
      error: (err) => {
        this.deletingOperator.set(false);
        this.showToast(err?.error?.message || 'Failed to delete operator.', 'error');
      }
    });
  }

  toggleOperatorStatus(op: AdminOperatorResponse) {
    const action = op.isActive ? this.adminService.deactivateOperator(op.id) : this.adminService.activateOperator(op.id);
    action.subscribe({
      next: (res) => {
        if (res.success) {
          this.showToast(`${op.name} ${op.isActive ? 'deactivated' : 'activated'} successfully!`, 'success');
          this.loadOperators();
        } else {
          this.showToast(res.message || 'Failed to update operator status.', 'error');
        }
      },
      error: (err) => {
        this.showToast(err?.error?.message || 'Failed to update operator status.', 'error');
      }
    });
  }

  viewPlans(op: AdminOperatorResponse) {
    this.router.navigate(['/admin/operators', op.id, 'plans']);
  }

  openAddPlan(op: AdminOperatorResponse) {
    this.selectedOperator.set(op);
    this.newPlan = {
      planName: '', price: 0, validityDays: 0, dataLimit: '',
      callBenefit: '', smsBenefit: '', additionalBenefits: '', category: ''
    };
    this.showAddPlan.set(true);
  }

  createPlan() {
    const op = this.selectedOperator();
    if (!op) return;
    if (!this.newPlan.planName || !this.newPlan.price || !this.newPlan.validityDays || !this.newPlan.category) {
      this.showToast('Please fill in all required fields.', 'error');
      return;
    }
    this.creatingPlan.set(true);
    this.adminService.createPlan(op.id, this.newPlan).subscribe({
      next: (res) => {
        this.creatingPlan.set(false);
        if (res.success) {
          this.showAddPlan.set(false);
          this.showToast(`Plan added to ${op.name} successfully!`, 'success');
          this.loadOperators(); // Refresh plan count
        } else {
          this.showToast(res.message || 'Failed to add plan.', 'error');
        }
      },
      error: (err) => {
        this.creatingPlan.set(false);
        this.showToast(err?.error?.message || 'Failed to add plan.', 'error');
      }
    });
  }

  getCategoryClass(category: string): string {
    switch (category) {
      case 'PREPAID': return 'bg-sky-500/10 text-sky-400 border border-sky-500/20';
      case 'POSTPAID': return 'bg-violet-500/10 text-violet-400 border border-violet-500/20';
      case 'DTH': return 'bg-accent-amber/10 text-accent-amber border border-accent-amber/20';
      case 'ELECTRICITY': return 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20';
      case 'GAS': return 'bg-orange-500/10 text-orange-400 border border-orange-500/20';
      case 'WATER': return 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20';
      default: return 'bg-white/[0.05] text-surface-600 border border-surface-200';
    }
  }

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
      return d.toLocaleString('en-IN', { month: 'short', day: '2-digit', year: 'numeric', hour: 'numeric', minute: '2-digit', hour12: true });
    } catch { return '—'; }
  }

  private showToast(message: string, type: 'success' | 'error') {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage.set(message);
    this.toastType.set(type);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), 4000);
  }
}
