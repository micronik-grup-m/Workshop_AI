// frontend/src/app/employees/employee.model.ts
export interface Employee {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    department: string;
    salary: number;
    hireDate: string;
}

export type EmployeeRequest = Omit<Employee, 'id'>;
