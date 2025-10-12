import apiClient from './apiClient';

export interface UserPreferences {
  theme: 'light' | 'dark';
  language?: string;
  notificationsEnabled?: boolean;
  emailNotifications?: boolean;
}

export const preferencesService = {
  /**
   * Get user's theme preference
   */
  async getThemePreference(): Promise<UserPreferences> {
    try {
      const response = await apiClient.get<UserPreferences>('/preferences/theme');
      return response.data;
    } catch (error) {
      console.error('Failed to get theme preference:', error);
      // Return default on error
      return { theme: 'light' };
    }
  },

  /**
   * Update user's theme preference
   */
  async updateThemePreference(theme: 'light' | 'dark'): Promise<void> {
    try {
      await apiClient.put('/preferences/theme', { theme });
    } catch (error) {
      console.error('Failed to update theme preference:', error);
      throw error;
    }
  },

  /**
   * Get all user preferences
   */
  async getAllPreferences(): Promise<UserPreferences> {
    try {
      const response = await apiClient.get<UserPreferences>('/preferences');
      return response.data;
    } catch (error) {
      console.error('Failed to get preferences:', error);
      return { theme: 'light' };
    }
  },

  /**
   * Update all user preferences
   */
  async updatePreferences(preferences: Partial<UserPreferences>): Promise<void> {
    try {
      await apiClient.put('/preferences', preferences);
    } catch (error) {
      console.error('Failed to update preferences:', error);
      throw error;
    }
  },
};

export default preferencesService;
