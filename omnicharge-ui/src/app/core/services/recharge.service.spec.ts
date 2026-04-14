import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RechargeService, RechargeRequest, RechargeResponse, RechargeHistoryItem } from './recharge.service';
import { environment } from '../../../environments/environment';

describe('RechargeService', () => {
  let service: RechargeService;
  let httpMock: HttpTestingController;

  const mockRechargeRes: RechargeResponse = {
    rechargeId: 'REC12345',
    mobileNumber: '9876543210',
    operatorName: 'Jio',
    planName: 'Prepaid 199',
    amount: 199,
    planValidityDays: 28,
    planExpiryDate: '2024-05-10',
    status: 'SUCCESS',
    createdDate: '2024-04-10'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RechargeService]
    });
    service = TestBed.inject(RechargeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('initiateRecharge()', () => {
    it('should POST to /api/recharges and set currentRecharge signal on success', () => {
      const reqPayload: RechargeRequest = { mobileNumber: '9999999999', operatorId: 1, planId: 1, paymentMethod: 'RAZORPAY' };
      
      service.initiateRecharge(reqPayload).subscribe(res => {
        expect(res.success).toBeTrue();
        expect(service.currentRecharge()).toEqual(mockRechargeRes);
        expect(service.isProcessing()).toBeFalse();
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/recharges`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(reqPayload);
      req.flush({ success: true, data: mockRechargeRes });
    });

    it('should handle failure properly without setting currentRecharge', () => {
      service.initiateRecharge({} as any).subscribe({
        error: () => {
          // tap() doesn't run on HTTP errors, so isProcessing stays true
          // currentRecharge should not be set
          expect(service.currentRecharge()).toBeNull();
        }
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/recharges`);
      req.flush({ success: false, message: 'Invalid payload' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should set isProcessing during flight', () => {
      service.initiateRecharge({} as any).subscribe();
      expect(service.isProcessing()).toBeTrue();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/recharges`).flush({ success: true, data: mockRechargeRes });
      expect(service.isProcessing()).toBeFalse();
    });
  });

  describe('getRechargeStatus()', () => {
    it('should GET status correctly', () => {
      service.getRechargeStatus('REC123').subscribe(res => {
        expect(res.success).toBeTrue();
        expect(res.data?.status).toBe('SUCCESS');
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/recharges/status/REC123`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, data: mockRechargeRes });
    });

    it('should handle 404 Not Found', () => {
      service.getRechargeStatus('UNKNOWN').subscribe({
        error: (err) => expect(err.status).toBe(404)
      });
      httpMock.expectOne(`${environment.apiBaseUrl}/api/recharges/status/UNKNOWN`).flush({}, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getRechargeById()', () => {
    it('should fetch single recharge record correctly', () => {
      service.getRechargeById('R1').subscribe();
      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/recharges/R1`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, data: mockRechargeRes });
    });
  });

  describe('getRechargeHistory()', () => {
    it('should fetch history with default pagination', () => {
      service.getRechargeHistory().subscribe(res => {
        expect(res.data?.content.length).toBe(1);
      });

      const req = httpMock.expectOne(request => request.url === `${environment.apiBaseUrl}/api/recharges/history`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush({ success: true, data: { content: [mockRechargeRes as RechargeHistoryItem], totalElements: 1 } });
    });

    it('should map custom pagination and date params correctly', () => {
      service.getRechargeHistory(2, 20, '2024-01-01', '2024-12-31').subscribe();

      const req = httpMock.expectOne(request => request.url === `${environment.apiBaseUrl}/api/recharges/history`);
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('20');
      expect(req.request.params.get('startDate')).toBe('2024-01-01');
      expect(req.request.params.get('endDate')).toBe('2024-12-31');
      req.flush({ success: true });
    });
  });

  describe('clearCurrentRecharge()', () => {
    it('should clear internal signals', () => {
      // Setup state
      const reqPayload: RechargeRequest = { mobileNumber: '', operatorId: 1, planId: 1, paymentMethod: '' };
      service.initiateRecharge(reqPayload).subscribe();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/recharges`).flush({ success: true, data: mockRechargeRes });
      
      expect(service.currentRecharge()).not.toBeNull();
      
      service.clearCurrentRecharge();
      
      expect(service.currentRecharge()).toBeNull();
      expect(service.isProcessing()).toBeFalse();
    });
  });
});
