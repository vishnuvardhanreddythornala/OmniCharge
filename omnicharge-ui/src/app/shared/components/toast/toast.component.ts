/**
 * ToastComponent — Renders stacked toast notifications at the top-right of the viewport.
 *
 * Fully standalone. Injected once at the root level in AppComponent.
 * Each toast slides in from the right, auto-dismisses, and can be manually closed.
 */
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-4 right-4 z-[100] flex flex-col gap-2.5 max-w-sm w-full pointer-events-none"
         aria-live="polite" aria-atomic="true">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="pointer-events-auto animate-toast-in flex items-start gap-3 px-4 py-3.5 rounded-xl border shadow-2xl backdrop-blur-xl transition-all duration-300"
             [class]="getToastClasses(toast)"
             role="alert">
          <!-- Icon -->
          <div class="shrink-0 mt-0.5">
            @switch (toast.type) {
              @case ('success') {
                <svg class="w-5 h-5 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              }
              @case ('error') {
                <svg class="w-5 h-5 text-accent-rose" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              }
              @case ('warning') {
                <svg class="w-5 h-5 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                </svg>
              }
              @case ('info') {
                <svg class="w-5 h-5 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              }
            }
          </div>

          <!-- Message & Action -->
          <div class="flex flex-col gap-2 flex-1">
            <span class="text-sm font-medium leading-snug">{{ toast.message }}</span>
            @if (toast.action) {
              <button (click)="toast.action.onClick(); toastService.dismiss(toast.id)"
                      class="text-xs font-semibold px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 transition-colors w-fit border border-white/5 shadow-sm active:scale-95">
                {{ toast.action.label }}
              </button>
            }
          </div>

          <!-- Close Button -->
          <button (click)="toastService.dismiss(toast.id)"
                  class="shrink-0 opacity-50 hover:opacity-100 transition-opacity -mt-0.5 -mr-1 p-1 rounded-lg hover:bg-white/[0.06]"
                  aria-label="Close notification">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes toastIn {
      from {
        opacity: 0;
        transform: translateX(100%) scale(0.95);
      }
      to {
        opacity: 1;
        transform: translateX(0) scale(1);
      }
    }
    .animate-toast-in {
      animation: toastIn 0.35s cubic-bezier(0.21, 1.02, 0.73, 1) forwards;
    }
  `]
})
export class ToastComponent {
  readonly toastService = inject(ToastService);

  getToastClasses(toast: Toast): string {
    switch (toast.type) {
      case 'success':
        return 'bg-surface-900/90 border-accent-emerald/25 text-accent-emerald shadow-accent-emerald/10';
      case 'error':
        return 'bg-surface-900/90 border-accent-rose/25 text-accent-rose shadow-accent-rose/10';
      case 'warning':
        return 'bg-surface-900/90 border-accent-amber/25 text-accent-amber shadow-accent-amber/10';
      case 'info':
        return 'bg-surface-900/90 border-omni-500/25 text-omni-300 shadow-omni-500/10';
      default:
        return 'bg-surface-900/90 border-white/10 text-white';
    }
  }
}
