// frontend/src/app/employees/employee-list/employee-list.ts
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmployeeService } from '../employee.service';
import { AuthService } from '../../core/auth/auth.service';
import { Employee } from '../employee.model';

@Component({
    selector: 'app-employee-list',
    standalone: true,
    imports: [RouterLink],
    templateUrl: './employee-list.html',
})
export class EmployeeList {
    private readonly employeeService = inject(EmployeeService);
    private readonly authService = inject(AuthService);

    readonly employees = signal<Employee[]>([]);
    readonly canWrite = this.authService.getRole() === 'ADMIN';

    constructor() {
        this.refresh();
    }

    deleteEmployee(id: number): void {
        this.employeeService.delete(id).subscribe(() => this.refresh());
    }

    private refresh(): void {
        this.employeeService.list().subscribe((employees) => this.employees.set(employees));
    }
}
