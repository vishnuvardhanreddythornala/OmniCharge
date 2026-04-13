/**
 * App Routes — Lazy-loaded feature modules for optimal bundle splitting.
 *
 * Public routes:  /, /recharge, /login, /register, /forgot-password
 * Protected:      /dashboard/**
 * Admin:          /admin/**
 * Error:          /error/403, /error/404, /error/500
 */
import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/guards/auth.guard';
import { canDeactivateGuard } from './core/guards/can-deactivate.guard';

export const routes: Routes = [
  // ── Public: Landing Page ──
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent),
    title: 'OmniCharge — Instant Mobile Recharge',
    data: { title: 'OmniCharge', description: 'Recharge your mobile instantly with OmniCharge. Supports Jio, Airtel, Vi, and BSNL with highly secure, zero-latency Razorpay payments.' }
  },

  // ── Public: Recharge Flow (auth enforced only at checkout) ──
  {
    path: 'recharge',
    loadComponent: () => import('./features/recharge/recharge-flow.component').then(m => m.RechargeFlowComponent),
    canDeactivate: [canDeactivateGuard],
    title: 'Recharge — OmniCharge',
    data: { title: 'Recharge Your Mobile', description: 'Enter your phone number to securely recharge your prepaid or postpaid mobile using OmniCharge. Instant, no hidden fees.', preload: true }
  },

  // ── Auth Pages ──
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    title: 'Sign In — OmniCharge',
    data: { title: 'Sign In', description: 'Sign in to your OmniCharge account for faster transactions and to view your recharge history.' }
  },

  // ── Protected: User Dashboard ──
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    title: 'Dashboard — OmniCharge',
    data: { preload: true }
  },
  
  // ── Protected: Admin Portal ──
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes),
    data: { preloadAdmin: true }
  },

  // ── Error Pages ──
  {
    path: 'error/403',
    loadComponent: () => import('./features/errors/error-pages.component').then(m => m.Error403Component),
    title: 'Access Denied — OmniCharge'
  },
  {
    path: 'error/404',
    loadComponent: () => import('./features/errors/error-pages.component').then(m => m.Error404Component),
    title: 'Not Found — OmniCharge'
  },
  {
    path: 'error/500',
    loadComponent: () => import('./features/errors/error-pages.component').then(m => m.Error500Component),
    title: 'Server Error — OmniCharge'
  },

  // ── Catch-all → 404 ──
  {
    path: '**',
    loadComponent: () => import('./features/errors/error-pages.component').then(m => m.Error404Component),
    title: 'Not Found — OmniCharge'
  }
];

