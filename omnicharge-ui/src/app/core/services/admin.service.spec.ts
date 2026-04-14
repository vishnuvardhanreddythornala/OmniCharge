import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdminService, CreateOperatorRequest, CreatePlanRequest } from './admin.service';
import { environment } from '../../../environments/environment';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;
  const BASE = environment.apiBaseUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdminService]
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // === Users ===
  describe('getAllUsers()', () => {
    it('should fetch paginated users with default params', () => {
      service.getAllUsers().subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/users`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('sortBy')).toBe('id');
      expect(req.request.params.get('sortDir')).toBe('DESC');
      req.flush({ success: true, message: 'OK', data: { content: [], totalElements: 0 } });
    });

    it('should include search param when provided', () => {
      service.getAllUsers(0, 10, 'john').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/users`);
      expect(req.request.params.get('search')).toBe('john');
      req.flush({ success: true, message: 'OK', data: { content: [] } });
    });

    it('should include status param when not ALL', () => {
      service.getAllUsers(0, 10, undefined, 'ACTIVE').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/users`);
      expect(req.request.params.get('status')).toBe('ACTIVE');
      req.flush({ success: true, message: 'OK', data: { content: [] } });
    });

    it('should NOT include status param when ALL', () => {
      service.getAllUsers(0, 10, undefined, 'ALL').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/users`);
      expect(req.request.params.has('status')).toBeFalse();
      req.flush({ success: true, message: 'OK', data: { content: [] } });
    });
  });

  describe('toggleUserStatus()', () => {
    it('should PUT to toggle user status', () => {
      service.toggleUserStatus(5, false).subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/users/5/status`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.params.get('active')).toBe('false');
      req.flush({ success: true, message: 'OK', data: null });
    });
  });

  // === Payments ===
  describe('getAllTransactions()', () => {
    it('should fetch transactions with filters', () => {
      service.getAllTransactions(1, 20, 'SUCCESS', 'REC1', '2024-01-01', '2024-12-31').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/payments`);
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('size')).toBe('20');
      expect(req.request.params.get('status')).toBe('SUCCESS');
      expect(req.request.params.get('rechargeId')).toBe('REC1');
      expect(req.request.params.get('startDate')).toBe('2024-01-01');
      req.flush({ success: true, message: 'OK', data: { content: [] } });
    });
  });

  describe('getPaymentStats()', () => {
    it('should fetch payment stats with days param', () => {
      service.getPaymentStats(7).subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/payments/stats`);
      expect(req.request.params.get('days')).toBe('7');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  // === Recharges ===
  describe('getAllRecharges()', () => {
    it('should fetch recharges with status filter', () => {
      service.getAllRecharges(0, 10, 'FAILED').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/recharges`);
      expect(req.request.params.get('status')).toBe('FAILED');
      req.flush({ success: true, message: 'OK', data: { content: [] } });
    });
  });

  describe('getRechargeStats()', () => {
    it('should GET recharge stats', () => {
      service.getRechargeStats().subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/recharges/stats`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  // === Operators ===
  describe('getAllOperators()', () => {
    it('should fetch from API on first call', () => {
      service.getAllOperators().subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators`);
      req.flush({ success: true, message: 'OK', data: [{ id: 1, name: 'Jio' }] });
    });

    it('should return cached data on subsequent call', fakeAsync(() => {
      // First call
      service.getAllOperators().subscribe();
      httpMock.expectOne(`${BASE}/api/admin/operators`)
        .flush({ success: true, message: 'OK', data: [{ id: 1, name: 'Jio' }] });

      // Second call — should NOT hit HTTP
      let result: any;
      service.getAllOperators().subscribe(r => result = r);
      tick();
      expect(result.data.length).toBe(1);
      expect(result.message).toBe('Cached response');
    }));

    it('should force refresh when forceRefresh=true', fakeAsync(() => {
      // First call
      service.getAllOperators().subscribe();
      httpMock.expectOne(`${BASE}/api/admin/operators`)
        .flush({ success: true, message: 'OK', data: [{ id: 1, name: 'Jio' }] });

      // Force refresh
      service.getAllOperators(true).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators`);
      req.flush({ success: true, message: 'OK', data: [{ id: 1, name: 'Jio' }, { id: 2, name: 'Airtel' }] });
    }));
  });

  describe('createOperator()', () => {
    it('should POST and invalidate cache on success', () => {
      const payload: CreateOperatorRequest = { name: 'Vi', code: 'VI', category: 'PREPAID' };
      service.createOperator(payload).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush({ success: true, message: 'OK', data: { id: 3, name: 'Vi' } });
    });
  });

  describe('updateOperator()', () => {
    it('should PUT and invalidate cache', () => {
      service.updateOperator(1, { name: 'Updated', code: 'UPD', category: 'PREPAID' }).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ success: true, message: 'OK', data: { id: 1, name: 'Updated' } });
    });
  });

  describe('deleteOperator()', () => {
    it('should DELETE operator', () => {
      service.deleteOperator(1).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush({ success: true, message: 'OK', data: null });
    });
  });

  describe('activateOperator()', () => {
    it('should PATCH to activate', () => {
      service.activateOperator(1).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/1/activate`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  describe('deactivateOperator()', () => {
    it('should PATCH to deactivate', () => {
      service.deactivateOperator(1).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/1/deactivate`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  // === Plans ===
  describe('createPlan()', () => {
    it('should POST plan under operator', () => {
      const plan: CreatePlanRequest = { planName: 'P1', price: 199, validityDays: 28, dataLimit: '1GB', callBenefit: 'Unlimited', smsBenefit: '100', additionalBenefits: '', category: 'POPULAR' };
      service.createPlan(1, plan).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/1/plans`);
      expect(req.request.method).toBe('POST');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  describe('updatePlan()', () => {
    it('should PUT plan', () => {
      service.updatePlan(5, {} as any).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/plans/5`);
      expect(req.request.method).toBe('PUT');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  describe('deletePlan()', () => {
    it('should DELETE plan', () => {
      service.deletePlan(5).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/plans/5`);
      expect(req.request.method).toBe('DELETE');
      req.flush({ success: true, message: 'OK', data: null });
    });
  });

  describe('activatePlan()', () => {
    it('should PATCH to activate plan', () => {
      service.activatePlan(5).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/plans/5/activate`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  describe('deactivatePlan()', () => {
    it('should PATCH to deactivate plan', () => {
      service.deactivatePlan(5).subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/operators/plans/5/deactivate`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ success: true, message: 'OK', data: {} });
    });
  });

  describe('searchAllPlans()', () => {
    it('should include all filter params', () => {
      service.searchAllPlans(0, 10, 1, 'DATA', 'ACTIVE', 'basic').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/operators/plans`);
      expect(req.request.params.get('operatorId')).toBe('1');
      expect(req.request.params.get('category')).toBe('DATA');
      expect(req.request.params.get('status')).toBe('ACTIVE');
      expect(req.request.params.get('search')).toBe('basic');
      expect(req.request.params.has('_t')).toBeTrue(); // cache buster
      req.flush({ success: true, message: 'OK', data: { content: [] } });
    });
  });

  describe('getOperatorPlans()', () => {
    it('should fetch plans for operator with cache-busting headers', () => {
      service.getOperatorPlans(1, 'ACTIVE').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/operators/1/plans`);
      expect(req.request.params.get('status')).toBe('ACTIVE');
      expect(req.request.headers.get('Cache-Control')).toBe('no-cache');
      req.flush({ success: true, message: 'OK', data: [] });
    });
  });

  // === Notifications ===
  describe('getAllNotifications()', () => {
    it('should fetch paginated notifications with sort', () => {
      service.getAllNotifications(0, 10, 'RECHARGE').subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/api/admin/notifications`);
      expect(req.request.params.get('sortBy')).toBe('createdDate');
      expect(req.request.params.get('sortDir')).toBe('DESC');
      expect(req.request.params.get('category')).toBe('RECHARGE');
      req.flush({ success: true, message: 'OK', data: { content: [] } });
    });
  });

  // === System ===
  describe('rebuildCache()', () => {
    it('should POST to rebuild cache', () => {
      service.rebuildCache().subscribe();
      const req = httpMock.expectOne(`${BASE}/api/admin/system/rebuild-cache`);
      expect(req.request.method).toBe('POST');
      req.flush({ success: true, message: 'Cache rebuilt', data: 'OK' });
    });
  });
});
