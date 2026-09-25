import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './login.html',
})
export class Login {
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);

    readonly username = signal('');
    readonly password = signal('');
    readonly errorMessage = signal<string | null>(null);

    submit(): void {
        this.errorMessage.set(null);
        this.authService.login(this.username(), this.password()).subscribe({
            next: () => this.router.navigate(['/employees']),
            error: () => this.errorMessage.set('Utilizator sau parolă incorect(ă).'),
        });
    }
}
