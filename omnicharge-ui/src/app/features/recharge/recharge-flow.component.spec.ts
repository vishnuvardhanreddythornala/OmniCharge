import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { RechargeFlowComponent } from './recharge-flow.component';
import { OperatorService, Operator, Plan } from '../../core/services/operator.service';
import { RechargeService } from '../../core/services/recharge.service';
import { PaymentService, TransactionResponse } from '../../core/services/payment.service';
import { AuthService } from '../../core/services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';

describe('RechargeFlowComponent', () => {
  let component: RechargeFlowComponent;
  let fixture: ComponentFixture<RechargeFlowComponent>;
  let operatorSpy: jasmine.SpyObj<OperatorService>;
  let rechargeSpy: jasmine.SpyObj<RechargeService>;
  let paymentSpy: jasmine.SpyObj<PaymentService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockOperator: Operator = { id: 1, name: 'Airtel', code: 'AIRTEL', type: 'Prepaid', description: '', isActive: true };
  const mockPlan: Plan = { id: 1, planName: 'Test Plan', price: 100, validityDays: 30, category: 'ALL', dataLimit: '', callBenefit: '', smsBenefit: '', additionalBenefits: '', operatorId: 1 };

  beforeEach(async () => {
    operatorSpy = jasmine.createSpyObj('OperatorService',
      ['detectOperator', 'clearSelection', 'loadActiveOperators', 'setManualOperator'],
      {
        operators: signal([mockOperator]),
        selectedOperator: signal<Operator | null>(null),
        plans: signal([mockPlan]),
        isLoadingPlans: signal(false),
        isDetecting: signal(false),
        detectionFailed: signal(false),
        isManualOverride: signal(false)
      }
    );

    rechargeSpy = jasmine.createSpyObj('RechargeService', ['initiateRecharge', 'clearCurrentRecharge']);
    paymentSpy = jasmine.createSpyObj('PaymentService',
      ['processPayment', 'openRazorpayCheckout', 'confirmPayment', 'failPayment', 'resetPaymentState'],
      { paymentState: signal<'idle' | 'processing' | 'success' | 'failed'>('idle'), currentTransaction: signal<any>(null) }
    );
    authSpy = jasmine.createSpyObj('AuthService',
      ['isAuthenticated', 'currentUser', 'isMobileVerified', 'getUserIdFromToken', 'sendMobileOtp', 'verifyMobileOtp', 'loadProfile']
    );
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RechargeFlowComponent],
      providers: [
        { provide: OperatorService, useValue: operatorSpy },
        { provide: RechargeService, useValue: rechargeSpy },
        { provide: PaymentService, useValue: paymentSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParams: {} } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RechargeFlowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── INITIALIZATION ───────────────────────────────────────────────────────
  describe('Initialization & Deep Linking', () => {
    it('should initialize with empty state', () => {
      expect(component.mobileNumber).toBe('');
      expect(component.currentStep()).toBe('input');
      expect(operatorSpy.clearSelection).toHaveBeenCalled();
    });

    it('should pre-fill mobile number from query params and detect operator', () => {
      operatorSpy.detectOperator.and.returnValue(of({ success: true, message: 'OK', data: { operatorId: 1, operatorName: 'Airtel', operatorCode: 'ART', type: 'Prepaid' } as any }));
      const activatedRoute = TestBed.inject(ActivatedRoute);
      activatedRoute.snapshot.queryParams = { mobile: '9999999999' };
      
      component.ngOnInit();
      
      expect(component.mobileNumber).toBe('9999999999');
      expect(operatorSpy.detectOperator).toHaveBeenCalledWith('9999999999');
      expect(component.currentStep()).toBe('plans');
    });

    it('should handle operator override from query params', () => {
      operatorSpy.detectOperator.and.returnValue(of({ success: true, message: 'OK', data: { operatorId: 1, operatorName: 'Airtel', operatorCode: 'ART', type: 'Prepaid' } as any }));
      const activatedRoute = TestBed.inject(ActivatedRoute);
      activatedRoute.snapshot.queryParams = { mobile: '9999999999', operatorId: '1' };
      
      component.ngOnInit();
      
      expect(operatorSpy.detectOperator).toHaveBeenCalled();
      expect(operatorSpy.setManualOperator).toHaveBeenCalledWith(mockOperator as any);
      expect(component.currentStep()).toBe('plans');
    });
  });

  // ─── MOBILE INPUT & OPERATOR DETECTION ─────────────────────────────────────
  describe('Mobile Input and Operator Detection', () => {
    it('should format mobile number to digits only', () => {
      component.mobileNumber = '123abc456';
      component.onMobileInput();
      expect(component.mobileNumber).toBe('123456');
    });

    it('should trigger detectOperator when 10 digits are entered', () => {
      operatorSpy.detectOperator.and.returnValue(of({ success: true, message: 'OK', data: { operatorId: 1, operatorName: 'Airtel', operatorCode: 'ART', type: 'Prepaid' } as any }));
      component.mobileNumber = '9999999999';
      component.onMobileInput();
      expect(operatorSpy.detectOperator).toHaveBeenCalledWith('9999999999');
      expect(component.detectionError()).toBe('');
    });

    it('should set detection error if detectOperator fails', () => {
      operatorSpy.detectOperator.and.returnValue(throwError(() => new Error('API failed')));
      component.mobileNumber = '9999999999';
      component.onMobileInput();
      expect(component.detectionError()).toBe('Could not detect operator. Please try again.');
    });

    it('should clear selection when number is less than 10 digits', () => {
      component.mobileNumber = '999';
      component.onMobileInput();
      expect(operatorSpy.clearSelection).toHaveBeenCalled();
    });
  });

  // ─── PLAN SELECTION ───────────────────────────────────────────────────────
  describe('Plan Selection', () => {
    it('goToPlans should move to plans step if operator selected', () => {
      (operatorSpy.selectedOperator as any).set(mockOperator);
      component.goToPlans();
      expect(component.currentStep()).toBe('plans');
      expect(component.showOperatorDropdown()).toBeFalse();
    });

    it('goToPlans should not move if operator is not selected', () => {
      (operatorSpy.selectedOperator as any).set(null);
      component.goToPlans();
      expect(component.currentStep()).toBe('input'); // Default
    });

    it('selectPlan should toggle plan selection', () => {
      expect(component.selectedPlan()).toBeNull();
      component.selectPlan(mockPlan);
      expect(component.selectedPlan()).toEqual(mockPlan);
      
      // Select same plan again should unselect
      component.selectPlan(mockPlan);
      expect(component.selectedPlan()).toBeNull();
    });

    it('should filter plans by active category', () => {
      const plans = [
        { id: 1, category: 'DATA' },
        { id: 2, category: 'UNLIMITED' }
      ];
      (operatorSpy.plans as any).set(plans);
      
      component.activeCategory.set('ALL');
      expect(component.filteredPlans().length).toBe(2);

      component.activeCategory.set('DATA');
      expect(component.filteredPlans().length).toBe(1);
      expect(component.filteredPlans()[0].id).toBe(1);
    });
  });

  // ─── CHECKOUT LOGIC ───────────────────────────────────────────────────────
  describe('Checkout Initiation', () => {
    beforeEach(() => {
      (operatorSpy.selectedOperator as any).set(mockOperator);
      component.selectedPlan.set(mockPlan);
    });

    it('should require login if not authenticated', async () => {
      authSpy.isAuthenticated.and.returnValue(false);
      await component.onProceedToCheckout();
      expect(component.showLoginModal()).toBeTrue();
    });

    it('should require mobile verification if user has no mobile and is not verified', async () => {
      authSpy.isAuthenticated.and.returnValue(true);
      authSpy.currentUser.and.returnValue({} as any);
      authSpy.isMobileVerified.and.returnValue(false);
      
      await component.onProceedToCheckout();
      
      expect(component.showVerificationModal()).toBeTrue();
      expect(component.verificationStep()).toBe('MOBILE');
    });

    it('should proceed to payment flow if authed and mobile verified', async () => {
      authSpy.isAuthenticated.and.returnValue(true);
      authSpy.currentUser.and.returnValue({ mobileNumber: '9999999999' } as any);
      authSpy.isMobileVerified.and.returnValue(true);
      authSpy.getUserIdFromToken.and.returnValue(1);

      // Mock the entire successful payment flow
      rechargeSpy.initiateRecharge.and.returnValue(of({ success: true, data: { rechargeId: 'R1' } } as any));
      paymentSpy.processPayment.and.returnValue(of({ success: true, data: { transactionId: 'T1', status: 'PENDING' } } as any));
      paymentSpy.openRazorpayCheckout.and.resolveTo({ paymentId: 'P1', signature: 'SIG1' });
      paymentSpy.confirmPayment.and.returnValue(of({ success: true } as any));

      await component.onProceedToCheckout();

      expect(component.currentStep()).toBe('receipt');
      expect(rechargeSpy.initiateRecharge).toHaveBeenCalled();
      expect(paymentSpy.processPayment).toHaveBeenCalled();
      expect(paymentSpy.openRazorpayCheckout).toHaveBeenCalled();
      expect(paymentSpy.confirmPayment).toHaveBeenCalled();
    });

    it('should catch payment flow errors and call failPayment', async () => {
      authSpy.isAuthenticated.and.returnValue(true);
      authSpy.currentUser.and.returnValue({ mobileNumber: '9999999999' } as any);
      authSpy.getUserIdFromToken.and.returnValue(1);

      rechargeSpy.initiateRecharge.and.returnValue(of({ success: true, data: { rechargeId: 'R1' } } as any));
      paymentSpy.processPayment.and.returnValue(of({ success: true, data: { transactionId: 'T1', status: 'PENDING' } } as any));
      paymentSpy.openRazorpayCheckout.and.rejectWith(new Error('User closed razorpay'));
      paymentSpy.failPayment.and.returnValue(of({ success: true } as any)); // Mock failPayment success

      await component.onProceedToCheckout();

      expect(component.currentStep()).toBe('receipt');
      expect(paymentSpy.failPayment).toHaveBeenCalledWith('T1', 'User closed razorpay');
      expect(component.failureReason()).toBe('User closed razorpay');
    });
  });

  // ─── LOGIN & OTP FLOWS ─────────────────────────────────────────────────────
  describe('Login & OTP Flow', () => {
    it('onConfirmLogin should navigate to login page with return url', () => {
      component.mobileNumber = '9999999999';
      component.onConfirmLogin();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/recharge?mobile=9999999999' } });
      expect(component.showLoginModal()).toBeFalse();
    });

    it('onCancelLogin should dismiss the login modal', () => {
      component.showLoginModal.set(true);
      component.onCancelLogin();
      expect(component.showLoginModal()).toBeFalse();
    });

    it('closeVerificationModal should close the modal', () => {
      component.showVerificationModal.set(true);
      component.closeVerificationModal();
      expect(component.showVerificationModal()).toBeFalse();
    });

    it('resetOtp should clear OTP digits', () => {
      component.verificationOtpDigits = ['1', '2', '3', '4', '5', '6'];
      component.resetOtp();
      expect(component.verificationOtpDigits).toEqual(['', '', '', '', '', '']);
    });

    it('onOtpInput should focus next input on valid digit', () => {
      document.body.innerHTML = `
        <input id="otp-input-0" type="text" />
        <input id="otp-input-1" type="text" />
      `;
      const nextSpy = spyOn(document.getElementById('otp-input-1') as HTMLInputElement, 'focus');
      component.onOtpInput(0, { target: { value: '5' } });
      expect(nextSpy).toHaveBeenCalled();
    });

    it('onOtpKeydown should focus prev input and clear on Backspace', () => {
      document.body.innerHTML = `
        <input id="otp-input-0" type="text" />
        <input id="otp-input-1" type="text" />
      `;
      component.verificationOtpDigits = ['1', '', '', '', '', ''];
      const prevSpy = spyOn(document.getElementById('otp-input-0') as HTMLInputElement, 'focus');
      component.onOtpKeydown(1, new KeyboardEvent('keydown', { key: 'Backspace' }));
      expect(prevSpy).toHaveBeenCalled();
      expect(component.verificationOtpDigits[0]).toBe('');
    });

    it('onOtpPaste should populate digits and focus last input', () => {
      document.body.innerHTML = `
        <input id="otp-input-0" type="text" />
        <input id="otp-input-1" type="text" />
        <input id="otp-input-2" type="text" />
        <input id="otp-input-3" type="text" />
        <input id="otp-input-4" type="text" />
        <input id="otp-input-5" type="text" />
      `;
      const focusSpy = spyOn(document.getElementById('otp-input-5') as HTMLInputElement, 'focus');
      
      const clipboardEvent = new Event('paste') as any;
      clipboardEvent.clipboardData = { getData: () => '123456' };
      
      component.onOtpPaste(clipboardEvent);
      expect(component.verificationOtpDigits).toEqual(['1', '2', '3', '4', '5', '6']);
      expect(focusSpy).toHaveBeenCalled();
    });

    it('requestVerificationOtp should validate mobile before sending', async () => {
      component.verificationMobileInput = '123';
      await component.requestVerificationOtp();
      expect(component.verificationError()).toBe('Enter a valid 10-digit number');
      expect(authSpy.sendMobileOtp).not.toHaveBeenCalled();
    });

    it('verifyOtpAndProceed should call verification API and continue flow', async () => {
      component.verificationOtpDigits = ['1', '2', '3', '4', '5', '6'];
      component.verificationMobileInput = '9999999999';
      
      authSpy.verifyMobileOtp.and.returnValue(of({ success: true } as any));
      authSpy.getUserIdFromToken.and.returnValue(1);
      authSpy.currentUser.and.returnValue({ email: 'test@test.com' } as any);
      
      (operatorSpy.selectedOperator as any).set(mockOperator);
      component.selectedPlan.set(mockPlan);

      // Prevent actual payment flow from doing network requests in this test
      rechargeSpy.initiateRecharge.and.returnValue(throwError(() => new Error('Mock fail')));

      await component.verifyOtpAndProceed();

      expect(authSpy.verifyMobileOtp).toHaveBeenCalledWith('+919999999999', '123456');
      expect(component.showVerificationModal()).toBeFalse();
      expect(authSpy.loadProfile).toHaveBeenCalled();
    });
  });

  // ─── OPERATOR DROPDOWN & MANUAL SELECTION ───────────────────────────────
  describe('Operator Dropdown', () => {
    it('toggleOperatorDropdown should toggle the dropdown state', () => {
      expect(component.showOperatorDropdown()).toBeFalse();
      component.toggleOperatorDropdown();
      expect(component.showOperatorDropdown()).toBeTrue();
    });

    it('toggleOperatorDropdown should load operators if list is empty', () => {
      (operatorSpy.operators as any).set([]);
      component.toggleOperatorDropdown();
      expect(operatorSpy.loadActiveOperators).toHaveBeenCalled();
    });

    it('selectManualOperator should set operator and close dropdown', () => {
      component.showOperatorDropdown.set(true);
      component.selectManualOperator(mockOperator);
      expect(operatorSpy.setManualOperator).toHaveBeenCalledWith(mockOperator);
      expect(component.showOperatorDropdown()).toBeFalse();
    });
  });

  // ─── PAYMENT FLOW EDGE CASES ──────────────────────────────────────────
  describe('Payment Flow Edge Cases', () => {
    beforeEach(() => {
      (operatorSpy.selectedOperator as any).set(mockOperator);
      component.selectedPlan.set(mockPlan);
    });

    it('should handle recharge initiation failure', async () => {
      authSpy.isAuthenticated.and.returnValue(true);
      authSpy.currentUser.and.returnValue({ mobileNumber: '9999999999' } as any);
      authSpy.isMobileVerified.and.returnValue(true);
      authSpy.getUserIdFromToken.and.returnValue(1);

      rechargeSpy.initiateRecharge.and.returnValue(of({ success: false, message: 'Server error' } as any));

      await component.onProceedToCheckout();

      expect(component.currentStep()).toBe('receipt');
      expect(component.failureReason()).toBeTruthy();
    });

    it('should handle payment processing failure', async () => {
      authSpy.isAuthenticated.and.returnValue(true);
      authSpy.currentUser.and.returnValue({ mobileNumber: '9999999999' } as any);
      authSpy.isMobileVerified.and.returnValue(true);
      authSpy.getUserIdFromToken.and.returnValue(1);

      rechargeSpy.initiateRecharge.and.returnValue(of({ success: true, data: { rechargeId: 'R1' } } as any));
      paymentSpy.processPayment.and.returnValue(of({ success: false, data: null } as any));

      await component.onProceedToCheckout();

      expect(component.currentStep()).toBe('receipt');
      expect(component.failureReason()).toBeTruthy();
    });

    it('should handle recharge initiation throwing error', async () => {
      authSpy.isAuthenticated.and.returnValue(true);
      authSpy.currentUser.and.returnValue({ mobileNumber: '9999999999' } as any);
      authSpy.isMobileVerified.and.returnValue(true);
      authSpy.getUserIdFromToken.and.returnValue(1);

      rechargeSpy.initiateRecharge.and.returnValue(throwError(() => new Error('Network error')));

      await component.onProceedToCheckout();

      expect(component.currentStep()).toBe('receipt');
      expect(component.failureReason()).toBe('Network error');
      expect(paymentSpy.resetPaymentState).toHaveBeenCalled();
    });

    it('should do nothing if plan or operator is not selected', async () => {
      authSpy.isAuthenticated.and.returnValue(true);
      authSpy.currentUser.and.returnValue({ mobileNumber: '9999999999' } as any);
      authSpy.isMobileVerified.and.returnValue(true);
      
      component.selectedPlan.set(null);
      await component.onProceedToCheckout();
      expect(rechargeSpy.initiateRecharge).not.toHaveBeenCalled();
    });
  });

  // ─── OTP VERIFICATION FLOW EXTENDED ─────────────────────────────────────
  describe('OTP Verification Extended', () => {
    it('requestVerificationOtp should call sendMobileOtp on valid number', async () => {
      component.verificationMobileInput = '9999999999';
      authSpy.sendMobileOtp.and.returnValue(of({ success: true } as any));
      
      await component.requestVerificationOtp();
      
      expect(authSpy.sendMobileOtp).toHaveBeenCalledWith('+919999999999');
      expect(component.verificationStep()).toBe('OTP');
    });

    it('requestVerificationOtp should handle API error', async () => {
      component.verificationMobileInput = '9999999999';
      authSpy.sendMobileOtp.and.returnValue(throwError(() => ({ error: { message: 'Already registered' } })));
      
      await component.requestVerificationOtp();
      
      expect(component.verificationError()).toBe('Already registered');
      expect(component.isVerifying()).toBeFalse();
    });

    it('verifyOtpAndProceed should show error for short OTP', async () => {
      component.verificationOtpDigits = ['1', '2', '3', '', '', ''];
      
      await component.verifyOtpAndProceed();
      
      expect(component.verificationError()).toBe('Enter a valid 6-digit OTP');
      expect(authSpy.verifyMobileOtp).not.toHaveBeenCalled();
    });

    it('verifyOtpAndProceed should handle API error', async () => {
      component.verificationOtpDigits = ['1', '2', '3', '4', '5', '6'];
      component.verificationMobileInput = '9999999999';
      authSpy.verifyMobileOtp.and.returnValue(throwError(() => ({ error: { message: 'Invalid OTP' } })));
      
      await component.verifyOtpAndProceed();
      
      expect(component.verificationError()).toBe('Invalid OTP');
      expect(component.isVerifying()).toBeFalse();
    });
  });

  // ─── STEP INDEX ───────────────────────────────────────────────────────
  describe('stepIndex', () => {
    it('should return correct index for each step', () => {
      expect(component.stepIndex('input')).toBe(0);
      expect(component.stepIndex('plans')).toBe(1);
      expect(component.stepIndex('processing')).toBe(2);
      expect(component.stepIndex('receipt')).toBe(3);
    });
  });

  // ─── MISC / GUARDS ───────────────────────────────────────────────────────
  describe('Misc / Guards', () => {
    it('hasUnsavedChanges should return true if on plans step with a plan selected', () => {
      component.currentStep.set('plans');
      component.selectedPlan.set(mockPlan);
      expect(component.hasUnsavedChanges()).toBeTrue();
    });

    it('hasUnsavedChanges should return false on receipt step', () => {
      component.currentStep.set('receipt');
      component.selectedPlan.set(mockPlan);
      expect(component.hasUnsavedChanges()).toBeFalse();
    });

    it('hasUnsavedChanges should return false if no plan selected', () => {
      component.currentStep.set('plans');
      component.selectedPlan.set(null);
      expect(component.hasUnsavedChanges()).toBeFalse();
    });

    it('startNewRecharge should reset all states', () => {
      component.mobileNumber = '9999999999';
      component.currentStep.set('receipt');
      component.selectedPlan.set(mockPlan);
      
      component.startNewRecharge();
      
      expect(component.mobileNumber).toBe('');
      expect(component.currentStep()).toBe('input');
      expect(component.selectedPlan()).toBeNull();
      expect(operatorSpy.clearSelection).toHaveBeenCalled();
      expect(paymentSpy.resetPaymentState).toHaveBeenCalled();
    });
  });
});
