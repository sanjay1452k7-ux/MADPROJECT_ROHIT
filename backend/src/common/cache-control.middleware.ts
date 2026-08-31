import { ServerResponse } from 'http';

/**
 * Tells clients what may be re-read from a cache, and for how long.
 *
 * The distinction matters (Section 36). Analytics and profiles are worth
 * holding briefly so the app can still show something useful without a
 * connection. Live attendance state must never be served from a cache: a stale
 * "session active" or a stale "already marked" would be worse than showing
 * nothing, because the user would act on it.
 *
 * Everything is marked private: these responses are one student's or one
 * teacher's data and must not be held by a shared proxy.
 *
 * This is middleware rather than an interceptor because an interceptor only
 * decorates a *successful* handler result - a rejected sign-in is produced by
 * the exception filter and would have gone out with no directive at all, which
 * is precisely the response that most needs `no-store`. Setting the header
 * before the route runs covers both outcomes.
 */
export function cacheControlMiddleware(
  request: { method?: string; url?: string },
  response: ServerResponse,
  next: () => void,
): void {
  // The query string says nothing about cacheability; compare the path alone.
  const path = (request.url ?? '').split('?')[0];
  response.setHeader('Cache-Control', directiveFor(request.method ?? 'GET', path));
  next();
}

function directiveFor(method: string, path: string): string {
  // Anything that changes state, and anything carrying a credential, is never
  // stored.
  if (method !== 'GET' || path.startsWith('/auth')) {
    return 'no-store';
  }

  // Live attendance state: a cached answer here would be actively misleading.
  if (path.includes('active-session') || path.startsWith('/attendance-sessions')) {
    return 'no-store';
  }

  // The audit trail is an accountability record; a stale copy defeats it.
  if (path.startsWith('/admin/audit')) {
    return 'no-store';
  }

  // Reference-ish reads: brief caching lets the app render offline, while the
  // window is short enough that a marked attendance shows up promptly.
  if (path.startsWith('/analytics') || path.startsWith('/students/me/profile')) {
    return 'private, max-age=60';
  }

  return 'private, max-age=30';
}
