import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <!-- ═══ WELCOME SPLASH OVERLAY ═══ -->
    @if (showSplash()) {
      <div class="admin-splash">
        <div class="splash-content">
          <div class="splash-icon">
            <svg class="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
          </div>
          <h1 class="splash-title">Welcome to<br/><span>Admin Portal</span></h1>
          <p class="splash-subtitle">{{ authService.currentUser()?.fullName || 'Administrator' }}</p>
          <div class="splash-bar"><div class="splash-bar-fill"></div></div>
        </div>
        <div class="splash-particles">
          <div class="particle p1"></div>
          <div class="particle p2"></div>
          <div class="particle p3"></div>
          <div class="particle p4"></div>
          <div class="particle p5"></div>
          <div class="particle p6"></div>
        </div>
      </div>
    }    <div class="min-h-screen bg-surface flex font-sans text-surface-200">
      
      <!-- Sidebar -->
      <aside class="admin-sidebar hidden md:flex flex-col">
        
        <!-- Brand -->
        <a routerLink="/admin/dashboard" class="sidebar-brand block cursor-pointer group no-underline">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-omni-500 to-omni-700
                        flex items-center justify-center shadow-glow border border-white/10 relative overflow-hidden transition-all duration-300 group-hover:shadow-[0_0_20px_rgba(168,85,247,0.4)]">
              <span class="text-white font-bold text-base">⚡</span>
            </div>
            <div class="flex flex-col">
              <span class="text-xl font-display font-black text-white tracking-tight leading-none">Omni<span class="text-gradient">Admin</span></span>
              <span class="text-[10px] uppercase tracking-widest text-surface-400 font-semibold mt-1">Workspace</span>
            </div>
          </div>
        </a>

        <!-- Navigation -->
        <nav class="sidebar-nav">
          <div class="nav-section-label">MAIN</div>
          <a routerLink="/admin/dashboard" routerLinkActive="nav-active" class="nav-item">
            <div class="nav-icon">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
            </div>
            <span>Dashboard</span>
          </a>
          <a routerLink="/admin/users" routerLinkActive="nav-active" class="nav-item">
            <div class="nav-icon">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/></svg>
            </div>
            <span>Users</span>
          </a>

          <div class="nav-section-label mt-6">ANALYTICS</div>
          <a routerLink="/admin/recharges" routerLinkActive="nav-active" class="nav-item">
            <div class="nav-icon">
              <!-- Outline battery icon for recharges -->
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
            </div>
            <span>Recharges</span>
          </a>
          <a routerLink="/admin/transactions" routerLinkActive="nav-active" class="nav-item">
            <div class="nav-icon">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/></svg>
            </div>
            <span>Transactions</span>
          </a>
          <a routerLink="/admin/operators" routerLinkActive="nav-active" class="nav-item">
            <div class="nav-icon">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
            </div>
            <span>Operators</span>
          </a>
          <a routerLink="/admin/plans" routerLinkActive="nav-active" class="nav-item">
            <div class="nav-icon">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/></svg>
            </div>
            <span>Plans</span>
          </a>
          <a routerLink="/admin/notifications" routerLinkActive="nav-active" class="nav-item">
            <div class="nav-icon">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
            </div>
            <span>Notifications</span>
          </a>
        </nav>

        <!-- Settings Portal -->
        <div class="sidebar-footer">
          <a routerLink="/admin/profile" class="switch-portal-btn">
            <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            Profile & Settings
          </a>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="flex-1 flex flex-col min-w-0 overflow-hidden relative ml-[260px]">
        <!-- Universal Top Header -->
        <header class="h-16 border-b border-white/[0.05] flex items-center justify-between px-4 sm:px-8 bg-surface/80 backdrop-blur-md sticky top-0 z-40">
          <!-- Mobile Brand -->
          <div class="flex items-center gap-2 md:hidden">
            <span class="text-white font-bold text-sm bg-gradient-to-br from-omni-500 to-omni-700 w-7 h-7 rounded-lg flex items-center justify-center">⚡</span>
            <span class="font-display font-bold text-white tracking-tight text-lg">Omni<span class="text-gradient">Admin</span></span>
          </div>

          <!-- Desktop Title Space -->
          <div class="hidden md:block">
            <h2 class="text-white font-semibold font-display tracking-wide text-sm text-surface-400">Administrator Console</h2>
          </div>

          <!-- Right Actions -->
          <div class="flex items-center gap-3">
            <a routerLink="/admin/notifications" class="p-2 text-surface-400 hover:text-white hover:bg-white/[0.06] rounded-xl transition-all relative">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"/></svg>
            </a>
            <a routerLink="/admin/profile" class="flex items-center gap-2 p-1.5 rounded-xl hover:bg-white/[0.06] transition-all cursor-pointer">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-omni-500 to-accent-teal flex items-center justify-center text-white text-xs font-bold shadow-md">
                {{ authService.userInitials() }}
              </div>
            </a>
            <button (click)="showLogoutModal.set(true)" title="Sign Out" class="p-2 text-rose-400 hover:text-white hover:bg-rose-500/10 rounded-xl transition-all relative">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"/></svg>
            </button>
            <!-- Mobile Menu Toggle Placeholder -->
            <button class="md:hidden p-2 text-surface-400 hover:text-white transition-colors"><svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/></svg></button>
          </div>
        </header>

        <div class="flex-1 overflow-y-auto p-4 sm:p-8 h-screen">
          <div class="max-w-6xl mx-auto">
            <router-outlet></router-outlet>
          </div>
        </div>
      </main>

      <!-- ═══ LOGOUT CONFIRMATION MODAL ═══ -->
      @if (showLogoutModal()) {
        <div class="fixed inset-0 z-[10000] flex items-center justify-center p-4">
          <!-- Backdrop -->
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity animate-fade-in" (click)="cancelLogout()"></div>
          
          <!-- Modal -->
          <div class="relative w-full max-w-sm glass-card border flex flex-col items-center border-white/10 shadow-2xl rounded-3xl p-6 sm:p-8 animate-scale-in text-center">
            <div class="w-16 h-16 rounded-full bg-gradient-to-br from-surface-800 to-surface-700 mb-4 flex items-center justify-center shadow-inner border border-white/5 text-accent-rose">
              <svg class="w-8 h-8 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"></path>
              </svg>
            </div>
            <h2 class="text-xl font-bold font-display text-white mb-2">Sign Out</h2>
            <p class="text-sm font-medium text-surface-400 mb-8">
              Are you sure you want to sign out safely from OmniAdmin?
            </p>
            
            <div class="w-full flex gap-3">
              <button (click)="cancelLogout()" class="flex-1 py-3 text-sm font-semibold text-surface-300 hover:text-white bg-surface-800 hover:bg-surface-700 border border-white/5 rounded-xl transition-colors">
                Cancel
              </button>
              <button (click)="confirmLogout()" class="flex-1 py-3 text-sm font-semibold text-white bg-accent-rose hover:bg-rose-600 rounded-xl transition-colors shadow-lg shadow-accent-rose/20">
                Yes, Sign Out
              </button>
            </div>
          </div>
        </div>
      }

    </div>
  `,
  styles: [`
    /* ═══ SIDEBAR ═══ */
      .admin-sidebar {
      width: 260px;
      flex-shrink: 0;
      background: linear-gradient(180deg, #0d1222 0%, #0b0f1a 100%);
      border-right: 1px solid rgba(255,255,255,0.05);
      flex-direction: column;

      /*FIXED SIDEBAR */
      position: fixed;
      top: 0;
      left: 0;
      height: 100vh;
      z-index: 1000;

      overflow-y: auto;
    }
    .admin-sidebar::before {\n      display: none;\n    }

    .sidebar-brand {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 24px 24px 28px;
      position: relative;
    }
    .brand-icon {
      width: 36px; height: 36px;
      border-radius: 10px;
      background: linear-gradient(135deg, #6366f1, #818cf8);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 20px rgba(99,102,241,0.3);
    }

    .sidebar-nav {
      flex: 1;
      padding: 0 16px;
      position: relative;
    }
    .nav-section-label {
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 0.12em;
      color: rgba(255,255,255,0.2);
      padding: 0 12px;
      margin-bottom: 8px;
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 500;
      color: rgba(255,255,255,0.45);
      transition: all 0.2s ease;
      margin-bottom: 2px;
      text-decoration: none;
      position: relative;
    }
    .nav-item:hover {
      color: rgba(255,255,255,0.85);
      background: rgba(255,255,255,0.04);
    }
    .nav-icon {
      width: 32px; height: 32px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255,255,255,0.03);
      transition: all 0.2s ease;
    }
    .nav-item:hover .nav-icon {
      background: rgba(99,102,241,0.1);
    }

    .nav-active {
      color: white !important;
      background: rgba(99,102,241,0.12) !important;
    }
    .nav-active .nav-icon {
      background: linear-gradient(135deg, rgba(99,102,241,0.25), rgba(129,140,248,0.15)) !important;
      box-shadow: 0 0 12px rgba(99,102,241,0.15);
    }
    .nav-active::before {
      content: '';
      position: absolute;
      left: 0; top: 50%;
      transform: translateY(-50%);
      width: 3px; height: 20px;
      border-radius: 0 4px 4px 0;
      background: linear-gradient(180deg, #6366f1, #818cf8);
    }

    .sidebar-footer {
      padding: 16px;
      border-top: 1px solid rgba(255,255,255,0.04);
    }
    .switch-portal-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      padding: 10px;
      border-radius: 10px;
      font-size: 12px;
      font-weight: 600;
      color: rgba(255,255,255,0.5);
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.06);
      transition: all 0.25s ease;
      text-decoration: none;
      cursor: pointer;
    }
    .switch-portal-btn:hover {
      color: white;
      background: rgba(99,102,241,0.1);
      border-color: rgba(99,102,241,0.25);
      box-shadow: 0 0 20px rgba(99,102,241,0.08);
    }

    /* ═══ WELCOME SPLASH ═══ */
    .admin-splash {
      position: fixed;
      inset: 0;
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
      background: radial-gradient(ellipse at center, #141b2d 0%, #0b0f1a 60%, #080c16 100%);
      animation: splashFadeOut 0.6s ease-in-out 2.2s forwards;
    }
    .splash-content {
      text-align: center;
      animation: splashContentIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.2s both;
    }
    .splash-icon {
      width: 80px; height: 80px;
      margin: 0 auto 24px;
      border-radius: 24px;
      background: linear-gradient(135deg, #6366f1, #818cf8);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 60px rgba(99,102,241,0.4), 0 0 120px rgba(129,140,248,0.15);
      animation: splashIconPulse 2s ease-in-out infinite;
    }
    .splash-title {
      font-family: 'Outfit', sans-serif;
      font-size: 28px;
      font-weight: 300;
      color: rgba(255,255,255,0.6);
      line-height: 1.3;
      margin-bottom: 8px;
    }
    .splash-title span {
      display: block;
      font-size: 40px;
      font-weight: 800;
      background: linear-gradient(135deg, #a5b4fc, #6366f1);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .splash-subtitle {
      font-size: 14px;
      color: rgba(255,255,255,0.35);
      font-weight: 500;
      margin-bottom: 32px;
    }
    .splash-bar {
      width: 200px;
      height: 3px;
      margin: 0 auto;
      background: rgba(255,255,255,0.06);
      border-radius: 99px;
      overflow: hidden;
    }
    .splash-bar-fill {
      height: 100%;
      width: 0;
      background: linear-gradient(90deg, #6366f1, #818cf8);
      border-radius: 99px;
      animation: splashBarFill 2s ease-in-out 0.4s forwards;
    }

    /* Particles */
    .splash-particles {
      position: absolute;
      inset: 0;
      pointer-events: none;
      overflow: hidden;
    }
    .particle {
      position: absolute;
      border-radius: 50%;
      opacity: 0;
      animation: particleFloat 3s ease-in-out forwards;
    }
    .p1 { width: 6px; height: 6px; background: #6366f1; top: 30%; left: 15%; animation-delay: 0.3s; }
    .p2 { width: 4px; height: 4px; background: #818cf8; top: 60%; left: 80%; animation-delay: 0.6s; }
    .p3 { width: 8px; height: 8px; background: #a5b4fc; top: 20%; left: 70%; animation-delay: 0.9s; }
    .p4 { width: 3px; height: 3px; background: #a5b4fc; top: 75%; left: 25%; animation-delay: 0.4s; }
    .p5 { width: 5px; height: 5px; background: #6366f1; top: 45%; left: 90%; animation-delay: 0.7s; }
    .p6 { width: 4px; height: 4px; background: #818cf8; top: 85%; left: 55%; animation-delay: 1s; }

    @keyframes splashFadeOut {
      to { opacity: 0; pointer-events: none; }
    }
    @keyframes splashContentIn {
      from { opacity: 0; transform: translateY(30px) scale(0.95); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    @keyframes splashIconPulse {
      0%, 100% { box-shadow: 0 0 60px rgba(99,102,241,0.4), 0 0 120px rgba(129,140,248,0.15); }
      50% { box-shadow: 0 0 80px rgba(99,102,241,0.6), 0 0 160px rgba(129,140,248,0.25); }
    }
    @keyframes splashBarFill {
      to { width: 100%; }
    }
    @keyframes particleFloat {
      0% { opacity: 0; transform: translateY(0) scale(0); }
      30% { opacity: 0.6; transform: translateY(-20px) scale(1); }
      100% { opacity: 0; transform: translateY(-80px) scale(0.5); }
    }
  `]
})
export class AdminLayoutComponent implements OnInit {
  readonly authService = inject(AuthService);
  private router = inject(Router);

  showSplash = signal(true);
  showLogoutModal = signal(false);

  ngOnInit(): void {
    // Auto-dismiss splash after animation completes
    setTimeout(() => this.showSplash.set(false), 2800);
  }

  confirmLogout(): void {
    this.showLogoutModal.set(false);
    this.authService.logout();
  }

  cancelLogout(): void {
    this.showLogoutModal.set(false);
  }
}
