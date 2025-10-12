import { Page, APIResponse } from '@playwright/test';

/**
 * API helper utilities for testing backend endpoints
 */

const API_BASE_URL = 'http://localhost:8080/api/v1';

/**
 * Get authorization header with token from page
 */
async function getAuthHeader(page: Page): Promise<Record<string, string>> {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

/**
 * Make authenticated GET request
 */
export async function apiGet(page: Page, endpoint: string): Promise<APIResponse> {
  const headers = await getAuthHeader(page);
  return await page.request.get(`${API_BASE_URL}${endpoint}`, { headers });
}

/**
 * Make authenticated POST request
 */
export async function apiPost(
  page: Page,
  endpoint: string,
  data?: any
): Promise<APIResponse> {
  const headers = await getAuthHeader(page);
  return await page.request.post(`${API_BASE_URL}${endpoint}`, {
    headers,
    data,
  });
}

/**
 * Make authenticated PUT request
 */
export async function apiPut(
  page: Page,
  endpoint: string,
  data?: any
): Promise<APIResponse> {
  const headers = await getAuthHeader(page);
  return await page.request.put(`${API_BASE_URL}${endpoint}`, {
    headers,
    data,
  });
}

/**
 * Make authenticated DELETE request
 */
export async function apiDelete(page: Page, endpoint: string): Promise<APIResponse> {
  const headers = await getAuthHeader(page);
  return await page.request.delete(`${API_BASE_URL}${endpoint}`, { headers });
}

/**
 * Create extraction via API
 */
export async function createExtraction(page: Page, config: any): Promise<string> {
  const response = await apiPost(page, '/extractions', config);
  if (!response.ok()) {
    throw new Error(`Failed to create extraction: ${response.status()}`);
  }
  const data = await response.json();
  return data.data.id;
}

/**
 * Delete extraction via API
 */
export async function deleteExtraction(page: Page, id: string): Promise<void> {
  const response = await apiDelete(page, `/extractions/${id}`);
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Failed to delete extraction: ${response.status()}`);
  }
}

/**
 * Create migration via API
 */
export async function createMigration(page: Page, config: any): Promise<string> {
  const response = await apiPost(page, '/migrations', config);
  if (!response.ok()) {
    throw new Error(`Failed to create migration: ${response.status()}`);
  }
  const data = await response.json();
  return data.data.id;
}

/**
 * Delete migration via API
 */
export async function deleteMigration(page: Page, id: string): Promise<void> {
  const response = await apiDelete(page, `/migrations/${id}`);
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Failed to delete migration: ${response.status()}`);
  }
}

/**
 * Get extractions list
 */
