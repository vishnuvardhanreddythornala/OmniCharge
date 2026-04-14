import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout.component';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminUsersComponent } from './admin-users.component';
import { AdminTransactionsComponent } from './admin-transactions.component';
import { AdminOperatorsComponent } from './admin-operators.component';
import { AdminNotificationsComponent } from './admin-notifications.component';
import { AdminOperatorPlansComponent } from './admin-operator-plans.component';
import { AdminProfileComponent } from './admin-profile.component';
import { AdminPlansComponent } from './admin-plans.component';

import { AdminRechargesComponent } from './admin-recharges.component';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'users', component: AdminUsersComponent },
      { path: 'transactions', component: AdminTransactionsComponent },
      { path: 'recharges', component: AdminRechargesComponent },
      { path: 'operators', component: AdminOperatorsComponent },
      { path: 'operators/:id/plans', component: AdminOperatorPlansComponent },
      { path: 'plans', component: AdminPlansComponent },
      { path: 'notifications', component: AdminNotificationsComponent },
      { path: 'profile', component: AdminProfileComponent }
    ]
  }
];
