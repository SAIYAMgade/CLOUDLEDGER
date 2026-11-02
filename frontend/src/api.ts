const API_BASE = '/api/v1';

export class ApiClient {
  private static token: string | null = localStorage.getItem('cloudledger_token');

  public static setToken(token: string | null) {
    this.token = token;
    if (token) {
      localStorage.setItem('cloudledger_token', token);
    } else {
      localStorage.removeItem('cloudledger_token');
    }
  }

  public static getToken(): string | null {
    return this.token;
  }

  public static async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});
    if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    if (this.token) {
      headers.set('Authorization', `Bearer ${this.token}`);
    }

    if (!headers.has('X-Correlation-ID')) {
      headers.set('X-Correlation-ID', 'web-' + Math.random().toString(36).substring(2, 10));
    }

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers
    });

    if (response.status === 401) {
      this.setToken(null);
      window.dispatchEvent(new Event('auth-unauthorized'));
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('text/csv')) {
      return (await response.text()) as unknown as T;
    }

    const json = await response.json();
    if (!response.ok || (json && json.success === false)) {
      throw new Error(json.message || `Request failed with status ${response.status}`);
    }

    return json.data as T;
  }
}
