import { Component, signal, inject, AfterViewInit, OnDestroy, ViewChild, ElementRef, NgZone, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

declare const google: any;

export type LoginViewMode = 'mobile' | 'otp' | 'email' | 'emailOtp' | 'admin' | 'adminOtp';

export const COUNTRY_CODES = [
  { code: '+91', flag: '🇮🇳', label: 'India' },
  { code: '+1', flag: '🇺🇸', label: 'United States' },
  { code: '+44', flag: '🇬🇧', label: 'United Kingdom' },
  { code: '+61', flag: '🇦🇺', label: 'Australia' },
  { code: '+971', flag: '🇦🇪', label: 'UAE' }
];

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styles: []
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private ngZone = inject(NgZone);

  @ViewChild('googleBtnContainer') googleBtnContainer!: ElementRef;

  viewMode = signal<LoginViewMode>('mobile');
  loadingAction = signal<'mobile' | 'otp' | 'email' | 'emailOtp' | 'admin' | 'adminOtp' | 'google' | ''>('');

  // Mobile UI state
  showCountryDropdown = signal(false);
  selectedCountryCode = signal('+91');
  mobileNumber = '';
  
  // Email state
  publicEmail = '';
  
  // Admin Legacy UI state
  adminEmail = '';
  password = '';
  showPassword = signal(false);

  // Common OTP state
  otp = '';
  otpDigits = signal<string[]>(['', '', '', '', '', '']);
  otpIndexes = [0, 1, 2, 3, 4, 5];
  resendTimer = signal(0);
  private timerInterval: any;

  errorMessage = signal('');

  currentCountryCodes() {
    return COUNTRY_CODES;
  }

  getSelectedCountry() {
    return COUNTRY_CODES.find(c => c.code === this.selectedCountryCode()) || COUNTRY_CODES[0];
  }

  selectCountry(code: string) {
    this.selectedCountryCode.set(code);
    this.showCountryDropdown.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick() {
    if (this.showCountryDropdown()) {
      this.showCountryDropdown.set(false);
    }
  }

  ngOnInit(): void {
    const method = this.route.snapshot.queryParams['method'];
    if (method === 'admin') {
      this.viewMode.set('admin');
    }
  }

  ngAfterViewInit(): void {
    this.initGoogleSignIn();
  }

  getOtpString(): string {
    return this.otpDigits().join('');
  }

  onOtpInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/[^0-9]/g, '');
    const digits = [...this.otpDigits()];
    digits[index] = value.charAt(0) || '';
    this.otpDigits.set(digits);
    this.otp = digits.join('');

    if (value && index < 5) {
      const next = document.getElementById('otp-' + (index + 1)) as HTMLInputElement;
      if (next) next.focus();
    }
  }

  onOtpKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      const digits = [...this.otpDigits()];
      if (!digits[index] && index > 0) {
        const prev = document.getElementById('otp-' + (index - 1)) as HTMLInputElement;
        if (prev) {
          digits[index - 1] = '';
          this.otpDigits.set(digits);
          this.otp = digits.join('');
          prev.focus();
        }
      } else {
        digits[index] = '';
        this.otpDigits.set(digits);
        this.otp = digits.join('');
      }
    }
  }

  onOtpPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const paste = (event.clipboardData?.getData('text') || '').replace(/[^0-9]/g, '').substring(0, 6);
    if (paste.length > 0) {
      const digits = [...this.otpDigits()];
      for (let i = 0; i < 6; i++) {
        digits[i] = paste[i] || '';
      }
      this.otpDigits.set(digits);
      this.otp = digits.join('');
      const lastIdx = Math.min(paste.length, 6) - 1;
      const el = document.getElementById('otp-' + lastIdx) as HTMLInputElement;
      if (el) el.focus();
    }
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  private initGoogleSignIn(): void {
    const checkGoogleLoaded = setInterval(() => {
      if (typeof google !== 'undefined' && google.accounts && this.googleBtnContainer?.nativeElement) {
        clearInterval(checkGoogleLoaded);
        google.accounts.id.initialize({
          client_id: environment.googleClientId,
          callback: (response: any) => this.handleGoogleCredentialResponse(response),
          auto_select: false,
          cancel_on_tap_outside: true,
        });
        google.accounts.id.renderButton(
          this.googleBtnContainer.nativeElement,
          { type: 'standard', theme: 'outline', size: 'large', text: 'continue_with', shape: 'rectangular', width: 350 }
        );
      }
    }, 100);
    setTimeout(() => clearInterval(checkGoogleLoaded), 10000);
  }

  private handleGoogleCredentialResponse(response: any): void {
    const idToken = response.credential;
    if (!idToken) return;

    this.ngZone.run(() => {
      this.errorMessage.set('');
      this.loadingAction.set('google');
      this.authService.googleAuth(idToken).subscribe({
        next: res => {
          if (res.success) this.handleSuccessfulNavigation();
          else this.errorMessage.set(res.message || 'Google Login failed.');
          this.loadingAction.set('');
        },
        error: err => {
          this.errorMessage.set(err.error?.message || 'Google authentication failed.');
          this.loadingAction.set('');
        }
      });
    });
  }

  // --- MOBILE OTP ---

  onRequestOtp(): void {
    if (this.mobileNumber.length !== 10) return;
    this.errorMessage.set('');
    this.loadingAction.set('mobile');

    const fullNumber = this.selectedCountryCode() + this.mobileNumber;
    this.authService.sendPublicMobileOtp(fullNumber).subscribe({
      next: res => {
        if (res.success) {
          this.viewMode.set('otp');
          this.resetOtpInputs();
        } else {
          this.errorMessage.set(res.message);
        }
        this.loadingAction.set('');
      },
      error: err => {
        this.errorMessage.set(err.error?.message || 'Failed to send OTP.');
        this.loadingAction.set('');
      }
    });
  }

  onVerifyMobileOtp(): void {
    if (this.otp.length !== 6) return;
    this.errorMessage.set('');
    this.loadingAction.set('otp');

    const fullNumber = this.selectedCountryCode() + this.mobileNumber;
    this.authService.verifyPublicMobileOtp(fullNumber, this.otp).subscribe({
      next: res => {
        if (res.success) this.handleSuccessfulNavigation();
        else this.errorMessage.set(res.message);
        this.loadingAction.set('');
      },
      error: err => {
        this.errorMessage.set(err.error?.message || 'Verification failed.');
        this.loadingAction.set('');
      }
    });
  }

  // --- EMAIL OTP ---

  onRequestEmailOtp(): void {
    if (!this.publicEmail) return;
    this.errorMessage.set('');
    this.loadingAction.set('email');

    this.authService.sendEmailLoginOtp(this.publicEmail).subscribe({
      next: res => {
        if (res.success) {
          this.viewMode.set('emailOtp');
          this.resetOtpInputs();
        } else {
          this.errorMessage.set(res.message);
        }
        this.loadingAction.set('');
      },
      error: err => {
        this.errorMessage.set(err.error?.message || 'Failed to send OTP.');
        this.loadingAction.set('');
      }
    });
  }

  onVerifyEmailOtp(): void {
    if (this.otp.length !== 6) return;
    this.errorMessage.set('');
    this.loadingAction.set('emailOtp');

    this.authService.verifyEmailLoginOtp(this.publicEmail, this.otp).subscribe({
      next: res => {
        if (res.success) this.handleSuccessfulNavigation();
        else this.errorMessage.set(res.message);
        this.loadingAction.set('');
      },
      error: err => {
        this.errorMessage.set(err.error?.message || 'Verification failed.');
        this.loadingAction.set('');
      }
    });
  }

  // --- ADMIN LOGIN ---

  onAdminLogin(): void {
    if (!this.adminEmail || !this.password) return;
    this.errorMessage.set('');
    this.loadingAction.set('admin');

    this.authService.login({ email: this.adminEmail, password: this.password }).subscribe({
      next: res => {
        if (res.success && res.data?.requires2fa) {
          this.viewMode.set('adminOtp');
          this.resetOtpInputs();
        } else {
          this.errorMessage.set(res.message || 'Login failed.');
        }
        this.loadingAction.set('');
      },
      error: err => {
        this.errorMessage.set(err.error?.message || 'Invalid email or password.');
        this.loadingAction.set('');
      }
    });
  }

  onVerifyAdmin2fa(): void {
    if (this.otp.length !== 6) return;
    this.errorMessage.set('');
    this.loadingAction.set('adminOtp');

    this.authService.verifyAdmin2fa(this.adminEmail, this.otp).subscribe({
      next: res => {
        if (res.success) this.handleSuccessfulNavigation();
        else this.errorMessage.set(res.message);
        this.loadingAction.set('');
      },
      error: err => {
        this.errorMessage.set(err.error?.message || '2FA Verification failed.');
        this.loadingAction.set('');
      }
    });
  }

  private resetOtpInputs(): void {
    this.otp = '';
    this.otpDigits.set(['', '', '', '', '', '']);
    this.startResendTimer();
  }

  private handleSuccessfulNavigation(): void {
    let returnUrl = this.route.snapshot.queryParams['returnUrl'];
    if (!returnUrl) {
      returnUrl = this.authService.isAdmin() ? '/admin' : '/dashboard';
    }
    this.router.navigateByUrl(returnUrl);
  }

  private startResendTimer(): void {
    this.resendTimer.set(60);
    if (this.timerInterval) clearInterval(this.timerInterval);

    this.timerInterval = setInterval(() => {
      const current = this.resendTimer();
      if (current > 0) this.resendTimer.set(current - 1);
      else clearInterval(this.timerInterval);
    }, 1000);
  }

  triggerGoogleSignIn(): void {
    const container = this.googleBtnContainer?.nativeElement;
    if (container) {
      const btn = container.querySelector('[role="button"]') || container.querySelector('div[tabindex]') || container.querySelector('iframe');
      if (btn && btn instanceof HTMLElement) {
        btn.click();
        return;
      }
    }
    if (typeof google !== 'undefined' && google.accounts) {
      google.accounts.id.prompt();
    }
  }
}
