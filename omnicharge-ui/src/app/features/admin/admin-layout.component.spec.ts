import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AdminLayoutComponent } from './admin-layout.component';
import { AuthService } from '../../core/services/auth.service';
import { RouterTestingModule } from '@angular/router/testing';
import { signal } from '@angular/core';

describe('AdminLayoutComponent', () => {
  let component: AdminLayoutComponent;
  let fixture: ComponentFixture<AdminLayoutComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['logout'], {
      currentUser: signal({ fullName: 'Admin User' }),
      userInitials: signal('AU')
    });

    await TestBed.configureTestingModule({
      imports: [AdminLayoutComponent, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLayoutComponent);
    component = fixture.componentInstance;
  });

  it('should auto-dismiss splash after 2800ms', fakeAsync(() => {
    fixture.detectChanges();
    expect(component.showSplash()).toBeTrue();
    tick(2800);
    expect(component.showSplash()).toBeFalse();
    discardPeriodicTasks();
  }));

  it('cancelLogout should dismiss logout modal', () => {
    fixture.detectChanges();
    component.showLogoutModal.set(true);
    component.cancelLogout();
    expect(component.showLogoutModal()).toBeFalse();
    expect(authSpy.logout).not.toHaveBeenCalled();
  });

  it('confirmLogout should dismiss logout modal and call authService.logout', () => {
    fixture.detectChanges();
    component.showLogoutModal.set(true);
    component.confirmLogout();
    expect(component.showLogoutModal()).toBeFalse();
    expect(authSpy.logout).toHaveBeenCalled();
  });
});
