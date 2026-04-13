import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService, PaymentRequest, PaymentResponse, TransactionResponse } from './payment.service';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  const mockPaymentRes: PaymentResponse = {
    transactionId: 'TXN123',
    status: 'PENDING',
    razorpayOrderId: 'order_123',
    amount: 199,
    timestamp: '2024-04-10T10:00:00Z'
  };

  const mockTransaction: TransactionResponse = {
    transactionId: 'TXN123',
    rechargeId: 'REC1',
    userId: 1,
    amount: 199,
    paymentMethod: 'RAZORPAY',
    status: 'SUCCESS',
    razorpayOrderId: 'order_123',
    razorpayPaymentId: 'pay_123',
    failureReason: '',
    createdDate: '2024-04-10T10:05:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        PaymentService,
        { provide: AuthService, useValue: {} }
      ]
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    delete (window as any).Razorpay;
  });

  describe('processPayment()', () => {
    it('should call POST /process and set paymentState to processing', () => {
      const reqPayload: PaymentRequest = { rechargeId: 'R1', userId: 1, amount: 199, paymentMethod: 'RAZORPAY', userEmail: 'm@m.com', userMobile: '999' };
      
      service.processPayment(reqPayload).subscribe(res => {
        expect(res.success).toBeTrue();
      });

      expect(service.paymentState()).toBe('processing');

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/payments/process`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(reqPayload);
      req.flush({ success: true, data: mockPaymentRes });
    });
  });

  describe('openRazorpayCheckout()', () => {
    it('should resolve with payment details on success', async () => {
      // Mock Razorpay
      (window as any).Razorpay = class {
        options: any;
        constructor(options: any) { this.options = options; }
        on() {}
        open() {
          // Simulate immediate success callback
          this.options.handler({
            razorpay_payment_id: 'pay_123',
            razorpay_signature: 'sig_123'
          });
        }
      };

      const result = await service.openRazorpayCheckout(mockPaymentRes, 'u@u.com', '1234567890');
      expect(result.paymentId).toBe('pay_123');
      expect(result.signature).toBe('sig_123');
    });

    it('should reject with Error on payment.failed', async () => {
      (window as any).Razorpay = class {
        options: any;
        failCallback: any;
        constructor(options: any) { this.options = options; }
        on(event: string, callback: any) {
          if (event === 'payment.failed') this.failCallback = callback;
        }
        open() {
          this.failCallback({ error: { description: 'Bank declined' } });
        }
      };

      try {
        await service.openRazorpayCheckout(mockPaymentRes, 'u@u.com', '1234567890');
        fail('Expected promise to be rejected');
      } catch (err: any) {
        expect(err.message).toBe('Bank declined');
      }
    });

    it('should reject on modal dismiss', async () => {
      (window as any).Razorpay = class {
        options: any;
        constructor(options: any) { this.options = options; }
        on() {}
        open() {
          this.options.modal.ondismiss();
        }
      };

      try {
        await service.openRazorpayCheckout(mockPaymentRes, 'u@u.com', '1234567890');
        fail('Expected promise to be rejected');
      } catch (err: any) {
        expect(err.message).toBe('Payment cancelled by user');
      }
    });
  });

  describe('confirmPayment()', () => {
    it('should resolve webhook confirm and set state to success', () => {
      service.confirmPayment('TXN123', 'pay_123', 'sig_123').subscribe(res => {
        expect(res.data?.status).toBe('SUCCESS');
      });

      const req = httpMock.expectOne(request => request.url === `${environment.apiBaseUrl}/api/payments/webhook/confirm/TXN123`);
      expect(req.request.method).toBe('POST');
      expect(req.request.params.get('razorpayPaymentId')).toBe('pay_123');
      expect(req.request.params.get('razorpaySignature')).toBe('sig_123');
      req.flush({ success: true, data: mockTransaction });

      expect(service.paymentState()).toBe('success');
      expect(service.currentTransaction()).toEqual(mockTransaction);
    });
  });

  describe('failPayment()', () => {
    it('should resolve webhook fail and set state to failed', () => {
      const failedTransaction = { ...mockTransaction, status: 'FAILED', failureReason: 'Bank issue' };
      
      service.failPayment('TXN123', 'Bank issue').subscribe();

      const req = httpMock.expectOne(request => request.url === `${environment.apiBaseUrl}/api/payments/webhook/fail/TXN123`);
      expect(req.request.method).toBe('POST');
      expect(req.request.params.get('reason')).toBe('Bank issue');
      req.flush({ success: true, data: failedTransaction });

      expect(service.paymentState()).toBe('failed');
      expect(service.currentTransaction()).toEqual(failedTransaction);
    });
  });

  describe('getPaymentHistory()', () => {
    it('should construct query params securely including custom filters', () => {
      service.getPaymentHistory(0, 10, { transactionId: 'TXN1', startDate: '2024-01-01', endDate: '2024-12-31' }).subscribe();

      const req = httpMock.expectOne(request => request.url.includes('/api/payments/history'));
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('transactionId')).toBe('TXN1');
      expect(req.request.params.get('startDate')).toBe('2024-01-01');
      expect(req.request.params.get('endDate')).toBe('2024-12-31');
      req.flush({ success: true });
    });
    
    it('should ignore undefined filters', () => {
      service.getPaymentHistory(1, 20).subscribe();
      const req = httpMock.expectOne(request => request.url.includes('/api/payments/history'));
      expect(req.request.params.has('transactionId')).toBeFalse();
      expect(req.request.params.has('startDate')).toBeFalse();
      req.flush({ success: true });
    });
  });

  describe('resetPaymentState()', () => {
    it('should reset signals', () => {
      // pollute state manually
      (service as any)._paymentState.set('success');
      (service as any)._currentTransaction.set(mockTransaction);
      
      service.resetPaymentState();
      
      expect(service.paymentState()).toBe('idle');
      expect(service.currentTransaction()).toBeNull();
    });
  });
});
