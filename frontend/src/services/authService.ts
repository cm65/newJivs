import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  email: string;
  roles: string[];
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
  permissions: string[];
}

class AuthService {
  /**
   * Login user
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await axios.post(`${API_URL}/auth/login`, credentials);

    if (response.data.data) {
      const loginData = response.data.data;

      // Store tokens in localStorage
      localStorage.setItem('accessToken', loginData.accessToken);
      localStorage.setItem('refreshToken', loginData.refreshToken);
      localStorage.setItem('user', JSON.stringify({
        id: loginData.userId,
        username: loginData.username,
        email: loginData.email,
        roles: loginData.roles,
      }));

      return loginData;
    }

    throw new Error('Login failed');
  }

  /**
   * Register new user
   */
  async register(userData: RegisterRequest): Promise<any> {
    const response = await axios.post(`${API_URL}/auth/register`, userData);
    return response.data.data;
  }

  /**
   * Logout user
   */
  logout(): void {
    // Clear local storage immediately for fast logout
    const token = this.getAccessToken();
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');

    // Fire and forget - call backend to invalidate token
    // Don't await this to prevent blocking the logout flow
    if (token) {
      axios.post(`${API_URL}/auth/logout`, {}, {
        headers: { Authorization: `Bearer ${token}` },
        timeout: 5000, // 5 second timeout
      }).catch(error => {
        console.error('Logout API call failed (non-blocking):', error);
      });
    }
  }

  /**
   * Get current user
   */
  async getCurrentUser(): Promise<User> {
    const response = await axios.get(`${API_URL}/auth/me`, {
      headers: this.getAuthHeader(),
    });
    return response.data.data;
  }

  /**
   * Update user profile
   */
  async updateProfile(data: Partial<User>): Promise<User> {
    const response = await axios.put(`${API_URL}/auth/me`, data, {
      headers: this.getAuthHeader(),
    });
    return response.data.data;
  }

  /**
   * Change password
   */
  async changePassword(oldPassword: string, newPassword: string): Promise<void> {
    await axios.post(
      `${API_URL}/auth/change-password`,
      { oldPassword, newPassword },
      { headers: this.getAuthHeader() }
    );
  }

  /**
   * Request password reset
   */
  async forgotPassword(email: string): Promise<void> {
    await axios.post(`${API_URL}/auth/forgot-password`, { email });
  }

  /**
   * Reset password with token
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    await axios.post(`${API_URL}/auth/reset-password`, { token, newPassword });
  }

  /**
   * Refresh access token
   */
  async refreshToken(): Promise<string> {
    const refreshToken = localStorage.getItem('refreshToken');

    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await axios.post(`${API_URL}/auth/refresh`, {
      refreshToken,
    });

    const newAccessToken = response.data.data.accessToken;
    localStorage.setItem('accessToken', newAccessToken);

    return newAccessToken;
  }

  /**
   * Get stored access token
   */
  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  /**
   * Get stored user
   */
  getStoredUser(): User | null {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    const user = this.getStoredUser();
    return user?.roles?.includes(role) || false;
  }

  /**
   * Check if user has any of the specified roles
   */
  hasAnyRole(roles: string[]): boolean {
    const user = this.getStoredUser();
    if (!user?.roles) return false;
    return roles.some(role => user.roles.includes(role));
  }

  /**
   * Get authorization header
   */
  private getAuthHeader(): Record<string, string> {
    const token = this.getAccessToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }
}

export default new AuthService();
