import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AdminUsersComponent } from './admin-users.component';
import { AdminService } from '../../core/services/admin.service';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('AdminUsersComponent', () => {
  let component: AdminUsersComponent;
  let fixture: ComponentFixture<AdminUsersComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  const mockUsersResponse = {
    success: true,
    data: {
      content: [{ id: 1, fullName: 'Alice', isActive: true, role: 'ROLE_USER' }, { id: 2, fullName: 'Bob', isActive: false, role: 'ROLE_ADMIN' }],
      totalElements: 2,
      totalPages: 1,
      number: 0
    }
  };

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', ['getAllUsers', 'toggleUserStatus']);
    
    adminSpy.getAllUsers.and.returnValue(of(mockUsersResponse as any));
    adminSpy.toggleUserStatus.and.returnValue(of({ success: true } as any));

    await TestBed.configureTestingModule({
      imports: [AdminUsersComponent],
      providers: [
        { provide: AdminService, useValue: adminSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsersComponent);
    component = fixture.componentInstance;
  });

  it('should create and load data on init', () => {
    fixture.detectChanges();
    
    // Stats fetched for ACTIVE and SUSPENDED counts
    expect(adminSpy.getAllUsers).toHaveBeenCalledWith(0, 1, undefined, 'ACTIVE');
    expect(adminSpy.getAllUsers).toHaveBeenCalledWith(0, 1, undefined, 'SUSPENDED');
    
    // Load users fetched with default 'ALL' filter
    expect(adminSpy.getAllUsers).toHaveBeenCalledWith(0, 10, undefined, 'ALL');
    
    expect(component.users().length).toBe(2);
    expect(component.totalUsersCount()).toBe(4); // activeCount(2) + suspendedCount(2)
  });

  it('should update search query via subject', fakeAsync(() => {
    fixture.detectChanges();
    component.onSearchChange('Alice');
    
    tick(600); // Wait for debounceTime(500)
    
    expect(component.searchQuery).toBe('Alice');
    expect(adminSpy.getAllUsers).toHaveBeenCalledWith(0, 10, 'Alice', 'ALL');
  }));

  it('should clear search', fakeAsync(() => {
    fixture.detectChanges();
    component.onSearchChange('Alice');
    tick(600);
    
    component.clearSearch();
    tick(600);
    
    expect(component.searchQuery).toBe('');
    expect(adminSpy.getAllUsers).toHaveBeenCalledWith(0, 10, undefined, 'ALL');
  }));

  it('setStatusFilter should fetch filtered users', () => {
    fixture.detectChanges();
    component.setStatusFilter('SUSPENDED');
    expect(component.activeStatus()).toBe('SUSPENDED');
    expect(adminSpy.getAllUsers).toHaveBeenCalledWith(0, 10, undefined, 'SUSPENDED');
  });

  it('should handle API errors gracefully in loadUsers', () => {
    adminSpy.getAllUsers.and.returnValue(throwError(() => new Error('API failed')));
    component.loadUsers(0);
    
    expect(component.loading()).toBeFalse();
  });

  it('getPageNumbers should return correct range', () => {
    component.totalPages.set(5);
    component.currentPage.set(2);
    expect(component.getPageNumbers()).toEqual([0, 1, 2, 3, 4]);
    
    component.totalPages.set(10);
    component.currentPage.set(5);
    expect(component.getPageNumbers()).toEqual([3, 4, 5, 6, 7]);
  });

  describe('toggleStatus', () => {
    it('should call api and update state directly on success', () => {
      fixture.detectChanges(); // load users
      component.toggleStatus({ id: 1, isActive: true } as any);
      
      expect(adminSpy.toggleUserStatus).toHaveBeenCalledWith(1, false);
      expect(component.users().find(u => u.id === 1)?.isActive).toBeFalse();
      expect(component.togglingUser()).toBeNull();
    });

    it('should ignore failures from toggle api', () => {
      fixture.detectChanges();
      adminSpy.toggleUserStatus.and.returnValue(of({ success: false } as any));
      component.toggleStatus({ id: 1, isActive: true } as any);
      
      expect(component.togglingUser()).toBeNull();
    });

    it('should recover gracefully from exceptions from toggle api', () => {
      fixture.detectChanges();
      adminSpy.toggleUserStatus.and.returnValue(throwError(() => new Error('API Error')));
      component.toggleStatus({ id: 1, isActive: true } as any);
      
      expect(component.togglingUser()).toBeNull();
    });
  });
});
