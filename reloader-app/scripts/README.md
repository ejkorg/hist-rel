# run-e2e-local.sh

This folder contains a helper script to run the Playwright end-to-end tests locally against the Spring Boot backend.

Usage
-----

Basic API-mode run (default):

```bash
cd reloader-app
./scripts/run-e2e-local.sh
```

This will:
- build the backend (Maven package)
- start the backend (spring-boot:run)
- wait for `/api/sites` to be available
- run the Playwright e2e tests under `reloader-app/frontend/e2e/` (API-style tests)
- tail the backend logs and shut down the backend

UI-mode run (serves frontend and runs UI Playwright tests):

```bash
E2E_MODE=ui ./scripts/run-e2e-local.sh
```

Configurable environment variables
----------------------------------
- `E2E_MODE` - `api` (default) or `ui` (serves the frontend and runs UI tests)
- `BACKEND_PORT` - backend HTTP port (default 8080)
- `FRONTEND_PORT` - port to serve frontend for UI mode (default 4200)
- `TIMEOUT_SECONDS` - how long to wait for services to come up (default 60)
- `SKIP_INSTALLS` - set to `true` to skip `npm ci` / `playwright install` steps (default `false`)

CI notes
--------
The repository includes `.github/workflows/e2e.yml` which builds the backend and runs Playwright e2e tests on pushes and PRs to `main`. The CI workflow uses an API-style Playwright run (fast and reliable). If you enable UI tests in CI you may want to run Playwright in headless mode and increase timeouts.

Troubleshooting
---------------
- If Playwright fails to launch browsers in your environment, run `npx playwright install --with-deps` manually to fetch browsers and OS deps.
- If ports are in use, adjust `BACKEND_PORT` and `FRONTEND_PORT` before running.
