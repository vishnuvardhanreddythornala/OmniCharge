import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { authGuard, adminGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  const mockRoute = {} as ActivatedRouteSnapshot;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'isAdmin']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  it('should allow authenticated non-admin users', () => {
    authSpy.isAuthenticated.and.returnValue(true);
    authSpy.isAdmin.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, { url: '/dashboard' } as RouterStateSnapshot));
    expect(result).toBeTrue();
  });

  it('should redirect admin users to /admin', () => {
    authSpy.isAuthenticated.and.returnValue(true);
    authSpy.isAdmin.and.returnValue(true);
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, { url: '/dashboard' } as RouterStateSnapshot));
    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('should redirect unauthenticated users to /login with returnUrl', () => {
    authSpy.isAuthenticated.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, { url: '/secret-page' } as RouterStateSnapshot));
    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/secret-page' } });
  });
});

describe('adminGuard', () => {
  let authSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  const mockRoute = {} as ActivatedRouteSnapshot;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'isAdmin']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  it('should allow authenticated admin users', () => {
    authSpy.isAuthenticated.and.returnValue(true);
    authSpy.isAdmin.and.returnValue(true);
    const result = TestBed.runInInjectionContext(() => adminGuard(mockRoute, { url: '/admin' } as RouterStateSnapshot));
    expect(result).toBeTrue();
  });

  it('should redirect unauthenticated users to /login', () => {
    authSpy.isAuthenticated.and.returnValue(false);
    authSpy.isAdmin.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => adminGuard(mockRoute, { url: '/admin/users' } as RouterStateSnapshot));
    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/admin/users' } });
  });

  it('should redirect authenticated non-admin users to /dashboard', () => {
    authSpy.isAuthenticated.and.returnValue(true);
    authSpy.isAdmin.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => adminGuard(mockRoute, { url: '/admin' } as RouterStateSnapshot));
    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