export async function getExtractions(
  page: Page,
  params?: { page?: number; size?: number; status?: string }
): Promise<any> {
  const queryParams = new URLSearchParams();
  if (params?.page !== undefined) queryParams.set('page', params.page.toString());
  if (params?.size !== undefined) queryParams.set('size', params.size.toString());
  if (params?.status) queryParams.set('status', params.status);

  const endpoint = `/extractions${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
  const response = await apiGet(page, endpoint);

  if (!response.ok()) {
    throw new Error(`Failed to get extractions: ${response.status()}`);
  }

  return await response.json();
}

/**
 * Get migrations list
 */
export async function getMigrations(
  page: Page,
  params?: { page?: number; size?: number; status?: string }
): Promise<any> {
  const queryParams = new URLSearchParams();
  if (params?.page !== undefined) queryParams.set('page', params.page.toString());
  if (params?.size !== undefined) queryParams.set('size', params.size.toString());
  if (params?.status) queryParams.set('status', params.status);

  const endpoint = `/migrations${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
  const response = await apiGet(page, endpoint);

  if (!response.ok()) {
    throw new Error(`Failed to get migrations: ${response.status()}`);
  }

  return await response.json();
}

/**
 * Wait for extraction status
 */
export async function waitForExtractionStatus(
  page: Page,
  id: string,
  status: string,
  timeout = 30000
): Promise<void> {
  const startTime = Date.now();
  while (Date.now() - startTime < timeout) {
    const response = await apiGet(page, `/extractions/${id}`);
    if (response.ok()) {
      const data = await response.json();
      if (data.data.status === status) {
        return;
      }
    }
    await page.waitForTimeout(1000);
  }
  throw new Error(`Extraction ${id} did not reach status ${status} within ${timeout}ms`);
}

/**
 * Wait for migration status
 */
export async function waitForMigrationStatus(
  page: Page,
  id: string,
  status: string,
  timeout = 30000
): Promise<void> {
  const startTime = Date.now();
  while (Date.now() - startTime < timeout) {
    const response = await apiGet(page, `/migrations/${id}`);
    if (response.ok()) {
      const data = await response.json();
      if (data.data.status === status) {
        return;
      }
    }
    await page.waitForTimeout(1000);
  }
  throw new Error(`Migration ${id} did not reach status ${status} within ${timeout}ms`);
}

/**
 * Create data quality rule via API
 */
export async function createQualityRule(page: Page, config: any): Promise<string> {
  const response = await apiPost(page, '/data-quality/rules', config);
  if (!response.ok()) {
    throw new Error(`Failed to create quality rule: ${response.status()}`);
  }
  const data = await response.json();
  return data.data.id;
}

/**
 * Delete data quality rule via API
 */
export async function deleteQualityRule(page: Page, id: string): Promise<void> {
  const response = await apiDelete(page, `/data-quality/rules/${id}`);
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Failed to delete quality rule: ${response.status()}`);
  }
}

/**
 * Get quality rules list
 */
export async function getQualityRules(
  page: Page,
  params?: { page?: number; size?: number; dimension?: string; severity?: string }
): Promise<any> {
  const queryParams = new URLSearchParams();
  if (params?.page !== undefined) queryParams.set('page', params.page.toString());
  if (params?.size !== undefined) queryParams.set('size', params.size.toString());
  if (params?.dimension) queryParams.set('dimension', params.dimension);
  if (params?.severity) queryParams.set('severity', params.severity);

  const endpoint = `/data-quality/rules${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
  const response = await apiGet(page, endpoint);

  if (!response.ok()) {
    throw new Error(`Failed to get quality rules: ${response.status()}`);
  }

  return await response.json();
}

/**
 * Execute quality rule via API
 */
export async function executeQualityRule(page: Page, id: string, data: any = {}): Promise<any> {
  const response = await apiPost(page, `/data-quality/rules/${id}/execute`, data);
  if (!response.ok()) {
    throw new Error(`Failed to execute quality rule: ${response.status()}`);
  }
  return await response.json();
}

/**
 * Get quality issues list
 */
export async function getQualityIssues(
  page: Page,
  params?: { page?: number; size?: number; status?: string; severity?: string }
): Promise<any> {
  const queryParams = new URLSearchParams();
  if (params?.page !== undefined) queryParams.set('page', params.page.toString());
  if (params?.size !== undefined) queryParams.set('size', params.size.toString());
  if (params?.status) queryParams.set('status', params.status);
  if (params?.severity) queryParams.set('severity', params.severity);

  const endpoint = `/data-quality/issues${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
  const response = await apiGet(page, endpoint);

  if (!response.ok()) {
    throw new Error(`Failed to get quality issues: ${response.status()}`);
  }

  return await response.json();
}

/**
 * Resolve quality issue via API
 */
export async function resolveQualityIssue(page: Page, id: string, resolution: string): Promise<void> {
  const response = await apiPost(page, `/data-quality/issues/${id}/resolve`, { resolution });
  if (!response.ok()) {
    throw new Error(`Failed to resolve quality issue: ${response.status()}`);
  }
}

/**
 * Profile dataset via API
 */
export async function profileDataset(page: Page, request: any): Promise<string> {
  const response = await apiPost(page, '/data-quality/profile', request);
  if (!response.ok()) {
    throw new Error(`Failed to profile dataset: ${response.status()}`);
  }
  const data = await response.json();
  return data.data.id;
}

/**
 * Get quality profiles list
 */
export async function getQualityProfiles(
  page: Page,
  params?: { page?: number; size?: number }
): Promise<any> {
  const queryParams = new URLSearchParams();
  if (params?.page !== undefined) queryParams.set('page', params.page.toString());
  if (params?.size !== undefined) queryParams.set('size', params.size.toString());

  const endpoint = `/data-quality/profiles${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
  const response = await apiGet(page, endpoint);

  if (!response.ok()) {
    throw new Error(`Failed to get quality profiles: ${response.status()}`);
  }

  return await response.json();
}

/**
 * Create data subject request via API
 */
export async function createDataSubjectRequest(page: Page, config: any): Promise<string> {
  const response = await apiPost(page, '/compliance/requests', config);
  if (!response.ok()) {
    throw new Error(`Failed to create data subject request: ${response.status()}`);
  }
  const data = await response.json();
  return data.data.id;
}

/**
 * Process data subject request via API
 */
export async function processDataSubjectRequest(page: Page, id: string): Promise<void> {
  const response = await apiPost(page, `/compliance/requests/${id}/process`);
  if (!response.ok()) {
    throw new Error(`Failed to process request: ${response.status()}`);
  }
}

/**
 * Create retention policy via API
 */
export async function createRetentionPolicy(page: Page, config: any): Promise<string> {
  const response = await apiPost(page, '/compliance/retention-policies', config);
  if (!response.ok()) {
    throw new Error(`Failed to create retention policy: ${response.status()}`);
  }
  const data = await response.json();
  return data.data.id;
}

/**
 * Delete retention policy via API
 */
export async function deleteRetentionPolicy(page: Page, id: string): Promise<void> {
  const response = await apiDelete(page, `/compliance/retention-policies/${id}`);
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Failed to delete retention policy: ${response.status()}`);
  }
}
