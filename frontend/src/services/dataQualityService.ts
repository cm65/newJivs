import apiClient from './apiClient';

export interface DataQualityRule {
  id: string;
  name: string;
  description: string;
  ruleType: string;
  dimension: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  enabled: boolean;
  parameters: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

export interface DataQualityIssue {
  id: string;
  ruleId: string;
  ruleName: string;
  dimension: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'IGNORED';
  description: string;
  recordId?: string;
  fieldName?: string;
  detectedAt: string;
  resolvedAt?: string;
}

export interface DataQualityProfile {
  id: string;
  datasetName: string;
  totalRecords: number;
  completenessScore: number;
  accuracyScore: number;
  consistencyScore: number;
  validityScore: number;
  uniquenessScore: number;
  timelinessScore: number;
  overallScore: number;
  profiledAt: string;
}

export interface DataQualityDashboard {
  overallScore: number;
  completenessScore: number;
  accuracyScore: number;
  consistencyScore: number;
  validityScore: number;
  uniquenessScore: number;
  timelinessScore: number;
  totalRules: number;
  activeRules: number;
  totalIssues: number;
  openIssues: number;
  criticalIssues: number;
  recentProfiles: DataQualityProfile[];
  issuesByDimension: Record<string, number>;
  issuesBySeverity: Record<string, number>;
  trendData: any[];
}

export interface RuleConfig {
  name: string;
  description: string;
  ruleType: 'NULL_CHECK' | 'FORMAT_VALIDATION' | 'RANGE_VALIDATION' | 'UNIQUENESS' | 'REFERENTIAL_INTEGRITY' | 'BUSINESS_RULE';
  dimension: 'COMPLETENESS' | 'ACCURACY' | 'CONSISTENCY' | 'VALIDITY' | 'UNIQUENESS' | 'TIMELINESS';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  enabled: boolean;
  parameters: Record<string, any>;
}

export interface ProfileRequest {
  datasetName: string;
  dataSource: string;
  tableName?: string;
  query?: string;
}

class DataQualityService {
  /**
   * Get data quality dashboard
   */
  async getDashboard(from?: Date, to?: Date): Promise<DataQualityDashboard> {
    const params = new URLSearchParams();
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/data-quality/dashboard?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get all data quality rules
   */
  async getRules(page = 0, size = 20, dimension?: string, severity?: string): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (dimension) params.append('dimension', dimension);
    if (severity) params.append('severity', severity);

    const response = await apiClient.get(`/data-quality/rules?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get rule by ID
   */
  async getRule(id: string): Promise<DataQualityRule> {
    const response = await apiClient.get(`/data-quality/rules/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Create new rule
   */
  async createRule(config: RuleConfig): Promise<DataQualityRule> {
    const response = await apiClient.post('/data-quality/rules', config);
    return response.data.data || response.data;
  }

  /**
   * Update rule
   */
  async updateRule(id: string, config: Partial<RuleConfig>): Promise<DataQualityRule> {
    const response = await apiClient.put(`/data-quality/rules/${id}`, config);
    return response.data.data || response.data;
  }

  /**
   * Delete rule
   */
  async deleteRule(id: string): Promise<void> {
    await apiClient.delete(`/data-quality/rules/${id}`);
  }

  /**
   * Execute rule
   */
  async executeRule(id: string, data: Record<string, any>): Promise<any> {
    const response = await apiClient.post(`/data-quality/rules/${id}/execute`, data);
    return response.data.data || response.data;
  }

  /**
   * Get all issues
   */
  async getIssues(
    page = 0,
    size = 20,
    status?: string,
    severity?: string,
    dimension?: string
  ): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (status) params.append('status', status);
    if (severity) params.append('severity', severity);
    if (dimension) params.append('dimension', dimension);

    const response = await apiClient.get(`/data-quality/issues?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get issue by ID
   */
  async getIssue(id: string): Promise<DataQualityIssue> {
    const response = await apiClient.get(`/data-quality/issues/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Update issue status
   */
  async updateIssueStatus(id: string, status: string): Promise<DataQualityIssue> {
    const response = await apiClient.put(`/data-quality/issues/${id}/status`, { status });
    return response.data.data || response.data;
  }

  /**
   * Resolve issue
   */
  async resolveIssue(id: string, resolution: string): Promise<DataQualityIssue> {
    const response = await apiClient.post(`/data-quality/issues/${id}/resolve`, { resolution });
    return response.data.data || response.data;
  }

  /**
   * Profile dataset
   */
  async profileDataset(request: ProfileRequest): Promise<DataQualityProfile> {
    const response = await apiClient.post('/data-quality/profile', request);
    return response.data.data || response.data;
  }

  /**
   * Get all profiles
   */
  async getProfiles(page = 0, size = 20): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await apiClient.get(`/data-quality/profiles?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get profile by ID
   */
  async getProfile(id: string): Promise<DataQualityProfile> {
    const response = await apiClient.get(`/data-quality/profiles/${id}`);
    return response.data.data || response.data;
  }
}

export default new DataQualityService();
