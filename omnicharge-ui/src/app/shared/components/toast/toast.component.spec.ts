import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ToastComponent } from './toast.component';
import { ToastService, Toast } from '../../../core/services/toast.service';

describe('ToastComponent', () => {
  let component: ToastComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .overrideComponent(ToastComponent, { set: { template: '<div></div>', imports: [], schemas: [NO_ERRORS_SCHEMA] } })
    .compileComponents();

    const fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getToastClasses()', () => {
    const makeToast = (type: string): Toast => ({ id: 1, message: 'test', type: type as any, duration: 0 });

    it('should return success classes', () => {
      expect(component.getToastClasses(makeToast('success'))).toContain('accent-emerald');
    });

    it('should return error classes', () => {
      expect(component.getToastClasses(makeToast('error'))).toContain('accent-rose');
    });

    it('should return warning classes', () => {
      expect(component.getToastClasses(makeToast('warning'))).toContain('accent-amber');
    });

    it('should return info classes', () => {
      expect(component.getToastClasses(makeToast('info'))).toContain('omni');
    });

    it('should return default classes for unknown type', () => {
      expect(component.getToastClasses(makeToast('unknown'))).toContain('text-white');
    });
  });
});
