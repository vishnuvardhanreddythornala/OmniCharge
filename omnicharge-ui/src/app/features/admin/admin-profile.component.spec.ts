import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AdminProfileComponent } from './admin-profile.component';
import { AuthService } from '../../core/services/auth.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { signal } from '@angular/core';

describe('AdminProfileComponent', () => {
  let component: AdminProfileComponent;
  let fixture: ComponentFixture<AdminProfileComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    // createSpyObj didn't work for signals directly, so we just construct an object
    authSpy = jasmine.createSpyObj('AuthService', ['updateProfile', 'loadProfile'], {
      currentUser: signal({ fullName: 'Admin User', email: 'admin@example.com' })
    });

    await TestBed.configureTestingModule({
      imports: [AdminProfileComponent, FormsModule],
      providers: [
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminProfileComponent);
    component = fixture.componentInstance;
  });

  it('should initialize profile name from current user', () => {
    fixture.detectChanges();
    expect(component.profileName).toBe('Admin User');
  });

  it('onUpdateProfile should show error if name is empty', () => {
    component.profileName = '';
    component.onUpdateProfile();
    
    expect(component.profileMsgError()).toBeTrue();
    expect(component.profileMsg()).toBe('Name is required');
    expect(authSpy.updateProfile).not.toHaveBeenCalled();
  });

  it('onUpdateProfile should update profile successfully and clear msg after 4s', fakeAsync(() => {
    authSpy.updateProfile.and.returnValue(of({ success: true, message: 'Settings successfully saved.' } as any));
    
    component.profileName = 'New Admin';
    component.onUpdateProfile();
    
    expect(component.profileSaving()).toBeFalse();
    expect(component.profileMsgError()).toBeFalse();
    expect(component.profileMsg()).toBe('Settings successfully saved.');
    expect(authSpy.updateProfile).toHaveBeenCalledWith({ fullName: 'New Admin' });
    expect(authSpy.loadProfile).toHaveBeenCalled();
    
    tick(4000);
    expect(component.profileMsg()).toBe('');
  }));

  it('onUpdateProfile should show error if API fails', () => {
    authSpy.updateProfile.and.returnValue(throwError(() => ({ error: { message: 'API Failed' } })));
    
    component.profileName = 'New Admin';
    component.onUpdateProfile();
    
    expect(component.profileSaving()).toBeFalse();
    expect(component.profileMsgError()).toBeTrue();
    expect(component.profileMsg()).toBe('API Failed');
  });
});
