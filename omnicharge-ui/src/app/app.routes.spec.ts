import { routes } from './app.routes';

describe('App Routes', () => {
  it('should contain the expected public routes', () => {
    const landingRoute = routes.find(r => r.path === '');
    expect(landingRoute).toBeDefined();
    expect(landingRoute?.title).toBe('OmniCharge — Instant Mobile Recharge');

    const rechargeRoute = routes.find(r => r.path === 'recharge');
    expect(rechargeRoute).toBeDefined();
    expect(rechargeRoute?.canDeactivate).toBeDefined();

    const loginRoute = routes.find(r => r.path === 'login');
    expect(loginRoute).toBeDefined();
  });

  it('should contain protected routes with guards', () => {
    const dashboardRoute = routes.find(r => r.path === 'dashboard');
    expect(dashboardRoute).toBeDefined();
    expect(dashboardRoute?.canActivate).toBeDefined();

    const adminRoute = routes.find(r => r.path === 'admin');
    expect(adminRoute).toBeDefined();
    expect(adminRoute?.canActivate).toBeDefined();
  });

  it('should contain error routes and wildcard', () => {
    expect(routes.find(r => r.path === 'error/403')).toBeDefined();
    expect(routes.find(r => r.path === 'error/404')).toBeDefined();
    expect(routes.find(r => r.path === 'error/500')).toBeDefined();
    expect(routes.find(r => r.path === '**')).toBeDefined();
  });

  it('lazy loading targets should be functions', () => {
    const landingRoute = routes.find(r => r.path === '');
    expect(typeof landingRoute?.loadComponent).toBe('function');

    const adminRoute = routes.find(r => r.path === 'admin');
    expect(typeof adminRoute?.loadChildren).toBe('function');
  });
});
