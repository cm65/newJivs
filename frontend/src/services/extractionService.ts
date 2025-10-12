import apiClient from './apiClient';

export interface Extraction {
  id: string;
  name: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'STOPPED';
  sourceType: string;
  recordsExtracted: number;
  progress?: number; // Optional progress percentage (0-100)
  createdAt: string;
  updatedAt: string;
}

export interface ExtractionConfig {
  name: string;
  sourceType: 'JDBC' | 'SAP' | 'FILE' | 'API';
  connectionConfig: Record<string, any>;
  extractionQuery?: string;
  schedule?: string;
}

export interface ExtractionStatistics {
  recordsExtracted: number;
  bytesExtracted: number;
  duration: number;
  throughput: number;
}

export interface BulkActionResponse {
  status: string;
  totalProcessed: number;
  successCount: number;
  failureCount: number;
  successfulIds: string[];
  failedIds: Record<string, string>;
  message: string;
  processingTimeMs: number;
}

class ExtractionService {
  /**
   * Create a new extraction job
   */
  async createExtraction(config: ExtractionConfig): Promise<Extraction> {
    const response = await apiClient.post('/extractions', config);
    return response.data.data || response.data;
  }

  /**
   * Get all extractions
   */
  async getExtractions(page = 0, size = 20, status?: string): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (status) params.append('status', status);

    const response = await apiClient.get(`/extractions?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get extraction by ID
   */
  async getExtraction(id: string): Promise<Extraction> {
    const response = await apiClient.get(`/extractions/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Start extraction job
   */
  async startExtraction(id: string): Promise<void> {
    await apiClient.post(`/extractions/${id}/start`);
  }

  /**
   * Stop extraction job
   */
  async stopExtraction(id: string): Promise<void> {
    await apiClient.post(`/extractions/${id}/stop`);
  }

  /**
   * Delete extraction job
   */
  async deleteExtraction(id: string): Promise<void> {
    await apiClient.delete(`/extractions/${id}`);
  }

  /**
   * Get extraction statistics
   */
  async getStatistics(id: string): Promise<ExtractionStatistics> {
    const response = await apiClient.get(`/extractions/${id}/statistics`);
    return response.data.data || response.data;
  }

  /**
   * Test extraction connection
   */
  async testConnection(connectionConfig: Record<string, any>): Promise<any> {
    const response = await apiClient.post('/extractions/test-connection', connectionConfig);
    return response.data.data || response.data;
  }

  /**
   * Get extraction logs
   */
  async getLogs(id: string, limit = 100): Promise<any[]> {
    const response = await apiClient.get(`/extractions/${id}/logs?limit=${limit}`);
    return response.data.data || response.data;
  }

  /**
   * Perform bulk action on multiple extractions
   */
  async bulkAction(ids: string[], action: string): Promise<BulkActionResponse> {
    const response = await apiClient.post('/extractions/bulk', {
      ids,
      action,
    });
    return response.data;
  }

  /**
   * Start multiple extractions
   */
  async bulkStart(ids: string[]): Promise<BulkActionResponse> {
    return this.bulkAction(ids, 'start');
  }

  /**
   * Stop multiple extractions
   */
  async bulkStop(ids: string[]): Promise<BulkActionResponse> {
    return this.bulkAction(ids, 'stop');
  }

  /**
   * Delete multiple extractions
   */
  async bulkDelete(ids: string[]): Promise<BulkActionResponse> {
    return this.bulkAction(ids, 'delete');
  }

  /**
   * Export multiple extractions
   */
  async bulkExport(ids: string[]): Promise<BulkActionResponse> {
    return this.bulkAction(ids, 'export');
  }
}

export default new ExtractionService();
