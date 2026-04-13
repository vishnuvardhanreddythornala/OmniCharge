import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavbarComponent } from './navbar.component';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let notifSpy: jasmine.SpyObj<NotificationService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'isAdmin', 'isProfileComplete', 'logout', 'userInitials', 'currentUser']);
    authSpy.isAuthenticated.and.returnValue(true);
    authSpy.isAdmin.and.returnValue(false);
    authSpy.isProfileComplete.and.returnValue(true);
    authSpy.userInitials.and.returnValue('JD');
    (authSpy as any).currentUser = signal({ fullName: 'John Doe', email: 'john@example.com', mobileNumber: '9999999999' });

    notifSpy = jasmine.createSpyObj('NotificationService', ['startPolling', 'stopPolling']);
    (notifSpy as any).unreadCount = signal(3);

    await TestBed.configureTestingModule({
      imports: [NavbarComponent, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: NotificationService, useValue: notifSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('toggleMenu should toggle menuOpen', () => {
    expect(component.menuOpen()).toBeFalse();
    component.toggleMenu();
    expect(component.menuOpen()).toBeTrue();
    component.toggleMenu();
    expect(component.menuOpen()).toBeFalse();
  });

  it('onLogout should close menus and show logout modal', () => {
    component.menuOpen.set(true);
    component.mobileMenuOpen.set(true);
    
    component.onLogout();
    
    expect(component.menuOpen()).toBeFalse();
    expect(component.mobileMenuOpen()).toBeFalse();
    expect(component.showLogoutModal()).toBeTrue();
  });

  it('confirmLogout should close modal and call authService.logout', () => {
    component.showLogoutModal.set(true);
    
    component.confirmLogout();
    
    expect(component.showLogoutModal()).toBeFalse();
    expect(authSpy.logout).toHaveBeenCalled();
  });

  it('cancelLogout should close modal', () => {
    component.showLogoutModal.set(true);
    
    component.cancelLogout();
    
    expect(component.showLogoutModal()).toBeFalse();
  });

  it('toggleNotifications should navigate to /dashboard with tab query for non-admin', () => {
    authSpy.isAdmin.and.returnValue(false);
    
    component.toggleNotifications();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard'], { queryParams: { tab: 'notifications' } });
  });

  it('navigateToProfile should navigate to /dashboard with profile tab', () => {
    component.navigateToProfile();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard'], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ tab: 'profile' }) }));
  });
});
