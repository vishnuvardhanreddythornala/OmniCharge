import { TestBed } from '@angular/core/testing';
import { ConfirmDialogService, ConfirmConfig } from './confirm-dialog.service';

describe('ConfirmDialogService', () => {
  let service: ConfirmDialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ConfirmDialogService] });
    service = TestBed.inject(ConfirmDialogService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
    expect(service.config()).toBeNull();
  });

  describe('open()', () => {
    it('should set config with provided values', () => {
      const config: ConfirmConfig = { title: 'Test', message: 'Are you sure?' };
      service.open(config);
      expect(service.config()?.title).toBe('Test');
      expect(service.config()?.message).toBe('Are you sure?');
      expect(service.config()?.confirmLabel).toBe('Confirm');
      expect(service.config()?.cancelLabel).toBe('Cancel');
    });

    it('should use custom button labels when provided', () => {
      service.open({ title: 'T', message: 'M', confirmLabel: 'Yes', cancelLabel: 'No' });
      expect(service.config()?.confirmLabel).toBe('Yes');
      expect(service.config()?.cancelLabel).toBe('No');
    });

    it('should return an observable', () => {
      const obs = service.open({ title: 'T', message: 'M' });
      expect(obs.subscribe).toBeDefined();
    });
  });

  describe('close()', () => {
    it('should emit true result when confirmed', (done) => {
      service.open({ title: 'T', message: 'M' }).subscribe(result => {
        expect(result).toBeTrue();
        done();
      });
      service.close(true);
    });

    it('should emit false result when cancelled', (done) => {
      service.open({ title: 'T', message: 'M' }).subscribe(result => {
        expect(result).toBeFalse();
        done();
      });
      service.close(false);
    });

    it('should clear config after close', () => {
      service.open({ title: 'T', message: 'M' });
      service.close(true);
      expect(service.config()).toBeNull();
    });

    it('should do nothing when called without open', () => {
      service.close(true); // Should not throw
      expect(service.config()).toBeNull();
    });
  });
});
