import apiClient from './apiClient';

export interface DataSubjectRequest {
  id: string;
  requestType: 'ACCESS' | 'ERASURE' | 'RECTIFICATION' | 'PORTABILITY' | 'RESTRICTION' | 'OBJECTION';
  regulation: 'GDPR' | 'CCPA';
  dataSubjectId: string;
  dataSubjectEmail: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  requestDetails: string;
  requestedAt: string;
  dueDate: string;
  processedAt?: string;
  processedBy?: string;
  responseData?: any;
}

export interface ConsentRecord {
  id: string;
  dataSubjectId: string;
  dataSubjectEmail: string;
  consentType: string;
  purpose: string;
  granted: boolean;
  grantedAt?: string;
  revokedAt?: string;
  expiresAt?: string;
  legalBasis: string;
  version: string;
}

export interface RetentionPolicy {
  id: string;
  name: string;
  description: string;
  dataType: string;
  retentionPeriod: number;
  retentionUnit: 'DAYS' | 'MONTHS' | 'YEARS';
  action: 'DELETE' | 'ARCHIVE' | 'COLD_STORAGE' | 'ANONYMIZE' | 'SOFT_DELETE' | 'NOTIFY';
  enabled: boolean;
  lastExecutedAt?: string;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  userId: string;
  username: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string;
  ipAddress: string;
  userAgent: string;
  timestamp: string;
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
}

export interface ComplianceDashboard {
  totalRequests: number;
  pendingRequests: number;
  overdueRequests: number;
  completedRequests: number;
  totalConsents: number;
  activeConsents: number;
  revokedConsents: number;
  expiredConsents: number;
  totalPolicies: number;
  activePolicies: number;
  complianceScore: number;
  gdprCompliance: number;
  ccpaCompliance: number;
  requestsByType: Record<string, number>;
  requestsByStatus: Record<string, number>;
  recentRequests: DataSubjectRequest[];
  recentAudits: AuditLog[];
}

export interface DataSubjectRequestConfig {
  requestType: 'ACCESS' | 'ERASURE' | 'RECTIFICATION' | 'PORTABILITY' | 'RESTRICTION' | 'OBJECTION';
  regulation: 'GDPR' | 'CCPA';
  dataSubjectId: string;
  dataSubjectEmail: string;
  requestDetails: string;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
}

export interface ConsentConfig {
  dataSubjectId: string;
  dataSubjectEmail: string;
  consentType: string;
  purpose: string;
  legalBasis: string;
  expiresAt?: string;
}

export interface RetentionPolicyConfig {
  name: string;
  description: string;
  dataType: string;
  retentionPeriod: number;
  retentionUnit: 'DAYS' | 'MONTHS' | 'YEARS';
  action: 'DELETE' | 'ARCHIVE' | 'COLD_STORAGE' | 'ANONYMIZE' | 'SOFT_DELETE' | 'NOTIFY';
  enabled: boolean;
}

