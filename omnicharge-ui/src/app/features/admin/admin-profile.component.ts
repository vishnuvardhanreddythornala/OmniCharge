import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-display font-bold text-white">Admin Profile Options</h1>
          <p class="text-surface-400 text-sm mt-1">Manage your administrative credentials and personal details.</p>
        </div>
      </div>

      <!-- Settings Cards -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <!-- General Profile -->
        <div class="glass-card p-6">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-omni-500/20 to-accent-teal/20 flex items-center justify-center border border-white/5">
              <svg class="w-5 h-5 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"/></svg>
            </div>
            <div>
              <h3 class="text-lg font-bold text-white">General Information</h3>
              <p class="text-sm text-surface-400">Update your account name.</p>
            </div>
          </div>

          <div class="space-y-4">
            <div class="form-group">
              <label>Full Name</label>
              <input type="text" [(ngModel)]="profileName" class="input-field" placeholder="Administrator Name" />
            </div>

            <div class="form-group">
              <label>Email Address</label>
              <div class="flex items-center gap-3">
                <div class="relative flex-1">
                  <input type="email" [value]="authService.currentUser()?.email" class="input-field !bg-white/[0.02] cursor-not-allowed text-surface-500" disabled />
                </div>
                <span class="text-xs text-accent-emerald font-semibold whitespace-nowrap">✓ Verified</span>
              </div>
            </div>

            @if (profileMsg()) {
              <div class="p-3 rounded-lg text-sm" [ngClass]="profileMsgError() ? 'bg-rose-500/10 text-accent-rose border border-rose-500/20' : 'bg-emerald-500/10 text-accent-emerald border border-emerald-500/20'">
                {{ profileMsg() }}
              </div>
            }

            <button (click)="onUpdateProfile()" [disabled]="profileSaving()" class="btn-primary w-full">
              {{ profileSaving() ? 'Saving...' : 'Update Profile' }}
            </button>
          </div>
        </div>

        <!-- Account Overview -->
        <div class="glass-card p-6">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-omni-500/20 to-accent-teal/20 flex items-center justify-center border border-white/5">
              <svg class="w-5 h-5 text-accent-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
            </div>
            <div>
              <h3 class="text-lg font-bold text-white">Account Overview</h3>
              <p class="text-sm text-surface-400">Your admin account details.</p>
            </div>
          </div>

          <div class="space-y-4">
            <div class="flex items-center justify-between p-4 rounded-xl bg-white/[0.02] border border-white/[0.05]">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-accent-emerald/10 flex items-center justify-center border border-accent-emerald/20">
                  <svg class="w-4 h-4 text-accent-emerald" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                </div>
                <div>
                  <p class="text-sm font-medium text-white">Account Status</p>
                  <p class="text-[10px] text-surface-500">Active since registration</p>
                </div>
              </div>
              <span class="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20">Active</span>
            </div>

            <div class="flex items-center justify-between p-4 rounded-xl bg-white/[0.02] border border-white/[0.05]">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-omni-500/10 flex items-center justify-center border border-omni-500/20">
                  <svg class="w-4 h-4 text-omni-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"/></svg>
                </div>
                <div>
                  <p class="text-sm font-medium text-white">Role</p>
                  <p class="text-[10px] text-surface-500">Full system access</p>
                </div>
              </div>
              <span class="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-omni-500/10 text-omni-400 border border-omni-500/20">Administrator</span>
            </div>

            <div class="flex items-center justify-between p-4 rounded-xl bg-white/[0.02] border border-white/[0.05]">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-accent-sky/10 flex items-center justify-center border border-accent-sky/20">
                  <svg class="w-4 h-4 text-accent-sky" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>
                </div>
                <div>
                  <p class="text-sm font-medium text-white">Support</p>
                  <p class="text-[10px] text-surface-500">Contact for help</p>
                </div>
              </div>
              <a href="mailto:omnicharge.app@gmail.com" class="text-xs text-omni-400 hover:text-omni-300 font-medium transition-colors">
                omnicharge.app&#64;gmail.com
              </a>
            </div>
          </div>
        </div>

      </div>
    </div>
  `
})
export class AdminProfileComponent implements OnInit {
  authService = inject(AuthService);

  // Profile Form
  profileName = '';
  profileSaving = signal(false);
  profileMsg = signal('');
  profileMsgError = signal(false);

  ngOnInit() {
    this.profileName = this.authService.currentUser()?.fullName || '';
  }

  onUpdateProfile() {
    if (!this.profileName) {
      this.profileMsg.set('Name is required');
      this.profileMsgError.set(true);
      return;
    }
    this.profileSaving.set(true);
    this.authService.updateProfile({ fullName: this.profileName }).subscribe({
      next: () => {
        this.profileMsg.set('Settings successfully saved.');
        this.profileMsgError.set(false);
        this.profileSaving.set(false);
        this.authService.loadProfile();
        
        setTimeout(() => {
          if (!this.profileMsgError()) this.profileMsg.set('');
        }, 4000);
      },
      error: (err: any) => {
        this.profileMsg.set(err.error?.message || 'Update failed');
        this.profileMsgError.set(true);
        this.profileSaving.set(false);
      }
    });
  }
}
