.PHONY: all
all: deps install test build-image

TAG?=$(shell ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

.PHONY: show-version
show-version:
	@echo $(TAG)

COSIGN_PASSWORD := $(COSIGN_PASSWORD)


REPACKAGE ?= true

.PHONY: clean
clean:
	./mvnw clean

# Build the customized GeoServer from the geoserver_submodule/geoserver
# submodule and install its artifacts into the local Maven repository.
# Run `git submodule update --init` first if the submodule is not checked out.
GEOSERVER_SUBMODULE = geoserver_submodule/geoserver
GEOSERVER_BUILD_FLAGS = --batch-mode -DskipTests -ntp -fae -Dsort.skip=true -Dspotless.apply.skip=true -T1C
GEOSERVER_COMMUNITY_PROFILES = pending,cog,acl,flatgeobuf,geoparquet,graticule,gwc-azure-blob,gwc-gcs-blob,pmtiles-store,oseo,features-templating

.PHONY: deps
deps: deps-core deps-extensions deps-community

.PHONY: deps-core
deps-core:
	./mvnw install -f $(GEOSERVER_SUBMODULE)/src/pom.xml $(GEOSERVER_BUILD_FLAGS)

.PHONY: deps-extensions
deps-extensions:
	./mvnw install -f $(GEOSERVER_SUBMODULE)/src/extension/pom.xml -Prelease $(GEOSERVER_BUILD_FLAGS)

.PHONY: deps-community
deps-community:
	./mvnw install -f $(GEOSERVER_SUBMODULE)/src/community/pom.xml -P$(GEOSERVER_COMMUNITY_PROFILES) $(GEOSERVER_BUILD_FLAGS)

.PHONY: build-tools
build-tools:
	./mvnw clean install -pl src/build-tools/build-config/ -ntp

.PHONY: lint
lint: build-tools
	./mvnw validate -Dqa -fae -ntp -T1C

.PHONY: lint-pom
lint-pom:
	./mvnw validate -Dqa -fae -Dspotless.skip=true -Dcheckstyle.skip=true -ntp -T1C

.PHONY: lint-java
lint-java: build-tools
	./mvnw validate -Dqa -fae -Dsortpom.skip=true -ntp -T1C

.PHONY: format
format: format-pom format-java

.PHONY: format-pom
format-pom:
	./mvnw sortpom:sort -ntp -T1C

.PHONY: format-java
format-java:
	./mvnw spotless:apply -ntp -T1C

.PHONY: install
install: build-tools
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
	TAG=$(TAG) docker compose -f docker-build/base-images.yml build jre
	TAG=$(TAG) docker compose -f docker-build/base-images.yml build spring-boot
	TAG=$(TAG) docker compose -f docker-build/base-images.yml build geoserver-common

.PHONY: build-image-infrastructure
build-image-infrastructure: package-infrastructure-images
	TAG=$(TAG) docker compose -f docker-build/infrastructure.yml build  $(filter-out $@ build-image build-image-multiplatform,$(MAKECMDGOALS))

# This uses $(MAKECMDGOALS) (all targets specified) and filters out the target itself ($@), passing the rest as arguments. The %: rule tells make to ignore any unrecognized "targets" (which are actually your service names).
# Then you can call:
#  make build-image-geoserver wcs wfs
.PHONY: build-image-geoserver
build-image-geoserver: package-geoserver-images
	TAG=$(TAG) docker compose -f docker-build/geoserver.yml build $(filter-out $@ build-image,$(MAKECMDGOALS))


.PHONY: package-base-images
package-base-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -Dfmt.skip -DskipTests -T1C -ntp -am -pl src/apps/base-images/jre,src/apps/base-images/spring-boot,src/apps/base-images/geoserver
else
	@echo "Not re-packaging base images, assuming the target/*-bin.jar files exist"
endif

.PHONY: package-infrastructure-images
package-infrastructure-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -Dfmt.skip -DskipTests -T1C -ntp -am -pl src/apps/infrastructure/config,src/apps/infrastructure/gateway-webmvc
else
	@echo "Not re-packaging infra images, assuming the target/*-bin.jar files exist"
endif

.PHONY: package-geoserver-images
package-geoserver-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -Dfmt.skip -DskipTests -T1C -ntp -am -pl src/apps/geoserver/gwc,src/apps/geoserver/restconfig,src/apps/geoserver/wcs,src/apps/geoserver/webui,src/apps/geoserver/wfs,src/apps/geoserver/wms,src/apps/geoserver/wcs,src/apps/geoserver/wps
else
	@echo "Not re-packaging geoserver images, assuming the target/*-bin.jar files exist"
endif

.PHONY: pull-images
pull-images:
	TAG=$$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout) \
	docker compose \
     -f docker-build/geoserver.yml \
     -f docker-build/infrastructure.yml \
     pull --quiet

.PHONY: sign-image
sign-image:
	@bash -c '\
	TAG=$$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout); \
	images=$$(TAG=$$TAG docker compose -f docker-build/geoserver.yml -f docker-build/infrastructure.yml config --images); \
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
	TAG=$$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout); \
	images=$$(TAG=$$TAG docker compose -f docker-build/geoserver.yml -f docker-build/infrastructure.yml config --images); \
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
	docker build --tag=geoservercloud/acceptance:latest acceptance_tests

.PHONY: acceptance-tests-datadir
acceptance-tests-datadir: build-acceptance start-acceptance-tests-datadir run-acceptance-tests-datadir

.PHONY: start-acceptance-tests-datadir
start-acceptance-tests-datadir:
	(cd compose/ && TAG=$(TAG) ./acceptance_datadir up -d)

.PHONY: run-acceptance-tests-datadir
run-acceptance-tests-datadir:
	(cd compose/ && TAG=$(TAG) ./acceptance_datadir run --rm -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && COLUMNS=120 pytest -v --color=yes --pyargs geoserver_acceptance_tests.tests  -k "not test_i18n_layers_default_locale"')

.PHONY: clean-acceptance-tests-datadir
clean-acceptance-tests-datadir:
	(cd compose/ && TAG=$(TAG) ./acceptance_datadir down -v --remove-orphans)

.PHONY: acceptance-tests-pgconfig
acceptance-tests-pgconfig: build-acceptance start-acceptance-tests-pgconfig run-acceptance-tests-pgconfig

.PHONY: start-acceptance-tests-pgconfig
start-acceptance-tests-pgconfig:
	(cd compose/ && TAG=$(TAG) ./acceptance_pgconfig up -d)

.PHONY: run-acceptance-tests-pgconfig
run-acceptance-tests-pgconfig:
	(cd compose/ && TAG=$(TAG) ./acceptance_pgconfig run --rm -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && COLUMNS=120 pytest -v --color=yes --pyargs geoserver_acceptance_tests.tests  -k "not test_i18n_layers_default_locale"')

.PHONY: clean-acceptance-tests-pgconfig
clean-acceptance-tests-pgconfig:
	(cd compose/ && TAG=$(TAG) ./acceptance_pgconfig down -v --remove-orphans)

# Prevent make from treating service names as targets when using $(MAKECMDGOALS) in build-image-geoserver
%:
	@:
