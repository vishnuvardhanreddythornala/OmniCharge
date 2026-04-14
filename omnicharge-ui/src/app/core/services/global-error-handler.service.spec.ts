import { NgZone } from '@angular/core';
import { GlobalErrorHandler } from './global-error-handler.service';
import { ToastService } from './toast.service';
import { HttpErrorResponse } from '@angular/common/http';

describe('GlobalErrorHandler', () => {
  let handler: GlobalErrorHandler;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let mockInjector: any;

  beforeEach(() => {
    toastSpy = jasmine.createSpyObj('ToastService', ['error', 'warning']);

    mockInjector = {
      get: jasmine.createSpy('get').and.returnValue(toastSpy)
    };

    const mockZone = {
      run: (fn: Function) => fn()
    } as any;

    handler = new GlobalErrorHandler(mockInjector, mockZone);
    spyOn(console, 'error');
    spyOn(console, 'warn');
  });

  it('should be created', () => {
    expect(handler).toBeTruthy();
  });

  it('should ignore NavigationCancel errors', () => {
    handler.handleError(new Error('NavigationCancel: Router cancelled'));
    expect(toastSpy.error).not.toHaveBeenCalled();
    expect(toastSpy.warning).not.toHaveBeenCalled();
  });

  it('should ignore NG04002 navigation errors', () => {
    handler.handleError(new Error('NG04002: some navigation cancel'));
    expect(toastSpy.error).not.toHaveBeenCalled();
  });

  it('should ignore HttpErrorResponse', () => {
    const httpError = new HttpErrorResponse({ status: 500 });
    handler.handleError(httpError);
    expect(toastSpy.error).not.toHaveBeenCalled();
  });

  it('should ignore errors with Http failure in the message', () => {
    handler.handleError(new Error('Http failure response for /api/test: 500 Internal Server Error'));
    expect(toastSpy.error).not.toHaveBeenCalled();
  });

  it('should show generic toast for uncaught exceptions', () => {
    handler.handleError(new TypeError('Cannot read property x of undefined'));
    expect(toastSpy.error).toHaveBeenCalledWith('An unexpected error occurred. Please try again in a moment.');
  });

  it('should fall back gracefully if ToastService injection fails', () => {
    mockInjector.get.and.throwError('Injector Error');
    handler.handleError(new Error('some generic error'));

    expect(console.error).toHaveBeenCalledWith('Could not display toast for error:', 'some generic error');
  });

  it('should log all errors to console', () => {
    handler.handleError(new Error('Test error'));
    expect(console.error).toHaveBeenCalled();
  });
});
