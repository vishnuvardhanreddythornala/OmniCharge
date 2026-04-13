import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, interval, switchMap, Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';
import { PagedResponse } from './recharge.service';

export interface Notification {
  id: number;
  userId: number;
  type: string;
  category: string;
  subject: string;
  message: string;
  status: string;
  referenceId?: string;
  isRead: boolean;
  createdDate: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly API = `${environment.apiBaseUrl}/api/notifications`;

  private _unreadCount = signal<number>(0);
  private _notifications = signal<Notification[]>([]);
  private _pollingSubscription: Subscription | null = null;
  private _previousUnreadCount = 0;

  /** Emits when new notifications arrive so dashboard can re-fetch the list */
  private _countChanged = signal<boolean>(false);

  readonly unreadCount = this._unreadCount.asReadonly();
  readonly notifications = this._notifications.asReadonly();
  readonly countChanged = this._countChanged.asReadonly();

  constructor(private http: HttpClient) {}

  /** Fetch unread notification count (badge) */
  fetchUnreadCount(): void {
    this.http.get<ApiResponse<number>>(`${this.API}/unread-count`).subscribe({
      next: res => {
        if (res.success) {
          const newCount = res.data ?? 0;
          if (newCount !== this._previousUnreadCount) {
            this._countChanged.set(true);
            this._previousUnreadCount = newCount;
          }
          this._unreadCount.set(newCount);
        }
      }
    });
  }

  /** Acknowledge the count change (called by dashboard after re-fetching list) */
  acknowledgeCountChange(): void {
    this._countChanged.set(false);
  }

  /** Start polling unread count every 15 seconds */
  startPolling(): void {
    // Prevent duplicate polling subscriptions
    if (this._pollingSubscription) return;

    this._pollingSubscription = interval(15000).pipe(
      switchMap(() => this.http.get<ApiResponse<number>>(`${this.API}/unread-count`))
    ).subscribe({
      next: res => {
        if (res.success) {
          const newCount = res.data ?? 0;
          if (newCount !== this._previousUnreadCount) {
            this._countChanged.set(true);
            this._previousUnreadCount = newCount;
          }
          this._unreadCount.set(newCount);
        }
      }
    });
  }

  stopPolling(): void {
    if (this._pollingSubscription) {
      this._pollingSubscription.unsubscribe();
      this._pollingSubscription = null;
    }
  }

  /** Get paginated notifications */
  getNotifications(page = 0, size = 10): Observable<ApiResponse<PagedResponse<Notification>>> {
    return this.http.get<ApiResponse<PagedResponse<Notification>>>(
      this.API, { params: { page: page.toString(), size: size.toString() } }
    ).pipe(
      tap(res => {
        if (res.success && res.data) {
          this._notifications.set(res.data.content);
        }
      })
    );
  }

  /** Mark a notification as read */
  markAsRead(notificationId: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.API}/${notificationId}/read`, {}).pipe(
      tap(() => {
        // Update local state
        this._notifications.update(list =>
          list.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
        );
        this._unreadCount.update(c => Math.max(0, c - 1));
      })
    );
  }
}
