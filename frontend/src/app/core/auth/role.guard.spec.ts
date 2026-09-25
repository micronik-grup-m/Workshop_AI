import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { roleGuard } from './role.guard';
import { AuthService } from './auth.service';

describe('roleGuard', () => {
    it('allows navigation when the user has the required role', () => {
        const authService = { getRole: () => 'ADMIN' } as Partial<AuthService>;
        TestBed.configureTestingModule({
            providers: [{ provide: AuthService, useValue: authService }, { provide: Router, useValue: { navigate: () => {} } }],
        });

        const result = TestBed.runInInjectionContext(() => roleGuard('ADMIN')({} as any, {} as any));

        expect(result).toBe(true);
    });

    it('blocks navigation and redirects to /employees when the role does not match', () => {
        const navigate = vi.fn();
        const authService = { getRole: () => 'USER' } as Partial<AuthService>;
        TestBed.configureTestingModule({
            providers: [{ provide: AuthService, useValue: authService }, { provide: Router, useValue: { navigate } }],
        });

        const result = TestBed.runInInjectionContext(() => roleGuard('ADMIN')({} as any, {} as any));

        expect(result).toBe(false);
        expect(navigate).toHaveBeenCalledWith(['/employees']);
    });
});
