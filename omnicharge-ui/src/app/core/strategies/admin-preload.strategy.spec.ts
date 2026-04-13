import { TestBed } from '@angular/core/testing';
import { AdminPreloadStrategy } from './admin-preload.strategy';
import { AuthService } from '../services/auth.service';
import { Route } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';

describe('AdminPreloadStrategy', () => {
  let strategy: AdminPreloadStrategy;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', [], {
      currentUser: signal(null)
    });

    TestBed.configureTestingModule({
      providers: [
        AdminPreloadStrategy,
        { provide: AuthService, useValue: authSpy }
      ]
    });
    strategy = TestBed.inject(AdminPreloadStrategy);
  });

  it('should be created', () => {
    expect(strategy).toBeTruthy();
  });

  it('should not preload if route has no data', () => {
    const route: Route = {};
    const loadFn = jasmine.createSpy('loadFn').and.returnValue(of('loaded'));
    
    strategy.preload(route, loadFn).subscribe(res => {
      expect(res).toBeNull();
      expect(loadFn).not.toHaveBeenCalled();
    });
  });

  it('should preload if route has preload=true flag', () => {
    const route: Route = { data: { preload: true } };
    const loadFn = jasmine.createSpy('loadFn').and.returnValue(of('loaded'));
    
    strategy.preload(route, loadFn).subscribe(res => {
      expect(res).toBe('loaded');
      expect(loadFn).toHaveBeenCalled();
    });
  });

  it('should not preload admin route if user is not admin', () => {
    (authSpy.currentUser as any).set({ role: 'ROLE_USER' });
    const route: Route = { data: { preloadAdmin: true } };
    const loadFn = jasmine.createSpy('loadFn').and.returnValue(of('loaded'));
    
    strategy.preload(route, loadFn).subscribe(res => {
      expect(res).toBeNull();
      expect(loadFn).not.toHaveBeenCalled();
    });
  });

  it('should not preload admin route if user is null', () => {
    (authSpy.currentUser as any).set(null);
    const route: Route = { data: { preloadAdmin: true } };
    const loadFn = jasmine.createSpy('loadFn').and.returnValue(of('loaded'));
    
    strategy.preload(route, loadFn).subscribe(res => {
      expect(res).toBeNull();
      expect(loadFn).not.toHaveBeenCalled();
    });
  });

  it('should preload admin route if user is admin', () => {
    (authSpy.currentUser as any).set({ role: 'ROLE_ADMIN' });
    const route: Route = { data: { preloadAdmin: true } };
    const loadFn = jasmine.createSpy('loadFn').and.returnValue(of('loaded'));
    
    strategy.preload(route, loadFn).subscribe(res => {
      expect(res).toBe('loaded');
      expect(loadFn).toHaveBeenCalled();
    });
  });
});