class ComplianceService {
  /**
   * Get compliance dashboard
   */
  async getDashboard(from?: Date, to?: Date): Promise<ComplianceDashboard> {
    const params = new URLSearchParams();
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/compliance/dashboard?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get all data subject requests
   */
  async getRequests(
    page = 0,
    size = 20,
    status?: string,
    type?: string,
    regulation?: string
  ): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (status) params.append('status', status);
    if (type) params.append('type', type);
    if (regulation) params.append('regulation', regulation);

    const response = await apiClient.get(`/compliance/requests?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get request by ID
   */
  async getRequest(id: string): Promise<DataSubjectRequest> {
    const response = await apiClient.get(`/compliance/requests/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Create new data subject request
   */
  async createRequest(config: DataSubjectRequestConfig): Promise<DataSubjectRequest> {
    const response = await apiClient.post('/compliance/requests', config);
    return response.data.data || response.data;
  }

  /**
   * Process data subject request
   */
  async processRequest(id: string): Promise<void> {
    await apiClient.post(`/compliance/requests/${id}/process`);
  }

  /**
   * Export data for access request
   */
  async exportRequestData(id: string, format = 'JSON'): Promise<Blob> {
    const response = await apiClient.get(`/compliance/requests/${id}/export`, {
      params: { format },
      responseType: 'blob',
    });
    return response.data;
  }

  /**
   * Update request status
   */
  async updateRequestStatus(id: string, status: string, notes?: string): Promise<DataSubjectRequest> {
    const response = await apiClient.put(`/compliance/requests/${id}/status`, { status, notes });
    return response.data.data || response.data;
  }

  /**
   * Get all consents
   */
  async getConsents(
    page = 0,
    size = 20,
    dataSubjectId?: string,
    granted?: boolean
  ): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (dataSubjectId) params.append('dataSubjectId', dataSubjectId);
    if (granted !== undefined) params.append('granted', granted.toString());

    const response = await apiClient.get(`/compliance/consents?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get consent by ID
   */
  async getConsent(id: string): Promise<ConsentRecord> {
    const response = await apiClient.get(`/compliance/consents/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Create new consent
   */
  async createConsent(config: ConsentConfig): Promise<ConsentRecord> {
    const response = await apiClient.post('/compliance/consents', config);
    return response.data.data || response.data;
  }

  /**
   * Revoke consent
   */
  async revokeConsent(id: string): Promise<void> {
    await apiClient.post(`/compliance/consents/${id}/revoke`);
  }

  /**
   * Get all retention policies
   */
  async getRetentionPolicies(page = 0, size = 20, enabled?: boolean): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (enabled !== undefined) params.append('enabled', enabled.toString());

    const response = await apiClient.get(`/compliance/retention-policies?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Get retention policy by ID
   */
  async getRetentionPolicy(id: string): Promise<RetentionPolicy> {
    const response = await apiClient.get(`/compliance/retention-policies/${id}`);
    return response.data.data || response.data;
  }

  /**
   * Create retention policy
   */
  async createRetentionPolicy(config: RetentionPolicyConfig): Promise<RetentionPolicy> {
    const response = await apiClient.post('/compliance/retention-policies', config);
    return response.data.data || response.data;
  }

  /**
   * Update retention policy
   */
  async updateRetentionPolicy(id: string, config: Partial<RetentionPolicyConfig>): Promise<RetentionPolicy> {
    const response = await apiClient.put(`/compliance/retention-policies/${id}`, config);
    return response.data.data || response.data;
  }

  /**
   * Delete retention policy
   */
  async deleteRetentionPolicy(id: string): Promise<void> {
    await apiClient.delete(`/compliance/retention-policies/${id}`);
  }

  /**
   * Execute retention policy
   */
  async executeRetentionPolicy(id: string): Promise<any> {
    const response = await apiClient.post(`/compliance/retention-policies/${id}/execute`);
    return response.data.data || response.data;
  }

  /**
   * Get audit logs
   */
  async getAuditLogs(
    page = 0,
    size = 20,
    userId?: string,
    action?: string,
    entityType?: string,
    from?: Date,
    to?: Date
  ): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (userId) params.append('userId', userId);
    if (action) params.append('action', action);
    if (entityType) params.append('entityType', entityType);
    if (from) params.append('from', from.toISOString().split('T')[0]);
    if (to) params.append('to', to.toISOString().split('T')[0]);

    const response = await apiClient.get(`/compliance/audit?${params.toString()}`);
    return response.data.data || response.data;
  }

  /**
   * Export audit logs
   */
  async exportAuditLogs(format = 'CSV', filters?: any): Promise<Blob> {
    const response = await apiClient.post('/compliance/audit/export', filters, {
      params: { format },
      responseType: 'blob',
    });
    return response.data;
  }

  /**
   * Discover personal data
   */
  async discoverPersonalData(dataSubjectId: string): Promise<any> {
    const response = await apiClient.post('/compliance/discover', { dataSubjectId });
    return response.data.data || response.data;
  }

  /**
   * Run compliance scan
   */
  async runComplianceScan(): Promise<any> {
    const response = await apiClient.post('/compliance/scan');
    return response.data.data || response.data;
  }
}

export default new ComplianceService();
