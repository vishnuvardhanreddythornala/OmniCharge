import { TestBed, fakeAsync, tick, flush, discardPeriodicTasks } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, AuthTokens, UserProfile } from './auth.service';
import { ToastService } from './toast.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  // Helper to create a valid JWT-like token
  function createMockToken(payload: Record<string, any>): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  }

  const tokenPayload = {
    userId: 1,
    role: 'ROLE_USER',
    isProfileComplete: true,
    isMobileVerified: true,
    exp: Math.floor(Date.now() / 1000) + 3600
  };

  const mockTokens: AuthTokens = {
    accessToken: createMockToken(tokenPayload),
    refreshToken: 'mock-refresh-token',
    isProfileComplete: true,
    isMobileVerified: true
  };

  const mockProfile: UserProfile = {
    id: 1,
    fullName: 'Test User',
    email: 'test@example.com',
    mobileNumber: '9876543210',
    role: 'ROLE_USER',
    authProvider: 'LOCAL'
  };

  beforeEach(() => {
    // Clear localStorage BEFORE creating the service so restoreSession() finds nothing
    localStorage.clear();

    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    toastSpy = jasmine.createSpyObj('ToastService', ['show']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('Initialization (restoreSession)', () => {
    it('should start unauthenticated if no tokens are present', () => {
      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()).toBeNull();
    });
  });

  describe('login()', () => {
    it('should initialize login via email/password successfully', fakeAsync(() => {
      const credentials = { email: 'admin@omnicharge.com', password: 'password123' };

      service.login(credentials).subscribe(res => {
        expect(res.success).toBeTrue();
        // It no longer authenticates fully, it should return requires2fa
        expect(res.data?.requires2fa).toBeTrue();
        expect(service.isAuthenticated()).toBeFalse(); // Still false until 2FA
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      req.flush({ success: true, message: 'OK', data: { requires2fa: true, email: 'admin@omnicharge.com' } });

      flush();
    }));

    it('should handle unsuccessful login response (success=false)', () => {
      service.login({ email: 'x', password: 'x' }).subscribe(res => {
        expect(res.success).toBeFalse();
        expect(service.isAuthenticated()).toBeFalse();
      });

      httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/login`)
        .flush({ success: false, message: 'Invalid', data: null as any });

      expect(service.isLoading()).toBeFalse();
    });

    it('should set isLoading to false when HTTP call errors', () => {
      service.login({ email: 'wrong@example.com', password: 'bad' }).subscribe({
        error: () => {
          expect(service.isLoading()).toBeFalse();
        }
      });

      httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/login`)
        .flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should handle 500 Server Error', () => {
      service.login({ email: 'x', password: 'x' }).subscribe({
        error: (err) => {
          expect(err.status).toBe(500);
        }
      });
      httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/login`)
        .error(new ProgressEvent('error'), { status: 500, statusText: 'Internal Server Error' });
    });
  });


  describe('logout()', () => {
    it('should clear tokens, reset state, and navigate to home', fakeAsync(() => {
      localStorage.setItem('omni_access_token', createMockToken(tokenPayload));
      localStorage.setItem('omni_refresh_token', 'ref');

      service.logout();
      tick();

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/logout`);
      expect(req.request.method).toBe('POST');
      req.flush({});

      expect(localStorage.getItem('omni_access_token')).toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    }));

    it('should navigate home even without a token', () => {
      service.logout();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  describe('Token Helpers', () => {
    it('getAccessToken should return token from localStorage', () => {
      localStorage.setItem('omni_access_token', 'test-token');
      expect(service.getAccessToken()).toBe('test-token');
    });

    it('getAccessToken should return null if not present', () => {
      expect(service.getAccessToken()).toBeNull();
    });

    it('getUserIdFromToken should decode the payload and return userId', () => {
      localStorage.setItem('omni_access_token', mockTokens.accessToken);
      expect(service.getUserIdFromToken()).toBe(1);
    });

    it('getUserIdFromToken should return null if token is malformed', () => {
      localStorage.setItem('omni_access_token', 'not-a-jwt');
      expect(service.getUserIdFromToken()).toBeNull();
    });

    it('getUserRoleFromToken should decode the payload and return role', () => {
      localStorage.setItem('omni_access_token', mockTokens.accessToken);
      expect(service.getUserRoleFromToken()).toBe('ROLE_USER');
    });

    it('getUserRoleFromToken should return null if no token', () => {
      expect(service.getUserRoleFromToken()).toBeNull();
    });
  });

  describe('Computed Signals', () => {
    it('isAdmin should return false for regular user', () => {
      expect(service.isAdmin()).toBeFalse();
    });

    it('userInitials should return emoji for unnamed user', () => {
      expect(service.userInitials()).toBe('👤');
    });
  });

  describe('Profile Operations', () => {
    it('updateProfile should PUT to /api/users/profile', () => {
      service.updateProfile({ fullName: 'New Name' }).subscribe();

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/users/profile`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ fullName: 'New Name' });
      req.flush({ success: true, message: 'OK', data: null as any });

      // loadProfile fires after success
      const loadReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/users/profile`);
      loadReq.flush({ success: true, message: 'OK', data: { ...mockProfile, fullName: 'New Name' } });

      // refreshToken also fires — but no refresh token in localStorage, so it returns synchronously
      // No HTTP call expected
    });

    it('changePassword should PUT to /api/users/change-password', () => {
      service.changePassword('oldpass', 'newpass').subscribe();
      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/users/change-password`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ currentPassword: 'oldpass', newPassword: 'newpass' });
      req.flush({ success: true, message: 'OK', data: null as any });
    });
  });

  describe('refreshToken()', () => {
    it('should return failure if no refresh token exists', () => {
      service.refreshToken().subscribe(res => {
        expect(res.success).toBeFalse();
        expect(res.message).toBe('No refresh token');
      });
    });

    it('should POST to refresh-token endpoint when token exists', fakeAsync(() => {
      localStorage.setItem('omni_refresh_token', 'my-refresh');
      service.refreshToken().subscribe(res => {
        expect(res.success).toBeTrue();
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/refresh-token`);
      expect(req.request.method).toBe('POST');
      req.flush({ success: true, message: 'OK', data: mockTokens });

      const profileReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/users/profile`);
      profileReq.flush({ success: true, message: 'OK', data: mockProfile });

      flush();
    }));
  });
});
