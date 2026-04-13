import { TestBed } from '@angular/core/testing';
import { canDeactivateGuard, HasUnsavedChanges } from './can-deactivate.guard';
import { ConfirmDialogService } from '../../shared/components/confirm-dialog/confirm-dialog.service';
import { of } from 'rxjs';

describe('canDeactivateGuard', () => {
  let dialogSpy: jasmine.SpyObj<ConfirmDialogService>;

  beforeEach(() => {
    dialogSpy = jasmine.createSpyObj('ConfirmDialogService', ['open']);
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfirmDialogService, useValue: dialogSpy }
      ]
    });
  });

  it('should allow deactivation if component does not have hasUnsavedChanges method', () => {
    const component = {} as any;
    const result = TestBed.runInInjectionContext(() => canDeactivateGuard(component, null as any, null as any, null as any));
    expect(result).toBeTrue();
  });

  it('should allow deactivation if component hasUnsavedChanges returns false', () => {
    const component: HasUnsavedChanges = {
      hasUnsavedChanges: () => false
    };
    const result = TestBed.runInInjectionContext(() => canDeactivateGuard(component, null as any, null as any, null as any));
    expect(result).toBeTrue();
    expect(dialogSpy.open).not.toHaveBeenCalled();
  });

  it('should prompt user and return dialog result true if hasUnsavedChanges is true', () => {
    const component: HasUnsavedChanges = {
      hasUnsavedChanges: () => true
    };
    dialogSpy.open.and.returnValue(of(true));
    
    const result = TestBed.runInInjectionContext(() => canDeactivateGuard(component, null as any, null as any, null as any)) as any;
    
    result.subscribe((val: boolean) => expect(val).toBeTrue());
    
    expect(dialogSpy.open).toHaveBeenCalledWith({
      title: 'Unsaved Changes',
      message: 'You have unsaved changes. Are you sure you want to leave this page?',
      confirmLabel: 'Leave Page',
      cancelLabel: 'Stay'
    });
  });

  it('should prompt user and return dialog result false if hasUnsavedChanges is true', () => {
    const component: HasUnsavedChanges = {
      hasUnsavedChanges: () => true
    };
    dialogSpy.open.and.returnValue(of(false));
    
    const result = TestBed.runInInjectionContext(() => canDeactivateGuard(component, null as any, null as any, null as any)) as any;
    
    result.subscribe((val: boolean) => expect(val).toBeFalse());
  });
});
