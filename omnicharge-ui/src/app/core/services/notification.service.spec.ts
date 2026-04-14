import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NotificationService, Notification } from './notification.service';
import { environment } from '../../../environments/environment';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  const mockNotification: Notification = {
    id: 1, userId: 1, type: 'IN_APP', category: 'RECHARGE',
    subject: 'Recharge Success', message: 'Your recharge was successful',
    status: 'SENT', isRead: false, createdDate: '2024-04-10'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NotificationService]
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    service.stopPolling();
    httpMock.verify();
  });

  describe('fetchUnreadCount()', () => {
    it('should fetch and update unread count', () => {
      service.fetchUnreadCount();
      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/notifications/unread-count`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, message: 'OK', data: 5 });
      expect(service.unreadCount()).toBe(5);
    });

    it('should set countChanged when count differs from previous', () => {
      service.fetchUnreadCount();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/notifications/unread-count`)
        .flush({ success: true, message: 'OK', data: 3 });
      expect(service.countChanged()).toBeTrue();
    });

    it('should not set countChanged when count is same', () => {
      // First fetch sets baseline
      service.fetchUnreadCount();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/notifications/unread-count`)
        .flush({ success: true, message: 'OK', data: 3 });
      service.acknowledgeCountChange();

      // Second fetch with same count
      service.fetchUnreadCount();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/notifications/unread-count`)
        .flush({ success: true, message: 'OK', data: 3 });
      expect(service.countChanged()).toBeFalse();
    });
  });

  describe('acknowledgeCountChange()', () => {
    it('should reset countChanged to false', () => {
      service.fetchUnreadCount();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/notifications/unread-count`)
        .flush({ success: true, message: 'OK', data: 1 });
      expect(service.countChanged()).toBeTrue();
      service.acknowledgeCountChange();
      expect(service.countChanged()).toBeFalse();
    });
  });

  describe('getNotifications()', () => {
    it('should fetch paginated notifications and store in signal', () => {
      service.getNotifications(0, 10).subscribe(res => {
        expect(res.success).toBeTrue();
      });
      const req = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/notifications`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush({ success: true, message: 'OK', data: { content: [mockNotification], totalElements: 1, totalPages: 1, number: 0, size: 10 } });
      expect(service.notifications().length).toBe(1);
    });
  });

  describe('markAsRead()', () => {
    it('should PUT to mark notification read and update local state', () => {
      // Seed notification state
      service.getNotifications().subscribe();
      httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/notifications`)
        .flush({ success: true, message: 'OK', data: { content: [mockNotification], totalElements: 1, totalPages: 1, number: 0, size: 10 } });

      // Set unread count
      (service as any)._unreadCount.set(3);

      service.markAsRead(1).subscribe();
      const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/notifications/1/read`);
      expect(req.request.method).toBe('PUT');
      req.flush({ success: true, message: 'OK', data: null });

      expect(service.notifications()[0].isRead).toBeTrue();
      expect(service.unreadCount()).toBe(2);
    });

    it('should not go below 0 for unread count', () => {
      (service as any)._unreadCount.set(0);
      service.markAsRead(99).subscribe();
      httpMock.expectOne(`${environment.apiBaseUrl}/api/notifications/99/read`)
        .flush({ success: true, message: 'OK', data: null });
      expect(service.unreadCount()).toBe(0);
    });
  });

  describe('startPolling() / stopPolling()', () => {
    it('should not create duplicate subscriptions', () => {
      service.startPolling();
      const initialSub = (service as any)._pollingSubscription;
      
      service.startPolling(); // Should be a no-op
      
      expect((service as any)._pollingSubscription).toBe(initialSub);
      expect((service as any)._pollingSubscription).toBeTruthy();
    });

    it('stopPolling should be safe to call when not polling', () => {
      service.stopPolling();
      expect(true).toBeTrue(); // No error
    });
  });
});
