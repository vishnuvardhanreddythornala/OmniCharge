import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, delay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api.models';
import { TransactionResponse } from './payment.service';
import { RechargeHistoryItem } from './recharge.service';

export interface UserProfileResponse {
  id: number;
  fullName: string;
  email: string;
  mobileNumber: string;
  role: string;
  isActive: boolean;
  isMobileVerified: boolean;
  createdDate: string;
}

export interface PaymentStatsResponse {
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  pendingTransactions: number;
  totalRevenue: number;
  successAmount: number;
  failedAmount: number;
  averageTransactionAmount: number;
  todayTransactions: number;
  todayRevenue: number;
  revenueByDate: DailyRevenueStats[];
  topUsers: TopUserStats[];
}

export interface DailyRevenueStats {
  date: string;
  transactionCount: number;
  revenue: number;
}

export interface TopUserStats {
  userId: number;
  transactionCount: number;
  totalSpent: number;
}

export interface RechargeStatsResponse {
  totalRecharges: number;
  successCount: number;
  failedCount: number;
  totalAmount: number;
}

export interface NotificationResponse {
  id: number;
  userId: number;
  type: 'EMAIL' | 'SMS' | 'IN_APP';
  category: 'RECHARGE' | 'PAYMENT' | 'ACCOUNT' | 'SYSTEM';
  subject: string;
  message: string;
  status: 'SENT' | 'FAILED' | 'PENDING';
  referenceId: string | null;
  isRead: boolean;
  createdDate: string;
}

export interface AdminOperatorResponse {
  id: number;
  name: string;
  code: string;
  category: string;
  logoUrl: string;
  isActive: boolean;
  planCount: number;
  lastModifiedDate?: string;
}

export interface CreateOperatorRequest {
  name: string;
  code: string;
  category: string;
  logoUrl?: string;
}

export interface CreatePlanRequest {
  planName: string;
  price: number;
  validityDays: number;
  dataLimit: string;
  callBenefit: string;
  smsBenefit: string;
  additionalBenefits: string;
  category: string;
}

export interface PlanResponse {
  id: number;
  planName: string;
  price: number;
  validityDays: number;
  dataLimit: string;
  callBenefit: string;
  smsBenefit: string;
  additionalBenefits: string;
  category: string;
  operatorId: number;
  operatorName?: string;
  isActive: boolean;
  deactivatedByOperator?: boolean;
  lastModifiedDate?: string;
  lastModifiedBy?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  
  private ADMIN_USERS_API = `${environment.apiBaseUrl}/api/admin/users`;
  private ADMIN_PAYMENTS_API = `${environment.apiBaseUrl}/api/admin/payments`;
  private ADMIN_RECHARGES_API = `${environment.apiBaseUrl}/api/admin/recharges`;
  private ADMIN_OPERATORS_API = `${environment.apiBaseUrl}/api/admin/operators`;
  private ADMIN_SYSTEM_API = `${environment.apiBaseUrl}/api/admin/system`;
  private ADMIN_NOTIFICATIONS_API = `${environment.apiBaseUrl}/api/admin/notifications`;

  // === Users ===
  getAllUsers(page: number = 0, size: number = 10, search?: string, status?: string): Observable<ApiResponse<PagedResponse<UserProfileResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sortBy', 'id').set('sortDir', 'DESC');
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    
    // Add cache-busting to prevent stale data
    params = params.set('_t', new Date().getTime().toString());
    const headers = { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' };
    
    return this.http.get<ApiResponse<PagedResponse<UserProfileResponse>>>(this.ADMIN_USERS_API, { params, headers });
  }

