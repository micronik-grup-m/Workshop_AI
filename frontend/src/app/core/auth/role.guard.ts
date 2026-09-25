import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export function roleGuard(requiredRole: 'ADMIN' | 'USER'): CanActivateFn {
    return () => {
        const authService = inject(AuthService);
        const router = inject(Router);

        if (authService.getRole() === requiredRole) {
            return true;
        }
        router.navigate(['/employees']);
        return false;
    };
}
