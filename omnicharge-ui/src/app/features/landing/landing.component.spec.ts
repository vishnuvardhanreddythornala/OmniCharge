import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LandingComponent } from './landing.component';
import { OperatorService } from '../../core/services/operator.service';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';

describe('LandingComponent', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;
  let operatorSpy: jasmine.SpyObj<OperatorService>;
  let router: Router;

  beforeEach(async () => {
    operatorSpy = jasmine.createSpyObj('OperatorService', [
      'loadActiveOperators', 'detectOperator', 'clearSelection', 'setManualOperator'
    ]);
    (operatorSpy as any).selectedOperator = signal(null);
    (operatorSpy as any).isDetecting = signal(false);
    (operatorSpy as any).detectionFailed = signal(false);
    (operatorSpy as any).isManualOverride = signal(false);
    (operatorSpy as any).operators = signal([{ id: 1, name: 'Jio' }, { id: 2, name: 'Airtel' }]);
    operatorSpy.detectOperator.and.returnValue(of({ operatorName: 'Jio', operatorId: 1, type: 'PREPAID' } as any));

    await TestBed.configureTestingModule({
      imports: [LandingComponent, RouterTestingModule, FormsModule],
      providers: [
        { provide: OperatorService, useValue: operatorSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(LandingComponent);
    component = fixture.componentInstance;
  });

  it('should create and load operators on init', () => {
    fixture.detectChanges();
    expect(operatorSpy.loadActiveOperators).toHaveBeenCalled();
  });

  it('onMobileInput should strip non-digits', () => {
    component.mobileNumber = '98abc76543';
    component.onMobileInput();
    expect(component.mobileNumber).toBe('9876543');
  });

  it('onMobileInput should detect operator for 10-digit numbers', () => {
    component.mobileNumber = '9876543210';
    component.onMobileInput();
    expect(operatorSpy.detectOperator).toHaveBeenCalledWith('9876543210');
  });

  it('onMobileInput should clear selection for less than 10 digits', () => {
    component.mobileNumber = '98765';
    component.onMobileInput();
    expect(operatorSpy.clearSelection).toHaveBeenCalled();
  });

  it('toggleOperatorDropdown should toggle dropdown signal', () => {
    expect(component.showOperatorDropdown()).toBeFalse();
    component.toggleOperatorDropdown();
    expect(component.showOperatorDropdown()).toBeTrue();
  });

  it('selectManualOperator should set operator and close dropdown', () => {
    component.showOperatorDropdown.set(true);
    const op = { id: 1, name: 'Jio' } as any;
    
    component.selectManualOperator(op);
    
    expect(operatorSpy.setManualOperator).toHaveBeenCalledWith(op as any);
    expect(component.showOperatorDropdown()).toBeFalse();
  });

  it('goToRecharge should navigate with mobile and operatorId when available', () => {
    component.mobileNumber = '9876543210';
    (operatorSpy as any).selectedOperator.set({ operatorId: 1, operatorName: 'Jio' });
    
    component.goToRecharge();
    
    expect(router.navigate).toHaveBeenCalledWith(['/recharge'], { queryParams: { mobile: '9876543210', operatorId: 1 } });
  });

  it('goToRecharge should navigate with only mobile when no operator detected', () => {
    component.mobileNumber = '9876543210';
    (operatorSpy as any).selectedOperator.set(null);
    
    component.goToRecharge();
    
    expect(router.navigate).toHaveBeenCalledWith(['/recharge'], { queryParams: { mobile: '9876543210' } });
  });

  it('goToRecharge should not navigate if mobile < 10 digits', () => {
    component.mobileNumber = '123';
    component.goToRecharge();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
