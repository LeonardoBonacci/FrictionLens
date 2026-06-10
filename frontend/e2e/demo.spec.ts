import { test, expect } from '@playwright/test';

const reports = [
  {
    jobTitle: 'Backend Engineer',
    team: 'Platform',
    category: 'Tooling',
    severity: 'HIGH',
    blockerText:
      'Our CI pipeline takes over 45 minutes for a single build. Developers are context-switching constantly while waiting for feedback, which kills productivity.',
  },
  {
    jobTitle: 'Frontend Developer',
    team: 'Product',
    category: 'Process',
    severity: 'MEDIUM',
    blockerText:
      'Design handoffs arrive without responsive specs or interaction states. We spend hours guessing edge cases and then get change requests after implementation.',
  },
  {
    jobTitle: 'Engineering Manager',
    team: 'Infrastructure',
    category: 'Communication',
    severity: 'CRITICAL',
    blockerText:
      'Cross-team dependency requests sit in Slack threads for days with no owner. There is no formal intake process so work gets lost between teams.',
  },
];

test('FrictionLens demo — submit reports, browse, and AI search', async ({
  page,
}) => {
  // ────────────────────────────────────────────────────────
  // 1. Navigate to the app
  // ────────────────────────────────────────────────────────
  await page.goto('/');
  await expect(page.locator('h1')).toContainText('FrictionLens');

  // ────────────────────────────────────────────────────────
  // 2. Submit three friction reports
  // ────────────────────────────────────────────────────────
  for (const report of reports) {
    // Make sure we're on the Report tab
    await page.getByRole('button', { name: 'Report', exact: true }).click();
    await expect(page.locator('h2')).toContainText('Report a Friction');

    // Fill in the form field by field with a slight pause for visibility
    await page.getByPlaceholder('e.g. Software Engineer').fill(report.jobTitle);
    await page.getByPlaceholder('e.g. Platform').fill(report.team);
    await page.getByPlaceholder('e.g. Tooling, Process').fill(report.category);
    await page.locator('select[name="severity"]').selectOption(report.severity);
    await page
      .getByPlaceholder('Describe the friction')
      .fill(report.blockerText);

    // Submit
    await page.getByRole('button', { name: 'Submit Report' }).click();

    // Wait for success confirmation
    await expect(page.getByText('Report submitted successfully')).toBeVisible({
      timeout: 30_000,
    });
  }

  // ────────────────────────────────────────────────────────
  // 3. Browse reports
  // ────────────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Browse' }).click();
  await expect(page.locator('h2')).toContainText('Friction Reports');

  // Wait for at least one report card to appear
  await expect(page.locator('.report-card').first()).toBeVisible({
    timeout: 15_000,
  });

  // Filter by team "Platform"
  await page.getByPlaceholder('Filter by team').fill('Platform');
  await expect(page.locator('.report-card').first()).toBeVisible({
    timeout: 15_000,
  });
  // Brief pause so the viewer sees filtered results
  await page.waitForTimeout(1500);

  // Clear filter
  await page.getByPlaceholder('Filter by team').fill('');
  await page.waitForTimeout(1000);

  // Filter by severity
  await page.locator('select[name="severity"]').selectOption('CRITICAL');
  await expect(page.locator('.report-card').first()).toBeVisible({
    timeout: 15_000,
  });
  await page.waitForTimeout(1500);

  // Reset severity filter
  await page.locator('select[name="severity"]').selectOption('');
  await page.waitForTimeout(1000);

  // ────────────────────────────────────────────────────────
  // 4. AI Query — ask a natural-language question
  // ────────────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Ask AI' }).click();
  await expect(page.locator('h2')).toContainText('Ask a Question');

  const aiQuestion = 'What are the biggest friction points slowing teams down?';
  await page
    .getByPlaceholder(
      'e.g. What are the biggest tooling problems across teams?',
    )
    .fill(aiQuestion);
  await page.getByRole('button', { name: 'Search' }).click();

  // Wait for the AI summary to appear (this can take a while with the LLM)
  await expect(page.locator('.summary-card')).toBeVisible({ timeout: 90_000 });

  // Ensure supporting reports are shown
  await expect(page.locator('.supporting-reports .report-card').first()).toBeVisible({
    timeout: 10_000,
  });

  // Pause so the viewer can read the AI response
  await page.waitForTimeout(3000);
});
