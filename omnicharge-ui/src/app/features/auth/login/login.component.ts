import { Component, signal, inject, AfterViewInit, OnDestroy, ViewChild, ElementRef, NgZone, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
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
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './login.component.html',
  styles: []
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private ngZone = inject(NgZone);
  private fb = inject(FormBuilder);

  @ViewChild('googleBtnContainer') googleBtnContainer!: ElementRef;

  viewMode = signal<LoginViewMode>('mobile');
  loadingAction = signal<'mobile' | 'otp' | 'email' | 'emailOtp' | 'admin' | 'adminOtp' | 'google' | ''>('');

  // Forms
  mobileForm = this.fb.group({
    mobileNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]]
  });

  emailForm = this.fb.group({
    publicEmail: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/)]]
  });

  adminForm = this.fb.group({
    adminEmail: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/)]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  // Mobile UI state
  showCountryDropdown = signal(false);
  selectedCountryCode = signal('+91');
  showPassword = signal(false);

  // Common OTP state
  otp = '';
  otpDigits = signal<string[]>(['', '', '', '', '', '']);
  otpIndexes = [0, 1, 2, 3, 4, 5];
  resendTimer = signal(0);
  private timerInterval: any;

  // Global Error & Success State
  errorMessage = signal('');
  successMessage = signal('');
  private errorTimeout: any;
  private successTimeout: any;

  displayError(msg: string = ''): void {
    this.errorMessage.set(msg);
    if (this.errorTimeout) clearTimeout(this.errorTimeout);
    if (msg) {
      this.errorTimeout = setTimeout(() => this.errorMessage.set(''), 5000);
    }
  }

  displaySuccess(msg: string = ''): void {
    this.successMessage.set(msg);
    if (this.successTimeout) clearTimeout(this.successTimeout);
    if (msg) {
      this.successTimeout = setTimeout(() => this.successMessage.set(''), 5000);
    }
  }

  // View Mode Switcher
  switchViewMode(mode: LoginViewMode): void {
    this.viewMode.set(mode);
    this.displayError();
    this.displaySuccess();
  }

  currentCountryCodes() { return COUNTRY_CODES; }

  getSelectedCountry() {
    return COUNTRY_CODES.find(c => c.code === this.selectedCountryCode()) || COUNTRY_CODES[0];
  }

  selectCountry(code: string) {
    this.selectedCountryCode.set(code);
    this.showCountryDropdown.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick() {
    if (this.showCountryDropdown()) this.showCountryDropdown.set(false);
  }

  ngOnInit(): void {
    if (this.route.snapshot.queryParams['method'] === 'admin') {
      this.viewMode.set('admin');
    }
  }

  ngAfterViewInit(): void {
    this.initGoogleSignIn();
  }

  getOtpString(): string {
    return this.otpDigits().join('');
  }

  onMobileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let val = input.value.replace(/[^0-9]/g, '');
    if (val.length > 10) val = val.substring(0, 10);
    this.mobileForm.get('mobileNumber')?.setValue(val, { emitEvent: false });
    input.value = val; // Synchronize DOM strictly to numeric
  }

  onOtpInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/[^0-9]/g, '');
    const digits = [...this.otpDigits()];
    digits[index] = value.charAt(0) || '';
    this.otpDigits.set(digits);
    this.otp = digits.join('');
    this.displayError(); // clear error when typing

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
    if (this.errorTimeout) clearTimeout(this.errorTimeout);
    if (this.successTimeout) clearTimeout(this.successTimeout);
  }

  private initGoogleSignIn(): void {
    const checkGoogleLoaded = setInterval(() => {
      if (typeof google !== 'undefined' && google.accounts && this.googleBtnContainer?.nativeElement) {
        clearInterval(checkGoogleLoaded);
        //google identity
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
      this.displayError();
      this.loadingAction.set('google');
      this.authService.googleAuth(idToken).subscribe({
        next: res => {
          if (res.success) this.handleSuccessfulNavigation();
          else this.displayError(res.message || 'Google Auth failed.');
          this.loadingAction.set('');
        },
        error: err => {
          this.displayError(err.error?.message || 'Google Auth encountered an error.');
          this.loadingAction.set('');
        }
      });
    });
  }

  submitOtp(): void {
    if (this.viewMode() === 'otp') {
      this.onVerifyMobileOtp();
    } else if (this.viewMode() === 'emailOtp') {
      this.onVerifyEmailOtp();
    } else if (this.viewMode() === 'adminOtp') {
      this.onVerifyAdmin2fa();
    }
  }

  // --- API CALLS ---

  onRequestOtp(): void {
    if (this.mobileForm.invalid || this.loadingAction() !== '') {
        this.mobileForm.markAllAsTouched();
        return;
    }
    this.displayError();
    this.loadingAction.set('mobile');

    const fullNumber = this.selectedCountryCode() + this.mobileForm.value.mobileNumber?.trim();
    this.authService.sendPublicMobileOtp(fullNumber).subscribe({  //API Communication
      next: res => {
        if (res.success) {
          this.switchViewMode('otp');
          this.displaySuccess('OTP sent successfully!');
          this.resetOtpInputs();
        } else {
          this.displayError(res.message || 'Failed to send OTP.');
        }
        this.loadingAction.set('');
      },
      error: err => {
        this.displayError(err.error?.message || 'Failed to send OTP. Please check your network.');
        this.loadingAction.set('');
      }
    });
  }

  onVerifyMobileOtp(): void {
    if (this.otp.length !== 6 || this.loadingAction() !== '') return;
    this.displayError();
    this.loadingAction.set('otp');

    const fullNumber = this.selectedCountryCode() + this.mobileForm.value.mobileNumber?.trim();
    this.authService.verifyPublicMobileOtp(fullNumber, this.otp).subscribe({
      next: res => {
        if (res.success) this.handleSuccessfulNavigation();
        else this.displayError(res.message || 'Invalid Request.');
        this.loadingAction.set('');
      },
      error: err => {
        this.displayError(err.error?.message || 'Verification failed. Incorrect OTP.');
        this.loadingAction.set('');
      }
    });
  }

  onRequestEmailOtp(): void {
    if (this.emailForm.invalid || this.loadingAction() !== '') {
        this.emailForm.markAllAsTouched();
        return;
    }
    this.displayError();
    this.loadingAction.set('email');

    const email = this.emailForm.value.publicEmail?.trim() || '';
    this.authService.sendEmailLoginOtp(email).subscribe({
      next: res => {
        if (res.success) {
          this.switchViewMode('emailOtp');
          this.displaySuccess('OTP sent successfully. Check your email.');
          this.resetOtpInputs();
        } else {
          this.displayError(res.message || 'Failed to send OTP.');
        }
        this.loadingAction.set('');
      },
      error: err => {
        this.displayError(err.error?.message || 'Failed to send OTP. Try again.');
        this.loadingAction.set('');
      }
    });
  }

  onVerifyEmailOtp(): void {
    if (this.otp.length !== 6 || this.loadingAction() !== '') return;
    this.displayError();
    this.loadingAction.set('emailOtp');

    const email = this.emailForm.value.publicEmail?.trim() || '';
    this.authService.verifyEmailLoginOtp(email, this.otp).subscribe({
      next: res => {
        if (res.success) this.handleSuccessfulNavigation();
        else this.displayError(res.message || 'Verification failed.');
        this.loadingAction.set('');
      },
      error: err => {
        this.displayError(err.error?.message || 'Verification failed. Incorrect OTP.');
        this.loadingAction.set('');
      }
    });
  }

  onAdminLogin(): void {
    if (this.adminForm.invalid || this.loadingAction() !== '') {
        this.adminForm.markAllAsTouched();
        return;
    }
    this.displayError();
    this.loadingAction.set('admin');

    const email = this.adminForm.value.adminEmail?.trim() || '';
    const password = this.adminForm.value.password || '';

    this.authService.login({ email, password }).subscribe({
      next: res => {
        if (res.success && res.data?.requires2fa) {
          this.switchViewMode('adminOtp');
          this.displaySuccess('Credentials verified. OTP sent to email.');
          this.resetOtpInputs();
        } else {
          this.displayError(res.message || 'Login failed.');
        }
        this.loadingAction.set('');
      },
      error: err => {
        this.displayError(err.error?.message || 'Invalid email or password.');
        this.loadingAction.set('');
      }
    });
  }

  onVerifyAdmin2fa(): void {
    if (this.otp.length !== 6 || this.loadingAction() !== '') return;
    this.displayError();
    this.loadingAction.set('adminOtp');

    const email = this.adminForm.value.adminEmail?.trim() || '';
    this.authService.verifyAdmin2fa(email, this.otp).subscribe({
      next: res => {
        if (res.success) this.handleSuccessfulNavigation();
        else this.displayError(res.message || 'Verification failed.');
        this.loadingAction.set('');
      },
      error: err => {
        this.displayError(err.error?.message || 'Verification failed. Incorrect OTP.');
        this.loadingAction.set('');
      }
    });
  }

  private resetOtpInputs(): void {
    this.otp = '';
    this.otpDigits.set(['', '', '', '', '', '']);
    this.startResendTimer();
  }

  private getReturnUrl(): string {
    let returnUrl = this.route.snapshot.queryParams['returnUrl'];
    if (!returnUrl) {
      returnUrl = this.authService.isAdmin() ? '/admin' : '/dashboard';
    }
    return returnUrl;
  }

  private handleSuccessfulNavigation(): void {
    this.router.navigateByUrl(this.getReturnUrl());
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

  get mobileControl() { return this.mobileForm.get('mobileNumber'); }
  get emailControl() { return this.emailForm.get('publicEmail'); }
  get adminEmailControl() { return this.adminForm.get('adminEmail'); }
  get adminPasswordControl() { return this.adminForm.get('password'); }
}
