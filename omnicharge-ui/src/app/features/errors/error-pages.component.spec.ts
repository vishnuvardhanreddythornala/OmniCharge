import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Error403Component, Error404Component, Error500Component } from './error-pages.component';
import { AuthService } from '../../core/services/auth.service';

describe('Error Pages', () => {
  let routerSpy: jasmine.SpyObj<Router>;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);
    authSpy = jasmine.createSpyObj('AuthService', ['isAdmin', 'isAuthenticated']);
  });

  describe('Error403Component', () => {
    let component: Error403Component;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [Error403Component],
        providers: [
          { provide: Router, useValue: routerSpy },
          { provide: AuthService, useValue: authSpy }
        ],
        schemas: [NO_ERRORS_SCHEMA]
      })
      .overrideComponent(Error403Component, { set: { template: '<div></div>', imports: [], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

      component = TestBed.createComponent(Error403Component).componentInstance;
    });

    it('should navigate to /admin/dashboard for admin', () => {
      authSpy.isAdmin.and.returnValue(true);
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/dashboard']);
    });

    it('should navigate to /dashboard for authenticated user', () => {
      authSpy.isAdmin.and.returnValue(false);
      authSpy.isAuthenticated.and.returnValue(true);
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should navigate to / for unauthenticated user', () => {
      authSpy.isAdmin.and.returnValue(false);
      authSpy.isAuthenticated.and.returnValue(false);
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });
  });

  describe('Error404Component', () => {
    let component: Error404Component;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [Error404Component],
        providers: [
          { provide: Router, useValue: routerSpy },
          { provide: AuthService, useValue: authSpy }
        ],
        schemas: [NO_ERRORS_SCHEMA]
      })
      .overrideComponent(Error404Component, { set: { template: '<div></div>', imports: [], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

      component = TestBed.createComponent(Error404Component).componentInstance;
    });

    it('should navigate to /admin/dashboard for admin', () => {
      authSpy.isAdmin.and.returnValue(true);
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/dashboard']);
    });

    it('should navigate to / for unauthenticated user', () => {
      authSpy.isAdmin.and.returnValue(false);
      authSpy.isAuthenticated.and.returnValue(false);
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });
  });

  describe('Error500Component', () => {
    let component: Error500Component;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [Error500Component],
        providers: [
          { provide: Router, useValue: routerSpy },
          { provide: AuthService, useValue: authSpy },
          { provide: ActivatedRoute, useValue: { snapshot: { queryParams: { returnUrl: '/recharge' } } } }
        ],
        schemas: [NO_ERRORS_SCHEMA]
      })
      .overrideComponent(Error500Component, { set: { template: '<div></div>', imports: [], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

      component = TestBed.createComponent(Error500Component).componentInstance;
    });

    it('should navigate to returnUrl on tryAgain', () => {
      component.tryAgain();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/recharge');
    });

    it('should default to / when no returnUrl', async () => {
      await TestBed.resetTestingModule().configureTestingModule({
        imports: [Error500Component],
        providers: [
          { provide: Router, useValue: routerSpy },
          { provide: AuthService, useValue: authSpy },
          { provide: ActivatedRoute, useValue: { snapshot: { queryParams: {} } } }
        ],
        schemas: [NO_ERRORS_SCHEMA]
      })
      .overrideComponent(Error500Component, { set: { template: '<div></div>', imports: [], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

      const newComp = TestBed.createComponent(Error500Component).componentInstance;
      newComp.tryAgain();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/');
    });

    it('should navigate to /admin/dashboard for admin goHome', () => {
      authSpy.isAdmin.and.returnValue(true);
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/dashboard']);
    });
  });
});
