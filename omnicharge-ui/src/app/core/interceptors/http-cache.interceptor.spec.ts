import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors, HttpResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { httpCacheInterceptor, clearHttpCache } from './http-cache.interceptor';

describe('httpCacheInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    clearHttpCache(); // Start fresh

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpCacheInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should NOT cache non-GET requests', () => {
    httpClient.post('/api/operators/active', {}).subscribe();
    httpMock.expectOne('/api/operators/active').flush({ data: 'ok' });

    httpClient.post('/api/operators/active', {}).subscribe();
    httpMock.expectOne('/api/operators/active').flush({ data: 'ok' }); // Should NOT be cached
  });

  it('should NOT cache non-whitelisted endpoints', () => {
    httpClient.get('/api/users/profile').subscribe();
    httpMock.expectOne('/api/users/profile').flush({ data: 'ok' });

    httpClient.get('/api/users/profile').subscribe();
    httpMock.expectOne('/api/users/profile').flush({ data: 'ok' }); // Should NOT be cached
  });

  it('should cache whitelisted GET endpoints', fakeAsync(() => {
    let firstResp: any, secondResp: any;

    httpClient.get('/api/operators/active').subscribe(r => firstResp = r);
    httpMock.expectOne('/api/operators/active').flush({ data: 'cached' });
    tick();

    httpClient.get('/api/operators/active').subscribe(r => secondResp = r);
    // No HTTP request should be made — served from cache
    httpMock.expectNone('/api/operators/active');
    tick();

    expect(firstResp).toEqual(secondResp);
  }));

  it('should bypass cache when Cache-Control: no-cache header is set', fakeAsync(() => {
    // Populate cache
    httpClient.get('/api/operators/active').subscribe();
    httpMock.expectOne('/api/operators/active').flush({ data: 'v1' });
    tick();

    // Request with no-cache should bypass
    httpClient.get('/api/operators/active', { headers: { 'Cache-Control': 'no-cache' } }).subscribe();
    httpMock.expectOne('/api/operators/active').flush({ data: 'v2' });
  }));

  it('clearHttpCache should invalidate all entries', fakeAsync(() => {
    httpClient.get('/api/operators/active').subscribe();
    httpMock.expectOne('/api/operators/active').flush({ data: 'v1' });
    tick();

    clearHttpCache();

    httpClient.get('/api/operators/active').subscribe();
    httpMock.expectOne('/api/operators/active').flush({ data: 'v2' }); // Not from cache
  }));
});
