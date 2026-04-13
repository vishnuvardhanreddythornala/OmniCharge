/**
 * CanDeactivate Guard — Protects against accidental navigation when forms have unsaved changes.
 *
 * Usage:
 *  1. Implement HasUnsavedChanges interface in your component.
 *  2. Add canDeactivate: [canDeactivateGuard] to the route config.
 */
import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfirmDialogService } from '../../shared/components/confirm-dialog/confirm-dialog.service';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean | Observable<boolean>;
}

export const canDeactivateGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (component.hasUnsavedChanges && component.hasUnsavedChanges()) {
    const dialogService = inject(ConfirmDialogService);
    return dialogService.open({
      title: 'Unsaved Changes',
      message: 'You have unsaved changes. Are you sure you want to leave this page?',
      confirmLabel: 'Leave Page',
      cancelLabel: 'Stay'
    });
  }
  return true;
};
