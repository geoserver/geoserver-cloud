.PHONY: all
all: install test build-image

TAG=$(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

COSIGN_PASSWORD := $(COSIGN_PASSWORD)


REPACKAGE ?= true

.PHONY: clean
clean:
	./mvnw clean

.PHONY: lint
lint: lint-pom lint-java

.PHONY: lint-pom
lint-pom:
	./mvnw sortpom:verify -Dsort.verifyFailOn=strict -Dsort.verifyFail=stop -ntp -T1C

.PHONY: lint-java
lint-java:
	./mvnw spotless:check -ntp -T1C

.PHONY: format
format: format-pom format-java

.PHONY: format-pom
format-pom:
	./mvnw sortpom:sort -ntp -T1C

.PHONY: format-java
format-java:
	./mvnw spotless:apply -ntp -T1C

.PHONY: install
install:
	./mvnw clean install -DskipTests -ntp -U -T1C

.PHONY: package
package:
	./mvnw clean package -Dfmt.skip -DskipTests -ntp -U -T1C

.PHONY: test
test:
	./mvnw verify -Dfmt.skip -ntp -T1C

.PHONY: build-image
build-image: build-base-images build-image-infrastructure build-image-geoserver

.PHONY: build-base-images
build-base-images: package-base-images
	COMPOSE_DOCKER_CLI_BUILD=0 DOCKER_BUILDKIT=0 TAG=$(TAG) \
	docker compose -f docker-build/base-images.yml build

.PHONY: build-image-infrastructure
build-image-infrastructure: package-infrastructure-images
	COMPOSE_DOCKER_CLI_BUILD=0 DOCKER_BUILDKIT=0 TAG=$(TAG) \
	docker compose -f docker-build/infrastructure.yml build

.PHONY: build-image-geoserver
build-image-geoserver: package-geoserver-images
	COMPOSE_DOCKER_CLI_BUILD=0 DOCKER_BUILDKIT=0  TAG=$(TAG) \
	docker compose -f docker-build/geoserver.yml build

.PHONY: build-image-multiplatform
build-image-multiplatform: build-base-images-multiplatform build-image-infrastructure-multiplatform build-image-geoserver-multiplatform

.PHONY: build-base-images-multiplatform
build-base-images-multiplatform: package-base-images
	COMPOSE_DOCKER_CLI_BUILD=1 DOCKER_BUILDKIT=1 TAG=$(TAG) \
	docker compose -f docker-build/base-images-multiplatform.yml build jre --push \
	&& COMPOSE_DOCKER_CLI_BUILD=1 DOCKER_BUILDKIT=1 TAG=$(TAG) \
	   docker compose -f docker-build/base-images-multiplatform.yml build spring-boot --push \
	&& COMPOSE_DOCKER_CLI_BUILD=1 DOCKER_BUILDKIT=1 TAG=$(TAG) \
	   docker compose -f docker-build/base-images-multiplatform.yml build geoserver-common --push

.PHONY: build-image-infrastructure-multiplatform
build-image-infrastructure-multiplatform: package-infrastructure-images
	COMPOSE_DOCKER_CLI_BUILD=1 \
	DOCKER_BUILDKIT=1 \
	TAG=$(TAG) \
	docker compose -f docker-build/infrastructure-multiplatform.yml build --push

.PHONY: build-image-geoserver-multiplatform
build-image-geoserver-multiplatform: package-geoserver-images
	COMPOSE_DOCKER_CLI_BUILD=1 DOCKER_BUILDKIT=1 TAG=$(TAG) \
	docker compose -f docker-build/geoserver-multiplatform.yml build --push

.PHONY: package-base-images
package-base-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -f src/apps/base-images -DskipTests -T4
else
	@echo "Not re-packaging base images, assuming the target/*-bin.jar files exist"
endif

.PHONY: package-infrastructure-images
package-infrastructure-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -f src/apps/infrastructure -DskipTests -T4
else
	@echo "Not re-packaging infra images, assuming the target/*-bin.jar files exist"
endif

.PHONY: package-geoserver-images
package-geoserver-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -f src/apps/geoserver -DskipTests -T4
else
	@echo "Not re-packaging geoserver images, assuming the target/*-bin.jar files exist"
endif

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

.PHONY: acceptance-tests-datadir
acceptance-tests-datadir: build-acceptance start-acceptance-tests-datadir run-acceptance-tests-datadir

.PHONY: start-acceptance-tests-datadir
start-acceptance-tests-datadir:
	(cd compose/ && ./acceptance_datadir up -d)

.PHONY: run-acceptance-tests-datadir
run-acceptance-tests-datadir:
	(cd compose/ && ./acceptance_datadir run --rm -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && pytest . -vvv --color=yes')

.PHONY: clean-acceptance-tests-datadir
clean-acceptance-tests-datadir:
	(cd compose/ && ./acceptance_datadir down -v)

.PHONY: acceptance-tests-pgconfig
acceptance-tests-pgconfig: build-acceptance start-acceptance-tests-pgconfig run-acceptance-tests-pgconfig

.PHONY: start-acceptance-tests-pgconfig
start-acceptance-tests-pgconfig:
	(cd compose/ && ./acceptance_pgconfig up -d)

.PHONY: run-acceptance-tests-pgconfig
run-acceptance-tests-pgconfig:
	(cd compose/ && ./acceptance_pgconfig run --rm -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && pytest . -vvv --color=yes')

.PHONY: clean-acceptance-tests-pgconfig
clean-acceptance-tests-pgconfig:
	(cd compose/ && ./acceptance_pgconfig down -v)

.PHONY: acceptance-tests-jdbcconfig
acceptance-tests-jdbcconfig: build-acceptance start-acceptance-tests-jdbcconfig run-acceptance-tests-jdbcconfig

.PHONY: start-acceptance-tests-jdbcconfig
start-acceptance-tests-jdbcconfig:
	(cd compose/ && ./acceptance_jdbcconfig up -d)

.PHONY: run-acceptance-tests-jdbcconfig
run-acceptance-tests-jdbcconfig:
	(cd compose/ && ./acceptance_jdbcconfig run --rm -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && pytest . -vvv --color=yes')

.PHONY: clean-acceptance-tests-jdbcconfig
clean-acceptance-tests-jdbcconfig:
	(cd compose/ && ./acceptance_jdbcconfig down -v)
