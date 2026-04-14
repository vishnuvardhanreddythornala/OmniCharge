import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminService } from '../../core/services/admin.service';
import { of, throwError } from 'rxjs';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', ['getPaymentStats', 'getRechargeStats', 'getAllUsers', 'rebuildCache']);

    adminSpy.getPaymentStats.and.returnValue(of({
      success: true, message: 'OK', data: {
        totalRevenue: 5000,
        totalTransactions: 100,
        successfulTransactions: 90,
        failedTransactions: 10,
        pendingTransactions: 0,
        averageTransactionAmount: 50,
        successAmount: 4500,
        failedAmount: 500,
        todayRevenue: 500,
        todayTransactions: 10,
        revenueByDate: [],
        topUsers: [{ userId: 1, transactionCount: 5, totalSpent: 250 }]
      }
    } as any));

    adminSpy.getRechargeStats.and.returnValue(of({
      success: true, message: 'OK', data: {
        totalRecharges: 50,
        totalAmount: 2500,
        successCount: 45,
        failedCount: 5,
        pendingCount: 0
      }
    } as any));

    adminSpy.getAllUsers.and.returnValue(of({
      success: true, message: 'OK', data: {
        content: [{ id: 1, fullName: 'John Doe' } as any],
        totalElements: 1, totalPages: 1, size: 10, number: 0
      }
    } as any));

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [{ provide: AdminService, useValue: adminSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    if ((component as any).toastTimer) {
      clearTimeout((component as any).toastTimer);
    }
  });

  it('should initialize and load stats', () => {
    fixture.detectChanges();
    expect(adminSpy.getPaymentStats).toHaveBeenCalledWith(30);
    expect(adminSpy.getRechargeStats).toHaveBeenCalled();
    expect(adminSpy.getAllUsers).toHaveBeenCalledWith(0, 100);
    expect(component.loading()).toBeFalse();
    expect(component.paymentStats()).toBeTruthy();
    expect(component.rechargeStats()).toBeTruthy();
    expect(component.userNameMap().get(1)).toBe('John Doe');
  });

  it('should handle API errors during initialization gracefully', () => {
    adminSpy.getPaymentStats.and.returnValue(throwError(() => new Error('API down')));
    adminSpy.getRechargeStats.and.returnValue(throwError(() => new Error('API down')));
    adminSpy.getAllUsers.and.returnValue(throwError(() => new Error('API down')));
    
    fixture.detectChanges();
    expect(component.loading()).toBeFalse();
    expect(component.paymentStats()).toBeNull();
  });

  describe('Calculations & Formatting', () => {
    it('getSuccessRate should return percentage', () => {
      fixture.detectChanges();
      expect(component.getSuccessRate()).toBe(90);
    });

    it('getSuccessRate should handle zero transactions', () => {
      adminSpy.getPaymentStats.and.returnValue(of({ success: true, message: '', data: { totalTransactions: 0, successfulTransactions: 0 } as any }));
      fixture.detectChanges();
      expect(component.getSuccessRate()).toBe(0);
    });

    it('formatNumber should format with commas', () => {
      expect(component.formatNumber(1500)).toBe('1,500');
      expect(component.formatNumber(null)).toBe('0');
      expect(component.formatNumber(undefined)).toBe('0');
    });

    it('getUserName should return mapped name or fallback ID', () => {
      fixture.detectChanges();
      expect(component.getUserName(1)).toBe('John Doe');
      expect(component.getUserName(99)).toBe('User (USR-00099)');
    });
  });

  describe('Cache Rebuilding', () => {
    it('should show success toast on cache rebuild success', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.rebuildCache.and.returnValue(of({ success: true, message: 'Rebuilt', data: 'OK' } as any));
      
      component.rebuildCache();
      expect(component.rebuildingCache()).toBeFalse();
      expect(component.toastVisible()).toBeTrue();
      expect(component.toastType()).toBe('success');
      expect(component.toastMessage()).toBe('Redis cache rebuilt successfully!');

      tick(4000);
      expect(component.toastVisible()).toBeFalse();
      discardPeriodicTasks();
    }));

    it('should show error toast on cache rebuild API failure', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.rebuildCache.and.returnValue(of({ success: false, message: 'Cache error', data: null as any }));
      
      component.rebuildCache();
      expect(component.toastType()).toBe('error');
      expect(component.toastMessage()).toBe('Cache error');

      tick(4000);
      discardPeriodicTasks();
    }));

    it('should show generic error toast if request throws', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.rebuildCache.and.returnValue(throwError(() => new Error('Network error')));
      
      component.rebuildCache();
      expect(component.toastType()).toBe('error');
      expect(component.toastMessage()).toBe('Failed to rebuild cache. Please try again.');

      tick(4000);
      discardPeriodicTasks();
    }));
  });
});
