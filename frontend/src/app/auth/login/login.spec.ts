import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Login } from './login';
import { AuthService } from '../../core/auth/auth.service';

describe('Login', () => {
    let fixture: ComponentFixture<Login>;
    let component: Login;
    let authService: { login: ReturnType<typeof vi.fn> };
    let router: { navigate: ReturnType<typeof vi.fn> };

    beforeEach(async () => {
        authService = { login: vi.fn() };
        router = { navigate: vi.fn() };

        await TestBed.configureTestingModule({
            imports: [Login],
            providers: [
                { provide: AuthService, useValue: authService },
                { provide: Router, useValue: router },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(Login);
        component = fixture.componentInstance;
    });

    it('navigates to /employees on successful login', () => {
        authService.login.mockReturnValue(of(undefined));

        component.username.set('admin');
        component.password.set('admin123');
        component.submit();

        expect(authService.login).toHaveBeenCalledWith('admin', 'admin123');
        expect(router.navigate).toHaveBeenCalledWith(['/employees']);
    });

    it('shows an error message on failed login', () => {
        authService.login.mockReturnValue(throwError(() => new Error('bad credentials')));

        component.username.set('admin');
        component.password.set('wrong');
        component.submit();

        expect(component.errorMessage()).toContain('incorect');
    });
});
