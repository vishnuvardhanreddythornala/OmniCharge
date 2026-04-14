import { ComponentFixture, TestBed, fakeAsync, tick, flush, discardPeriodicTasks } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let activatedRouteStub: any;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', [
      'login', 'sendPublicMobileOtp', 'verifyPublicMobileOtp', 
      'sendEmailLoginOtp', 'verifyEmailLoginOtp', 
      'verifyAdmin2fa', 'googleAuth'
    ], {
      isAdmin: signal(false),
      isLoading: signal(false)
    });

    routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);

    activatedRouteStub = {
      snapshot: { queryParams: {} }
    };

    (authSpy.isAdmin as any).set(false);
    (authSpy.isLoading as any).set(false);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    
    // Mock google global
    (window as any).google = { 
        accounts: { 
            id: { 
                initialize: jasmine.createSpy('initialize'), 
                renderButton: jasmine.createSpy('renderButton'), 
                prompt: jasmine.createSpy('prompt') 
            } 
        } 
    };

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    delete (window as any).google;
  });

  describe('Initialization', () => {
    it('should create the component with real behavioral assertions', () => {
      expect(component).toBeTruthy();
      expect(component.viewMode()).toBe('mobile');
      expect(component.mobileForm).toBeTruthy();
      expect(component.emailForm).toBeTruthy();
      expect(component.adminForm).toBeTruthy();
    });

    it('should set mode to admin if method=admin in query route', () => {
      activatedRouteStub.snapshot.queryParams['method'] = 'admin';
      component.ngOnInit();
      expect(component.viewMode()).toBe('admin');
    });
  });

  describe('Admin Login Form', () => {
    it('should not call login if form is invalid', () => {
      component.adminForm.patchValue({ adminEmail: '', password: '' });
      component.onAdminLogin();
      expect(authSpy.login).not.toHaveBeenCalled();
      expect(component.adminForm.touched).toBeTrue();
    });

    it('should call login and transition to adminOtp state on success', () => {
      component.adminForm.patchValue({ adminEmail: 'admin@omni.com', password: 'password123' });
      spyOn(component, 'displaySuccess');
      spyOn(component, 'switchViewMode');
      
      authSpy.login.and.returnValue(of({ success: true, message: 'OK', data: { requires2fa: true } as any }));
      
      component.onAdminLogin();
      
      expect(authSpy.login).toHaveBeenCalledWith({ email: 'admin@omni.com', password: 'password123' });
      expect(component.switchViewMode).toHaveBeenCalledWith('adminOtp');
      expect(component.displaySuccess).toHaveBeenCalledWith('Credentials verified. OTP sent to email.');
    });

    it('should display error if admin login response is unsuccessful', () => {
      component.adminForm.patchValue({ adminEmail: 'admin@omni.com', password: 'badpassword' });
      spyOn(component, 'displayError');
      
      authSpy.login.and.returnValue(of({ success: false, message: 'Invalid Credentials', data: null as any }));
      
      component.onAdminLogin();
      
      expect(component.displayError).toHaveBeenCalledWith('Invalid Credentials');
    });

    it('should handle admin network/500 failures gracefully', () => {
      component.adminForm.patchValue({ adminEmail: 'admin@omni.com', password: 'password123' });
      authSpy.login.and.returnValue(throwError(() => ({ error: { message: 'Server down' } })));
      
      component.onAdminLogin();
      
      expect(component.errorMessage()).toBe('Server down');
      expect(component.loadingAction()).toBe(''); // Ensure loading stops
    });
  });

  describe('Mobile OTP Request', () => {
    it('should block if mobile form invalid', () => {
      component.mobileForm.patchValue({ mobileNumber: '123' }); // invalid length
      component.onRequestOtp();
      expect(authSpy.sendPublicMobileOtp).not.toHaveBeenCalled();
    });

    it('should send mobile OTP with correct country code and transition to OTP view', () => {
      component.mobileForm.patchValue({ mobileNumber: '9876543210' });
      component.selectedCountryCode.set('+1');
      authSpy.sendPublicMobileOtp.and.returnValue(of({ success: true, message: 'Sent', data: null }));
      
      component.onRequestOtp();
      
      expect(authSpy.sendPublicMobileOtp).toHaveBeenCalledWith('+19876543210');
      expect(component.viewMode()).toBe('otp');
    });

    it('should handle mobile OTP network errors safely', () => {
      component.mobileForm.patchValue({ mobileNumber: '9876543210' });
      authSpy.sendPublicMobileOtp.and.returnValue(throwError(() => ({ error: { message: 'SMS Gateway Error' } })));
      
      component.onRequestOtp();
      
      expect(component.errorMessage()).toBe('SMS Gateway Error');
    });
  });

  describe('OTP Validations', () => {
    it('should prevent submission if OTP is less than 6 digits', () => {
      component.viewMode.set('otp');
      component.otp = '123';
      component.onVerifyMobileOtp();
      expect(authSpy.verifyPublicMobileOtp).not.toHaveBeenCalled();
    });

    it('should navigate to dashboard upon successful mobile OTP verification', () => {
      component.mobileForm.patchValue({ mobileNumber: '9876543210' });
      component.selectedCountryCode.set('+91');
      component.otp = '123456';
      
      authSpy.verifyPublicMobileOtp.and.returnValue(of({ success: true, message: '', data: {} as any }));
      (authSpy.isAdmin as any).set(false);
      
      component.onVerifyMobileOtp();
      
      expect(authSpy.verifyPublicMobileOtp).toHaveBeenCalledWith('+919876543210', '123456');
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/dashboard'); // returnUrl fallback
    });

    it('should navigate back to proper returnUrl if query param exists', () => {
      activatedRouteStub.snapshot.queryParams['returnUrl'] = '/checkout/123';
      component.mobileForm.patchValue({ mobileNumber: '9876543210' });
      component.otp = '123456';
      
      authSpy.verifyPublicMobileOtp.and.returnValue(of({ success: true, message: '', data: {} as any }));
      component.onVerifyMobileOtp();
      
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/checkout/123');
    });

    it('should report incorrect mobile OTP securely', () => {
      component.mobileForm.patchValue({ mobileNumber: '9876543210' });
      component.otp = '000000';
      authSpy.verifyPublicMobileOtp.and.returnValue(throwError(() => ({ error: { message: 'Wrong OTP' } })));
      
      component.onVerifyMobileOtp();
      
      expect(component.errorMessage()).toBe('Wrong OTP');
      expect(component.loadingAction()).toBe('');
    });
  });

  describe('Email Flow', () => {
    it('should send email OTP and transition properly', () => {
      component.emailForm.patchValue({ publicEmail: 'test@example.com' });
      authSpy.sendEmailLoginOtp.and.returnValue(of({ success: true, message: 'Sent', data: null }));
      
      component.onRequestEmailOtp();
      
      expect(authSpy.sendEmailLoginOtp).toHaveBeenCalledWith('test@example.com');
      expect(component.viewMode()).toBe('emailOtp');
    });

    it('should fail validation on invalid email address', () => {
      component.emailForm.patchValue({ publicEmail: 'notanemail' });
      component.onRequestEmailOtp();
      expect(authSpy.sendEmailLoginOtp).not.toHaveBeenCalled();
      expect(component.emailForm.touched).toBeTrue();
    });

    it('should verify email OTP and navigate to dashboard', () => {
      component.emailForm.patchValue({ publicEmail: 'test@example.com' });
      component.otp = '123456';
      authSpy.verifyEmailLoginOtp.and.returnValue(of({ success: true, message: '', data: {} as any }));
      (authSpy.isAdmin as any).set(false);
      
      component.onVerifyEmailOtp();
      
      expect(authSpy.verifyEmailLoginOtp).toHaveBeenCalledWith('test@example.com', '123456');
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/dashboard');
    });
  });

  describe('Admin 2FA Form', () => {
    it('should verify 2FA and route to admin on success', () => {
      component.adminForm.patchValue({ adminEmail: 'admin@omni.com' });
      component.viewMode.set('adminOtp');
      component.otp = '654321';
      
      authSpy.verifyAdmin2fa.and.returnValue(of({ success: true, message: 'Valid', data: {} as any }));
      (authSpy.isAdmin as any).set(true);
      
      component.onVerifyAdmin2fa();
      
      expect(authSpy.verifyAdmin2fa).toHaveBeenCalledWith('admin@omni.com', '654321');
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/admin');
    });

    it('should catch 2FA verification errors', () => {
      component.adminForm.patchValue({ adminEmail: 'admin@omni.com' });
      component.otp = '111111';
      authSpy.verifyAdmin2fa.and.returnValue(of({ success: false, message: 'Expired OTP', data: {} as any }));
      
      component.onVerifyAdmin2fa();
      expect(component.errorMessage()).toBe('Expired OTP');
    });

    it('should handle admin 2FA network error', () => {
      component.adminForm.patchValue({ adminEmail: 'admin@omni.com' });
      component.otp = '111111';
      authSpy.verifyAdmin2fa.and.returnValue(throwError(() => ({ error: { message: 'Network error' } })));
      
      component.onVerifyAdmin2fa();
      expect(component.errorMessage()).toBe('Network error');
      expect(component.loadingAction()).toBe('');
    });
  });

  describe('OTP Input Handlers', () => {
    it('getOtpString should join digits', () => {
      component.otpDigits.set(['1', '2', '3', '4', '5', '6']);
      expect(component.getOtpString()).toBe('123456');
    });

    it('onOtpInput should set digit', () => {
      const event = { target: { value: '5' } } as any;
      component.onOtpInput(event, 0);
      expect(component.otpDigits()[0]).toBe('5');
    });

    it('onOtpKeydown should clear current digit on Backspace', () => {
      component.otpDigits.set(['1', '2', '', '', '', '']);
      component.onOtpKeydown(new KeyboardEvent('keydown', { key: 'Backspace' }), 1);
      expect(component.otpDigits()[1]).toBe('');
    });

    it('onOtpPaste should populate digits', () => {
      const clipboardEvent = new Event('paste') as any;
      clipboardEvent.clipboardData = { getData: () => '123456' };
      clipboardEvent.preventDefault = () => {};
      component.onOtpPaste(clipboardEvent);
      expect(component.otp).toBe('123456');
    });
  });

  describe('submitOtp dispatcher', () => {
    it('should call onVerifyMobileOtp when viewMode is otp', () => {
      component.viewMode.set('otp');
      spyOn(component, 'onVerifyMobileOtp');
      component.submitOtp();
      expect(component.onVerifyMobileOtp).toHaveBeenCalled();
    });

    it('should call onVerifyEmailOtp when viewMode is emailOtp', () => {
      component.viewMode.set('emailOtp');
      spyOn(component, 'onVerifyEmailOtp');
      component.submitOtp();
      expect(component.onVerifyEmailOtp).toHaveBeenCalled();
    });

    it('should call onVerifyAdmin2fa when viewMode is adminOtp', () => {
      component.viewMode.set('adminOtp');
      spyOn(component, 'onVerifyAdmin2fa');
      component.submitOtp();
      expect(component.onVerifyAdmin2fa).toHaveBeenCalled();
    });
  });

  describe('displayError and displaySuccess', () => {
    it('displayError should set error and auto-clear', fakeAsync(() => {
      component.displayError('Test error');
      expect(component.errorMessage()).toBe('Test error');
      tick(5000);
      expect(component.errorMessage()).toBe('');
      discardPeriodicTasks();
    }));

    it('displaySuccess should set message and auto-clear', fakeAsync(() => {
      component.displaySuccess('Success msg');
      expect(component.successMessage()).toBe('Success msg');
      tick(5000);
      expect(component.successMessage()).toBe('');
      discardPeriodicTasks();
    }));

    it('displayError with empty string should clear', () => {
      component.errorMessage.set('old');
      component.displayError('');
      expect(component.errorMessage()).toBe('');
    });
  });

  describe('Country Code', () => {
    it('selectCountry should set code and close dropdown', () => {
      component.showCountryDropdown.set(true);
      component.selectCountry('+44');
      expect(component.selectedCountryCode()).toBe('+44');
      expect(component.showCountryDropdown()).toBeFalse();
    });

    it('getSelectedCountry should return correct country', () => {
      component.selectedCountryCode.set('+1');
      expect(component.getSelectedCountry().label).toBe('United States');
    });

    it('onDocumentClick should close dropdown if open', () => {
      component.showCountryDropdown.set(true);
      component.onDocumentClick();
      expect(component.showCountryDropdown()).toBeFalse();
    });

    it('currentCountryCodes should return list', () => {
      expect(component.currentCountryCodes().length).toBeGreaterThan(0);
    });
  });

  describe('onMobileInput', () => {
    it('should strip non-numeric chars and limit to 10', () => {
      const input = document.createElement('input');
      input.value = '12abc345678901234';
      component.onMobileInput({ target: input } as any);
      expect(input.value).toBe('1234567890');
    });
  });

  describe('switchViewMode', () => {
    it('should switch view and clear messages', () => {
      component.errorMessage.set('old');
      component.switchViewMode('email');
      expect(component.viewMode()).toBe('email');
      expect(component.errorMessage()).toBe('');
    });
  });

  describe('Form getters', () => {
    it('mobileControl', () => {
      expect(component.mobileControl).toBe(component.mobileForm.get('mobileNumber'));
    });
    it('emailControl', () => {
      expect(component.emailControl).toBe(component.emailForm.get('publicEmail'));
    });
    it('adminEmailControl', () => {
      expect(component.adminEmailControl).toBe(component.adminForm.get('adminEmail'));
    });
    it('adminPasswordControl', () => {
      expect(component.adminPasswordControl).toBe(component.adminForm.get('password'));
    });
  });

  describe('Email OTP Error Paths', () => {
    it('should handle email OTP send failure', () => {
      component.emailForm.patchValue({ publicEmail: 'test@example.com' });
      authSpy.sendEmailLoginOtp.and.returnValue(of({ success: false, message: 'Rate limit', data: null }));
      component.onRequestEmailOtp();
      expect(component.errorMessage()).toBe('Rate limit');
    });

    it('should handle email OTP send network error', () => {
      component.emailForm.patchValue({ publicEmail: 'test@example.com' });
      authSpy.sendEmailLoginOtp.and.returnValue(throwError(() => ({ error: { message: 'Network fail' } })));
      component.onRequestEmailOtp();
      expect(component.errorMessage()).toBe('Network fail');
    });

    it('should not submit email OTP if otp is short', () => {
      component.otp = '123';
      component.onVerifyEmailOtp();
      expect(authSpy.verifyEmailLoginOtp).not.toHaveBeenCalled();
    });

    it('should handle email OTP verify failure', () => {
      component.emailForm.patchValue({ publicEmail: 'test@example.com' });
      component.otp = '123456';
      authSpy.verifyEmailLoginOtp.and.returnValue(of({ success: false, message: 'Invalid OTP', data: null as any }));
      component.onVerifyEmailOtp();
      expect(component.errorMessage()).toBe('Invalid OTP');
    });

    it('should handle email OTP verify network error', () => {
      component.emailForm.patchValue({ publicEmail: 'test@example.com' });
      component.otp = '123456';
      authSpy.verifyEmailLoginOtp.and.returnValue(throwError(() => ({ error: { message: 'Server error' } })));
      component.onVerifyEmailOtp();
      expect(component.errorMessage()).toBe('Server error');
    });
  });

  describe('Mobile OTP error paths', () => {
    it('should display error on unsuccessful verify', () => {
      component.mobileForm.patchValue({ mobileNumber: '9876543210' });
      component.otp = '123456';
      authSpy.verifyPublicMobileOtp.and.returnValue(of({ success: false, message: 'Expired', data: null as any }));
      component.onVerifyMobileOtp();
      expect(component.errorMessage()).toBe('Expired');
    });

    it('should display error on unsuccessful send', () => {
      component.mobileForm.patchValue({ mobileNumber: '9876543210' });
      authSpy.sendPublicMobileOtp.and.returnValue(of({ success: false, message: 'Blocked', data: null }));
      component.onRequestOtp();
      expect(component.errorMessage()).toBe('Blocked');
    });
  });
});

