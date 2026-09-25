// frontend/src/app/employees/employee-form/employee-form.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { EmployeeForm } from './employee-form';
import { EmployeeService } from '../employee.service';

describe('EmployeeForm', () => {
    let fixture: ComponentFixture<EmployeeForm>;
    let component: EmployeeForm;
    let employeeService: { create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn> };
    let router: { navigate: ReturnType<typeof vi.fn> };

    async function setup(paramMap: Record<string, string>) {
        employeeService = { create: vi.fn(), update: vi.fn(), get: vi.fn() };
        employeeService.get.mockReturnValue(of({
            id: 7, firstName: 'Maria', lastName: 'Ionescu', email: 'm@example.com',
            department: 'Marketing', salary: 6000, hireDate: '2024-01-15',
        } as any));
        router = { navigate: vi.fn() };

        await TestBed.configureTestingModule({
            imports: [EmployeeForm],
            providers: [
                { provide: EmployeeService, useValue: employeeService },
                { provide: Router, useValue: router },
                { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: (key: string) => paramMap[key] ?? null } } } },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(EmployeeForm);
        component = fixture.componentInstance;
        fixture.detectChanges();
    }

    it('creates a new employee when there is no id', async () => {
        await setup({});
        employeeService.create.mockReturnValue(of({} as any));

        component.firstName.set('Maria');
        component.lastName.set('Ionescu');
        component.email.set('m@example.com');
        component.department.set('Marketing');
        component.salary.set(6000);
        component.hireDate.set('2024-01-15');
        component.submit();

        expect(employeeService.create).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/employees']);
    });

    it('updates an existing employee when an id is present', async () => {
        await setup({ id: '7' });
        employeeService.update.mockReturnValue(of({} as any));

        component.submit();

        expect(employeeService.update).toHaveBeenCalledWith(7, expect.any(Object));
    });

    it('sets an error message and does not navigate when the request fails', async () => {
        await setup({});
        employeeService.create.mockReturnValue(throwError(() => new Error('forbidden')));

        component.submit();

        expect(component.errorMessage()).toBe('Nu ai permisiunea necesară sau datele introduse sunt invalide.');
        expect(router.navigate).not.toHaveBeenCalled();
    });
});
