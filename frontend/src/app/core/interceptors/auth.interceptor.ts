import { HttpInterceptorFn } from '@angular/common/http';

const PUBLIC_AUTH_URLS = [
  '/api/auth/login',
  '/api/auth/register-driver',
  '/api/auth/refresh',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
  '/api/auth/reset-password-by-code',
  '/api/auth/resend-verification',
  '/api/auth/verify-reset-code',
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const isPublicAuthUrl = PUBLIC_AUTH_URLS.some(url => req.url.includes(url));

  if (isPublicAuthUrl) {
    return next(req);
  }

  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token');

  if (!token) return next(req);

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    })
  );
};