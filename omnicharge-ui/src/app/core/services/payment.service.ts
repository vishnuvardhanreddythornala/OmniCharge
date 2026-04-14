/**
 * PaymentService — Handles Razorpay payment flow.
 *
 * Flow:
 *  1. Frontend calls `POST /api/payments/process` → gets { transactionId, razorpayOrderId, status: "PENDING" }
 *  2. Frontend opens Razorpay Checkout with the orderId.
 *  3. On success callback → `POST /api/payments/webhook/confirm/{transactionId}`
 *  4. On failure callback → `POST /api/payments/webhook/fail/{transactionId}`
 */
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, AuthService } from './auth.service';
import { PagedResponse } from './recharge.service';

/* ── Interfaces ── */
export interface PaymentRequest {
  rechargeId: string;
  userId: number;
  amount: number;
  paymentMethod: string;
  userEmail: string;
  userMobile: string;
}

export interface PaymentResponse {
  transactionId: string;
  status: string;        // PENDING, SUCCESS, FAILED
  razorpayOrderId: string | null;
  amount: number;
  timestamp: string;
}

export interface TransactionResponse {
  transactionId: string;
  rechargeId: string;
  userId: number;
  amount: number;
  paymentMethod: string;
  status: string;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  failureReason: string;
  createdDate: string;
}

/* ── Service ── */
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly API = `${environment.apiBaseUrl}/api/payments`;

  private _paymentState = signal<'idle' | 'processing' | 'success' | 'failed'>('idle');
  private _currentTransaction = signal<TransactionResponse | null>(null);

  readonly paymentState = this._paymentState.asReadonly();
  readonly currentTransaction = this._currentTransaction.asReadonly();

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** Step 1: Create Razorpay Order via backend */
  processPayment(request: PaymentRequest): Observable<ApiResponse<PaymentResponse>> {
    this._paymentState.set('processing');
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.API}/process`, request);
  }

  /** Step 2: Open Razorpay Checkout in browser */
  openRazorpayCheckout(paymentResponse: PaymentResponse, userEmail: string, userMobile: string): Promise<{ paymentId: string; signature: string }> {
    return new Promise((resolve, reject) => {
      const options = {
        key: environment.razorpayKeyId, // Your live/test key
        amount: paymentResponse.amount * 100, // in paise
        currency: 'INR',
        name: 'OmniCharge',
        description: 'Mobile Recharge Payment',
        order_id: paymentResponse.razorpayOrderId,
        prefill: {
          email: userEmail,
          contact: userMobile,
        },
        theme: {
          color: '#6366f1', // OmniCharge brand primary
        },
        handler: (response: any) => {
          resolve({
            paymentId: response.razorpay_payment_id,
            signature: response.razorpay_signature,
          });
        },
        modal: {
          ondismiss: () => {
            reject(new Error('Payment cancelled by user'));
          },
        },
      };

      const razorpay = new (window as any).Razorpay(options);
      razorpay.on('payment.failed', (response: any) => {
        reject(new Error(response.error.description || 'Payment failed'));
      });
      razorpay.open();
    });
  }

  /** Step 3a: Confirm payment after successful Razorpay checkout */
  confirmPayment(transactionId: string, razorpayPaymentId: string, razorpaySignature: string): Observable<ApiResponse<TransactionResponse>> {
    return this.http.post<ApiResponse<TransactionResponse>>(
      `${this.API}/webhook/confirm/${transactionId}`,
      null,
      { params: { razorpayPaymentId, razorpaySignature } }
    ).pipe(
      tap(res => {
        if (res.success && res.data) {
          this._currentTransaction.set(res.data);
          this._paymentState.set('success');
        }
      })
    );
  }

  /** Step 3b: Record payment failure */
  failPayment(transactionId: string, reason: string): Observable<ApiResponse<TransactionResponse>> {
    return this.http.post<ApiResponse<TransactionResponse>>(
      `${this.API}/webhook/fail/${transactionId}`,
      null,
      { params: { reason } }
    ).pipe(
      tap(res => {
        if (res.success && res.data) {
          this._currentTransaction.set(res.data);
          this._paymentState.set('failed');
        }
      })
    );
  }

  /** Step 3c: Server-side verification fallback (checks Razorpay API directly) */
  verifyPayment(transactionId: string): Observable<ApiResponse<TransactionResponse>> {
    return this.http.post<ApiResponse<TransactionResponse>>(
      `${this.API}/verify/${transactionId}`,
      null
    ).pipe(
      tap(res => {
        if (res.success && res.data) {
          this._currentTransaction.set(res.data);
          if (res.data.status === 'SUCCESS') {
            this._paymentState.set('success');
          }
        }
      })
    );
  }

  /** Get payment history */
  getPaymentHistory(page = 0, size = 10, filters?: { transactionId?: string; startDate?: string; endDate?: string }): Observable<ApiResponse<PagedResponse<TransactionResponse>>> {
    const params: Record<string, string> = { page: page.toString(), size: size.toString() };
    if (filters?.transactionId) {
      params['transactionId'] = filters.transactionId;
    }
    if (filters?.startDate) {
      params['startDate'] = filters.startDate;
    }
    if (filters?.endDate) {
      params['endDate'] = filters.endDate;
    }
    return this.http.get<ApiResponse<PagedResponse<TransactionResponse>>>(
      `${this.API}/history`, { params }
    );
  }

  /** Reset payment state for new flow */
  resetPaymentState(): void {
    this._paymentState.set('idle');
    this._currentTransaction.set(null);
  }
}
