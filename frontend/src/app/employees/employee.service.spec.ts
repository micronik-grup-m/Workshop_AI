// frontend/src/app/employees/employee.service.spec.ts
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { EmployeeService } from './employee.service';
import { environment } from '../../environments/environment';
import { Employee, EmployeeRequest } from './employee.model';

describe('EmployeeService', () => {
    let service: EmployeeService;
    let httpMock: HttpTestingController;

    const sample: Employee = {
        id: 1,
        firstName: 'Maria',
        lastName: 'Ionescu',
        email: 'maria.ionescu@example.com',
        department: 'Marketing',
        salary: 6000,
        hireDate: '2024-01-15',
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [EmployeeService, provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(EmployeeService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('lists employees', () => {
        service.list().subscribe((employees) => expect(employees).toEqual([sample]));

        httpMock.expectOne(`${environment.apiBaseUrl}/employees`).flush([sample]);
    });

    it('creates an employee', () => {
        const request: EmployeeRequest = { ...sample } as unknown as EmployeeRequest;

        service.create(request).subscribe((created) => expect(created).toEqual(sample));

        const req = httpMock.expectOne(`${environment.apiBaseUrl}/employees`);
        expect(req.request.method).toBe('POST');
        req.flush(sample);
    });

    it('deletes an employee', () => {
        service.delete(1).subscribe();

        const req = httpMock.expectOne(`${environment.apiBaseUrl}/employees/1`);
        expect(req.request.method).toBe('DELETE');
        req.flush(null);
    });
});
