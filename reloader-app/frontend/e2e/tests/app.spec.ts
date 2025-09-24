import { test, expect } from '@playwright/test';

test('app index shows welcome', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/Reloader/);
});
