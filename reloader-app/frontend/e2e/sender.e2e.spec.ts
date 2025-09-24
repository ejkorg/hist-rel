import { test, expect } from '@playwright/test';

test('enqueue and run sender via API', async ({ request }) => {
  // Ensure sites are available
  const sites = await request.get('/api/sites');
  expect(sites.ok()).toBeTruthy();
  const body = await sites.json();
  expect(Array.isArray(body)).toBeTruthy();

  // Enqueue two payloads for sender 1
  const enqueueRes = await request.post('/api/senders/1/enqueue', {
    data: { senderId: 1, payloadIds: ['E2E1', 'E2E2'], source: 'playwright' }
  });
  expect(enqueueRes.ok()).toBeTruthy();

  // Trigger run
  const runRes = await request.post('/api/senders/1/run?limit=10');
  expect(runRes.status()).toBe(202);

  // Allow a short delay for backend processing (since run may be synchronous in-process, but keep small sleep)
  await new Promise((r) => setTimeout(r, 200));

  // Check queue is empty for sender 1 (status NEW)
  const q = await request.get('/api/senders/1/queue?status=NEW&limit=10');
  expect(q.ok()).toBeTruthy();
  const qbody = await q.json();
  expect(Array.isArray(qbody)).toBeTruthy();
  expect(qbody.length).toBe(0);
});
