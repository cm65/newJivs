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
