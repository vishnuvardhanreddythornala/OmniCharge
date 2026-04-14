import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminNotificationsComponent } from './admin-notifications.component';
import { AdminService } from '../../core/services/admin.service';
import { of, throwError } from 'rxjs';

describe('AdminNotificationsComponent', () => {
  let component: AdminNotificationsComponent;
  let fixture: ComponentFixture<AdminNotificationsComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  const mockNotifs = [
    { id: 1, userId: 123, type: 'SMS', category: 'PAYMENT_SUCCESS', subject: 'Paid', message: 'Body', status: 'SENT', referenceId: 'TXN1', createdDate: '2023-01-01' },
    { id: 2, userId: null, type: 'EMAIL', category: 'PLAN_UPDATE', subject: 'Plan', message: 'Body', status: 'FAILED', referenceId: null, createdDate: '2023-01-01' }
  ];

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', ['getAllNotifications']);

    adminSpy.getAllNotifications.and.returnValue(of({
      success: true, message: 'OK', data: {
        content: mockNotifs,
        totalElements: 2,
        totalPages: 1,
        number: 0
      } as any
    }));

    await TestBed.configureTestingModule({
      imports: [AdminNotificationsComponent],
      providers: [
        { provide: AdminService, useValue: adminSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminNotificationsComponent);
    component = fixture.componentInstance;
  });

  it('should load notifications on init', () => {
    fixture.detectChanges();
    expect(adminSpy.getAllNotifications).toHaveBeenCalledWith(0, 10, 'ALL');
    expect(component.notifications().length).toBe(2);
    expect(component.totalElements()).toBe(2);
  });

  it('setTab should change tab and reload', () => {
    fixture.detectChanges();
    adminSpy.getAllNotifications.calls.reset();

    component.setTab('SYSTEM');
    expect(component.activeTab()).toBe('SYSTEM');
    expect(adminSpy.getAllNotifications).toHaveBeenCalledWith(0, 10, 'SYSTEM');
  });

  it('getPageNumbers should handle sliding window', () => {
    component.totalPages.set(10);
    component.currentPage.set(2);
    expect(component.getPageNumbers()).toEqual([0, 1, 2, 3, 4]);
  });

  it('should correctly interpret badge classes', () => {
    expect(component.getCategoryIconClass('PAYMENT_SUCCESS')).toContain('bg-accent-emerald');
    expect(component.getCategoryIconClass('PAYMENT_FAILED')).toContain('bg-accent-rose');
    expect(component.getCategoryIconClass('PLAN_UPDATE')).toContain('bg-accent-amber');
    
    expect(component.getStatusBadgeClass('SENT')).toContain('bg-accent-emerald');
    expect(component.getStatusBadgeClass('FAILED')).toContain('bg-accent-rose');
    expect(component.getStatusBadgeClass('PENDING')).toContain('bg-accent-amber');
  });

  it('should handle API errors gracefully', () => {
    fixture.detectChanges();
    adminSpy.getAllNotifications.and.returnValue(throwError(() => new Error('Error')));
    
    component.loadNotifications(0);
    expect(component.loading()).toBeFalse();
  });
});
