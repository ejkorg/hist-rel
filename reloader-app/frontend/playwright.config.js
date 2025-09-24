/** @type {import('@playwright/test').PlaywrightTestConfig} */
module.exports = {
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:8080',
    extraHTTPHeaders: {
      'Accept': 'application/json'
    }
  },
  testDir: 'e2e'
};
