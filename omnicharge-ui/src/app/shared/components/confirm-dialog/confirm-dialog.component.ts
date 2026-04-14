import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmDialogService } from './confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (dialogService.config(); as config) {
      <div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
        <div class="glass-card w-full max-w-sm p-6 sm:p-8 animate-scale-in relative border-omni-500/30 shadow-glow">
          
          <div class="text-center mb-6">
            <div class="w-16 h-16 rounded-full bg-accent-amber/10 flex items-center justify-center mx-auto mb-4 border border-accent-amber/20 shadow-[0_0_20px_rgba(245,158,11,0.2)]">
              <svg class="w-8 h-8 text-accent-amber" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/>
              </svg>
            </div>
            <h2 class="text-xl font-display font-bold text-white mb-2">{{ config.title }}</h2>
            <p class="text-surface-400 text-sm leading-relaxed">
              {{ config.message }}
            </p>
          </div>

          <div class="flex gap-3">
            <button (click)="dialogService.close(false)" class="btn-secondary flex-1 !py-3 text-sm border border-white/10 hover:border-white/20">
              {{ config.cancelLabel }}
            </button>
            <button (click)="dialogService.close(true)" class="btn-primary flex-1 !py-3 text-sm flex items-center justify-center gap-2">
              {{ config.confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ConfirmDialogComponent {
  dialogService = inject(ConfirmDialogService);
}
