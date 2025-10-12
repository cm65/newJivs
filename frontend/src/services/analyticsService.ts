import apiClient from './apiClient';

export interface DashboardAnalytics {
  totalExtractions: number;
  successfulExtractions: number;
  failedExtractions: number;
  extractionSuccessRate: number;
  totalMigrations: number;
  completedMigrations: number;
  migrationSuccessRate: number;
  totalDataExtracted: number;
  totalDataMigrated: number;
  dataQualityScore: number;
  complianceScore: number;
  activeUsers: number;
  systemHealthScore: number;
}

export interface ExtractionAnalytics {
  dailyExtractions: Record<string, number>;
  extractionsBySource: Record<string, number>;
  averageExtractionTime: number;
  totalVolume: number;
}

export interface DataQualityAnalytics {
  overallScore: number;
  dimensionScores: Record<string, number>;
  issuesBySeverity: Record<string, number>;
  qualityTrend: number[];
}

export interface ComplianceAnalytics {
  overallScore: number;
  pendingRequests: number;
  completedRequests: number;
  averageResponseTime: number;
  requestsByType: Record<string, number>;
  retentionCompliance: number;
  consentRate: number;
}

class AnalyticsService {
  /**
   * Get dashboard analytics
   */
  async getDashboardAnalytics(from?: Date, to?: Date): Promise<DashboardAnalytics> {
    const params = new URLSearchParams();
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/analytics/dashboard?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get extraction analytics
   */
  async getExtractionAnalytics(from?: Date, to?: Date): Promise<ExtractionAnalytics> {
    const params = new URLSearchParams();
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/analytics/extractions?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get migration analytics
   */
  async getMigrationAnalytics(from?: Date, to?: Date): Promise<any> {
    const params = new URLSearchParams();
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/analytics/migrations?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get data quality analytics
   */
  async getDataQualityAnalytics(): Promise<DataQualityAnalytics> {
    const response = await apiClient.get('/analytics/data-quality');
    return response.data.data || response.data;
  }

  /**
   * Get usage analytics
   */
  async getUsageAnalytics(from?: Date, to?: Date): Promise<any> {
    const params = new URLSearchParams();
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/analytics/usage?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get compliance analytics
   */
  async getComplianceAnalytics(): Promise<ComplianceAnalytics> {
    const response = await apiClient.get('/analytics/compliance');
    return response.data.data || response.data;
  }

  /**
   * Get performance analytics
   */
  async getPerformanceAnalytics(from?: Date, to?: Date): Promise<any> {
    const params = new URLSearchParams();
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/analytics/performance?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Export analytics report
   */
  async exportReport(format: 'csv' | 'excel' | 'pdf', reportType: string): Promise<Blob> {
    const response = await apiClient.post(
      '/analytics/export',
      { format, reportType },
      { responseType: 'blob' }
    );
    return response.data;
  }
}

export default new AnalyticsService();
