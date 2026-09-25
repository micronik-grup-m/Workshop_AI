import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

interface LoginResponse {
    token: string;
}

type Role = 'ADMIN' | 'USER';

const TOKEN_KEY = 'workshop_ai_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly http = inject(HttpClient);

    login(username: string, password: string): Observable<void> {
        return this.http
            .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, { username, password })
            .pipe(map((response) => localStorage.setItem(TOKEN_KEY, response.token)));
    }

    logout(): void {
        localStorage.removeItem(TOKEN_KEY);
    }

    isLoggedIn(): boolean {
        return this.getToken() !== null;
    }

    getToken(): string | null {
        return localStorage.getItem(TOKEN_KEY);
    }

    getRole(): Role | null {
        const token = this.getToken();
        if (!token) {
            return null;
        }
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.role ?? null;
        } catch {
            return null;
        }
    }
}
