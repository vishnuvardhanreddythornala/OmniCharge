import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AdminPlansComponent } from './admin-plans.component';
import { AdminService } from '../../core/services/admin.service';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';

describe('AdminPlansComponent', () => {
  let component: AdminPlansComponent;
  let fixture: ComponentFixture<AdminPlansComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  const mockPlans = [
    { id: 1, planName: 'Plan A', price: 100, validityDays: 30, category: 'DATA', operatorId: 1, operatorName: 'Airtel', isActive: true },
    { id: 2, planName: 'Plan B', price: 200, validityDays: 60, category: 'UNLIMITED', operatorId: 2, operatorName: 'Jio', isActive: false }
  ];

  const mockOperators = [
    { id: 1, name: 'Airtel', code: 'ART', category: 'PREPAID', logoUrl: '', isActive: true, planCount: 1 },
    { id: 2, name: 'Jio', code: 'JIO', category: 'PREPAID', logoUrl: '', isActive: true, planCount: 1 }
  ];

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', [
      'getAllOperators', 'searchAllPlans', 'activatePlan', 'deactivatePlan', 'updatePlan', 'createPlan'
    ]);

    adminSpy.getAllOperators.and.returnValue(of({ success: true, message: 'OK', data: mockOperators }));
    adminSpy.searchAllPlans.and.returnValue(of({
      success: true, message: 'OK', data: {
        content: mockPlans,
        totalElements: 2,
        totalPages: 1,
        size: 10,
        number: 0
      }
    } as any));

    await TestBed.configureTestingModule({
      imports: [AdminPlansComponent, RouterTestingModule, FormsModule],
      providers: [
        { provide: AdminService, useValue: adminSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPlansComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    if ((component as any).toastTimer) {
      clearTimeout((component as any).toastTimer);
    }
  });

  it('should list plans on init', () => {
    fixture.detectChanges();
    expect(adminSpy.searchAllPlans).toHaveBeenCalledWith(0, 10, undefined, undefined, undefined, '');
    expect(component.allPlans().length).toBe(2);
    expect(component.kpiActive()).toBe(1);
    expect(component.kpiInactive()).toBe(1);
  });

  it('should trigger search searchAllPlans on filter change', () => {
    fixture.detectChanges();
    adminSpy.searchAllPlans.calls.reset();
    
    component.searchQuery = 'test';
    component.onFilterChange();
    
    expect(adminSpy.searchAllPlans).toHaveBeenCalledWith(0, 10, undefined, undefined, undefined, 'test');
  });

  it('createPlan should post against the correct operator', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.createPlan.and.returnValue(of({ success: true, message: 'OK', data: {} as any }));
    
    component.openAddModal();
    component.editForm.operatorId = 1;
    component.editForm.planName = 'New Plan';
    component.savePlan();
    
    expect(adminSpy.createPlan).toHaveBeenCalledWith(1, jasmine.any(Object));
    expect(component.showEditModal()).toBeFalse();
    expect(component.toastType()).toBe('success');
    tick(4000);
    discardPeriodicTasks();
  }));

  it('updatePlan should call update if editPlanId exists', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.updatePlan.and.returnValue(of({ success: true, message: 'OK', data: mockPlans[0] as any }));
    
    component.openEditModal(mockPlans[0] as any);
    component.editForm.price = 150;
    component.savePlan();
    
    expect(adminSpy.updatePlan).toHaveBeenCalledWith(1, jasmine.any(Object));
    expect(component.showEditModal()).toBeFalse();
    expect(component.toastType()).toBe('success');
    tick(4000);
    discardPeriodicTasks();
  }));

  it('togglePlanStatus should activate or deactivate correctly', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.deactivatePlan.and.returnValue(of({ success: true, message: 'OK', data: mockPlans[0] as any }));
    
    component.togglePlanStatus(mockPlans[0] as any);
    
    expect(adminSpy.deactivatePlan).toHaveBeenCalledWith(1);
    expect(component.toastType()).toBe('success');
    tick(4000);
    discardPeriodicTasks();
  }));
  
  it('prevPage / nextPage should navigate properly', () => {
    fixture.detectChanges();
    adminSpy.searchAllPlans.calls.reset();
    
    component.totalPages.set(3);
    component.currentPage.set(1);
    
    component.nextPage();
    expect(component.currentPage()).toBe(2);
    expect(adminSpy.searchAllPlans).toHaveBeenCalledTimes(1);

    component.prevPage();
    expect(component.currentPage()).toBe(1);
    expect(adminSpy.searchAllPlans).toHaveBeenCalledTimes(2);
  });

  describe('Error Handling', () => {
    it('loadAllPlans should handle API errors gracefully', () => {
      fixture.detectChanges();
      adminSpy.searchAllPlans.and.returnValue(throwError(() => new Error('API down')));
      
      component.loadAllPlans();
      expect(component.loading()).toBeFalse();
      expect(component.allPlans().length).toBe(0);
    });

    it('togglePlanStatus should handle API errors gracefully', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.deactivatePlan.and.returnValue(throwError(() => new Error('API down')));
      
      component.togglePlanStatus(mockPlans[0] as any);
      expect(component.toastType()).toBe('error');
      tick(4000);
      discardPeriodicTasks();
    }));

    it('createPlan should handle API errors gracefully', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.createPlan.and.returnValue(throwError(() => new Error('API down')));
      
      component.openAddModal();
      component.editForm.operatorId = 1;
      component.savePlan();
      
      expect(component.toastType()).toBe('error');
      tick(4000);
      discardPeriodicTasks();
    }));

    it('createPlan should complain if no operator is selected', () => {
      component.openAddModal();
      component.editForm.operatorId = null;
      component.savePlan();
      expect(component.toastType()).toBe('error');
      expect(component.toastMessage()).toBe('Please select an Operator');
    });

    it('updatePlan should handle API errors gracefully', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.updatePlan.and.returnValue(throwError(() => new Error('API down')));
      
      component.openEditModal(mockPlans[0] as any);
      component.savePlan();
      
      expect(component.toastType()).toBe('error');
      tick(4000);
      discardPeriodicTasks();
    }));
  });

  describe('Helpers', () => {
    it('formatDate formats array dates', () => {
      expect(component.formatDate([2023, 5, 20, 14, 30, 0])).toContain('May');
      expect(component.formatDate([2023, 5, 20])).toContain('May');
      expect(component.formatDate(null)).toBe('—');
      expect(component.formatDate('not a date')).toBe('—');
    });

    it('getCategoryBadge maps categories', () => {
      expect(component.getCategoryBadge('RECOMMENDED')).toContain('omni-500');
      expect(component.getCategoryBadge('DATA')).toContain('sky');
      expect(component.getCategoryBadge('UNLIMITED')).toContain('emerald');
      expect(component.getCategoryBadge('TALKTIME')).toContain('violet');
      expect(component.getCategoryBadge('OTHER')).toContain('white');
    });
  });
});
