/**
 * AuthService — Manages JWT tokens, user session state, and auth API calls.
 *
 * Uses Angular Signals for reactive state management (modern alternative to NgRx
 * for mid-complexity apps). Components can read `authService.currentUser()` as
 * a Signal and the view auto-updates.
 */
import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of, map } from 'rxjs';
import { ToastService } from './toast.service';
import { environment } from '../../../environments/environment';

/* ── Interfaces ── */
export interface LoginRequest {
  email: string;
  password: string;
}

export interface AdminLoginInitResponse {
  requires2fa: boolean;
  email: string;
  message: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  isProfileComplete: boolean;
  isMobileVerified?: boolean;
}

export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  mobileNumber: string;
  role: string;
  authProvider: string;
  isMobileVerified?: boolean;
}

// Import from shared models and re-export for backward compatibility
import { ApiResponse } from '../models/api.models';
export { ApiResponse } from '../models/api.models';

/* ── Service ── */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = `${environment.apiBaseUrl}/api/auth`;
  private readonly USERS_API = `${environment.apiBaseUrl}/api/users`;

  // ── Reactive Signals ──
  private _currentUser = signal<UserProfile | null>(null);
  private _authRole = signal<string | null>(null);
  private _isAuthenticated = signal<boolean>(false);
  private _isProfileComplete = signal<boolean>(false);
  private _isMobileVerified = signal<boolean>(false);
  private _isLoading = signal<boolean>(false);
  private _toastService = inject(ToastService);
  private _expiryTimer: any;

  // Public computed signals for components
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly isProfileComplete = this._isProfileComplete.asReadonly();
  readonly isMobileVerified = this._isMobileVerified.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();

  readonly isAdmin = computed(() => {
    if (this._currentUser()?.role === 'ROLE_ADMIN') return true;
    return this._authRole() === 'ROLE_ADMIN';
  });
  readonly userInitials = computed(() => {
    const user = this._currentUser();
    const name = user?.fullName;
    if (!name || name.startsWith('User ')) {
      // Mobile-only user without a real name set — show a person icon via text
      return '👤';
    }
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  });

  constructor(private http: HttpClient, private router: Router) {
    // Restore session from localStorage on app init
    this.restoreSession();
  }

  /* ── Auth API Methods ── */

  login(credentials: LoginRequest): Observable<ApiResponse<AdminLoginInitResponse>> {
    this._isLoading.set(true);
    return this.http.post<ApiResponse<AdminLoginInitResponse>>(`${this.API}/login`, credentials).pipe(
      tap(() => this._isLoading.set(false)),
      catchError(err => {
        this._isLoading.set(false);
        throw err;
      })
    );
  }

  verifyAdmin2fa(email: string, otp: string): Observable<ApiResponse<AuthTokens>> {
    this._isLoading.set(true);
    return this.http.post<ApiResponse<AuthTokens>>(`${this.API}/admin/verify-2fa`, { email, otp }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeTokens(res.data);
          this._isAuthenticated.set(true);
          this._isProfileComplete.set(res.data.isProfileComplete);
          this._isMobileVerified.set(res.data.isMobileVerified ?? false);
          this.loadProfile();
        }
        this._isLoading.set(false);
      }),
      catchError(err => {
        this._isLoading.set(false);
        throw err;
      })
    );
  }

  sendEmailLoginOtp(email: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.API}/email/send-login-otp`, { email });
  }

  verifyEmailLoginOtp(email: string, otp: string): Observable<ApiResponse<AuthTokens>> {
    this._isLoading.set(true);
    return this.http.post<ApiResponse<AuthTokens>>(`${this.API}/email/verify-login-otp`, { email, otp }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeTokens(res.data);
          this._isAuthenticated.set(true);
          this._isProfileComplete.set(res.data.isProfileComplete);
          this._isMobileVerified.set(res.data.isMobileVerified ?? false);
          this.loadProfile();
        }
        this._isLoading.set(false);
      }),
      catchError(err => {
        this._isLoading.set(false);
        throw err;
      })
    );
  }

  googleAuth(idToken: string): Observable<ApiResponse<AuthTokens>> {
    this._isLoading.set(true);
    return this.http.post<ApiResponse<AuthTokens>>(`${this.API}/google`, { idToken }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeTokens(res.data);
          this._isAuthenticated.set(true);
          this._isProfileComplete.set(res.data.isProfileComplete);
          this._isMobileVerified.set(res.data.isMobileVerified ?? false);
          this.loadProfile();
        }
        this._isLoading.set(false);
      }),
      catchError(err => {
        this._isLoading.set(false);
        throw err;
      })
    );
  }

  sendPublicMobileOtp(mobileNumber: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.API}/mobile/send-otp`, { mobileNumber });
  }

  verifyPublicMobileOtp(mobileNumber: string, otp: string): Observable<ApiResponse<AuthTokens>> {
    this._isLoading.set(true);
    return this.http.post<ApiResponse<AuthTokens>>(`${this.API}/mobile/verify-otp`, { mobileNumber, otp }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeTokens(res.data);
          this._isAuthenticated.set(true);
          this._isProfileComplete.set(res.data.isProfileComplete);
          this._isMobileVerified.set(res.data.isMobileVerified ?? true);
          this.loadProfile();
        }
        this._isLoading.set(false);
      }),
      catchError(err => {
        this._isLoading.set(false);
        throw err;
      })
    );
  }

  // Legacy/Authenticated endpoints for changing mobile
  sendMobileOtp(mobileNumber: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.USERS_API}/mobile-otp/send`, { mobileNumber });
  }

  verifyMobileOtp(mobileNumber: string, otp: string): Observable<ApiResponse<AuthTokens>> {
    return this.http.post<ApiResponse<AuthTokens>>(`${this.USERS_API}/mobile-otp/verify`, { mobileNumber, otp }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeTokens(res.data);
          this._isMobileVerified.set(res.data.isMobileVerified ?? true);
          if (res.data.isProfileComplete !== undefined) {
             this._isProfileComplete.set(res.data.isProfileComplete);
          }
        }
      })
    );
  }

  refreshToken(): Observable<ApiResponse<AuthTokens>> {
    const refreshToken = localStorage.getItem('omni_refresh_token');
    if (!refreshToken) {
      return of({ success: false, message: 'No refresh token', data: null as any });
    }
    return this.http.post<ApiResponse<AuthTokens>>(`${this.API}/refresh-token`, { refreshToken }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeTokens(res.data);
          this._isAuthenticated.set(true);
          this._isProfileComplete.set(res.data.isProfileComplete);
          this._isMobileVerified.set(res.data.isMobileVerified ?? false);
          this.loadProfile();
        }
      }),
      catchError(() => {
        this.logout();
        return of({ success: false, message: 'Refresh failed', data: null as any });
      })
    );
  }

  logout(): void {
    const token = this.getAccessToken();
    const refreshToken = localStorage.getItem('omni_refresh_token');
    if (token) {
      // Fire and forget — notify backend to blacklist the JWT and refresh token
      this.http.post(`${this.API}/logout`, { refreshToken }, {
        headers: { Authorization: `Bearer ${token}` }
      }).subscribe({ error: () => {} });
    }
    this.clearSession();
    this.router.navigate(['/']);
  }

  /* ── Profile ── */

  loadProfile(): void {
    this.http.get<ApiResponse<UserProfile>>(`${this.USERS_API}/profile`).subscribe({
      next: res => {
        if (res.success && res.data) {
          this._currentUser.set(res.data);
        }
      },
      error: () => {} // Silently fail; interceptor handles 401
    });
  }

  updateProfile(data: { fullName: string }): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.USERS_API}/profile`, data).pipe(
      tap(res => {
        if (res.success) {
          this.loadProfile();
          // After profile update, refresh token to get isProfileComplete=true
          this.refreshToken().subscribe();
        }
      })
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.USERS_API}/change-password`, {
      currentPassword, newPassword
    });
  }

  /* ── Token Helpers ── */

  getAccessToken(): string | null {
    return localStorage.getItem('omni_access_token');
  }

  getUserIdFromToken(): number | null {
    const token = this.getAccessToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.userId;
    } catch {
      return null;
    }
  }

  getUserRoleFromToken(): string | null {
    const token = this.getAccessToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role;
    } catch {
      return null;
    }
  }

  private storeTokens(tokens: AuthTokens): void {
    localStorage.setItem('omni_access_token', tokens.accessToken);
    localStorage.setItem('omni_refresh_token', tokens.refreshToken);
    this._authRole.set(this.getUserRoleFromToken());
    this.setupExpiryWarning();
  }

  private setupExpiryWarning(): void {
    if (this._expiryTimer) clearTimeout(this._expiryTimer);
    const token = this.getAccessToken();
    if (!token) return;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp * 1000;
      const timeToExpiry = expiry - Date.now();
      const alertTime = timeToExpiry - 120000; // 2 minutes before expiry

      if (alertTime > 0) {
        this._expiryTimer = setTimeout(() => {
          this._toastService.show(
            'Your session will expire in 2 minutes.', 
            'warning', 
            0, // Don't auto-dismiss
            { 
               label: 'Stay Signed In', 
               onClick: () => this.refreshToken().subscribe()
            }
          );
        }, alertTime);
      }
    } catch {}
  }

  private clearSession(): void {
    if (this._expiryTimer) clearTimeout(this._expiryTimer);
    localStorage.removeItem('omni_access_token');
    localStorage.removeItem('omni_refresh_token');
    this._currentUser.set(null);
    this._authRole.set(null);
    this._isAuthenticated.set(false);
    this._isProfileComplete.set(false);
    this._isMobileVerified.set(false);
  }

  private restoreSession(): void {
    const token = this.getAccessToken();
    const refreshToken = localStorage.getItem('omni_refresh_token');

    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const expiry = payload.exp * 1000;
        
        // Check if token is valid with a 10s buffer
        if (Date.now() < expiry - 10000) {
          this._isAuthenticated.set(true);
          this._authRole.set(payload.role ?? null);
          this._isProfileComplete.set(payload.isProfileComplete ?? false);
          this._isMobileVerified.set(payload.isMobileVerified ?? false);
          this.setupExpiryWarning();
          setTimeout(() => this.loadProfile(), 0);
        } else if (refreshToken) {
          // Token expired, but refresh token exists -> Attempt silent refresh
          setTimeout(() => {
            this.refreshToken().subscribe({
              next: (res) => {
                if (!res.success) {
                  this.clearSession();
                }
              },
              error: () => this.clearSession()
            });
          }, 0);
        } else {
          this.clearSession();
        }
      } catch {
        this.clearSession();
      }
    } else if (refreshToken) {
       // Only refresh token exists, try refreshing
       setTimeout(() => {
         this.refreshToken().subscribe({
            next: (res) => {
               if (!res.success) {
                  this.clearSession();
               }
            },
            error: () => this.clearSession()
         });
       }, 0);
    }
  }
}
