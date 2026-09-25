import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let authService: { isLoggedIn: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };
  let router: Router;

  async function setup(loggedIn: boolean) {
    authService = { isLoggedIn: vi.fn().mockReturnValue(loggedIn), logout: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  }

  it('should create the app', async () => {
    await setup(false);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('shows the nav when logged in', async () => {
    await setup(true);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('nav')).toBeTruthy();
  });

  it('hides the nav when not logged in', async () => {
    await setup(false);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('nav')).toBeFalsy();
  });

  it('logs out and navigates to /login', async () => {
    await setup(true);
    fixture.componentInstance.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
