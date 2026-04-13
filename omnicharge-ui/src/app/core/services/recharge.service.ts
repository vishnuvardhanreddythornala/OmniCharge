/**
 * RechargeService — Initiates recharges & fetches history.
 * All endpoints require authentication (protected).
 */
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

/* ── Interfaces ── */
export interface RechargeRequest {
  mobileNumber: string;
  operatorId: number;
  planId: number;
  paymentMethod: string; // 'RAZORPAY'
}

export interface RechargeResponse {
  rechargeId: string;
  mobileNumber: string;
  operatorName: string;
  planName: string;
  amount: number;
  planValidityDays: number;
  planExpiryDate: string;
  status: string; // INITIATED, PAYMENT_PENDING, SUCCESS, FAILED
  createdDate: string;
}

export interface RechargeHistoryItem {
  rechargeId: string;
  mobileNumber: string;
  operatorName: string;
  planName: string;
  amount: number;
  planValidityDays: number;
  planExpiryDate: string;
  status: string;
  createdDate: string;
}

// Import from shared models and re-export for backward compatibility
import { PagedResponse } from '../models/api.models';
export { PagedResponse } from '../models/api.models';

/* ── Service ── */
@Injectable({ providedIn: 'root' })
export class RechargeService {
  private readonly API = `${environment.apiBaseUrl}/api/recharges`;

  private _currentRecharge = signal<RechargeResponse | null>(null);
  private _isProcessing = signal<boolean>(false);

  readonly currentRecharge = this._currentRecharge.asReadonly();
  readonly isProcessing = this._isProcessing.asReadonly();

  constructor(private http: HttpClient) {}

  /** Step 1: Initiate a recharge (creates the recharge record in backend) */
  initiateRecharge(request: RechargeRequest): Observable<ApiResponse<RechargeResponse>> {
    this._isProcessing.set(true);
    return this.http.post<ApiResponse<RechargeResponse>>(this.API, request).pipe(
      tap(res => {
        if (res.success && res.data) {
          this._currentRecharge.set(res.data);
        }
        this._isProcessing.set(false);
      })
    );
  }

  /** Poll recharge status */
  getRechargeStatus(rechargeId: string): Observable<ApiResponse<RechargeResponse>> {
    return this.http.get<ApiResponse<RechargeResponse>>(`${this.API}/status/${rechargeId}`);
  }

  /** Get single recharge details */
  getRechargeById(rechargeId: string): Observable<ApiResponse<RechargeResponse>> {
    return this.http.get<ApiResponse<RechargeResponse>>(`${this.API}/${rechargeId}`);
  }

  /** Fetch paginated recharge history with optional date filtering */
  getRechargeHistory(page = 0, size = 10, startDate?: string, endDate?: string): Observable<ApiResponse<PagedResponse<RechargeHistoryItem>>> {
    const params: Record<string, string> = { page: page.toString(), size: size.toString() };
    if (startDate) params['startDate'] = startDate;
    if (endDate) params['endDate'] = endDate;
    return this.http.get<ApiResponse<PagedResponse<RechargeHistoryItem>>>(
      `${this.API}/history`, { params }
    );
  }

  /** Reset current recharge state */
  clearCurrentRecharge(): void {
    this._currentRecharge.set(null);
    this._isProcessing.set(false);
  }
}
