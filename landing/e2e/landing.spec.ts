import { test, expect } from '@playwright/test';

test('landing page loads correctly', async ({ page }) => {
    await page.goto('/');

    // Verify the main heading
    await expect(page.getByRole('heading', { level: 1 })).toContainText('SCAN - Secret detection for Gradle builds');

    // Verify "Install" and "Docs" links in the main content area to avoid header duplicates
    await expect(page.getByRole('main').getByRole('link', { name: 'Install' }).first()).toBeVisible();
    await expect(page.getByRole('main').getByRole('link', { name: 'Docs' }).first()).toBeVisible();

    // Verify Feature Cards exist
    await expect(page.getByText('Pattern Recognition')).toBeVisible();
    await expect(page.getByText('Entropy Analysis')).toBeVisible();
    await expect(page.getByText('Context-Aware')).toBeVisible();

    // Verify Detection Examples section
    await expect(page.getByRole('heading', { name: 'Detection Examples' })).toBeVisible();
});
