import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AdminTransactionsComponent } from './admin-transactions.component';
import { AdminService } from '../../core/services/admin.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';

describe('AdminTransactionsComponent', () => {
  let component: AdminTransactionsComponent;
  let fixture: ComponentFixture<AdminTransactionsComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  const mockTransactions = [
    { transactionId: 'TXN1', rechargeId: 'REC1', amount: 100, status: 'SUCCESS', email: 'test1@test.com', createdDate: '2023-01-01', failureReason: null },
    { transactionId: 'TXN2', rechargeId: 'REC2', amount: 200, status: 'FAILED', email: 'test2@test.com', createdDate: '2023-01-01', failureReason: 'Bank issue' },
    { transactionId: 'TXN3', rechargeId: 'REC3', amount: 300, status: 'PENDING', email: 'test3@test.com', createdDate: '2023-01-01', failureReason: null }
  ];

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', ['getAllTransactions']);

    adminSpy.getAllTransactions.and.returnValue(of({
      success: true, message: 'OK', data: {
        content: mockTransactions,
        totalElements: 3,
        totalPages: 1,
        number: 0
      } as any
    }));

    await TestBed.configureTestingModule({
      imports: [AdminTransactionsComponent, FormsModule],
      providers: [
        { provide: AdminService, useValue: adminSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminTransactionsComponent);
    component = fixture.componentInstance;
  });

  it('should initialize and load transactions', () => {
    fixture.detectChanges();
    
    expect(adminSpy.getAllTransactions).toHaveBeenCalledWith(0, 10, 'ALL', '', undefined, undefined);
    expect(component.transactions().length).toBe(3);
    
    // Check stats count logic matching what's in component
    expect(component.successCount()).toBe(1);
    expect(component.failedCount()).toBe(1);
    expect(component.pendingCount()).toBe(1);
    expect(component.totalElements()).toBe(3);
  });

  it('setCategory should change category and reload', () => {
    fixture.detectChanges();
    adminSpy.getAllTransactions.calls.reset();

    component.setCategory('SUCCESS');
    expect(component.transactionCategory()).toBe('SUCCESS');
    expect(adminSpy.getAllTransactions).toHaveBeenCalledWith(0, 10, 'SUCCESS', '', undefined, undefined);
  });

  it('should trigger search filter with debounce', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.getAllTransactions.calls.reset();

    component.onSearchChange('TXN2');
    
    tick(200);
    expect(adminSpy.getAllTransactions).not.toHaveBeenCalled();

    tick(200);
    expect(adminSpy.getAllTransactions).toHaveBeenCalledWith(0, 10, 'ALL', 'TXN2', undefined, undefined);
    
    discardPeriodicTasks();
  }));

  it('clearSearch should reset search query and reload via subject', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.getAllTransactions.calls.reset();

    component.searchQuery = 'TEST';
    component.clearSearch();
    
    expect(component.searchQuery).toBe('');
    
    tick(400); // debounce
    expect(adminSpy.getAllTransactions).toHaveBeenCalledWith(0, 10, 'ALL', '', undefined, undefined);
    
    discardPeriodicTasks();
  }));

  it('applyDateFilter should pass date format correctly', () => {
    fixture.detectChanges();
    adminSpy.getAllTransactions.calls.reset();

    component.filterStartDate = '2023-11-01';
    component.filterEndDate = '2023-11-30';
    
    component.applyDateFilter();
    
    expect(adminSpy.getAllTransactions).toHaveBeenCalledWith(
      0, 10, 'ALL', '', '2023-11-01T00:00:00', '2023-11-30T23:59:59'
    );
  });

  it('clearDateFilter should reset dates and reload', () => {
    fixture.detectChanges();
    adminSpy.getAllTransactions.calls.reset();

    component.filterStartDate = '2023-11-01';
    component.filterEndDate = '2023-11-30';
    
    component.clearDateFilter();
    
    expect(component.filterStartDate).toBe('');
    expect(component.filterEndDate).toBe('');
    expect(adminSpy.getAllTransactions).toHaveBeenCalledWith(0, 10, 'ALL', '', undefined, undefined);
  });

  it('loadTransactions should handle API errors gracefully', () => {
    fixture.detectChanges();
    adminSpy.getAllTransactions.and.returnValue(throwError(() => new Error('API Error')));
    
    component.loadTransactions(0);
    expect(component.loading()).toBeFalse();
    expect(component.applyingDateFilter()).toBeFalse();
  });

  it('getPageNumbers should return correct range', () => {
    component.totalPages.set(5);
    component.currentPage.set(2);
    expect(component.getPageNumbers()).toEqual([0, 1, 2, 3, 4]);
    
    component.totalPages.set(10);
    component.currentPage.set(5);
    expect(component.getPageNumbers()).toEqual([3, 4, 5, 6, 7]);
  });
});
