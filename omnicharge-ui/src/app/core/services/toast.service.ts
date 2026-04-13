/**
 * ToastService — Global notification toast system.
 *
 * Signal-based service for showing toast notifications from any component,
 * service, or interceptor. Supports stacking (max 5), auto-dismiss, and
 * manual close.
 *
 * Usage:
 *   inject(ToastService).show('Payment confirmed!', 'success');
 *   inject(ToastService).show('Network error', 'error', 6000);
 */
import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  duration: number;
  action?: { label: string, onClick: () => void };
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private _toasts = signal<Toast[]>([]);
  private _nextId = 0;
  private readonly MAX_TOASTS = 5;

  readonly toasts = this._toasts.asReadonly();

  /**
   * Show a toast notification.
   * @param message - The message to display
   * @param type - 'success' | 'error' | 'warning' | 'info'
   * @param duration - Auto-dismiss duration in ms (default 4000, 0 = no auto-dismiss)
   * @param action - Optional interactive button configuration
   */
  show(message: string, type: ToastType = 'info', duration: number = 4000, action?: { label: string, onClick: () => void }): void {
    const id = this._nextId++;

    const toast: Toast = { id, message, type, duration, action };

    this._toasts.update(list => {
      const updated = [...list, toast];
      // FIFO eviction if over max
      if (updated.length > this.MAX_TOASTS) {
        return updated.slice(updated.length - this.MAX_TOASTS);
      }
      return updated;
    });

    // Auto-dismiss
    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
  }

  /** Convenience shortcuts */
  success(message: string, duration?: number): void {
    this.show(message, 'success', duration);
  }

  error(message: string, duration?: number): void {
    this.show(message, 'error', duration ?? 6000);
  }

  warning(message: string, duration?: number): void {
    this.show(message, 'warning', duration ?? 5000);
  }

  info(message: string, duration?: number): void {
    this.show(message, 'info', duration);
  }

  /** Remove a specific toast by ID */
  dismiss(id: number): void {
    this._toasts.update(list => list.filter(t => t.id !== id));
  }

  /** Clear all toasts */
  clearAll(): void {
    this._toasts.set([]);
  }
}
