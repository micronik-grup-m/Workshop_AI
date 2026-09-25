import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { EmployeeList } from './employees/employee-list/employee-list';
import { EmployeeForm } from './employees/employee-form/employee-form';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'employees', component: EmployeeList, canActivate: [authGuard] },
    { path: 'employees/new', component: EmployeeForm, canActivate: [authGuard, roleGuard('ADMIN')] },
    { path: 'employees/:id/edit', component: EmployeeForm, canActivate: [authGuard, roleGuard('ADMIN')] },
    { path: '', redirectTo: 'login', pathMatch: 'full' },
];
