import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';
import { ToastService } from '../services/toast.service';

describe('errorInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    toastSpy = jasmine.createSpyObj('ToastService', ['error', 'warning']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should pass through 401 without handling', () => {
    httpClient.get('/api/test').subscribe({
      error: (err) => {
        expect(err.status).toBe(401);
        expect(toastSpy.error).not.toHaveBeenCalled();
      }
    });
    httpMock.expectOne('/api/test').flush({}, { status: 401, statusText: 'Unauthorized' });
  });

  it('should show toast on 500 server error', () => {
    httpClient.get('/api/test').subscribe({ error: () => {} });
    httpMock.expectOne('/api/test').flush({}, { status: 500, statusText: 'Internal Server Error' });
    expect(toastSpy.error).toHaveBeenCalledWith('Something went wrong on our server. Please try again in a moment.');
  });

  it('should show toast on 502/503/504', () => {
    httpClient.get('/api/a').subscribe({ error: () => {} });
    httpMock.expectOne('/api/a').flush({}, { status: 502, statusText: 'Bad Gateway' });
    expect(toastSpy.error).toHaveBeenCalled();
  });

  it('should show network warning on status 0', () => {
    httpClient.get('/api/test').subscribe({ error: () => {} });
    httpMock.expectOne('/api/test').error(new ProgressEvent('error'), { status: 0 });
    expect(toastSpy.warning).toHaveBeenCalledWith('The server is temporarily unreachable. Please try again later.');
  });

  it('should navigate to /error/403 on non-API 403', () => {
    httpClient.get('/page/something').subscribe({ error: () => {} });
    httpMock.expectOne('/page/something').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/error/403']);
  });

  it('should NOT navigate on API 403', () => {
    httpClient.get('/api/protected').subscribe({ error: () => {} });
    httpMock.expectOne('/api/protected').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should navigate to /error/404 on non-API 404', () => {
    httpClient.get('/page/missing').subscribe({ error: () => {} });
    httpMock.expectOne('/page/missing').flush({}, { status: 404, statusText: 'Not Found' });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/error/404']);
  });

  it('should NOT navigate on API 404', () => {
    httpClient.get('/api/resource/999').subscribe({ error: () => {} });
    httpMock.expectOne('/api/resource/999').flush({}, { status: 404, statusText: 'Not Found' });
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should format Spring validation errors on 400', () => {
    httpClient.get('/api/register').subscribe({
      error: (err) => {
        expect(err.error.message).toContain('Validation failed');
      }
    });
    httpMock.expectOne('/api/register').flush(
      { errors: { email: 'Email required', name: 'Name required' } },
      { status: 400, statusText: 'Bad Request' }
    );
  });
});
