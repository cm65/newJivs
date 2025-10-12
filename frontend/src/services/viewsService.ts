import apiClient from './apiClient';
import { FilterGroup } from '../components/FilterBuilder';
import { SortConfig } from '../hooks/useAdvancedFilters';

export interface SavedView {
  id: string;
  name: string;
  module: string;
  filters: FilterGroup[];
  sort: SortConfig[];
  isShared: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateViewRequest {
  name: string;
  module: string;
  filters: FilterGroup[];
  sort: SortConfig[];
  isShared: boolean;
}

class ViewsService {
  /**
   * Get all saved views for a module
   */
  async getViews(module: string): Promise<SavedView[]> {
    const response = await apiClient.get(`/views?module=${module}`);
    return response.data.data || response.data;
  }

  /**
   * Get a saved view by ID
   */
  async getView(id: string): Promise<SavedView> {
    const response = await apiClient.get(`/views/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Save a new view
   */
  async saveView(request: CreateViewRequest): Promise<SavedView> {
    const response = await apiClient.post('/views', request);
    return response.data.data || response.data;
  }

  /**
   * Update an existing view
   */
  async updateView(id: string, request: Partial<CreateViewRequest>): Promise<SavedView> {
    const response = await apiClient.put(`/views/${id}`, request);
    return response.data.data || response.data;
  }

  /**
   * Delete a saved view
   */
  async deleteView(id: string): Promise<void> {
    await apiClient.delete(`/views/${id}`);
  }

  /**
   * Share a view with team
   */
  async shareView(id: string): Promise<SavedView> {
    const response = await apiClient.post(`/views/${id}/share`);
    return response.data.data || response.data;
  }

  /**
   * Unshare a view
   */
  async unshareView(id: string): Promise<SavedView> {
    const response = await apiClient.post(`/views/${id}/unshare`);
    return response.data.data || response.data;
  }
}

export default new ViewsService();
