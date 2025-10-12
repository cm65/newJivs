import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Login Page
 * Represents /login route
 */
export class LoginPage {
  readonly page: Page;

  // Locators
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly passwordVisibilityToggle: Locator;
  readonly rememberMeCheckbox: Locator;
  readonly submitButton: Locator;
  readonly errorAlert: Locator;
  readonly forgotPasswordLink: Locator;
  readonly contactAdminLink: Locator;
  readonly pageTitle: Locator;

  constructor(page: Page) {
    this.page = page;

    // Initialize locators
    this.usernameInput = page.locator('[name="username"]');
    this.passwordInput = page.locator('[name="password"]');
    this.passwordVisibilityToggle = page.locator('[aria-label="toggle password visibility"]');
    this.rememberMeCheckbox = page.locator('[name="rememberMe"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.errorAlert = page.locator('[role="alert"]');
    this.forgotPasswordLink = page.locator('text=Forgot password?');
    this.contactAdminLink = page.locator('text=Contact Admin');
    this.pageTitle = page.locator('text=JiVS Platform Login');
  }

  /**
   * Navigate to login page
   */
  async goto(): Promise<void> {
    await this.page.goto('/login');
    await this.waitForLoad();
  }

  /**
   * Wait for page to load
   */
  async waitForLoad(): Promise<void> {
    await this.pageTitle.waitFor({ state: 'visible' });
    await this.usernameInput.waitFor({ state: 'visible' });
  }

  /**
   * Fill username field
   */
  async fillUsername(username: string): Promise<void> {
    await this.usernameInput.fill(username);
  }

  /**
   * Fill password field
   */
  async fillPassword(password: string): Promise<void> {
    await this.passwordInput.fill(password);
  }

  /**
   * Toggle password visibility
   */
  async togglePasswordVisibility(): Promise<void> {
    await this.passwordVisibilityToggle.click();
  }

  /**
   * Check/uncheck remember me
   */
  async setRememberMe(checked: boolean): Promise<void> {
    if (await this.rememberMeCheckbox.isChecked() !== checked) {
      await this.rememberMeCheckbox.click();
    }
  }

  /**
   * Click submit button
   */
  async clickSubmit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Perform login action
   */
  async login(username: string, password: string, rememberMe = false): Promise<void> {
    await this.fillUsername(username);
    await this.fillPassword(password);
    if (rememberMe) {
      await this.setRememberMe(true);
    }
    await this.clickSubmit();
  }

  /**
   * Get error message text
   */
  async getErrorMessage(): Promise<string> {
    await this.errorAlert.waitFor({ state: 'visible', timeout: 5000 });
    return await this.errorAlert.textContent() || '';
  }

  /**
   * Check if error is displayed
   */
  async hasError(): Promise<boolean> {
    return await this.errorAlert.isVisible();
  }

  /**
   * Check if login was successful (redirected to dashboard)
   */
  async isLoginSuccessful(): Promise<boolean> {
    try {
      await this.page.waitForURL('/dashboard', { timeout: 10000 });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Wait for redirect after successful login
   */
  async waitForRedirect(expectedUrl = '/dashboard'): Promise<void> {
    await this.page.waitForURL(expectedUrl, { timeout: 10000 });
  }

  /**
   * Check if submit button is disabled
   */
  async isSubmitDisabled(): Promise<boolean> {
    return await this.submitButton.isDisabled();
  }

  /**
   * Check if loading state is shown
   */
  async isLoading(): Promise<boolean> {
    const loadingIndicator = this.page.locator('[role="progressbar"]');
    return await loadingIndicator.isVisible();
  }

  /**
   * Get password input type
   */
  async getPasswordInputType(): Promise<string> {
    return await this.passwordInput.getAttribute('type') || 'password';
  }

  /**
   * Verify page elements are present
   */
  async verifyPageElements(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.usernameInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
    await expect(this.submitButton).toBeVisible();
    await expect(this.rememberMeCheckbox).toBeVisible();
    await expect(this.forgotPasswordLink).toBeVisible();
    await expect(this.contactAdminLink).toBeVisible();
  }
}
