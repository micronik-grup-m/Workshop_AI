// frontend/src/app/employees/employee.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Employee, EmployeeRequest } from './employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiBaseUrl}/employees`;

    list(department?: string): Observable<Employee[]> {
        const options = department ? { params: { department } } : {};
        return this.http.get<Employee[]>(this.baseUrl, options);
    }

    get(id: number): Observable<Employee> {
        return this.http.get<Employee>(`${this.baseUrl}/${id}`);
    }

    search(min: number, max: number): Observable<Employee[]> {
        return this.http.get<Employee[]>(`${this.baseUrl}/search`, {
            params: { min: min.toString(), max: max.toString() }
        });
    }

    create(request: EmployeeRequest): Observable<Employee> {
        return this.http.post<Employee>(this.baseUrl, request);
    }

    update(id: number, request: EmployeeRequest): Observable<Employee> {
        return this.http.put<Employee>(`${this.baseUrl}/${id}`, request);
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`);
    }
}
