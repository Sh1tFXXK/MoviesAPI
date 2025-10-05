# Makefile for local docker workflow (MySQL)
ENV_FILE := .env

.PHONY: docker-up docker-down test-e2e wait-health

docker-up:
	@echo "Building and starting containers..."
	docker compose up -d --build
	@$(MAKE) wait-health

docker-down:
	@echo "Stopping and removing containers and volumes..."
	docker compose down -v

wait-health:
	@echo -n "Waiting for app /healthz to become available"
	@attempts=0; \
	while ! curl -fsS http://127.0.0.1:8080/healthz >/dev/null 2>&1; do \
	  attempts=$$((attempts+1)); \
	  if [ $$attempts -ge 120 ]; then \
	    echo "\nTimed out waiting for /healthz"; \
	    exit 1; \
	  fi; \
	  printf "."; sleep 1; \
	done; \
	echo " ok"

test-e2e:
	@# ensure .env exists
	@if [ ! -f "$(ENV_FILE)" ]; then \
	  if [ -f .env.example ]; then cp .env.example .env; echo "Copied .env.example -> .env. Please edit .env to set AUTH_TOKEN and DB_URL."; exit 1; \
	  else echo ".env not found and no .env.example present"; exit 1; fi \
	fi
	$(MAKE) docker-up
	@echo "Running e2e-test.sh..."
	./e2e-test.sh