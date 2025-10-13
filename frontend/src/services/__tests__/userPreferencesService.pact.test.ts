/**
 * User Preferences API Contract Tests
 *
 * These tests define the contract between the frontend and backend
 * for user preferences and settings management.
 *
 * Coverage: 4 endpoints (LOW priority)
 */

import { Pact } from '@pact-foundation/pact';
import { MatchersV3 } from '@pact-foundation/pact';
import path from 'path';
import { userPreferencesService } from '../userPreferencesService';

const {
  like,
  eachLike,
  boolean,
  string,
  regex,
  iso8601DateTime
} = MatchersV3;

// Create pact provider
const provider = new Pact({
  consumer: 'JiVS Frontend',
  provider: 'JiVS Backend',
  port: 9096,
  dir: path.resolve(process.cwd(), 'pacts'),
  logLevel: 'info',
});

describe('User Preferences API Contract Tests', () => {
  beforeAll(() => provider.setup());
  afterAll(() => provider.finalize());
  afterEach(() => provider.verify());

  describe('Get User Preferences', () => {
    it('should fetch current user preferences', async () => {
      const expectedResponse = like({
        userId: string('USER-001'),
        theme: string('light'),
        language: string('en-US'),
        timezone: string('America/New_York'),
        notifications: like({
          email: boolean(true),
          sms: boolean(false),
          push: boolean(true),
          inApp: boolean(true)
        }),
        displaySettings: like({
          dateFormat: string('MM/DD/YYYY'),
          timeFormat: string('12h'),
          numberFormat: string('1,234.56'),
          pageSize: string('20')
        }),
        dashboardConfig: like({
          defaultView: string('grid'),
          refreshInterval: string('60'),
          widgetLayout: eachLike({
            widgetId: string('extraction-stats'),
            position: like({
              x: string('0'),
              y: string('0'),
              w: string('4'),
              h: string('2')
            }),
            visible: boolean(true)
          }, { min: 4 })
        }),
        dataTablePreferences: like({
          compactMode: boolean(false),
          showFilters: boolean(true),
          stickyHeader: boolean(true),
          highlightRows: boolean(true)
        }),
        exportPreferences: like({
          defaultFormat: string('xlsx'),
          includeHeaders: boolean(true),
          includeMetadata: boolean(false),
          compression: boolean(true)
        }),
        createdAt: iso8601DateTime(),
        updatedAt: iso8601DateTime()
      });

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and has preferences' }],
        uponReceiving: 'a request to get user preferences',
        withRequest: {
          method: 'GET',
          path: '/api/v1/preferences',
          headers: {
            'Authorization': regex(/^Bearer .+/, 'Bearer valid-token')
          }
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      const result = await userPreferencesService.getPreferences();
      expect(result).toBeDefined();
      expect(result.userId).toBeDefined();
    });
  });

  describe('Update User Preferences', () => {
    it('should update user preferences', async () => {
      const updateRequest = {
        theme: 'dark',
        language: 'en-US',
        timezone: 'America/Los_Angeles',
        notifications: {
          email: true,
          sms: true,
          push: false,
          inApp: true
        },
        displaySettings: {
          dateFormat: 'DD/MM/YYYY',
          timeFormat: '24h',
          numberFormat: '1.234,56',
          pageSize: '50'
        }
      };

      const expectedResponse = like({
        ...updateRequest,
        userId: string('USER-001'),
        updatedAt: iso8601DateTime(),
        message: string('Preferences updated successfully')
      });

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and can update preferences' }],
        uponReceiving: 'a request to update user preferences',
        withRequest: {
          method: 'PUT',
          path: '/api/v1/preferences',
          headers: {
            'Authorization': regex(/^Bearer .+/, 'Bearer valid-token'),
            'Content-Type': 'application/json'
          },
          body: updateRequest
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      const result = await userPreferencesService.updatePreferences(updateRequest);
      expect(result).toBeDefined();
      expect(result.message).toContain('successfully');
    });
  });

  describe('Reset User Preferences', () => {
    it('should reset preferences to defaults', async () => {
      const expectedResponse = like({
        userId: string('USER-001'),
        theme: string('light'),
        language: string('en-US'),
        timezone: string('UTC'),
        notifications: like({
          email: boolean(true),
          sms: boolean(false),
          push: boolean(false),
          inApp: boolean(true)
        }),
        displaySettings: like({
          dateFormat: string('MM/DD/YYYY'),
          timeFormat: string('12h'),
          numberFormat: string('1,234.56'),
          pageSize: string('20')
        }),
        message: string('Preferences reset to defaults'),
        resetAt: iso8601DateTime()
      });

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and can reset preferences' }],
        uponReceiving: 'a request to reset preferences to defaults',
        withRequest: {
          method: 'POST',
          path: '/api/v1/preferences/reset',
          headers: {
            'Authorization': regex(/^Bearer .+/, 'Bearer valid-token')
          }
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      const result = await userPreferencesService.resetPreferences();
      expect(result).toBeDefined();
      expect(result.message).toContain('reset');
    });
  });

  describe('Export User Preferences', () => {
    it('should export user preferences as JSON', async () => {
      const expectedResponse = {
        export: like({
          userId: string('USER-001'),
          exportDate: iso8601DateTime(),
          version: string('1.0'),
          preferences: like({
            theme: string('dark'),
            language: string('en-US'),
            timezone: string('America/New_York'),
            notifications: like({
              email: boolean(true),
              sms: boolean(false),
              push: boolean(true),
              inApp: boolean(true)
            }),
            displaySettings: like({
              dateFormat: string('MM/DD/YYYY'),
              timeFormat: string('12h'),
              numberFormat: string('1,234.56'),
              pageSize: string('20')
            }),
            dashboardConfig: like({
              defaultView: string('grid'),
              refreshInterval: string('60')
            }),
            dataTablePreferences: like({
              compactMode: boolean(false),
              showFilters: boolean(true)
            }),
            exportPreferences: like({
              defaultFormat: string('xlsx'),
              includeHeaders: boolean(true)
            })
          })
        })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and can export preferences' }],
        uponReceiving: 'a request to export preferences',
        withRequest: {
          method: 'GET',
          path: '/api/v1/preferences/export',
          headers: {
            'Authorization': regex(/^Bearer .+/, 'Bearer valid-token')
          }
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
            'Content-Disposition': regex(/attachment; filename=.+\.json/, 'attachment; filename=preferences.json')
          },
          body: expectedResponse
        }
      });

      const result = await userPreferencesService.exportPreferences();
      expect(result).toBeDefined();
      expect(result.export).toBeDefined();
    });
  });
});

/**
 * WHY USER PREFERENCES CONTRACT TESTS MATTER:
 *
 * 1. User experience consistency across sessions
 * 2. Personalization settings must persist correctly
 * 3. Theme and language changes affect entire UI
 * 4. Notification preferences impact communication
 * 5. Export/import enables preference portability
 *
 * These tests ensure:
 * - Preference structure consistency
 * - Default values are properly set
 * - Updates persist correctly
 * - Reset functionality works
 * - Export format compatibility
 *
 * Impact on user experience:
 * - Consistent UI across devices
 * - Preserved user customizations
 * - Reliable notification delivery
 * - Smooth preference migration
 */