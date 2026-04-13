import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../core/services/auth.service';
import { RechargeService, RechargeHistoryItem } from '../../core/services/recharge.service';
import { PaymentService, TransactionResponse } from '../../core/services/payment.service';
import { NotificationService } from '../../core/services/notification.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let rechargeSpy: jasmine.SpyObj<RechargeService>;
  let paymentSpy: jasmine.SpyObj<PaymentService>;
  let notificationSpy: jasmine.SpyObj<NotificationService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const emptyPagedResponse = (content: any[] = []) => ({
    success: true, message: 'OK',
    data: { content, totalElements: content.length, totalPages: content.length > 0 ? 1 : 0, size: 10, number: 0 }
  });

  const mockUser = {
    id: 1, fullName: 'John Doe', email: 'john@example.com',
    mobileNumber: '9999999999', role: 'ROLE_USER', authProvider: 'LOCAL'
  };

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService',
      ['updateProfile', 'sendMobileOtp', 'verifyMobileOtp', 'loadProfile', 'getAccessToken', 'restoreSession'],
      {
        currentUser: signal(mockUser),
        userInitials: signal('JD'),
        isAdmin: signal(false),
        isAuthenticated: signal(true)
      }
    );

    rechargeSpy = jasmine.createSpyObj('RechargeService', ['getRechargeHistory']);
    paymentSpy = jasmine.createSpyObj('PaymentService', ['getPaymentHistory']);
    notificationSpy = jasmine.createSpyObj('NotificationService',
      ['getNotifications', 'markAsRead', 'fetchUnreadCount', 'startPolling', 'stopPolling', 'acknowledgeCountChange'],
      { unreadCount: signal(0), countChanged: signal(false), notifications: signal([]) }
    );
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    // Default mocks that ngOnInit will call
    rechargeSpy.getRechargeHistory.and.returnValue(of(emptyPagedResponse()));
    paymentSpy.getPaymentHistory.and.returnValue(of(emptyPagedResponse()));
    notificationSpy.getNotifications.and.returnValue(of(emptyPagedResponse()));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: RechargeService, useValue: rechargeSpy },
        { provide: PaymentService, useValue: paymentSpy },
        { provide: NotificationService, useValue: notificationSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: { queryParams: of({}), snapshot: { queryParams: {} } }
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .overrideComponent(DashboardComponent, {
      set: {
        imports: [CommonModule, FormsModule],
        schemas: [NO_ERRORS_SCHEMA]
      }
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    if (component && (component as any)._notifRefreshInterval) {
      clearInterval((component as any)._notifRefreshInterval);
    }
  });

  // ─── INITIALIZATION ───────────────────────────────────────────────────────
  describe('Initialization', () => {
    it('should create and call initial data-load methods on ngOnInit', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      expect(component).toBeTruthy();
      expect(rechargeSpy.getRechargeHistory).toHaveBeenCalledWith(0, 10, undefined, undefined);
      expect(paymentSpy.getPaymentHistory).toHaveBeenCalled();
      expect(notificationSpy.getNotifications).toHaveBeenCalled();
      expect(notificationSpy.fetchUnreadCount).toHaveBeenCalled();
      expect(notificationSpy.startPolling).toHaveBeenCalled();
      discardPeriodicTasks();
    }));

    it('should set profileName from currentUser on init', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      expect(component.profileName).toBe('John Doe');
      discardPeriodicTasks();
    }));

    it('should auto-dismiss splash after 2300ms', fakeAsync(() => {
      fixture.detectChanges();
      expect(component.showSplash()).toBeTrue();
      tick(2300);
      expect(component.showSplash()).toBeFalse();
      discardPeriodicTasks();
    }));
  });

  // ─── DISPLAY NAME ─────────────────────────────────────────────────────────
  describe('getDisplayName()', () => {
    it('should return fullName for a named user', () => {
      expect(component.getDisplayName()).toBe('John Doe');
    });

    it('should return "User" when fullName starts with "User "', () => {
      (authSpy.currentUser as any).set({ ...mockUser, fullName: 'User 12345' });
      expect(component.getDisplayName()).toBe('User');
    });

    it('should return "User" when currentUser is null', () => {
      (authSpy.currentUser as any).set(null);
      expect(component.getDisplayName()).toBe('User');
    });
  });

  // ─── RECHARGES ────────────────────────────────────────────────────────────
  describe('Recharge Loading', () => {
    it('should load recharges and populate signal + stats', fakeAsync(() => {
      const now = new Date();
      const mockRecharges: Partial<RechargeHistoryItem>[] = [
        { rechargeId: 'R1', status: 'SUCCESS', mobileNumber: '9999999999', createdDate: now.toISOString(), planValidityDays: 30, amount: 100, planName: 'Plan A' },
        { rechargeId: 'R2', status: 'SUCCESS', mobileNumber: '9999999999', createdDate: '2020-01-01T00:00:00Z', planValidityDays: 1, amount: 50, planName: 'Plan B' },
        { rechargeId: 'R3', status: 'PROCESSING', mobileNumber: '9999999999', createdDate: now.toISOString(), planValidityDays: 10, amount: 20, planName: 'Plan C' },
        { rechargeId: 'R4', status: 'FAILED', mobileNumber: '9999999999', createdDate: now.toISOString(), planValidityDays: 10, amount: 20, planName: 'Plan D' }
      ];

      rechargeSpy.getRechargeHistory.and.returnValue(of(emptyPagedResponse(mockRecharges as any)));
      fixture.detectChanges();
      tick(2500);

      expect(component.recharges().length).toBe(4);
      expect(component.stats().active).toBe(1);
      expect(component.stats().expired).toBe(1);
      expect(component.stats().processing).toBe(1);
      expect(component.stats().failed).toBe(1);
      discardPeriodicTasks();
    }));

    it('should set rechLoading to false on error', fakeAsync(() => {
      rechargeSpy.getRechargeHistory.and.returnValue(throwError(() => new Error('fail')));
      fixture.detectChanges();
      tick(2500);

      expect(component.rechLoading()).toBeFalse();
      expect(component.recharges().length).toBe(0);
      discardPeriodicTasks();
    }));

    it('should not load negative page numbers', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      const callsBefore = rechargeSpy.getRechargeHistory.calls.count();
      component.loadRecharges(-1);
      expect(rechargeSpy.getRechargeHistory.calls.count()).toBe(callsBefore);
      discardPeriodicTasks();
    }));
  });

  describe('Recharge Filtering', () => {
    it('should filter recharges by pack category', fakeAsync(() => {
      const now = new Date();
      const mockRecharges: Partial<RechargeHistoryItem>[] = [
        { rechargeId: 'R1', status: 'SUCCESS', mobileNumber: '9999999999', createdDate: now.toISOString(), planValidityDays: 30, amount: 100, planName: 'Active Plan' },
        { rechargeId: 'R2', status: 'FAILED', mobileNumber: '9999999999', createdDate: now.toISOString(), planValidityDays: 10, amount: 20, planName: 'Failed Plan' }
      ];
      rechargeSpy.getRechargeHistory.and.returnValue(of(emptyPagedResponse(mockRecharges as any)));
      fixture.detectChanges();
      tick(2500);

      component.packCategory.set('ALL');
      expect(component.filteredRecharges().length).toBe(2);

      component.packCategory.set('ACTIVE');
      expect(component.filteredRecharges().length).toBe(1);

      component.packCategory.set('FAILED');
      expect(component.filteredRecharges().length).toBe(1);

      component.packCategory.set('EXPIRED');
      expect(component.filteredRecharges().length).toBe(0);
      discardPeriodicTasks();
    }));

    it('should apply date filter and reload from page 0', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      rechargeSpy.getRechargeHistory.calls.reset();

      component.rechStartDate = '2024-01-01';
      component.rechEndDate = '2024-12-31';
      component.applyRechDateFilter();

      expect(rechargeSpy.getRechargeHistory).toHaveBeenCalledWith(0, 10, '2024-01-01T00:00:00', '2024-12-31T23:59:59');
      discardPeriodicTasks();
    }));

    it('should clear date filters', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      component.rechStartDate = '2024-01-01';
      component.rechEndDate = '2024-12-31';
      component.clearRechDateFilter();

      expect(component.rechStartDate).toBe('');
      expect(component.rechEndDate).toBe('');
      discardPeriodicTasks();
    }));
  });

  // ─── PACK STATUS ──────────────────────────────────────────────────────────
  describe('getPackStatus()', () => {
    it('should return "active" for a SUCCESS recharge with future expiry', () => {
      const r = { status: 'SUCCESS', createdDate: new Date().toISOString(), planValidityDays: 30 } as any;
      expect(component.getPackStatus(r).type).toBe('active');
      expect(component.getPackStatus(r).label).toBe('ACTIVE');
    });

    it('should return "expired" for a SUCCESS recharge with past expiry', () => {
      const r = { status: 'SUCCESS', createdDate: '2020-01-01T00:00:00Z', planValidityDays: 1 } as any;
      expect(component.getPackStatus(r).type).toBe('expired');
    });

    it('should return "processing" for a PROCESSING recharge', () => {
      const r = { status: 'PROCESSING', createdDate: new Date().toISOString(), planValidityDays: 30 } as any;
      expect(component.getPackStatus(r).type).toBe('processing');
    });

    it('should return "failed" for a FAILED recharge', () => {
      const r = { status: 'FAILED', createdDate: new Date().toISOString(), planValidityDays: 30 } as any;
      expect(component.getPackStatus(r).type).toBe('failed');
    });
  });

  // ─── TIME CALCULATIONS ────────────────────────────────────────────────────
  describe('Time / Date helpers', () => {
    it('getExactExpiryDate should compute from createdDate + planValidityDays', () => {
      const created = new Date('2024-04-01T00:00:00Z');
      const r = { createdDate: created.toISOString(), planValidityDays: 10 } as any;
      const expiry = component.getExactExpiryDate(r);
      expect(expiry).toBeTruthy();
      expect(expiry!.getTime()).toBe(created.getTime() + 10 * 24 * 60 * 60 * 1000);
    });

    it('getExactExpiryDate should fallback to planExpiryDate', () => {
      const r = { planExpiryDate: '2024-05-01T00:00:00Z' } as any;
      const expiry = component.getExactExpiryDate(r);
      expect(expiry).toBeTruthy();
    });

    it('getExactExpiryDate should return null when no data available', () => {
      const r = {} as any;
      expect(component.getExactExpiryDate(r)).toBeNull();
    });

    it('getDaysLeft should return positive days for future expiry', () => {
      const now = new Date();
      const r = { createdDate: now.toISOString(), planValidityDays: 10 } as any;
      const days = component.getDaysLeft(r);
      expect(days).toBeGreaterThanOrEqual(9);
      expect(days).toBeLessThanOrEqual(10);
    });

    it('getDaysLeft should return -1 when expiry is unknown', () => {
      const r = {} as any;
      expect(component.getDaysLeft(r)).toBe(-1);
    });

    it('getTimeLeft should return "Expired" when past', () => {
      const r = { createdDate: '2020-01-01T00:00:00Z', planValidityDays: 1 } as any;
      expect(component.getTimeLeft(r)).toBe('Expired');
    });

    it('getTimeLeft should return days for future > 24h', () => {
      const now = new Date();
      const r = { createdDate: now.toISOString(), planValidityDays: 10 } as any;
      expect(component.getTimeLeft(r)).toContain('Day');
    });

    it('formatDate should handle array format [Y,M,D,h,m,s]', () => {
      expect(component.formatDate([2024, 4, 15, 12, 0, 0])).toContain('2024');
    });

    it('formatDate should handle ISO string', () => {
      expect(component.formatDate('2024-04-15')).toContain('2024');
    });

    it('formatDate should return dash for null/undefined', () => {
      expect(component.formatDate(null)).toBe('—');
      expect(component.formatDate(undefined)).toBe('—');
    });
  });

  // ─── PROFILE ──────────────────────────────────────────────────────────────
  describe('Profile Management', () => {
    it('should update profile successfully', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      component.profileName = 'Jane Doe';
      authSpy.updateProfile.and.returnValue(of({ success: true, message: 'OK', data: null as any }));

      component.onUpdateProfile();

      expect(authSpy.updateProfile).toHaveBeenCalledWith({ fullName: 'Jane Doe' });
      expect(component.profileMsg()).toBe('Profile updated successfully.');
      expect(component.profileMsgError()).toBeFalse();
      expect(component.profileSaving()).toBeFalse();
      tick(4000); // auto-dismiss timer
      discardPeriodicTasks();
    }));

    it('should show validation error when profileName is empty', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      component.profileName = '';
      component.onUpdateProfile();

      expect(authSpy.updateProfile).not.toHaveBeenCalled();
      expect(component.profileMsg()).toBe('Please enter your full name.');
      expect(component.profileMsgError()).toBeTrue();
      discardPeriodicTasks();
    }));

    it('should show error message on updateProfile HTTP error', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      component.profileName = 'Jane';
      authSpy.updateProfile.and.returnValue(throwError(() => ({ error: { message: 'Server error' } })));

      component.onUpdateProfile();

      expect(component.profileMsg()).toBe('Server error');
      expect(component.profileMsgError()).toBeTrue();
      expect(component.profileSaving()).toBeFalse();
      discardPeriodicTasks();
    }));
  });

  // ─── MOBILE VERIFICATION ─────────────────────────────────────────────────
  describe('Mobile Verification', () => {
    it('should call verifyMobileOtp and clear state on success', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      component.newMobile = '8888888888';
      component.mobileOtpDigits.set(['1', '2', '3', '4', '5', '6']);

      authSpy.verifyMobileOtp.and.returnValue(of({
        success: true, message: 'OK',
        data: { accessToken: 'a', refreshToken: 'r', isProfileComplete: true } as any
      }));

      component.onVerifyMobile();

      expect(authSpy.verifyMobileOtp).toHaveBeenCalledWith('+918888888888', '123456');
      expect(component.mobileOtpSent()).toBeFalse();
      expect(component.mobileVerifying()).toBeFalse();
      expect(component.profileMsg()).toBe('Mobile number linked and verified successfully!');
      expect(authSpy.loadProfile).toHaveBeenCalled();
      discardPeriodicTasks();
    }));

    it('should show error when verifyMobileOtp fails', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      component.newMobile = '8888888888';
      component.mobileOtpDigits.set(['1', '2', '3', '4', '5', '6']);

      authSpy.verifyMobileOtp.and.returnValue(throwError(() => ({ error: { message: 'Wrong OTP' } })));

      component.onVerifyMobile();

      expect(component.profileMsg()).toBe('Wrong OTP');
      expect(component.profileMsgError()).toBeTrue();
      expect(component.mobileVerifying()).toBeFalse();
      discardPeriodicTasks();
    }));

    it('should not verify when OTP is incomplete', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);

      component.newMobile = '8888888888';
      component.mobileOtpDigits.set(['1', '2', '3', '', '', '']);

      component.onVerifyMobile();

      expect(authSpy.verifyMobileOtp).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });

  // ─── PAYMENTS ─────────────────────────────────────────────────────────────
  describe('Payments', () => {
    it('should load payments and store them', fakeAsync(() => {
      const mockPayments: Partial<TransactionResponse>[] = [
        { transactionId: 'T1', rechargeId: 'R1', status: 'SUCCESS', amount: 100, createdDate: new Date().toISOString(), paymentMethod: 'UPI' },
        { transactionId: 'T2', rechargeId: 'R2', status: 'FAILED', amount: 50, createdDate: new Date().toISOString(), paymentMethod: 'CARD', failureReason: 'Insufficient funds' }
      ];

      paymentSpy.getPaymentHistory.and.returnValue(of(emptyPagedResponse(mockPayments as any)));
      fixture.detectChanges();
      tick(2500);

      expect(component.payments().length).toBe(2);
      expect(component.payLoading()).toBeFalse();
      discardPeriodicTasks();
    }));

    it('should filter payments by category', fakeAsync(() => {
      const mockPayments: Partial<TransactionResponse>[] = [
        { transactionId: 'T1', rechargeId: 'R1', status: 'SUCCESS', amount: 100, createdDate: new Date().toISOString(), paymentMethod: 'UPI' },
        { transactionId: 'T2', rechargeId: 'R2', status: 'FAILED', amount: 50, createdDate: new Date().toISOString(), paymentMethod: 'CARD' }
      ];

      paymentSpy.getPaymentHistory.and.returnValue(of(emptyPagedResponse(mockPayments as any)));
      fixture.detectChanges();
      tick(2500);

      component.paymentCategory.set('ALL');
      expect(component.filteredPayments().length).toBe(2);

      component.paymentCategory.set('SUCCESS');
      expect(component.filteredPayments().length).toBe(1);

      component.paymentCategory.set('FAILED');
      expect(component.filteredPayments().length).toBe(1);
      discardPeriodicTasks();
    }));

    it('should handle payment load error gracefully', fakeAsync(() => {
      paymentSpy.getPaymentHistory.and.returnValue(throwError(() => new Error('fail')));
      fixture.detectChanges();
      tick(2500);

      expect(component.payLoading()).toBeFalse();
      discardPeriodicTasks();
    }));
  });

  // ─── NAVIGATION ───────────────────────────────────────────────────────────
  describe('Navigation', () => {
    it('goToRecharge should navigate to /recharge', () => {
      component.goToRecharge();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/recharge']);
    });

    it('hasRealName should return true for a real name', () => {
      expect(component.hasRealName()).toBeTrue();
    });

    it('hasRealName should return false for auto-generated name', () => {
      (authSpy.currentUser as any).set({ ...mockUser, fullName: 'User 12345' });
      expect(component.hasRealName()).toBeFalse();
    });
  });

  // ─── NOTIFICATION STYLE ───────────────────────────────────────────────────
  describe('getNotifStyle()', () => {
    it('should classify success notifications', () => {
      const n = { subject: 'Recharge Success', message: 'Your recharge was successful' } as any;
      const style = component.getNotifStyle(n);
      expect(style.type).toBe('success');
      expect(style.badge).toBe('RECHARGE_SUCCESS');
    });

    it('should classify payment success notifications', () => {
      const n = { subject: 'Payment Success', message: 'Your payment was successful' } as any;
      const style = component.getNotifStyle(n);
      expect(style.type).toBe('success');
      expect(style.badge).toBe('PAYMENT_SUCCESS');
    });

    it('should classify failure notifications', () => {
      const n = { subject: 'Payment Failed', message: 'Your payment has failed' } as any;
      const style = component.getNotifStyle(n);
      expect(style.type).toBe('error');
      expect(style.badge).toBe('PAYMENT_FAILED');
    });

    it('should classify recharge failure notifications', () => {
      const n = { subject: 'Recharge Failed', message: 'Your recharge has failed' } as any;
      const style = component.getNotifStyle(n);
      expect(style.type).toBe('error');
      expect(style.badge).toBe('RECHARGE_FAILED');
    });

    it('should classify expired notifications', () => {
      const n = { subject: 'Plan Expired', message: 'Your plan has expired' } as any;
      const style = component.getNotifStyle(n);
      expect(style.type).toBe('error');
    });

    it('should classify info notifications', () => {
      const n = { subject: 'Welcome', message: 'Welcome to OmniCharge' } as any;
      const style = component.getNotifStyle(n);
      expect(style.type).toBe('info');
      expect(style.badge).toBe('INFO');
    });
  });

  // ─── PROFILE UPDATE ──────────────────────────────────────────────────────
  describe('Profile Update', () => {
    it('should show error if profileName is empty', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      
      component.profileName = '';
      component.onUpdateProfile();
      
      expect(component.profileMsg()).toBe('Please enter your full name.');
      expect(component.profileMsgError()).toBeTrue();
      expect(authSpy.updateProfile).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));

    it('should call updateProfile and show success', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      
      authSpy.updateProfile.and.returnValue(of({ success: true, message: 'OK', data: null } as any));
      component.profileName = 'New Name';
      component.onUpdateProfile();
      
      expect(authSpy.updateProfile).toHaveBeenCalledWith({ fullName: 'New Name' });
      expect(component.profileMsg()).toBe('Profile updated successfully.');
      expect(component.profileMsgError()).toBeFalse();
      
      tick(4000);
      discardPeriodicTasks();
    }));

    it('should handle updateProfile error', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      
      authSpy.updateProfile.and.returnValue(throwError(() => ({ error: { message: 'Server error' } })));
      component.profileName = 'New Name';
      component.onUpdateProfile();
      
      expect(component.profileMsg()).toBe('Server error');
      expect(component.profileMsgError()).toBeTrue();
      discardPeriodicTasks();
    }));
  });

  // ─── DISPLAY NAME ────────────────────────────────────────────────────────
  describe('getDisplayName', () => {
    it('should return User if no user', () => {
      (authSpy.currentUser as any).set(null);
      expect(component.getDisplayName()).toBe('User');
    });

    it('should return User if auto-generated name', () => {
      (authSpy.currentUser as any).set({ ...mockUser, fullName: 'User 12345' });
      expect(component.getDisplayName()).toBe('User');
    });

    it('should return real name', () => {
      expect(component.getDisplayName()).toBe('John Doe');
    });
  });

  // ─── DATE FILTERS ────────────────────────────────────────────────────────
  describe('Date Filters', () => {
    it('applyRechDateFilter should trigger reload', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      rechargeSpy.getRechargeHistory.calls.reset();
      
      component.rechStartDate = '2023-01-01';
      component.rechEndDate = '2023-12-31';
      component.applyRechDateFilter();
      
      expect(rechargeSpy.getRechargeHistory).toHaveBeenCalledWith(0, 10, '2023-01-01T00:00:00', '2023-12-31T23:59:59');
      discardPeriodicTasks();
    }));

    it('clearRechDateFilter should clear dates and reload', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      rechargeSpy.getRechargeHistory.calls.reset();
      
      component.rechStartDate = '2023-01-01';
      component.rechEndDate = '2023-12-31';
      component.clearRechDateFilter();
      
      expect(component.rechStartDate).toBe('');
      expect(component.rechEndDate).toBe('');
      discardPeriodicTasks();
    }));

    it('applyPayDateFilter should trigger reload', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      paymentSpy.getPaymentHistory.calls.reset();
      
      component.payStartDate = '2023-01-01';
      component.payEndDate = '2023-12-31';
      component.applyPayDateFilter();
      
      expect(paymentSpy.getPaymentHistory).toHaveBeenCalled();
      discardPeriodicTasks();
    }));

    it('clearPayDateFilter should clear dates and reload', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      paymentSpy.getPaymentHistory.calls.reset();
      
      component.payStartDate = '2023-01-01';
      component.payEndDate = '2023-12-31';
      component.clearPayDateFilter();
      
      expect(component.payStartDate).toBe('');
      expect(component.payEndDate).toBe('');
      discardPeriodicTasks();
    }));
  });

  // ─── PAGINATION ──────────────────────────────────────────────────────────
  describe('Pagination', () => {
    it('getPayPageNumbers should return correct range', () => {
      component.payTotalPages.set(10);
      component.payPage.set(5);
      expect(component.getPayPageNumbers()).toEqual([3, 4, 5, 6, 7]);
    });

    it('getNotifPageNumbers should return correct range', () => {
      component.notifTotalPages.set(10);
      component.notifPage.set(5);
      expect(component.getNotifPageNumbers()).toEqual([3, 4, 5, 6, 7]);
    });

    it('loadPayments should not call for negative page', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      paymentSpy.getPaymentHistory.calls.reset();
      
      component.loadPayments(-1);
      expect(paymentSpy.getPaymentHistory).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));

    it('loadNotifications should not call for negative page', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      notificationSpy.getNotifications.calls.reset();
      
      component.loadNotifications(-1);
      expect(notificationSpy.getNotifications).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });

  // ─── NOTIFICATION HANDLING ───────────────────────────────────────────────
  describe('Notifications', () => {
    it('loadNotifications should filter noise notifications', fakeAsync(() => {
      const mockNotifs = [
        { id: 1, message: 'Recharge success', subject: 'Test', isRead: false },
        { id: 2, message: 'Email sent to user', subject: 'Test', isRead: false },
        { id: 3, message: 'Confirmation email sent', subject: 'Test', isRead: true }
      ];
      notificationSpy.getNotifications.and.returnValue(of(emptyPagedResponse(mockNotifs as any)));
      notificationSpy.markAsRead.and.returnValue(of({ success: true } as any));
      
      fixture.detectChanges();
      tick(2500);
      
      // Noise notifications should be filtered
      expect(component.notifications().length).toBe(1);
      discardPeriodicTasks();
    }));

    it('markRead should call markAsRead and update local state', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      
      const notif = { id: 1, message: 'test', isRead: false } as any;
      component.notifications.set([notif]);
      notificationSpy.markAsRead.and.returnValue(of({ success: true } as any));
      
      component.markRead(notif);
      
      expect(notificationSpy.markAsRead).toHaveBeenCalledWith(1);
      expect(component.notifications()[0].isRead).toBeTrue();
      discardPeriodicTasks();
    }));

    it('markRead should not call API if already read', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      notificationSpy.markAsRead.calls.reset();
      
      const notif = { id: 1, message: 'test', isRead: true } as any;
      component.markRead(notif);
      
      expect(notificationSpy.markAsRead).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });

  // ─── MOBILE OTP HELPERS ──────────────────────────────────────────────────
  describe('Mobile OTP Helpers', () => {
    it('getMobileOtpString should concatenate digits', () => {
      component.mobileOtpDigits.set(['1', '2', '3', '4', '5', '6']);
      expect(component.getMobileOtpString()).toBe('123456');
    });

    it('onMobileOtpInput should set digit and auto-advance', fakeAsync(() => {
      component.mobileOtpDigits.set(['', '', '', '', '', '']);
      const event = { target: { value: '5' } } as any;
      component.onMobileOtpInput(event, 0);
      tick(100);
      expect(component.mobileOtpDigits()[0]).toBe('5');
      discardPeriodicTasks();
    }));
  });

  // ─── EMAIL OTP HELPERS ───────────────────────────────────────────────────
  describe('Email OTP Helpers', () => {
    it('getEmailOtpString should concatenate digits', () => {
      component.emailOtpDigits.set(['1', '2', '3', '4', '5', '6']);
      expect(component.getEmailOtpString()).toBe('123456');
    });

    it('onEmailOtpInput should set digit and auto-advance', fakeAsync(() => {
      component.emailOtpDigits.set(['', '', '', '', '', '']);
      const event = { target: { value: '5' } } as any;
      component.onEmailOtpInput(event, 0);
      tick(100);
      expect(component.emailOtpDigits()[0]).toBe('5');
      discardPeriodicTasks();
    }));
  });

  // ─── SEND MOBILE VERIFICATION ────────────────────────────────────────────
  describe('Send Mobile Verification', () => {
    it('should do nothing if newMobile is empty', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      
      component.newMobile = '';
      component.onSendMobileVerification();
      expect(authSpy.sendMobileOtp).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));

    it('should send OTP and show success', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      
      component.newMobile = '8888888888';
      authSpy.sendMobileOtp.and.returnValue(of({ success: true } as any));
      component.onSendMobileVerification();
      
      expect(authSpy.sendMobileOtp).toHaveBeenCalledWith('+918888888888');
      expect(component.mobileOtpSent()).toBeTrue();
      expect(component.profileMsgError()).toBeFalse();
      discardPeriodicTasks();
    }));

    it('should handle send OTP error', fakeAsync(() => {
      fixture.detectChanges();
      tick(2500);
      
      component.newMobile = '8888888888';
      authSpy.sendMobileOtp.and.returnValue(throwError(() => ({ error: { message: 'Too many attempts' } })));
      component.onSendMobileVerification();
      
      expect(component.profileMsg()).toBe('Too many attempts');
      expect(component.profileMsgError()).toBeTrue();
      discardPeriodicTasks();
    }));
  });

  // ─── FAQ ──────────────────────────────────────────────────────────────────
  describe('FAQ toggle', () => {
    it('should toggle FAQ open/close', () => {
      expect(component.faqOpen()).toBeNull();
      component.toggleFaq(0);
      expect(component.faqOpen()).toBe(0);
      component.toggleFaq(0);
      expect(component.faqOpen()).toBeNull();
    });

    it('should switch to different FAQ item', () => {
      component.toggleFaq(0);
      expect(component.faqOpen()).toBe(0);
      component.toggleFaq(2);
      expect(component.faqOpen()).toBe(2);
    });
  });

  // ─── PAYMENT DEDUP ───────────────────────────────────────────────────────
  describe('Payment deduplication', () => {
    it('should deduplicate by rechargeId keeping SUCCESS over PENDING', fakeAsync(() => {
      const mockPayments = [
        { transactionId: 'T1', rechargeId: 'R1', status: 'PENDING', amount: 100, createdDate: new Date().toISOString(), paymentMethod: 'UPI' },
        { transactionId: 'T2', rechargeId: 'R1', status: 'SUCCESS', amount: 100, createdDate: new Date().toISOString(), paymentMethod: 'UPI' }
      ];
      
      paymentSpy.getPaymentHistory.and.returnValue(of(emptyPagedResponse(mockPayments as any)));
      fixture.detectChanges();
      tick(2500);
      
      expect(component.payments().length).toBe(1);
      expect(component.payments()[0].status).toBe('SUCCESS');
      discardPeriodicTasks();
    }));
  });
});

