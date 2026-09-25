import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
    it('allows navigation when logged in', () => {
        const authService = { isLoggedIn: () => true } as Partial<AuthService>;
        TestBed.configureTestingModule({
            providers: [{ provide: AuthService, useValue: authService }, { provide: Router, useValue: { navigate: () => {} } }],
        });

        const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

        expect(result).toBe(true);
    });

    it('blocks navigation and redirects when logged out', () => {
        const navigate = vi.fn();
        const authService = { isLoggedIn: () => false } as Partial<AuthService>;
        TestBed.configureTestingModule({
            providers: [{ provide: AuthService, useValue: authService }, { provide: Router, useValue: { navigate } }],
        });

        const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

        expect(result).toBe(false);
        expect(navigate).toHaveBeenCalledWith(['/login']);
    });
});
