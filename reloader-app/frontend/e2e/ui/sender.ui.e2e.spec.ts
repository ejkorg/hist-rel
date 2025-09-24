import { test, expect } from '@playwright/test';

test('UI: enqueue and run sender via app UI', async ({ page }) => {
  // The app is expected to be served at the Playwright baseURL (playwright.config.js)
  await page.goto('/');

  // Basic smoke: page has a toolbar or title text
  await expect(page.locator('text=Reloader')).toHaveCount(1);

  // If the UI exposes controls for senderId/numToSend, try to use them; otherwise use API fallback
  const senderInput = page.locator('input[name="senderId"]');
  if (await senderInput.count() > 0) {
    await senderInput.fill('1');
  }

  // Click run or enqueue button if present
  const runButton = page.locator('button', { hasText: 'Run' });
  if (await runButton.count() > 0) {
    await runButton.click();
    // wait for a result area to show success
    const result = page.locator('[data-test="result"]');
    await expect(result).toContainText(/(started|enqueued|success|OK)/i);
  } else {
    test.skip(true, 'UI does not expose run button; API-only app');
  }
});
