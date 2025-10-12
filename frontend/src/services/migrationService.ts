import apiClient from './apiClient';

export interface Migration {
  id: string;
  name: string;
  status: 'PENDING' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'ROLLING_BACK';
  phase: string;
  progress: number;
  recordsMigrated: number;
  totalRecords: number;
  createdAt: string;
  updatedAt: string;
}

export interface MigrationConfig {
  name: string;
  sourceConfig: Record<string, any>;
  targetConfig: Record<string, any>;
  transformationRules?: any[];
  schedule?: string;
}

export interface MigrationProgress {
  overallProgress: number;
  currentPhase: string;
  recordsMigrated: number;
  totalRecords: number;
  startTime: string;
  estimatedCompletion: string;
}

export interface MigrationStatistics {
  recordsMigrated: number;
  recordsFailed: number;
  bytesMigrated: number;
  duration: number;
  throughput: number;
}

class MigrationService {
  /**
   * Create a new migration job
   */
  async createMigration(config: MigrationConfig): Promise<Migration> {
    const response = await apiClient.post('/migrations', config);
    return response.data.data || response.data;
  }

  /**
   * Get all migrations
   */
  async getMigrations(page = 0, size = 20, status?: string): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (status) params.append('status', status);

    const response = await apiClient.get(`/migrations?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get migration by ID
   */
  async getMigration(id: string): Promise<Migration> {
    const response = await apiClient.get(`/migrations/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Start migration job
   */
  async startMigration(id: string): Promise<void> {
    await apiClient.post(`/migrations/${id}/start`);
  }

  /**
   * Pause migration job
   */
  async pauseMigration(id: string): Promise<void> {
    await apiClient.post(`/migrations/${id}/pause`);
  }

  /**
   * Resume migration job
   */
  async resumeMigration(id: string): Promise<void> {
    await apiClient.post(`/migrations/${id}/resume`);
  }

  /**
   * Rollback migration job
   */
  async rollbackMigration(id: string): Promise<void> {
    await apiClient.post(`/migrations/${id}/rollback`);
  }

  /**
   * Delete migration job
   */
  async deleteMigration(id: string): Promise<void> {
    await apiClient.delete(`/migrations/${id}`);
  }

  /**
   * Get migration progress
   */
  async getProgress(id: string): Promise<MigrationProgress> {
    const response = await apiClient.get(`/migrations/${id}/progress`);
    return response.data.data || response.data;
  }

  /**
   * Get migration statistics
   */
  async getStatistics(id: string): Promise<MigrationStatistics> {
    const response = await apiClient.get(`/migrations/${id}/statistics`);
    return response.data.data || response.data;
  }

  /**
   * Validate migration configuration
   */
  async validateMigration(config: MigrationConfig): Promise<any> {
    const response = await apiClient.post('/migrations/validate', config);
    return response.data.data || response.data;
  }
}

export default new MigrationService();
