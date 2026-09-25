import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

// Mock localStorage for the test environment
const localStorageMock = (() => {
    let store: Record<string, string> = {};
    return {
        getItem: (key: string) => store[key] || null,
        setItem: (key: string, value: string) => {
            store[key] = value.toString();
        },
        removeItem: (key: string) => {
            delete store[key];
        },
        clear: () => {
            store = {};
        }
    };
})();

Object.defineProperty(window, 'localStorage', {
    value: localStorageMock
});

describe('authInterceptor', () => {
    let http: HttpClient;
    let httpMock: HttpTestingController;
    let authService: { logout: ReturnType<typeof vi.fn> };

    beforeEach(() => {
        localStorage.clear();
        authService = { logout: vi.fn() };
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(withInterceptors([authInterceptor])),
                provideHttpClientTesting(),
                provideRouter([]),
                { provide: AuthService, useValue: authService },
            ],
        });
        http = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('attaches the bearer token when present', () => {
        localStorage.setItem('workshop_ai_token', 'fake-token');

        http.get('/api/employees').subscribe();

        const req = httpMock.expectOne('/api/employees');
        expect(req.request.headers.get('Authorization')).toBe('Bearer fake-token');
    });

    it('does not attach a header when no token is stored', () => {
        http.get('/api/employees').subscribe();

        const req = httpMock.expectOne('/api/employees');
        expect(req.request.headers.has('Authorization')).toBe(false);
    });

    it('logs out and redirects to /login on a 401 response', () => {
        const router = TestBed.inject(Router);
        const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
        localStorage.setItem('workshop_ai_token', 'expired-token');

        const errorHandler = vi.fn();
        http.get('/api/employees').subscribe({ next: () => undefined, error: errorHandler });

        const req = httpMock.expectOne('/api/employees');
        req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

        expect(errorHandler).toHaveBeenCalled();
        expect(authService.logout).toHaveBeenCalled();
        expect(navigateSpy).toHaveBeenCalledWith(['/login']);
    });
});
