import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AdminRechargesComponent } from './admin-recharges.component';
import { AdminService } from '../../core/services/admin.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';

describe('AdminRechargesComponent', () => {
  let component: AdminRechargesComponent;
  let fixture: ComponentFixture<AdminRechargesComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  const mockRecharges = [
    { rechargeId: 'REC1', mobileNumber: '9999999999', amount: 100, status: 'SUCCESS', operatorName: 'Airtel', planValidityDays: 28 },
    { rechargeId: 'REC2', mobileNumber: '8888888888', amount: 200, status: 'FAILED', operatorName: 'Jio', planValidityDays: 56 },
    { rechargeId: 'REC3', mobileNumber: '7777777777', amount: 300, status: 'PROCESSING', operatorName: 'VI', planValidityDays: 84 }
  ];

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', ['getAllRecharges']);

    adminSpy.getAllRecharges.and.returnValue(of({
      success: true, message: 'OK', data: {
        content: mockRecharges,
        totalElements: 3,
        totalPages: 1,
        number: 0
      } as any
    }));

    await TestBed.configureTestingModule({
      imports: [AdminRechargesComponent, FormsModule],
      providers: [
        { provide: AdminService, useValue: adminSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminRechargesComponent);
    component = fixture.componentInstance;
  });

  it('should initialize and load recharges', () => {
    fixture.detectChanges();
    
    expect(adminSpy.getAllRecharges).toHaveBeenCalledWith(0, 10, 'ALL', undefined, undefined);
    expect(component.recharges().length).toBe(3);
    
    // Check initial stats based on 'ALL' condition
    expect(component.successCount()).toBe(1);
    expect(component.failedCount()).toBe(1);
    expect(component.processingCount()).toBe(1);
    expect(component.totalElements()).toBe(3);
  });

  it('setCategory should clear filters or change status and reload', () => {
    fixture.detectChanges();
    adminSpy.getAllRecharges.calls.reset();

    component.setCategory('SUCCESS');
    expect(component.rechargeCategory()).toBe('SUCCESS');
    expect(adminSpy.getAllRecharges).toHaveBeenCalledWith(0, 10, 'SUCCESS', undefined, undefined);
  });

  it('should trigger search filter with debounce', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.getAllRecharges.calls.reset();

    component.onSearchChange('REC2');
    
    tick(200);
    expect(adminSpy.getAllRecharges).not.toHaveBeenCalled();

    tick(200);
    expect(adminSpy.getAllRecharges).toHaveBeenCalled(); // API doesn't handle search, local filtering done instead
    expect(component.recharges().length).toBe(1);
    expect(component.recharges()[0].rechargeId).toBe('REC2');
    
    discardPeriodicTasks();
  }));

  it('applyDateFilter should pass date format correctly', () => {
    fixture.detectChanges();
    adminSpy.getAllRecharges.calls.reset();

    component.filterStartDate = '2023-10-01';
    component.filterEndDate = '2023-10-31';
    
    component.applyDateFilter();
    
    expect(adminSpy.getAllRecharges).toHaveBeenCalledWith(
      0, 10, 'ALL', '2023-10-01T00:00:00', '2023-10-31T23:59:59'
    );
  });

  it('clearDateFilter should reset dates and reload', () => {
    fixture.detectChanges();
    adminSpy.getAllRecharges.calls.reset();

    component.filterStartDate = '2023-10-01';
    component.filterEndDate = '2023-10-31';
    
    component.clearDateFilter();
    
    expect(component.filterStartDate).toBe('');
    expect(component.filterEndDate).toBe('');
    expect(adminSpy.getAllRecharges).toHaveBeenCalledWith(0, 10, 'ALL', undefined, undefined);
  });

  it('clearFilters should reset search query, dates, category and reload', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.getAllRecharges.calls.reset();

    component.searchQuery = 'TEST';
    component.filterStartDate = '2023-10-01';
    component.rechargeCategory.set('FAILED');
    
    component.clearFilters();
    expect(component.searchQuery).toBe('');
    expect(component.filterStartDate).toBe('');
    expect(component.rechargeCategory()).toBe('ALL');
    
    tick(400); // Because clearSearch emits to subject
    expect(adminSpy.getAllRecharges).toHaveBeenCalledWith(0, 10, 'ALL', undefined, undefined);
    
    discardPeriodicTasks();
  }));

  it('getPageNumbers should handle sliding window logic properly', () => {
    component.totalPages.set(10);
    component.currentPage.set(2);
    expect(component.getPageNumbers()).toEqual([0, 1, 2, 3, 4]);

    component.currentPage.set(8);
    expect(component.getPageNumbers()).toEqual([5, 6, 7, 8, 9]);

    component.totalPages.set(3);
    component.currentPage.set(1);
    expect(component.getPageNumbers()).toEqual([0, 1, 2]);
  });

  it('loadRecharges should handle API errors gracefully', () => {
    fixture.detectChanges();
    adminSpy.getAllRecharges.and.returnValue(throwError(() => new Error('API down')));
    
    component.loadRecharges(0);
    expect(component.loading()).toBeFalse();
    expect(component.applyingDateFilter()).toBeFalse();
  });
});

