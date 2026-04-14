import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OperatorService, Operator, DetectedOperator, Plan } from './operator.service';
import { environment } from '../../../environments/environment';

describe('OperatorService', () => {
  let service: OperatorService;
  let httpMock: HttpTestingController;

  const mockOperators: Operator[] = [
    { id: 1, name: 'Jio', code: 'JIO', type: 'PREPAID', description: '', isActive: true },
    { id: 2, name: 'Airtel', code: 'AIRTEL', type: 'PREPAID', description: '', isActive: true }
  ];

  const mockDetected: DetectedOperator = {
    operatorId: 1, operatorName: 'Jio', operatorCode: 'JIO', type: 'PREPAID'
  };

  const mockPlans: Plan[] = [
    { id: 1, planName: 'Basic', price: 199, validityDays: 28, dataLimit: '1.5GB/day', callBenefit: 'Unlimited', smsBenefit: '100/day', additionalBenefits: '', category: 'POPULAR', operatorId: 1 }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OperatorService]
    });
    service = TestBed.inject(OperatorService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('loadActiveOperators()', () => {
    it('should fetch and store active operators', () => {
      service.loadActiveOperators();
      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/operators/active`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, message: 'OK', data: mockOperators });
      expect(service.operators().length).toBe(2);
      expect(service.operators()[0].name).toBe('Jio');
    });

    it('should not update operators if success is false', () => {
      service.loadActiveOperators();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/operators/active`)
        .flush({ success: false, message: 'Error', data: null });
      expect(service.operators().length).toBe(0);
    });
  });

  describe('detectOperator()', () => {
    it('should detect operator and auto-load plans', () => {
      service.detectOperator('9876543210').subscribe();

      const detectReq = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/operators/detect`);
      expect(detectReq.request.params.get('mobileNumber')).toBe('9876543210');
      detectReq.flush({ success: true, message: 'OK', data: mockDetected });

      expect(service.detectedOperator()).toEqual(mockDetected);
      expect(service.selectedOperator()).toEqual(mockDetected);
      expect(service.isDetecting()).toBeFalse();

      // Should auto-load plans
      const plansReq = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/plans/search`);
      expect(plansReq.request.params.get('operatorId')).toBe('1');
      plansReq.flush({ success: true, message: 'OK', data: { content: mockPlans } });
      expect(service.plans().length).toBe(1);
    });

    it('should flag detectionFailed and load operators on failure response', () => {
      service.detectOperator('1234567890').subscribe();

      httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/operators/detect`)
        .flush({ success: false, message: 'Not found', data: null });

      expect(service.detectionFailed()).toBeTrue();
      expect(service.isDetecting()).toBeFalse();

      // Should load active operators as fallback
      const opReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/operators/active`);
      opReq.flush({ success: true, message: 'OK', data: mockOperators });
    });

    it('should handle HTTP error gracefully', () => {
      service.detectOperator('0000000000').subscribe();

      httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/operators/detect`)
        .error(new ProgressEvent('error'), { status: 500 });

      expect(service.detectionFailed()).toBeTrue();
      expect(service.isDetecting()).toBeFalse();

      const opReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/operators/active`);
      opReq.flush({ success: true, message: 'OK', data: mockOperators });
    });
  });

  describe('setManualOperator()', () => {
    it('should set selected operator and load plans', () => {
      service.setManualOperator(mockOperators[0]);

      expect(service.selectedOperator()?.operatorName).toBe('Jio');
      expect(service.isManualOverride()).toBeTrue();

      const plansReq = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/plans/search`);
      plansReq.flush({ success: true, message: 'OK', data: { content: mockPlans } });
    });
  });

  describe('loadPlans()', () => {
    it('should fetch paginated plans with content', () => {
      service.loadPlans(1);
      const req = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/plans/search`);
      expect(req.request.params.get('operatorId')).toBe('1');
      expect(req.request.params.get('size')).toBe('100');
      req.flush({ success: true, message: 'OK', data: { content: mockPlans } });
      expect(service.plans().length).toBe(1);
      expect(service.isLoadingPlans()).toBeFalse();
    });

    it('should handle flat array fallback', () => {
      service.loadPlans(1);
      httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/plans/search`)
        .flush({ success: true, message: 'OK', data: mockPlans });
      expect(service.plans().length).toBe(1);
    });

    it('should handle error and reset loading flag', () => {
      service.loadPlans(1);
      httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/plans/search`)
        .error(new ProgressEvent('error'), { status: 500 });
      expect(service.isLoadingPlans()).toBeFalse();
    });
  });

  describe('getPlanById()', () => {
    it('should fetch a single plan', () => {
      service.getPlanById(1).subscribe(res => {
        expect(res.data?.planName).toBe('Basic');
      });
      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/plans/1`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, message: 'OK', data: mockPlans[0] });
    });
  });

  describe('clearSelection()', () => {
    it('should reset all detection state', () => {
      // Pollute state
      (service as any)._detectedOperator.set(mockDetected);
      (service as any)._selectedOperator.set(mockDetected);
      (service as any)._plans.set(mockPlans);
      (service as any)._detectionFailed.set(true);
      (service as any)._isManualOverride.set(true);

      service.clearSelection();

      expect(service.detectedOperator()).toBeNull();
      expect(service.selectedOperator()).toBeNull();
      expect(service.plans().length).toBe(0);
      expect(service.detectionFailed()).toBeFalse();
      expect(service.isManualOverride()).toBeFalse();
    });
  });
});
