import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('páginas de entrada e estados sem acesso têm labels, contraste e layout adaptável', async ({ page }, info) => {
  for (const path of ['/entrar', '/cadastro', '/recuperar-senha', '/acompanhar', '/avaliar']) {
    await page.goto(path);
    await expect(page.locator('h1')).toBeVisible();
    if (await page.locator('.form-wrap').count()) {
      await page.locator('.form-wrap').evaluate(async element => {
        await Promise.all(element.getAnimations().map(animation => animation.finished));
      });
    }
    if (path === '/avaliar') await expect(page.getByRole('button', { name: 'Tentar novamente' })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), path).toBeTruthy();
    const small = await page.locator('button:not(:disabled), input:not([type=hidden]), select').evaluateAll(elements => elements
      .filter(element => element.getClientRects().length)
      .map(element => ({ name: element.getAttribute('id') || element.textContent, rect: element.getBoundingClientRect() }))
      .filter(element => element.rect.width < 44 || element.rect.height < 44)
      .map(element => element.name));
    expect.soft(small, `${path}: alvos 44px`).toEqual([]);
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']).analyze();
    expect.soft(result.violations.map(item => ({ id: item.id, targets: item.nodes.map(node => node.target) })), path).toEqual([]);
    await page.screenshot({ path: info.outputPath(`${path.slice(1)}.png`), fullPage: true });
  }
});
