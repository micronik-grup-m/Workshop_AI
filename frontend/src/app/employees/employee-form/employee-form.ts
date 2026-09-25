// frontend/src/app/employees/employee-form/employee-form.ts
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EmployeeService } from '../employee.service';

@Component({
    selector: 'app-employee-form',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './employee-form.html',
})
export class EmployeeForm {
    private readonly employeeService = inject(EmployeeService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    readonly employeeId = this.route.snapshot.paramMap.get('id');

    readonly firstName = signal('');
    readonly lastName = signal('');
    readonly email = signal('');
    readonly department = signal('');
    readonly salary = signal(0);
    readonly hireDate = signal('');
    readonly errorMessage = signal<string | null>(null);

    constructor() {
        if (this.employeeId) {
            this.employeeService.get(Number(this.employeeId)).subscribe((employee) => {
                this.firstName.set(employee.firstName);
                this.lastName.set(employee.lastName);
                this.email.set(employee.email);
                this.department.set(employee.department);
                this.salary.set(employee.salary);
                this.hireDate.set(employee.hireDate);
            });
        }
    }

    submit(): void {
        this.errorMessage.set(null);
        const request = {
            firstName: this.firstName(),
            lastName: this.lastName(),
            email: this.email(),
            department: this.department(),
            salary: this.salary(),
            hireDate: this.hireDate(),
        };

        const result$ = this.employeeId
            ? this.employeeService.update(Number(this.employeeId), request)
            : this.employeeService.create(request);

        result$.subscribe({
            next: () => this.router.navigate(['/employees']),
            error: () => this.errorMessage.set('Nu ai permisiunea necesară sau datele introduse sunt invalide.'),
        });
    }
}
