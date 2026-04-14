import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AdminOperatorsComponent } from './admin-operators.component';
import { AdminService, AdminOperatorResponse } from '../../core/services/admin.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('AdminOperatorsComponent', () => {
  let component: AdminOperatorsComponent;
  let fixture: ComponentFixture<AdminOperatorsComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockOperators: AdminOperatorResponse[] = [
    { id: 1, name: 'Airtel', code: 'ART', category: 'PREPAID', logoUrl: '', isActive: true, planCount: 10 },
    { id: 2, name: 'Jio', code: 'JIO', category: 'PREPAID', logoUrl: '', isActive: false, planCount: 5 }
  ];

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', [
      'getAllOperators', 'createOperator', 'updateOperator', 'deleteOperator',
      'activateOperator', 'deactivateOperator', 'createPlan'
    ]);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    adminSpy.getAllOperators.and.returnValue(of({ success: true, message: 'OK', data: mockOperators }));

    await TestBed.configureTestingModule({
      imports: [AdminOperatorsComponent],
      providers: [
        { provide: AdminService, useValue: adminSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminOperatorsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    if ((component as any).toastTimer) {
      clearTimeout((component as any).toastTimer);
    }
  });

  it('should list operators on init', () => {
    fixture.detectChanges();
    expect(adminSpy.getAllOperators).toHaveBeenCalled();
    expect(component.operators().length).toBe(2);
    expect(component.activeOperatorCount()).toBe(1);
    expect(component.inactiveOperatorCount()).toBe(1);
  });

  describe('createOperator', () => {
    it('should call API and close modal on success', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.createOperator.and.returnValue(of({ success: true, message: 'OK', data: mockOperators[0] }));
      
      component.newOperator = { name: 'Vodafone', code: 'VIL', category: 'PREPAID' };
      component.createOperator();
      
      expect(adminSpy.createOperator).toHaveBeenCalled();
      expect(component.showCreateOperator()).toBeFalse();
      expect(component.toastType()).toBe('success');
      tick(4000);
      discardPeriodicTasks();
    }));

    it('should stop if required fields are missing', () => {
      component.newOperator = { name: '', code: '', category: '' };
      component.createOperator();
      expect(component.toastType()).toBe('error');
      expect(adminSpy.createOperator).not.toHaveBeenCalled();
    });

    it('should handle API errors gracefully', fakeAsync(() => {
      fixture.detectChanges();
      component.newOperator = { name: 'Vodafone', code: 'VIL', category: 'PREPAID' };
      adminSpy.createOperator.and.returnValue(throwError(() => new Error('Creation failed')));
      component.createOperator();
      expect(component.toastType()).toBe('error');
      tick(4000);
      discardPeriodicTasks();
    }));
  });

  describe('updateOperator', () => {
    it('should call API and close modal on success', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.updateOperator.and.returnValue(of({ success: true, message: 'OK', data: mockOperators[0] }));
      
      component.editOperator = { id: 1, name: 'Airtel updated', code: 'ART', category: 'PREPAID', logoUrl: '' };
      component.updateOperator();
      
      expect(adminSpy.updateOperator).toHaveBeenCalledWith(1, jasmine.any(Object));
      expect(component.showEditOperator()).toBeFalse();
      expect(component.toastType()).toBe('success');
      tick(4000);
      discardPeriodicTasks();
    }));

    it('should stop if required fields are missing', () => {
      component.editOperator = { id: 1, name: '', code: '', category: '', logoUrl: '' };
      component.updateOperator();
      expect(component.toastType()).toBe('error');
      expect(adminSpy.updateOperator).not.toHaveBeenCalled();
    });

    it('should handle API errors gracefully', fakeAsync(() => {
      fixture.detectChanges();
      component.editOperator = { id: 1, name: 'Airtel', code: 'ART', category: 'PREPAID', logoUrl: '' };
      adminSpy.updateOperator.and.returnValue(throwError(() => new Error('API down')));
      component.updateOperator();
      expect(component.toastType()).toBe('error');
      tick(4000);
      discardPeriodicTasks();
    }));
  });

  describe('deleteOperator', () => {
    it('should call API and close confirmation', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.deleteOperator.and.returnValue(of({ success: true, message: 'OK' } as any));
      
      component.operatorToDelete.set(mockOperators[0]);
      component.deleteOperator();
      
      expect(adminSpy.deleteOperator).toHaveBeenCalledWith(1);
      expect(component.showDeleteConfirm()).toBeFalse();
      tick(4000);
      discardPeriodicTasks();
    }));

    it('should do nothing if operatorToDelete is null', () => {
      component.operatorToDelete.set(null);
      component.deleteOperator();
      expect(adminSpy.deleteOperator).not.toHaveBeenCalled();
    });

    it('should handle API errors gracefully', fakeAsync(() => {
      fixture.detectChanges();
      component.operatorToDelete.set(mockOperators[0]);
      adminSpy.deleteOperator.and.returnValue(throwError(() => new Error('Cannot delete')));
      component.deleteOperator();
      expect(component.toastType()).toBe('error');
      tick(4000);
      discardPeriodicTasks();
    }));
  });

  it('toggleOperatorStatus should correctly activate or deactivate', fakeAsync(() => {
    fixture.detectChanges();
    adminSpy.deactivateOperator.and.returnValue(of({ success: true, message: 'OK', data: mockOperators[0] }));
    
    component.toggleOperatorStatus(mockOperators[0]); // initially active
    
    expect(adminSpy.deactivateOperator).toHaveBeenCalledWith(1);
    tick(4000);
    discardPeriodicTasks();
  }));

  describe('createPlan', () => {
    it('should post against the correct operator', fakeAsync(() => {
      fixture.detectChanges();
      adminSpy.createPlan.and.returnValue(of({ success: true, message: 'OK', data: {} as any }));
      
      component.selectedOperator.set(mockOperators[0]);
      component.newPlan = { planName: 'Test Plan', price: 10, validityDays: 1, dataLimit: '', callBenefit: '', smsBenefit: '', additionalBenefits: '', category: 'ALL' };
      component.createPlan();
      
      expect(adminSpy.createPlan).toHaveBeenCalledWith(1, jasmine.any(Object));
      expect(component.showAddPlan()).toBeFalse();
      tick(4000);
      discardPeriodicTasks();
    }));

    it('should abort if operator is not selected or fields are missing', () => {
      component.selectedOperator.set(mockOperators[0]);
      component.newPlan = { planName: '', price: 0, validityDays: 0, category: '', dataLimit: '', callBenefit: '', smsBenefit: '', additionalBenefits: '' };
      component.createPlan();
      expect(component.toastType()).toBe('error');
      expect(adminSpy.createPlan).not.toHaveBeenCalled();
    });

    it('should handle API errors gracefully', fakeAsync(() => {
      fixture.detectChanges();
      component.selectedOperator.set(mockOperators[0]);
      component.newPlan = { planName: 'Test Plan', price: 10, validityDays: 1, category: 'ALL', dataLimit: '', callBenefit: '', smsBenefit: '', additionalBenefits: '' };
      adminSpy.createPlan.and.returnValue(throwError(() => new Error('Error')));
      component.createPlan();
      expect(component.toastType()).toBe('error');
      tick(4000);
      discardPeriodicTasks();
    }));
  });

  it('viewPlans should navigate to plans sub-route', () => {
    fixture.detectChanges();
    component.viewPlans(mockOperators[0]);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/operators', 1, 'plans']);
  });

  describe('Helpers', () => {
    it('getCategoryClass returns correct class', () => {
      expect(component.getCategoryClass('PREPAID')).toContain('sky');
      expect(component.getCategoryClass('POSTPAID')).toContain('violet');
      expect(component.getCategoryClass('DTH')).toContain('amber');
      expect(component.getCategoryClass('OTHER')).toContain('bg-white');
    });

    it('formatDate formats array dates', () => {
      expect(component.formatDate([2023, 5, 20, 14, 30, 0])).toContain('2023');
      expect(component.formatDate([2023, 5, 20])).toContain('2023');
      expect(component.formatDate(null)).toBe('—');
      expect(component.formatDate('not a date')).toBe('—');
    });
  });
});
