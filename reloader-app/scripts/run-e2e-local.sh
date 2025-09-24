#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

# Configurable env vars (defaults)
E2E_MODE="${E2E_MODE:-api}"   # api or ui
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-4200}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-60}"
SKIP_INSTALLS="${SKIP_INSTALLS:-false}"

PID_FILE="$BACKEND_DIR/backend.pid"
LOG_FILE="$BACKEND_DIR/backend.log"
FRONTEND_PID_FILE="$FRONTEND_DIR/frontend.pid"
FRONTEND_LOG="$FRONTEND_DIR/frontend.log"

function cleanup() {
  if [ -f "$PID_FILE" ]; then
    pid=$(cat "$PID_FILE")
    echo "Stopping backend pid=$pid"
    kill "$pid" || true
    rm -f "$PID_FILE"
  fi
}


trap cleanup EXIT

echo "Building backend..."
cd "$BACKEND_DIR"
mvn -DskipTests package

echo "Starting backend..."
nohup mvn -DskipTests spring-boot:run > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

echo "Waiting for backend to respond on /api/sites..."
for i in $(seq 1 $TIMEOUT_SECONDS); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$BACKEND_PORT/api/sites || echo 000)
  echo "try $i -> $code"
  if [ "$code" = "200" ] || [ "$code" = "204" ] || [ "$code" = "404" ]; then
    echo "backend ready"
    break
  fi
  sleep 1
done

if [ "$E2E_MODE" = "ui" ]; then
  echo "Starting frontend dev server (ng serve) on port $FRONTEND_PORT..."
  cd "$FRONTEND_DIR"
  if [ "$SKIP_INSTALLS" != "true" ]; then
    npm ci
  fi
  nohup npx ng serve --port $FRONTEND_PORT --host 0.0.0.0 > "$FRONTEND_LOG" 2>&1 &
  echo $! > "$FRONTEND_PID_FILE"
  echo "Waiting for frontend to be available..."
  for i in $(seq 1 $TIMEOUT_SECONDS); do
    code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$FRONTEND_PORT || echo 000)
    echo "try frontend $i -> $code"
    if [ "$code" = "200" ] || [ "$code" = "301" ] || [ "$code" = "302" ]; then
      echo "frontend ready"
      break
    fi
    sleep 1
  done
  # Run Playwright UI tests (pointing at frontend)
  if [ "$SKIP_INSTALLS" != "true" ]; then
    npx playwright install --with-deps
  fi
  npx playwright test e2e/ui --reporter=list
else
  echo "Running frontend e2e tests (Playwright API mode)..."
  cd "$FRONTEND_DIR"
  if [ "$SKIP_INSTALLS" != "true" ]; then
    npm ci
    npx playwright install --with-deps
  fi
  npx playwright test --reporter=list
fi

echo "All done. Backend logs (tail):"
tail -n 80 "$LOG_FILE" || true

echo "Script finished successfully. Cleaning up backend..."
cleanup
