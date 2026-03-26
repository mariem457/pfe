import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const role =
    localStorage.getItem('role') || sessionStorage.getItem('role');
  const allowed = route.data['roles'] as string[];

  if (role && allowed?.includes(role)) return true;

  router.navigate(['/']);
  return false;
};