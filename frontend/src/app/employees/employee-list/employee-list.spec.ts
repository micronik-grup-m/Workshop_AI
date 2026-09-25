// frontend/src/app/employees/employee-list/employee-list.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { EmployeeList } from './employee-list';
import { EmployeeService } from '../employee.service';
import { AuthService } from '../../core/auth/auth.service';
import { Employee } from '../employee.model';

describe('EmployeeList', () => {
    let fixture: ComponentFixture<EmployeeList>;
    let employeeService: { list: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };

    const employees: Employee[] = [
        { id: 1, firstName: 'Maria', lastName: 'Ionescu', email: 'm@example.com', department: 'Marketing', salary: 6000, hireDate: '2024-01-15' },
    ];

    async function setup(role: 'ADMIN' | 'USER') {
        employeeService = { list: vi.fn(), delete: vi.fn() };
        employeeService.list.mockReturnValue(of(employees));
        employeeService.delete.mockReturnValue(of(undefined));
        const authService = { getRole: () => role } as Partial<AuthService>;

        await TestBed.configureTestingModule({
            imports: [EmployeeList],
            providers: [
                provideRouter([]),
                { provide: EmployeeService, useValue: employeeService },
                { provide: AuthService, useValue: authService },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(EmployeeList);
        fixture.detectChanges();
    }

    it('loads and displays employees for ADMIN', async () => {
        await setup('ADMIN');

        expect(fixture.componentInstance.employees()).toEqual(employees);
        expect(fixture.componentInstance.canWrite).toBe(true);
    });

    it('hides write actions for USER', async () => {
        await setup('USER');

        expect(fixture.componentInstance.canWrite).toBe(false);
    });

    it('deletes an employee and refreshes the list', async () => {
        await setup('ADMIN');

        fixture.componentInstance.deleteEmployee(1);

        expect(employeeService.delete).toHaveBeenCalledWith(1);
        expect(employeeService.list).toHaveBeenCalledTimes(2);
    });
});
