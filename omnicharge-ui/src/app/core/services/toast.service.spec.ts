import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ToastService] });
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
    expect(service.toasts().length).toBe(0);
  });

  describe('show()', () => {
    it('should add a toast to the list', () => {
      service.show('Hello', 'info');
      expect(service.toasts().length).toBe(1);
      expect(service.toasts()[0].message).toBe('Hello');
      expect(service.toasts()[0].type).toBe('info');
    });

    it('should auto-dismiss after duration', fakeAsync(() => {
      service.show('Temp', 'info', 1000);
      expect(service.toasts().length).toBe(1);
      tick(1000);
      expect(service.toasts().length).toBe(0);
    }));

    it('should NOT auto-dismiss when duration is 0', fakeAsync(() => {
      service.show('Persistent', 'warning', 0);
      tick(10000);
      expect(service.toasts().length).toBe(1);
    }));

    it('should evict oldest toast when exceeding MAX_TOASTS (5)', () => {
      for (let i = 0; i < 6; i++) {
        service.show(`Toast ${i}`, 'info', 0);
      }
      expect(service.toasts().length).toBe(5);
      expect(service.toasts()[0].message).toBe('Toast 1'); // Toast 0 evicted
    });

    it('should support action parameter', () => {
      const actionFn = jasmine.createSpy('onClick');
      service.show('Act', 'info', 0, { label: 'Retry', onClick: actionFn });
      expect(service.toasts()[0].action?.label).toBe('Retry');
      service.toasts()[0].action?.onClick();
      expect(actionFn).toHaveBeenCalled();
    });
  });

  describe('convenience methods', () => {
    it('success() should add success toast with default duration', () => {
      service.success('OK');
      expect(service.toasts()[0].type).toBe('success');
    });

    it('error() should add error toast with 6000ms duration', () => {
      service.error('Fail');
      expect(service.toasts()[0].type).toBe('error');
      expect(service.toasts()[0].duration).toBe(6000);
    });

    it('warning() should add warning toast with 5000ms duration', () => {
      service.warning('Caution');
      expect(service.toasts()[0].type).toBe('warning');
      expect(service.toasts()[0].duration).toBe(5000);
    });

    it('info() should add info toast', () => {
      service.info('FYI');
      expect(service.toasts()[0].type).toBe('info');
    });
  });

  describe('dismiss()', () => {
    it('should remove a specific toast by ID', () => {
      service.show('A', 'info', 0);
      service.show('B', 'info', 0);
      const idToRemove = service.toasts()[0].id;
      service.dismiss(idToRemove);
      expect(service.toasts().length).toBe(1);
      expect(service.toasts()[0].message).toBe('B');
    });

    it('should do nothing if ID does not exist', () => {
      service.show('A', 'info', 0);
      service.dismiss(999);
      expect(service.toasts().length).toBe(1);
    });
  });

  describe('clearAll()', () => {
    it('should remove all toasts', () => {
      service.show('A', 'info', 0);
      service.show('B', 'error', 0);
      service.show('C', 'warning', 0);
      service.clearAll();
      expect(service.toasts().length).toBe(0);
    });
  });
});
