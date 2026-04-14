/**
 * RechargeFlowComponent — The primary recharge flow.
 *
 * This is the CORE product experience: entering a mobile number, seeing the
 * auto-detected operator, browsing plans in tabbed cards, and initiating
 * checkout. This page is PUBLIC until the user clicks "Pay", at which
 * point we enforce auth.
 *
 * Steps:
 *  1. Mobile Input + Operator Auto-Detection
 *  2. Plan Browser (tabs by category)
 *  3. Checkout (auth required → Razorpay)
 *  4. Receipt (success/failure)
 */
import { Component, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { OperatorService, Operator, Plan } from '../../core/services/operator.service';
import { RechargeService } from '../../core/services/recharge.service';
import { PaymentService } from '../../core/services/payment.service';
import { AuthService } from '../../core/services/auth.service';
import { HasUnsavedChanges } from '../../core/guards/can-deactivate.guard';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';

type FlowStep = 'input' | 'plans' | 'processing' | 'receipt';

@Component({
  selector: 'app-recharge-flow',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="section-container py-8 sm:py-12 page-enter">

      <!-- ── Step Indicator ── -->
      <div class="flex items-center justify-center gap-2 mb-10">
        @for (s of stepsConfig; track s.key; let i = $index) {
          <div class="flex items-center gap-2">
            <div class="flex items-center gap-2 px-3 py-1.5 rounded-full transition-all duration-300"
                 [class]="currentStep() === s.key ? 'bg-omni-500/20 border border-omni-500/30' :
                          (stepIndex(s.key) < stepIndex(currentStep()) ? 'bg-accent-emerald/10' : 'bg-white/[0.03]')">
              <div class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold"
                   [class]="stepIndex(s.key) < stepIndex(currentStep()) ? 'bg-accent-emerald text-white' :
                            currentStep() === s.key ? 'bg-omni-500 text-white' : 'bg-white/[0.08] text-surface-500'">
                @if (stepIndex(s.key) < stepIndex(currentStep())) {
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
                } @else {
                  {{ i + 1 }}
                }
              </div>
              <span class="text-xs font-medium hidden sm:inline"
                    [class]="currentStep() === s.key ? 'text-white' : 'text-surface-500'">{{ s.label }}</span>
            </div>
            @if (i < stepsConfig.length - 1) {
              <div class="w-6 sm:w-10 h-0.5 rounded"
                   [class]="stepIndex(s.key) < stepIndex(currentStep()) ? 'bg-accent-emerald' : 'bg-white/[0.06]'"></div>
            }
          </div>
        }
      </div>

      <!-- ═══════════ STEP 1: MOBILE INPUT ═══════════ -->
      @if (currentStep() === 'input') {
        <div class="max-w-lg mx-auto animate-slide-up">
          <div class="text-center mb-8">
            <h1 class="text-2xl sm:text-3xl font-display font-bold mb-2">Recharge any number</h1>
            <p class="text-surface-400">Enter a 10-digit mobile number to begin</p>
          </div>

          <div class="glass-card p-6 sm:p-8">
            <!-- Mobile Input -->
            <div class="relative mb-5">
              <div class="absolute left-4 top-1/2 -translate-y-1/2 flex items-center gap-2 pointer-events-none">
                <span class="text-surface-300 text-base font-medium">+91</span>
                <div class="w-px h-6 bg-white/10"></div>
              </div>
              <input type="tel"
                     [(ngModel)]="mobileNumber"
                     (input)="onMobileInput()"
                     (keydown.enter)="goToPlans()"
                     maxlength="10"
                     placeholder="Enter mobile number"
                     class="input-field text-xl py-4 !pl-[76px] tracking-widest font-mono"
                     autofocus
                     id="recharge-mobile-input" />
            </div>

            <!-- Detection State -->
            @if (mobileNumber.length === 10 && operatorService.isDetecting()) {
              <div class="flex items-center gap-3 p-4 rounded-xl bg-white/[0.03] mb-5 animate-fade-in">
                <div class="w-10 h-10 rounded-xl bg-omni-500/20 flex items-center justify-center">
                  <div class="w-5 h-5 border-2 border-omni-400 border-t-transparent rounded-full animate-spin"></div>
                </div>
                <div>
                  <p class="text-sm font-medium text-white">Detecting operator...</p>
                  <p class="text-xs text-surface-500">Connecting to Numverify API</p>
                </div>
              </div>
            }

            <!-- Detected Operator Card (auto-detected, not overridden) -->
            @if (mobileNumber.length === 10 && operatorService.selectedOperator(); as op) {
              @if (!showOperatorDropdown()) {
                <div class="flex items-center gap-3 p-4 rounded-xl mb-5 animate-scale-in"
                     [class]="operatorService.isManualOverride() ? 'bg-omni-500/5 border border-omni-500/20' : 'bg-accent-emerald/5 border border-accent-emerald/20'">
                  <div class="w-10 h-10 rounded-xl flex items-center justify-center"
                       [class]="operatorService.isManualOverride() ? 'bg-omni-500/20' : 'bg-accent-emerald/20'">
                    <svg class="w-5 h-5" [class]="operatorService.isManualOverride() ? 'text-omni-400' : 'text-accent-emerald'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                    </svg>
                  </div>
                  <div class="flex-1">
                    <p class="text-sm font-semibold text-white">{{ op.operatorName }}</p>
                    <p class="text-xs text-surface-400">{{ op.type }} • {{ mobileNumber }}</p>
                  </div>
                  <div class="flex items-center gap-2">
                    <span [class]="operatorService.isManualOverride() ? 'text-[10px] px-2 py-0.5 rounded-full bg-omni-500/20 text-omni-300 font-semibold' : 'badge-success'">
                      {{ operatorService.isManualOverride() ? 'Manual' : 'Detected' }}
                    </span>
                    <button (click)="toggleOperatorDropdown()" class="text-xs text-surface-400 hover:text-omni-400 transition-colors underline underline-offset-2 decoration-dashed">
                      Change
                    </button>
                  </div>
                </div>
              }
            }

            <!-- Operator Dropdown (shown on detection failure OR user clicking "Change") -->
            @if (mobileNumber.length === 10 && (showOperatorDropdown() || (operatorService.detectionFailed() && !operatorService.selectedOperator()))) {
              <div class="mb-5 animate-scale-in">
                @if (operatorService.detectionFailed() && !operatorService.selectedOperator()) {
                  <div class="flex items-center gap-2 p-3 rounded-xl bg-accent-amber/5 border border-accent-amber/20 mb-3">
                    <svg class="w-5 h-5 text-accent-amber shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/>
                    </svg>
                    <p class="text-xs text-accent-amber">Could not auto-detect operator. Please select manually.</p>
                  </div>
                }
                <p class="text-xs font-medium text-surface-400 mb-2 uppercase tracking-wider">Select Operator</p>
                <div class="grid grid-cols-2 gap-2">
                  @for (op of operatorService.operators(); track op.id) {
                    <button (click)="selectManualOperator(op)"
                            class="p-3 rounded-xl border transition-all duration-200 text-left group"
                            [class]="operatorService.selectedOperator()?.operatorId === op.id
                              ? 'bg-omni-500/10 border-omni-500/40 shadow-[0_0_12px_rgba(139,92,246,0.15)]'
                              : 'bg-white/[0.02] border-white/[0.06] hover:border-white/20 hover:bg-white/[0.04]'">
                      <p class="text-sm font-semibold text-white">{{ op.name }}</p>
                      <p class="text-[10px] text-surface-500 uppercase tracking-wider">{{ op.type }}</p>
                    </button>
                  }
                </div>
                @if (showOperatorDropdown() && operatorService.selectedOperator()) {
                  <button (click)="showOperatorDropdown.set(false)" class="mt-3 text-xs text-surface-400 hover:text-white transition w-full text-center">
                    Cancel
                  </button>
                }
              </div>
            }

            @if (detectionError() && !operatorService.detectionFailed()) {
              <div class="p-4 rounded-xl bg-accent-rose/5 border border-accent-rose/20 mb-5 animate-scale-in">
                <p class="text-sm text-accent-rose">{{ detectionError() }}</p>
              </div>
            }

            <button (click)="goToPlans()"
                    [disabled]="!operatorService.selectedOperator()"
                    class="btn-primary w-full text-base !py-3.5">
              View Plans
              <svg class="w-4 h-4 ml-2 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"/>
              </svg>
            </button>
          </div>
        </div>
      }

      <!-- ═══════════ STEP 2: PLAN BROWSER ═══════════ -->
      @if (currentStep() === 'plans') {
        <div class="max-w-4xl mx-auto animate-slide-up">

          <!-- Header with selected info -->
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <div>
              <button (click)="currentStep.set('input')" class="text-sm text-omni-400 hover:text-omni-300 transition mb-2 flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                Change Number
              </button>
              <h2 class="text-xl font-display font-bold">
                Plans for {{ operatorService.selectedOperator()?.operatorName }}
              </h2>
              <p class="text-sm text-surface-400">{{ mobileNumber }} • {{ operatorService.selectedOperator()?.type }}</p>
            </div>
          </div>

          <!-- Search and Filters Section -->
          <div class="flex flex-col md:flex-row gap-4 mb-6">
            <!-- Search Bar -->
            <div class="relative flex-1 group">
              <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <svg class="h-5 w-5 text-surface-400 group-focus-within:text-omni-400 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <input type="text"
                     [ngModel]="searchQuery()"
                     (ngModelChange)="searchQuery.set($event)"
                     placeholder="Search for 'data', '299', or 'unlimited'..."
                     class="w-full bg-surface-800/50 border border-white/10 rounded-xl py-3 pl-11 pr-4 text-white placeholder-surface-500 focus:outline-none focus:ring-2 focus:ring-omni-500/50 focus:border-omni-500/50 transition-all shadow-inner backdrop-blur-sm" />
              @if (searchQuery()) {
                <button (click)="searchQuery.set('')" class="absolute inset-y-0 right-0 pr-4 flex items-center text-surface-400 hover:text-white transition-colors">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                </button>
              }
            </div>

            <!-- Category Tabs -->
            <div class="flex gap-2 overflow-x-auto pb-2 md:pb-0 items-center scrollbar-none">
              <button (click)="activeCategory.set('ALL')"
                      [class]="activeCategory() === 'ALL' ? 'tab-item-active' : 'tab-item'">
                All Plans
              </button>
              @for (cat of planCategories(); track cat) {
                <button (click)="activeCategory.set(cat)"
                        [class]="activeCategory() === cat ? 'tab-item-active' : 'tab-item'">
                  {{ cat | titlecase }}
                </button>
              }
            </div>
          </div>

          <!-- Plans Grid -->
          @if (operatorService.isLoadingPlans()) {
            <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
              @for (i of [1,2,3,4,5,6]; track i) {
                <div class="glass-card p-5">
                  <div class="skeleton h-6 w-20 mb-3"></div>
                  <div class="skeleton h-4 w-full mb-2"></div>
                  <div class="skeleton h-4 w-3/4 mb-4"></div>
                  <div class="skeleton h-10 w-full"></div>
                </div>
              }
            </div>
          } @else if (filteredPlans().length > 0) {
            <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
              @for (plan of filteredPlans(); track plan.id) {
                <div class="glass-card-hover p-5 relative group cursor-pointer"
                     [class.border-omni-500]="selectedPlan()?.id === plan.id"
                     [class.bg-omni-950]="selectedPlan()?.id === plan.id"
                     (click)="selectPlan(plan)">

                  <!-- Selected indicator -->
                  @if (selectedPlan()?.id === plan.id) {
                    <div class="absolute top-3 right-3 w-6 h-6 rounded-full bg-omni-500 flex items-center justify-center animate-scale-in">
                      <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/>
                      </svg>
                    </div>
                  }

                  <!-- Price -->
                  <div class="flex items-baseline gap-1 mb-3">
                    <span class="text-2xl font-display font-bold text-white">₹{{ plan.price }}</span>
                    <span class="text-xs text-surface-500">/ {{ plan.validityDays }} days</span>
                  </div>

                  <!-- Plan Name -->
                  <h3 class="text-sm font-semibold text-surface-200 mb-3">{{ plan.planName }}</h3>

                  <!-- Benefits -->
                  <div class="space-y-2 mb-4">
                    @if (plan.dataLimit) {
                      <div class="flex items-center gap-2">
                        <div class="w-6 h-6 rounded-md bg-accent-sky/10 flex items-center justify-center shrink-0">
                          <svg class="w-3.5 h-3.5 text-accent-sky" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z"/></svg>
                        </div>
                        <span class="text-xs text-surface-300">{{ plan.dataLimit }}</span>
                      </div>
                    }
                    @if (plan.callBenefit) {
                      <div class="flex items-center gap-2">
                        <div class="w-6 h-6 rounded-md bg-accent-emerald/10 flex items-center justify-center shrink-0">
                          <svg class="w-3.5 h-3.5 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/></svg>
                        </div>
                        <span class="text-xs text-surface-300">{{ plan.callBenefit }}</span>
                      </div>
                    }
                    @if (plan.smsBenefit) {
                      <div class="flex items-center gap-2">
                        <div class="w-6 h-6 rounded-md bg-accent-amber/10 flex items-center justify-center shrink-0">
                          <svg class="w-3.5 h-3.5 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
                        </div>
                        <span class="text-xs text-surface-300">{{ plan.smsBenefit }}</span>
                      </div>
                    }
                    @if (plan.additionalBenefits) {
                      <div class="flex items-center gap-2">
                        <div class="w-6 h-6 rounded-md bg-omni-500/10 flex items-center justify-center shrink-0">
                          <svg class="w-3.5 h-3.5 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
                        </div>
                        <span class="text-xs text-surface-300">{{ plan.additionalBenefits }}</span>
                      </div>
                    }
                  </div>

                  <!-- Category Badge -->
                  <div class="flex items-center justify-between">
                    <span class="text-[10px] uppercase tracking-wider font-semibold px-2 py-0.5 rounded-md
                                 bg-white/[0.05] text-surface-400">{{ plan.category }}</span>
                  </div>
                </div>
              }
            </div>
          } @else {
            <div class="text-center py-16 glass-card">
              <p class="text-surface-400">No plans found for this category.</p>
            </div>
          }

          <!-- Floating Checkout Bar -->
          @if (selectedPlan(); as plan) {
            <div class="fixed bottom-0 left-0 right-0 z-40 p-4 animate-slide-up">
              <div class="max-w-4xl mx-auto glass-card p-4 flex items-center justify-between gap-4
                          border-omni-500/20 shadow-glow">
                <div class="flex items-center gap-3 min-w-0">
                  <div class="w-10 h-10 rounded-xl bg-omni-500/20 flex items-center justify-center shrink-0">
                    <span class="text-lg font-bold text-omni-400">₹</span>
                  </div>
                  <div class="min-w-0">
                    <p class="text-sm font-semibold text-white truncate">{{ plan.planName }}</p>
                    <p class="text-xs text-surface-400">₹{{ plan.price }} • {{ plan.validityDays }} days</p>
                  </div>
                </div>
                <button (click)="onProceedToCheckout()" class="btn-primary !py-3 !px-6 shrink-0 flex items-center gap-2">
                  <span>Pay ₹{{ plan.price }}</span>
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"/>
                  </svg>
                </button>
              </div>
            </div>
          }
        </div>
      }

      <!-- ═══════════ STEP 3: PROCESSING ═══════════ -->
      @if (currentStep() === 'processing') {
        <div class="max-w-md mx-auto text-center py-20 animate-fade-in">
          <div class="w-24 h-24 mx-auto mb-8 rounded-full bg-omni-500/10 flex items-center justify-center animate-glow-pulse">
            <div class="w-12 h-12 border-4 border-omni-500/30 border-t-omni-400 rounded-full animate-spin"></div>
          </div>
          <h2 class="text-xl font-display font-semibold mb-2">Processing your recharge</h2>
          <p class="text-surface-400 text-sm mb-4">{{ processingMessage() }}</p>
          <p class="text-xs text-surface-500">Please don't close this page</p>
        </div>
      }

      <!-- ═══════════ STEP 4: RECEIPT ═══════════ -->
      @if (currentStep() === 'receipt') {
        <div class="max-w-md mx-auto animate-scale-in">
          <div class="glass-card p-8 text-center">
            @if (paymentService.paymentState() === 'success') {
              <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-accent-emerald/10 flex items-center justify-center">
                <svg class="w-10 h-10 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
              </div>
              <h2 class="text-2xl font-display font-bold text-accent-emerald mb-2">Recharge Successful!</h2>
              <p class="text-surface-400 text-sm mb-6">Your recharge has been activated.</p>
            } @else {
              <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-accent-rose/10 flex items-center justify-center">
                <svg class="w-10 h-10 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </div>
              <h2 class="text-2xl font-display font-bold text-accent-rose mb-2">Payment Failed</h2>
              <p class="text-surface-400 text-sm mb-6">{{ failureReason() || 'Something went wrong. Please try again.' }}</p>
            }

            <!-- Transaction details -->
            @if (paymentService.currentTransaction(); as txn) {
              <div class="bg-white/[0.03] rounded-xl p-4 text-left space-y-3 mb-6">
                <div class="flex justify-between text-sm">
                  <span class="text-surface-400">Transaction ID</span>
                  <span class="font-mono text-xs text-surface-200">{{ txn.transactionId }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-surface-400">Amount</span>
                  <span class="font-semibold text-white">₹{{ txn.amount }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-surface-400">Status</span>
                  <span [class]="txn.status === 'SUCCESS' ? 'badge-success' : 'badge-failed'">{{ txn.status }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-surface-400">Date</span>
                  <span class="text-surface-200 text-xs">{{ txn.createdDate }}</span>
                </div>
              </div>
            }

            <div class="flex gap-3 mt-6">
              <a routerLink="/dashboard" class="btn-secondary flex-1 text-center">Dashboard</a>
              <button (click)="startNewRecharge()" class="btn-primary flex-1">Recharge Again</button>
            </div>
          </div>
        </div>
      }

      <!-- ═══════════ MODAL: MOBILE VERIFICATION ═══════════ -->
      @if (showVerificationModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div class="glass-card w-full max-w-md p-6 sm:p-8 animate-scale-in relative border-omni-500/30 shadow-glow">
            <!-- Close Button -->
            <button (click)="closeVerificationModal()" class="absolute top-4 right-4 text-surface-400 hover:text-white transition" [disabled]="isVerifying()">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
            
            <div class="text-center mb-6">
              <div class="w-16 h-16 rounded-full bg-omni-500/10 flex items-center justify-center mx-auto mb-4 border border-omni-500/20 shadow-[0_0_15px_rgba(139,92,246,0.2)]">
                <svg class="w-8 h-8 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"/>
                </svg>
              </div>
              <h2 class="text-2xl font-display font-bold text-white mb-2">Verify Your Account</h2>
              <p class="text-surface-400 text-sm">One-time mobile verification is required before proceeding to payment.</p>
            </div>

            @if (verificationError()) {
              <div class="mb-6 p-4 rounded-xl bg-accent-rose/10 border border-accent-rose/20 text-accent-rose text-sm text-center font-medium">
                {{ verificationError() }}
              </div>
            }

            @if (verificationStep() === 'MOBILE') {
              <div class="relative mb-6">
                <div class="absolute left-4 top-1/2 -translate-y-1/2 flex items-center gap-2 pointer-events-none">
                  <span class="text-surface-300 font-medium">+91</span>
                  <div class="w-px h-5 bg-white/10"></div>
                </div>
                <input type="tel"
                       [(ngModel)]="verificationMobileInput"
                       maxlength="10"
                       placeholder="Enter mobile number"
                       class="input-field py-4 !pl-[70px] tracking-widest font-mono text-lg"
                       [disabled]="isVerifying()"
                       autofocus />
              </div>
              <button (click)="requestVerificationOtp()" [disabled]="isVerifying() || verificationMobileInput.length !== 10" class="btn-primary w-full py-3.5 flex justify-center items-center gap-2 text-base">
                @if (isVerifying()) {
                  <div class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  <span>Sending...</span>
                } @else {
                  <span>Send OTP</span>
                }
              </button>
            } @else {
              <p class="text-center text-sm text-surface-300 mb-6">Enter the 6-digit code sent to <span class="font-bold text-white">+91 {{ verificationMobileInput }}</span></p>
              <div class="flex justify-between gap-2 mb-6">
                @for (digit of verificationOtpDigits; track $index) {
                  <input type="text"
                         maxlength="1"
                         [(ngModel)]="verificationOtpDigits[$index]"
                         (input)="onOtpInput($index, $event)"
                         (keydown)="onOtpKeydown($index, $event)"
                         (paste)="onOtpPaste($event)"
                         class="w-12 h-14 sm:w-14 sm:h-16 text-center font-mono text-2xl font-bold bg-white/[0.03] border border-white/10 rounded-xl focus:border-omni-500 focus:bg-omni-500/10 transition-colors outline-none text-white"
                         [disabled]="isVerifying()"
                         id="otp-input-{{$index}}" />
                }
              </div>
              <button (click)="verifyOtpAndProceed()" [disabled]="isVerifying() || verificationOtpInput.length !== 6" class="btn-primary w-full py-3.5 flex justify-center items-center gap-2 text-base">
                @if (isVerifying()) {
                  <div class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  <span>Verifying...</span>
                } @else {
                  <span>Verify & Proceed</span>
                }
              </button>
              <div class="mt-6 text-center">
                <button (click)="verificationStep.set('MOBILE'); resetOtp(); verificationError.set('')" class="text-sm text-surface-400 hover:text-white transition" [disabled]="isVerifying()">
                  Change number
                </button>
              </div>
            }
          </div>
        </div>
      }

      <!-- ═══════════ MODAL: LOGIN REQUIRED ═══════════ -->
      @if (showLoginModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div class="glass-card w-full max-w-sm p-6 sm:p-8 animate-scale-in relative border-omni-500/30 shadow-glow">
            <!-- Close -->
            <button (click)="onCancelLogin()" class="absolute top-4 right-4 text-surface-400 hover:text-white transition">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>

            <div class="text-center mb-6">
              <div class="w-16 h-16 rounded-full bg-omni-500/10 flex items-center justify-center mx-auto mb-4 border border-omni-500/20 shadow-[0_0_20px_rgba(99,102,241,0.2)]">
                <svg class="w-8 h-8 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"/>
                </svg>
              </div>
              <h2 class="text-xl font-display font-bold text-white mb-2">Login Required</h2>
              <p class="text-surface-400 text-sm leading-relaxed">
                To complete your recharge, please log in or create an account. Your selected plan will be saved.
              </p>
            </div>

            <!-- Selected plan summary -->
            @if (selectedPlan(); as plan) {
              <div class="mb-6 p-3 rounded-xl bg-white/[0.03] border border-white/[0.06] flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-omni-500/15 flex items-center justify-center shrink-0">
                  <span class="text-sm font-bold text-omni-400">₹</span>
                </div>
                <div class="min-w-0">
                  <p class="text-sm font-semibold text-white truncate">{{ plan.planName }}</p>
                  <p class="text-xs text-surface-500">₹{{ plan.price }} • {{ plan.validityDays }} days</p>
                </div>
              </div>
            }

            <div class="flex gap-3">
              <button (click)="onCancelLogin()" class="btn-secondary flex-1 !py-3 text-sm border border-white/10 hover:border-white/20">Cancel</button>
              <button (click)="onConfirmLogin()" class="btn-primary flex-1 !py-3 text-sm flex items-center justify-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"/></svg>
                Continue to Login
              </button>
            </div>
          </div>
        </div>
      }

    </div>
  `,
  styles: [`
    .scrollbar-none::-webkit-scrollbar { display: none; }
    .scrollbar-none { -ms-overflow-style: none; scrollbar-width: none; }
  `]
})
export class RechargeFlowComponent implements OnInit, HasUnsavedChanges {
  readonly operatorService = inject(OperatorService);
  readonly rechargeService = inject(RechargeService);
  readonly paymentService = inject(PaymentService);
  readonly authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  /** CanDeactivate guard: warn user if they have a plan selected mid-flow */
  private bypassUnsavedWarn = false;
  hasUnsavedChanges(): boolean {
    if (this.bypassUnsavedWarn) return false;
    const step = this.currentStep();
    return step === 'plans' && this.selectedPlan() !== null;
  }

  mobileNumber = '';
  currentStep = signal<FlowStep>('input');
  selectedPlan = signal<Plan | null>(null);
  activeCategory = signal<string>('ALL');
  searchQuery = signal<string>('');
  detectionError = signal<string>('');
  processingMessage = signal<string>('Initiating recharge...');
  failureReason = signal<string>('');
  showOperatorDropdown = signal<boolean>(false);
  showLoginModal = signal<boolean>(false);

  // Modal State
  showVerificationModal = signal<boolean>(false);
  verificationStep = signal<'MOBILE' | 'OTP'>('MOBILE');
  verificationError = signal<string>('');
  isVerifying = signal<boolean>(false);
  verificationMobileInput = '';
  
  verificationOtpDigits: string[] = ['', '', '', '', '', ''];
  get verificationOtpInput(): string {
    return this.verificationOtpDigits.join('');
  }

  stepsConfig = [
    { key: 'input' as FlowStep, label: 'Number' },
    { key: 'plans' as FlowStep, label: 'Plans' },
    { key: 'processing' as FlowStep, label: 'Payment' },
    { key: 'receipt' as FlowStep, label: 'Receipt' },
  ];

  /** Unique plan categories for tabs */
  planCategories = computed(() => {
    const cats = new Set(this.operatorService.plans().map(p => p.category));
    return Array.from(cats);
  });

  /** Filtered plans based on active tab and search query */
  filteredPlans = computed(() => {
    let plans = this.operatorService.plans();
    
    // 1. Filter by category
    if (this.activeCategory() !== 'ALL') {
      plans = plans.filter(p => p.category === this.activeCategory());
    }
    
    // 2. Filter by search query
    const q = this.searchQuery().toLowerCase().trim();
    if (q) {
      plans = plans.filter(p => 
        p.planName?.toLowerCase().includes(q) ||
        p.price.toString().includes(q) ||
        p.dataLimit?.toLowerCase().includes(q) ||
        p.callBenefit?.toLowerCase().includes(q) ||
        p.validityDays?.toString().includes(q)
      );
    }
    
    return plans;
  });

  ngOnInit(): void {
    // Clear any lingering state
    this.mobileNumber = '';
    this.operatorService.clearSelection();
    this.selectedPlan.set(null);
    this.currentStep.set('input');

    // Pre-fill from query params (if coming from landing page)
    const mobile = this.route.snapshot.queryParams['mobile'];
    const passedOperatorId = this.route.snapshot.queryParams['operatorId'];
    
    if (mobile && mobile.length === 10) {
      this.mobileNumber = mobile;
      
      if (passedOperatorId) {
        // Find and set the operator manually
        this.operatorService.loadActiveOperators(); // Ensure operators are loaded
        // We simulate detection success to move to next step, but then override the operator
        this.operatorService.detectOperator(mobile).subscribe({
          next: res => {
            if (res?.success) {
               this.currentStep.set('plans');
               // Override with passed operator
               const manualOp = this.operatorService.operators().find((o: any) => o.id == passedOperatorId);
               if (manualOp) {
                 this.operatorService.setManualOperator(manualOp);
               }
            }
          }
        });
      } else {
        this.operatorService.detectOperator(mobile).subscribe({
          next: res => {
            if (res?.success) {
              this.currentStep.set('plans');
            }
          }
        });
      }
    }
  }

  stepIndex(key: FlowStep): number {
    return this.stepsConfig.findIndex(s => s.key === key);
  }

  onMobileInput(): void {
    this.mobileNumber = this.mobileNumber.replace(/\D/g, '');
    this.detectionError.set('');
    this.showOperatorDropdown.set(false);
    if (this.mobileNumber.length === 10) {
      this.operatorService.detectOperator(this.mobileNumber).subscribe({
        error: () => this.detectionError.set('Could not detect operator. Please try again.')
      });
    } else {
      this.operatorService.clearSelection();
    }
  }

  goToPlans(): void {
    if (this.operatorService.selectedOperator()) {
      this.showOperatorDropdown.set(false);
      this.currentStep.set('plans');
    }
  }

  toggleOperatorDropdown(): void {
    this.showOperatorDropdown.update(v => !v);
    if (this.showOperatorDropdown() && this.operatorService.operators().length === 0) {
      this.operatorService.loadActiveOperators();
    }
  }

  selectManualOperator(operator: Operator): void {
    this.operatorService.setManualOperator(operator);
    this.showOperatorDropdown.set(false);
  }

  selectPlan(plan: Plan): void {
    this.selectedPlan.set(this.selectedPlan()?.id === plan.id ? null : plan);
  }

  /** Login modal: user confirmed → navigate to login */
  onConfirmLogin(): void {
    this.showLoginModal.set(false);
    this.bypassUnsavedWarn = true;
    this.router.navigate(['/login'], {
      queryParams: { returnUrl: `/recharge?mobile=${this.mobileNumber}` }
    });
  }

  /** Login modal: user cancelled → dismiss */
  onCancelLogin(): void {
    this.showLoginModal.set(false);
  }

  async onProceedToCheckout(): Promise<void> {
    // Enforce auth at checkout
    if (!this.authService.isAuthenticated()) {
      this.showLoginModal.set(true);
      return;
    }

    const plan = this.selectedPlan();
    const operator = this.operatorService.selectedOperator();
    if (!plan || !operator) return;

    // MANDATORY MOBILE VERIFICATION CHECK (especially for Google Auth users)
    const user = this.authService.currentUser();
    const hasMobileVerified = this.authService.isMobileVerified();

    if (!user?.mobileNumber && !hasMobileVerified) {
       this.showVerificationModal.set(true);
       this.verificationStep.set('MOBILE');
       this.verificationMobileInput = this.mobileNumber;
       this.resetOtp();
       this.verificationError.set('');
       this.isVerifying.set(false);
       return;
    }

    await this.initiatePaymentFlow();
  }

  async requestVerificationOtp(): Promise<void> {
    const mobile = this.verificationMobileInput.replace(/\D/g, '');
    if (mobile.length !== 10) {
      this.verificationError.set('Enter a valid 10-digit number');
      return;
    }
    this.isVerifying.set(true);
    this.verificationError.set('');
    try {
      await firstValueFrom(this.authService.sendMobileOtp(`+91${mobile}`));
      this.verificationStep.set('OTP');
      setTimeout(() => { document.getElementById('otp-input-0')?.focus(); }, 100);
    } catch (err: any) {
      this.verificationError.set(err?.error?.message || err?.message || 'Failed to send OTP. This number might be registered already.');
    } finally {
      this.isVerifying.set(false);
    }
  }

  resetOtp(): void {
    this.verificationOtpDigits = ['', '', '', '', '', ''];
  }

  onOtpInput(index: number, event: any): void {
    const value = event.target.value;
    
    if (value && index < 5 && /^\d$/.test(value)) {
      const nextInput = document.getElementById(`otp-input-${index + 1}`) as HTMLInputElement;
      if (nextInput) {
        nextInput.focus();
        nextInput.select();
      }
    }
  }

  onOtpKeydown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace' && !this.verificationOtpDigits[index] && index > 0) {
      const prevInput = document.getElementById(`otp-input-${index - 1}`) as HTMLInputElement;
      if (prevInput) {
        prevInput.focus();
        this.verificationOtpDigits[index - 1] = '';
      }
    }
  }

  onOtpPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pastedData = event.clipboardData?.getData('text') || '';
    const chars = pastedData.split('').filter(c => /^\d$/.test(c)).slice(0, 6);
    
    for (let i = 0; i < chars.length; i++) {
        this.verificationOtpDigits[i] = chars[i];
    }
    
    const focusIndex = Math.min(chars.length, 5);
    const focusInput = document.getElementById(`otp-input-${focusIndex === 6 ? 5 : focusIndex}`) as HTMLInputElement;
    if (focusInput) focusInput.focus();
  }

  async verifyOtpAndProceed(): Promise<void> {
    const otp = this.verificationOtpInput.replace(/\D/g, '');
    if (otp.length !== 6) {
      this.verificationError.set('Enter a valid 6-digit OTP');
      return;
    }
    this.isVerifying.set(true);
    this.verificationError.set('');
    try {
      await firstValueFrom(this.authService.verifyMobileOtp(`+91${this.verificationMobileInput}`, otp));
      this.authService.loadProfile();
      this.showVerificationModal.set(false);
      
      // Proceed to payment after successful verification
      await this.initiatePaymentFlow();
    } catch (err: any) {
      this.verificationError.set(err?.error?.message || err?.message || 'Invalid OTP');
    } finally {
      this.isVerifying.set(false);
    }
  }

  closeVerificationModal(): void {
    this.showVerificationModal.set(false);
  }

  private async initiatePaymentFlow(): Promise<void> {
    const plan = this.selectedPlan();
    const operator = this.operatorService.selectedOperator();
    if (!plan || !operator) return;

    this.currentStep.set('processing');
    this.processingMessage.set('Initiating recharge...');

    let activeTransactionId: string | null = null;

    try {
      // Step 1: Initiate recharge in backend
      const rechargeRes = await firstValueFrom(this.rechargeService.initiateRecharge({
        mobileNumber: this.mobileNumber,
        operatorId: operator.operatorId,
        planId: plan.id,
        paymentMethod: 'RAZORPAY'
      }));

      if (!rechargeRes?.success || !rechargeRes.data) {
        throw new Error(rechargeRes?.message || 'Failed to initiate recharge');
      }

      const recharge = rechargeRes.data;
      this.processingMessage.set('Creating payment order...');

      // Step 2: Process payment (creates Razorpay order)
      const userId = this.authService.getUserIdFromToken()!;
      const user = this.authService.currentUser();

      const paymentRes = await firstValueFrom(this.paymentService.processPayment({
        rechargeId: recharge.rechargeId,
        userId: userId,
        amount: plan.price,
        paymentMethod: 'RAZORPAY',
        userEmail: user?.email || '',
        userMobile: user?.mobileNumber || this.mobileNumber,
      }));

      if (!paymentRes?.success || !paymentRes.data || paymentRes.data.status === 'FAILED') {
        throw new Error('Payment order creation failed');
      }

      activeTransactionId = paymentRes.data.transactionId;
      this.processingMessage.set('Opening Razorpay...');

      // Step 3: Open Razorpay checkout
      const razorpayResult = await this.paymentService.openRazorpayCheckout(
        paymentRes.data,
        user?.email || '',
        user?.mobileNumber || this.mobileNumber
      );

      this.processingMessage.set('Confirming payment...');

      // Step 4: Confirm payment
      await firstValueFrom(this.paymentService.confirmPayment(
        activeTransactionId,
        razorpayResult.paymentId,
        razorpayResult.signature
      ));

      this.currentStep.set('receipt');

    } catch (err: any) {
      // Razorpay popup closed or handler didn't fire.
      // CRITICAL FALLBACK: Check Razorpay API server-side before marking as failed.
      if (activeTransactionId) {
        this.processingMessage.set('Verifying payment status...');

        try {
          // Wait a moment for Razorpay to process the payment
          await new Promise(resolve => setTimeout(resolve, 3000));

          // Poll backend which checks Razorpay API directly
          const verifyRes = await firstValueFrom(
            this.paymentService.verifyPayment(activeTransactionId)
          );

          if (verifyRes?.success && verifyRes.data?.status === 'SUCCESS') {
            // Payment WAS successful — Razorpay confirmed it server-side!
            console.log('Payment verified via server-side check!');
            this.currentStep.set('receipt');
            return;
          }

          // If still not captured, try once more after a longer wait
          await new Promise(resolve => setTimeout(resolve, 5000));
          const retryRes = await firstValueFrom(
            this.paymentService.verifyPayment(activeTransactionId)
          );

          if (retryRes?.success && retryRes.data?.status === 'SUCCESS') {
            console.log('Payment verified on retry!');
            this.currentStep.set('receipt');
            return;
          }
        } catch (verifyErr) {
          console.warn('Verification check failed:', verifyErr);
        }

        // If verification didn't find a successful payment, mark as failed
        const errorMsg = err?.message || 'Payment failed';
        this.failureReason.set(errorMsg);
        try {
          await firstValueFrom(this.paymentService.failPayment(activeTransactionId, errorMsg));
        } catch (failErr) {
          console.warn('Failed to notify backend of cancellation:', failErr);
        }
      } else {
        this.failureReason.set(err?.message || 'Payment failed');
        this.paymentService.resetPaymentState();
      }

      this.currentStep.set('receipt');
    }
  }

  startNewRecharge(): void {
    this.mobileNumber = '';
    this.selectedPlan.set(null);
    this.activeCategory.set('ALL');
    this.searchQuery.set('');
    this.detectionError.set('');
    this.failureReason.set('');
    this.operatorService.clearSelection();
    this.rechargeService.clearCurrentRecharge();
    this.paymentService.resetPaymentState();
    this.currentStep.set('input');
  }
}