  toggleUserStatus(userId: number, active: boolean): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.ADMIN_USERS_API}/${userId}/status`, null, {
      params: new HttpParams().set('active', active)
    });
  }

  // === Payments ===
  getAllTransactions(page: number = 0, size: number = 10, status?: string, search?: string, startDate?: string, endDate?: string): Observable<ApiResponse<PagedResponse<TransactionResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status && status !== 'ALL') params = params.set('status', status);
    if (search && search.trim()) params = params.set('rechargeId', search.trim());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<PagedResponse<TransactionResponse>>>(this.ADMIN_PAYMENTS_API, { params });
  }

  getPaymentStats(days: number = 30): Observable<ApiResponse<PaymentStatsResponse>> {
    return this.http.get<ApiResponse<PaymentStatsResponse>>(`${this.ADMIN_PAYMENTS_API}/stats`, {
      params: new HttpParams().set('days', days)
    });
  }

  // === Recharges ===
  getAllRecharges(page: number = 0, size: number = 10, status?: string, startDate?: string, endDate?: string): Observable<ApiResponse<PagedResponse<RechargeHistoryItem>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<PagedResponse<RechargeHistoryItem>>>(this.ADMIN_RECHARGES_API, { params });
  }

  getRechargeStats(): Observable<ApiResponse<RechargeStatsResponse>> {
    return this.http.get<ApiResponse<RechargeStatsResponse>>(`${this.ADMIN_RECHARGES_API}/stats`);
  }

  private operatorsCache: AdminOperatorResponse[] | null = null;

  // === Operators ===
  getAllOperators(forceRefresh: boolean = false): Observable<ApiResponse<AdminOperatorResponse[]>> {
    if (this.operatorsCache && !forceRefresh) {
      return of({ success: true, message: 'Cached response', data: this.operatorsCache }).pipe(delay(0));
    }
    return this.http.get<ApiResponse<AdminOperatorResponse[]>>(this.ADMIN_OPERATORS_API).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.operatorsCache = res.data;
        }
      })
    );
  }

  createOperator(request: CreateOperatorRequest): Observable<ApiResponse<AdminOperatorResponse>> {
    return this.http.post<ApiResponse<AdminOperatorResponse>>(this.ADMIN_OPERATORS_API, request).pipe(
      tap(res => { if (res.success) this.operatorsCache = null; })
    );
  }

  // === Plans ===
  createPlan(operatorId: number, request: CreatePlanRequest): Observable<ApiResponse<PlanResponse>> {
    return this.http.post<ApiResponse<PlanResponse>>(`${this.ADMIN_OPERATORS_API}/${operatorId}/plans`, request);
  }

  // === System ===
  rebuildCache(): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.ADMIN_SYSTEM_API}/rebuild-cache`, {});
  }

  // === Notifications ===
  getAllNotifications(page: number = 0, size: number = 10, category?: string): Observable<ApiResponse<PagedResponse<NotificationResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sortBy', 'createdDate').set('sortDir', 'DESC');
    if (category && category !== 'ALL') params = params.set('category', category);
    return this.http.get<ApiResponse<PagedResponse<NotificationResponse>>>(this.ADMIN_NOTIFICATIONS_API, { params });
  }

  // === Operators - Extended ===
  updateOperator(id: number, request: CreateOperatorRequest): Observable<ApiResponse<AdminOperatorResponse>> {
    return this.http.put<ApiResponse<AdminOperatorResponse>>(`${this.ADMIN_OPERATORS_API}/${id}`, request).pipe(
      tap(res => { if (res.success) this.operatorsCache = null; })
    );
  }

  deleteOperator(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.ADMIN_OPERATORS_API}/${id}`).pipe(
      tap(res => { if (res.success) this.operatorsCache = null; })
    );
  }

  activateOperator(id: number): Observable<ApiResponse<AdminOperatorResponse>> {
    return this.http.patch<ApiResponse<AdminOperatorResponse>>(`${this.ADMIN_OPERATORS_API}/${id}/activate`, {}).pipe(
      tap(res => { if (res.success) this.operatorsCache = null; })
    );
  }

  deactivateOperator(id: number): Observable<ApiResponse<AdminOperatorResponse>> {
    return this.http.patch<ApiResponse<AdminOperatorResponse>>(`${this.ADMIN_OPERATORS_API}/${id}/deactivate`, {}).pipe(
      tap(res => { if (res.success) this.operatorsCache = null; })
    );
  }

  getOperatorPlans(operatorId: number, status?: string): Observable<ApiResponse<PlanResponse[]>> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    // Add cache-busting to prevent stale data
    const headers = { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' };
    return this.http.get<ApiResponse<PlanResponse[]>>(`${this.ADMIN_OPERATORS_API}/${operatorId}/plans`, { params, headers });
  }

  // Uses Server-Side Pagination & Filtering
  searchAllPlans(page: number = 0, size: number = 10, operatorId?: number | null, category?: string, status?: string, search?: string): Observable<ApiResponse<PagedResponse<PlanResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    
    if (operatorId) params = params.set('operatorId', operatorId);
    if (category && category !== 'ALL') params = params.set('category', category);
    if (status && status !== 'ALL') params = params.set('status', status);
    if (search && search.trim()) params = params.set('search', search.trim());
    
    // Hard cache bust to ensure plan updates and disappearances are accurately represented instantly
    params = params.set('_t', new Date().getTime().toString());
    
    const headers = { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' };
    return this.http.get<ApiResponse<PagedResponse<PlanResponse>>>(`${this.ADMIN_OPERATORS_API}/plans`, { params, headers });
  }

  updatePlan(planId: number, request: CreatePlanRequest): Observable<ApiResponse<PlanResponse>> {
    return this.http.put<ApiResponse<PlanResponse>>(`${this.ADMIN_OPERATORS_API}/plans/${planId}`, request);
  }

  deletePlan(planId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.ADMIN_OPERATORS_API}/plans/${planId}`);
  }

  activatePlan(planId: number): Observable<ApiResponse<PlanResponse>> {
    return this.http.patch<ApiResponse<PlanResponse>>(`${this.ADMIN_OPERATORS_API}/plans/${planId}/activate`, {});
  }

  deactivatePlan(planId: number): Observable<ApiResponse<PlanResponse>> {
    return this.http.patch<ApiResponse<PlanResponse>>(`${this.ADMIN_OPERATORS_API}/plans/${planId}/deactivate`, {});
  }
}
