/**
 * OperatorService — Handles operator detection and plan browsing.
 * These are PUBLIC endpoints — no auth required.
 */
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

/* ── Interfaces ── */
export interface Operator {
  id: number;
  name: string;
  code: string;
  type: string; // PREPAID | POSTPAID
  description: string;
  isActive: boolean;
}

export interface Plan {
  id: number;
  planName: string;
  price: number;
  validityDays: number;
  dataLimit: string;
  callBenefit: string;
  smsBenefit: string;
  additionalBenefits: string;
  category: string; // POPULAR, DATA, UNLIMITED, TALKTIME, etc.
  operatorId: number;
}

export interface DetectedOperator {
  operatorId: number;
  operatorName: string;
  operatorCode: string;
  type: string;
}

/* ── Service ── */
@Injectable({ providedIn: 'root' })
export class OperatorService {
  private readonly OPERATORS_API = `${environment.apiBaseUrl}/api/operators`;
  private readonly PLANS_API = `${environment.apiBaseUrl}/api/plans`;

  // Cached operators signal
  private _operators = signal<Operator[]>([]);
  private _detectedOperator = signal<DetectedOperator | null>(null);
  private _selectedOperator = signal<DetectedOperator | null>(null);
  private _plans = signal<Plan[]>([]);
  private _isDetecting = signal<boolean>(false);
  private _isLoadingPlans = signal<boolean>(false);
  private _detectionFailed = signal<boolean>(false);
  private _isManualOverride = signal<boolean>(false);

  readonly operators = this._operators.asReadonly();
  readonly detectedOperator = this._detectedOperator.asReadonly();
  readonly selectedOperator = this._selectedOperator.asReadonly();
  readonly plans = this._plans.asReadonly();
  readonly isDetecting = this._isDetecting.asReadonly();
  readonly isLoadingPlans = this._isLoadingPlans.asReadonly();
  readonly detectionFailed = this._detectionFailed.asReadonly();
  readonly isManualOverride = this._isManualOverride.asReadonly();

  constructor(private http: HttpClient) {}

  /** Fetch all active operators (public, cached) */
  loadActiveOperators(): void {
    // Intentionally omitting length check to ensure fresh list is fetched
    this.http.get<ApiResponse<Operator[]>>(`${this.OPERATORS_API}/active`).subscribe({
      next: res => {
        if (res.success && res.data) {
          this._operators.set(res.data);
        }
      }
    });
  }

  /** Auto-detect operator from mobile number (public, uses Numverify) */
  detectOperator(mobileNumber: string): Observable<ApiResponse<DetectedOperator>> {
    this._isDetecting.set(true);
    this._detectedOperator.set(null);
    this._selectedOperator.set(null);
    this._plans.set([]);
    this._detectionFailed.set(false);
    this._isManualOverride.set(false);
    return this.http.get<ApiResponse<DetectedOperator>>(
      `${this.OPERATORS_API}/detect`, { params: { mobileNumber } }
    ).pipe(
      tap(res => {
        if (res.success && res.data) {
          this._detectedOperator.set(res.data);
          this._selectedOperator.set(res.data);
          // Auto-load plans for the detected operator
          this.loadPlans(res.data.operatorId);
        } else {
          this._detectionFailed.set(true);
          this.loadActiveOperators();
        }
        this._isDetecting.set(false);
      }),
      catchError(err => {
        this._isDetecting.set(false);
        this._detectionFailed.set(true);
        this.loadActiveOperators();
        return of(err);
      })
    );
  }

  /** Manually set an operator (MNP override or detection-failure fallback) */
  setManualOperator(operator: Operator): void {
    const manual: DetectedOperator = {
      operatorId: operator.id,
      operatorName: operator.name,
      operatorCode: operator.code,
      type: operator.type
    };
    this._selectedOperator.set(manual);
    this._isManualOverride.set(true);
    this.loadPlans(operator.id);
  }

  /** Fetch plans for a given operator (public) */
  loadPlans(operatorId: number): void {
    this._isLoadingPlans.set(true);
    this.http.get<ApiResponse<any>>(
      `${this.PLANS_API}/search`, { params: { operatorId: operatorId.toString(), size: '100' } }
    ).subscribe({
      next: res => {
        if (res.success && res.data && res.data.content) {
          this._plans.set(res.data.content);
        } else if (res.success && res.data) {
          // fallback if backend ever reverts to a flat list
          this._plans.set(Array.isArray(res.data) ? res.data : []);
        }
        this._isLoadingPlans.set(false);
      },
      error: () => this._isLoadingPlans.set(false)
    });
  }

  /** Get a single plan by ID */
  getPlanById(planId: number): Observable<ApiResponse<Plan>> {
    return this.http.get<ApiResponse<Plan>>(`${this.PLANS_API}/${planId}`);
  }

  /** Clear selection (reset the flow) */
  clearSelection(): void {
    this._detectedOperator.set(null);
    this._selectedOperator.set(null);
    this._plans.set([]);
    this._detectionFailed.set(false);
    this._isManualOverride.set(false);
  }
}
