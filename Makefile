.PHONY: all
all: install test build-image

TAG=$(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
COSIGN_PASSWORD := $(COSIGN_PASSWORD)
COMPOSE_PGCONFIG_OPTIONS ?= -f compose.yml -f catalog-pgconfig.yml
COMPOSE_DATADIR_OPTIONS ?= -f compose.yml -f catalog-datadir.yml
COMPOSE_ACCEPTANCE_PGCONFIG_OPTIONS ?= --project-name gscloud-acceptance-pgconfig $(COMPOSE_PGCONFIG_OPTIONS) -f acceptance.yml
COMPOSE_ACCEPTANCE_DATADIR_OPTIONS ?= --project-name gscloud-acceptance-datadir $(COMPOSE_DATADIR_OPTIONS) -f acceptance.yml
UID=$(shell id -u)
GID=$(shell id -g)

.PHONY: clean
clean:
	./mvnw clean

.PHONY: lint
lint:
	./mvnw fmt:check sortpom:verify -Dsort.verifyFailOn=strict -Dsort.verifyFail=stop -ntp

.PHONY: format
format:
	./mvnw sortpom:sort fmt:format -ntp

.PHONY: install
install:
	./mvnw clean install -DskipTests -ntp -T4 -U

.PHONY: test
test:
	./mvnw verify -ntp -T4

.PHONY: build-base-images
build-base-images:
	./mvnw clean package -f src/apps/base-images -DskipTests -T4 && \
	COMPOSE_DOCKER_CLI_BUILD=1 \
	DOCKER_BUILDKIT=1 \
	TAG=$(TAG) \
	docker compose -f docker-build/base-images.yml build 

.PHONY: build-image-infrastructure
build-image-infrastructure:
	./mvnw clean package -f src/apps/infrastructure -DskipTests -T4 && \
	COMPOSE_DOCKER_CLI_BUILD=1 \
	DOCKER_BUILDKIT=1 \
	TAG=$(TAG) \
	docker compose -f docker-build/infrastructure.yml build

.PHONY: build-image-geoserver
build-image-geoserver:
	./mvnw clean package -f src/apps/geoserver -DskipTests -T4 && \
	COMPOSE_DOCKER_CLI_BUILD=1 \
	DOCKER_BUILDKIT=1 \
	TAG=$(TAG) \
	docker compose -f docker-build/geoserver.yml build 

.PHONY: build-image
build-image: build-base-images build-image-infrastructure build-image-geoserver

.PHONY: push-image
push-image:
	TAG=$(TAG) \
	docker compose \
	-f docker-build/infrastructure.yml \
	-f docker-build/geoserver.yml \
	push

.PHONY: sign-image
sign-image:
	@bash -c '\
	images=$$(docker images --format "{{.Repository}}@{{.Digest}}" | grep "geoserver-cloud-"); \
	for image in $$images; do \
	  echo "Signing $$image"; \
	  output=$$(cosign sign --yes --key env://COSIGN_KEY --recursive $$image 2>&1); \
	  if [ $$? -ne 0 ]; then \
	    echo "Error occurred: $$output"; \
	    exit 1; \
	  else \
	    echo "Signing successful: $$output"; \
	  fi; \
	done'

.PHONY: verify-image
verify-image:
	@bash -c '\
	images=$$(docker images --format "{{.Repository}}@{{.Digest}}" | grep "geoserver-cloud-"); \
	for image in $$images; do \
	  echo "Verifying $$image"; \
	  output=$$(cosign verify --key env://COSIGN_PUB_KEY $$image 2>&1); \
	  if [ $$? -ne 0 ]; then \
	    echo "Error occurred: $$output"; \
	    exit 1; \
	  else \
	    echo "Verification successful: $$output"; \
	  fi; \
	done'

.PHONY: build-acceptance
build-acceptance:
	docker build --tag=acceptance:$(TAG) acceptance_tests

.PHONY: acceptance-tests-pgconfig
acceptance-tests-pgconfig:
acceptance-tests-pgconfig: build-acceptance
	(cd compose/ && TAG=$(TAG) GS_USER=$(UID):$(GID) docker compose $(COMPOSE_ACCEPTANCE_PGCONFIG_OPTIONS) up -d)
	(cd compose/ && TAG=$(TAG) GS_USER=$(UID):$(GID) docker compose $(COMPOSE_ACCEPTANCE_PGCONFIG_OPTIONS) exec -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && pytest . -vvv --color=yes')

.PHONY: clean-acceptance-tests-pgconfig
clean-acceptance-tests-pgconfig:
	(cd compose/ && TAG=$(TAG) GS_USER=$(UID):$(GID) docker compose $(COMPOSE_ACCEPTANCE_PGCONFIG_OPTIONS) down -v)

.PHONY: acceptance-tests-datadir
acceptance-tests-datadir:
acceptance-tests-datadir: build-acceptance
	(cd compose/ && TAG=$(TAG) GS_USER=$(UID):$(GID) docker compose $(COMPOSE_ACCEPTANCE_DATADIR_OPTIONS) up -d)
	(cd compose/ && TAG=$(TAG) GS_USER=$(UID):$(GID) docker compose $(COMPOSE_ACCEPTANCE_DATADIR_OPTIONS) exec -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && pytest . -vvv --color=yes')

.PHONY: clean-acceptance-tests-datadir
clean-acceptance-tests-datadir:
	(cd compose/ && TAG=$(TAG) GS_USER=$(UID):$(GID) docker compose $(COMPOSE_ACCEPTANCE_DATADIR_OPTIONS) down -v)
	rm -rf compose/catalog-datadir/*
	touch compose/catalog-datadir/.keep
