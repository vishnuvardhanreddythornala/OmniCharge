import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AdminOperatorPlansComponent } from './admin-operator-plans.component';
import { AdminService } from '../../core/services/admin.service';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';

describe('AdminOperatorPlansComponent', () => {
  let component: AdminOperatorPlansComponent;
  let fixture: ComponentFixture<AdminOperatorPlansComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  const mockOperator = { id: 1, name: 'Airtel', code: 'ART', category: 'PREPAID', logoUrl: '', isActive: true, planCount: 2 };
  const mockPlans = [
    { id: 1, planName: 'Plan 1', price: 100, validityDays: 30, category: 'DATA', operatorId: 1, isActive: true },
    { id: 2, planName: 'Plan 2', price: 200, validityDays: 60, category: 'UNLIMITED', operatorId: 1, isActive: false, deactivatedByOperator: true }
  ];

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', [
      'getAllOperators', 'getOperatorPlans', 'createPlan', 'updatePlan', 'deletePlan', 'activatePlan', 'deactivatePlan'
    ]);

    adminSpy.getAllOperators.and.returnValue(of({ success: true, message: 'OK', data: [mockOperator] }));
    adminSpy.getOperatorPlans.and.returnValue(of({ success: true, message: 'OK', data: mockPlans as any }));

    await TestBed.configureTestingModule({
      imports: [AdminOperatorPlansComponent, RouterTestingModule, FormsModule],
      providers: [
        { provide: AdminService, useValue: adminSpy },
        { 
          provide: ActivatedRoute, 
          useValue: { snapshot: { paramMap: { get: () => '1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminOperatorPlansComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    if ((component as any).toastTimer) {
      clearTimeout((component as any).toastTimer);
    }
  });

  it('should load operator and plans on init based on route param', () => {
    fixture.detectChanges();
    expect(adminSpy.getAllOperators).toHaveBeenCalled();
    expect(adminSpy.getOperatorPlans).toHaveBeenCalledWith(1);
    expect(component.operator()?.name).toBe('Airtel');
    expect(component.plans().length).toBe(2);
  });

  it('filteredPlans should apply category and status filters', () => {
    fixture.detectChanges();
    
    // Test Category filter
    component.categoryFilter.set('DATA');
    expect(component.filteredPlans().length).toBe(1);
    expect(component.filteredPlans()[0].planName).toBe('Plan 1');

    // Test Status filter (AUTO - deactivated by operator)
    component.categoryFilter.set('ALL');
    component.statusFilter.set('AUTO');
    expect(component.filteredPlans().length).toBe(1);
    expect(component.filteredPlans()[0].planName).toBe('Plan 2');

    // Test Search filter
    component.statusFilter.set('ALL');
    component.searchQuery = 'Plan 2';
    expect(component.filteredPlans().length).toBe(1);
    expect(component.filteredPlans()[0].price).toBe(200);
  });

  it('bulkActivate should call activatePlan for all selected items', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.activatePlan.and.returnValue(of({ success: true, message: 'Activated', data: {} as any }));

    component.selectedIds.set(new Set([1, 2]));
    component.bulkActivate();
    
    expect(adminSpy.activatePlan).toHaveBeenCalledTimes(2);
    expect(adminSpy.activatePlan).toHaveBeenCalledWith(1);
    expect(adminSpy.activatePlan).toHaveBeenCalledWith(2);
    
    tick(4000);
    discardPeriodicTasks();
  }));

  it('bulkDeactivate should call deactivatePlan for all selected items', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.deactivatePlan.and.returnValue(of({ success: true, message: 'Deactivated', data: {} as any }));

    component.selectedIds.set(new Set([1]));
    component.bulkDeactivate();
    
    expect(adminSpy.deactivatePlan).toHaveBeenCalledTimes(1);
    expect(adminSpy.deactivatePlan).toHaveBeenCalledWith(1);
    
    tick(4000);
    discardPeriodicTasks();
  }));

  it('toggleSelectAll should select all or clear', () => {
    fixture.detectChanges();
    
    component.toggleSelectAll();
    expect(component.selectedIds().size).toBe(2);
    
    component.toggleSelectAll();
    expect(component.selectedIds().size).toBe(0);
  });

  it('deletePlan should call deletePlan API on confirmation', fakeAsync(() => {
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(true);
    adminSpy.deletePlan.and.returnValue(of({ success: true, message: 'Deleted' } as any));

    component.deletePlan(mockPlans[0] as any);
    
    expect(adminSpy.deletePlan).toHaveBeenCalledWith(1);
    expect(component.toastType()).toBe('success');
    
    tick(4000);
    discardPeriodicTasks();
  }));

  describe('togglePlanStatus', () => {
    it('should show error if operator is inactive', () => {
      fixture.detectChanges();
      // Wait for load first
      component.operator.set({ ...mockOperator, isActive: false } as any);
      
      component.togglePlanStatus(mockPlans[0] as any);
      expect(component.toastType()).toBe('error');
      expect(component.toastMessage()).toBe('Cannot modify plans while operator is inactive');
      expect(adminSpy.deactivatePlan).not.toHaveBeenCalled();
    });

    it('should call deactivate if plan is active', () => {
      fixture.detectChanges();
      adminSpy.deactivatePlan.and.returnValue(of({ success: true } as any));
      component.togglePlanStatus({ ...mockPlans[0], isActive: true } as any);
      expect(adminSpy.deactivatePlan).toHaveBeenCalled();
    });

    it('should call activate if plan is inactive', () => {
      fixture.detectChanges();
      adminSpy.activatePlan.and.returnValue(of({ success: true } as any));
      component.togglePlanStatus({ ...mockPlans[0], isActive: false } as any);
      expect(adminSpy.activatePlan).toHaveBeenCalled();
    });

    it('should handle API errors', () => {
      fixture.detectChanges();
      adminSpy.deactivatePlan.and.returnValue(throwError(() => new Error('API failed')));
      component.togglePlanStatus({ ...mockPlans[0], isActive: true } as any);
      expect(component.toastType()).toBe('error');
    });
  });

  describe('saveEditPlan', () => {
    it('should do nothing if editPlanId is null', () => {
      component.editPlanId = null;
      component.saveEditPlan();
      expect(adminSpy.updatePlan).not.toHaveBeenCalled();
    });

    it('should call updatePlan and clear state on success', () => {
      fixture.detectChanges();
      component.editPlanId = 1;
      component.editForm = { planName: 'Test' } as any;
      adminSpy.updatePlan.and.returnValue(of({ success: true } as any));
      
      component.saveEditPlan();
      expect(adminSpy.updatePlan).toHaveBeenCalledWith(1, component.editForm as any);
      expect(component.showEditModal()).toBeFalse();
    });

    it('should show toast on failure', () => {
      fixture.detectChanges();
      component.editPlanId = 1;
      adminSpy.updatePlan.and.returnValue(of({ success: false, message: 'Bad' } as any));
      component.saveEditPlan();
      expect(component.toastType()).toBe('error');
    });

    it('should handle exceptions gracefully', () => {
      fixture.detectChanges();
      component.editPlanId = 1;
      adminSpy.updatePlan.and.returnValue(throwError(() => new Error('API error')));
      component.saveEditPlan();
      expect(component.toastType()).toBe('error');
    });
  });

  describe('saveNewPlan', () => {
    it('should do nothing if operatorId is not set', () => {
      component.operatorId.set(null);
      component.saveNewPlan();
      expect(adminSpy.createPlan).not.toHaveBeenCalled();
    });

    it('should call createPlan and clear state on success', () => {
      fixture.detectChanges();
      component.operatorId.set(1);
      adminSpy.createPlan.and.returnValue(of({ success: true } as any));
      
      component.saveNewPlan();
      expect(adminSpy.createPlan).toHaveBeenCalled();
      expect(component.showAddModal()).toBeFalse();
      expect(component.addForm.planName).toBe('');
    });

    it('should handle API errors gracefully', () => {
      fixture.detectChanges();
      component.operatorId.set(1);
      adminSpy.createPlan.and.returnValue(throwError(() => new Error('API error')));
      component.saveNewPlan();
      expect(component.toastType()).toBe('error');
    });
  });

  describe('Utility Methods', () => {
    it('openEditModal should setup edit state', () => {
      component.openEditModal(mockPlans[0] as any);
      expect(component.editPlanId).toBe(1);
      expect(component.showEditModal()).toBeTrue();
      expect(component.editForm.planName).toBe('Plan 1');
    });

    it('formatDate should return formatted string', () => {
      const date = '2023-01-01T12:00:00.000Z';
      const formatted = component.formatDate(date);
      expect(formatted).not.toBe(date);
    });

    it('formatDate should return Invalid Date when invalid', () => {
      const result = component.formatDate('not a date');
      expect(result).toBe('Invalid Date');
    });
  });
});
