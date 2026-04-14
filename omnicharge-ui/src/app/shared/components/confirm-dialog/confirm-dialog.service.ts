import { Injectable, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export interface ConfirmConfig {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private configSignal = signal<ConfirmConfig | null>(null);
  config = this.configSignal.asReadonly();

  private responseSubject: Subject<boolean> | null = null;

  open(config: ConfirmConfig): Observable<boolean> {
    this.configSignal.set({
      title: config.title,
      message: config.message,
      confirmLabel: config.confirmLabel || 'Confirm',
      cancelLabel: config.cancelLabel || 'Cancel'
    });
    
    this.responseSubject = new Subject<boolean>();
    return this.responseSubject.asObservable();
  }

  close(result: boolean) {
    if (this.responseSubject) {
      this.responseSubject.next(result);
      this.responseSubject.complete();
      this.responseSubject = null;
    }
    this.configSignal.set(null);
  }
}
