import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

function fakeJwt(role: string): string {
    const header = btoa(JSON.stringify({ alg: 'none' }));
    const payload = btoa(JSON.stringify({ sub: 'admin', role }));
    return `${header}.${payload}.signature`;
}

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

describe('AuthService', () => {
    let service: AuthService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        localStorage.clear();
        TestBed.configureTestingModule({
            providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(AuthService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('stores the token and role on successful login', () => {
        service.login('admin', 'admin123').subscribe();

        const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
        expect(req.request.method).toBe('POST');
        req.flush({ token: fakeJwt('ADMIN') });

        expect(service.isLoggedIn()).toBe(true);
        expect(service.getRole()).toBe('ADMIN');
    });

    it('clears the token on logout', () => {
        service.login('admin', 'admin123').subscribe();
        httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`).flush({ token: fakeJwt('ADMIN') });

        service.logout();

        expect(service.isLoggedIn()).toBe(false);
        expect(service.getToken()).toBeNull();
    });

    it('returns null for getRole() when token is malformed', () => {
        // Directly set a malformed token in localStorage
        localStorage.setItem('workshop_ai_token', 'malformed.token.that.is.not.valid');

        expect(service.getRole()).toBeNull();
    });
});
