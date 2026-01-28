.PHONY: all
all: install test build-image

TAG?=$(shell ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

COSIGN_PASSWORD := $(COSIGN_PASSWORD)


REPACKAGE ?= true

.PHONY: clean
clean:
	./mvnw clean

.PHONY: build-tools
build-tools:
	./mvnw clean install -pl build-tools/

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
	TAG=$(TAG) docker compose -f docker-build/infrastructure.yml build

# This uses $(MAKECMDGOALS) (all targets specified) and filters out the target itself ($@), passing the rest as arguments. The %: rule tells make to ignore any unrecognized "targets" (which are actually your service names).
# Then you can call:
#  make build-image-geoserver wcs wfs
.PHONY: build-image-geoserver
build-image-geoserver: package-geoserver-images
	TAG=$(TAG) docker compose -f docker-build/geoserver.yml build $(filter-out $@ build-image build-image-multiplatform,$(MAKECMDGOALS))

.PHONY: build-image-multiplatform
build-image-multiplatform: build-base-images-multiplatform build-image-infrastructure-multiplatform build-image-geoserver-multiplatform

.PHONY: build-base-images-multiplatform
build-base-images-multiplatform: package-base-images
	TAG=$(TAG) docker compose -f docker-build/base-images-multiplatform.yml build jre --push \
	&& TAG=$(TAG) docker compose -f docker-build/base-images-multiplatform.yml build spring-boot --push \
	&& TAG=$(TAG) docker compose -f docker-build/base-images-multiplatform.yml build geoserver-common --push

.PHONY: build-image-infrastructure-multiplatform
build-image-infrastructure-multiplatform: package-infrastructure-images
	TAG=$(TAG) docker compose -f docker-build/infrastructure-multiplatform.yml build --push

# This uses $(MAKECMDGOALS) (all targets specified) and filters out the target itself ($@), passing the rest as arguments. The %: rule tells make to ignore any unrecognized "targets" (which are actually your service names).
# Then you can call:
#  make build-image-geoserver-multiplatform wcs wfs
.PHONY: build-image-geoserver-multiplatform
build-image-geoserver-multiplatform: package-geoserver-images
	TAG=$(TAG) docker compose -f docker-build/geoserver-multiplatform.yml build --push $(filter-out $@ build-image build-image-multiplatform,$(MAKECMDGOALS))

.PHONY: package-base-images
package-base-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -DskipTests -T1C -ntp -am -pl src/apps/base-images/jre,src/apps/base-images/spring-boot,src/apps/base-images/geoserver
else
	@echo "Not re-packaging base images, assuming the target/*-bin.jar files exist"
endif

.PHONY: package-infrastructure-images
package-infrastructure-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -DskipTests -T1C -ntp -am -pl src/apps/infrastructure/config,src/apps/infrastructure/discovery,src/apps/infrastructure/gateway
else
	@echo "Not re-packaging infra images, assuming the target/*-bin.jar files exist"
endif

.PHONY: package-geoserver-images
package-geoserver-images:
ifeq ($(REPACKAGE), true)
	./mvnw clean package -DskipTests -T1C -ntp -am -pl src/apps/geoserver/gwc,src/apps/geoserver/restconfig,src/apps/geoserver/wcs,src/apps/geoserver/webui,src/apps/geoserver/wfs,src/apps/geoserver/wms,src/apps/geoserver/wcs,src/apps/geoserver/wps
else
	@echo "Not re-packaging geoserver images, assuming the target/*-bin.jar files exist"
endif

.PHONY: pull-images
pull-images:
	TAG=$$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout) \
	docker compose \
     -f docker-build/geoserver-multiplatform.yml \
     -f docker-build/infrastructure-multiplatform.yml \
     pull --quiet

.PHONY: sign-image
sign-image:
	@bash -c '\
	TAG=$$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout); \
	images=$$(TAG=$$TAG docker compose -f docker-build/geoserver-multiplatform.yml -f docker-build/infrastructure-multiplatform.yml config --images); \
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
	images=$$(TAG=$$TAG docker compose -f docker-build/geoserver-multiplatform.yml -f docker-build/infrastructure-multiplatform.yml config --images); \
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
	(cd compose/ && ./acceptance_datadir run --rm -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && pytest . -vvv --color=yes')

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
	(cd compose/ && ./acceptance_pgconfig run --rm -T acceptance bash -c 'until [ -f /tmp/healthcheck ]; do echo "Waiting for /tmp/healthcheck to be available..."; sleep 5; done && pytest . -vvv --color=yes --ignore=tests/test_imagemosaic.py --ignore=tests/test_imagemosaic_cog.py')

.PHONY: clean-acceptance-tests-pgconfig
clean-acceptance-tests-pgconfig:
	(cd compose/ && TAG=$(TAG) ./acceptance_pgconfig down -v --remove-orphans)

# Prevent make from treating service names as targets when using $(MAKECMDGOALS) in build-image-geoserver/build-image-geoserver-multiplatform
%:
	@:
