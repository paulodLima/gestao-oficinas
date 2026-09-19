import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try { await auth.me(); return true; }
  catch { auth.owner.set(null); return router.createUrlTree(['/entrar']); }
};

