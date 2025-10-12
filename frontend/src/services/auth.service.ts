import { apiClient } from './api.client';
import { LoginResponse, User } from '../types/user';

class AuthService {
  async login(username: string, password: string): Promise<LoginResponse> {
    const response = await apiClient.post<{ data: LoginResponse }>('/auth/login', {
      username,
      password,
    });

    const loginData = response.data.data;

    // Store tokens
    localStorage.setItem('accessToken', loginData.accessToken);
    localStorage.setItem('refreshToken', loginData.refreshToken);

    return loginData;
  }

  logout(): void {
    // Clear local storage immediately for fast logout
    const token = this.getAccessToken();
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // Fire and forget - call backend to invalidate token
    // Don't await this to prevent blocking the logout flow
    if (token) {
      apiClient.post('/auth/logout').catch(error => {
        console.error('Logout API call failed (non-blocking):', error);
      });
    }
  }

  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<{ data: User }>('/auth/me');
    return response.data.data;
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}

export const authService = new AuthService();