all: install test build-image

TAG=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`

lint:
	./mvnw fmt:check sortpom:verify -Dsort.verifyFailOn=strict -Dsort.verifyFail=stop -ntp

format:
	./mvnw sortpom:sort fmt:format -ntp

install:
	./mvnw clean install -DskipTests -ntp -T4 -U

test:
	./mvnw verify -ntp -T4


build-base-images:
	./mvnw clean package -f src/apps/base-images -DksipTests -T4 && \
	COMPOSE_DOCKER_CLI_BUILD=1 \
	DOCKER_BUILDKIT=1 \
	TAG=$(TAG) \
	docker compose -f docker-build/base-images.yml build 

build-image-infrastructure:
	./mvnw clean package -f src/apps/infrastructure -DskipTests -T4 && \
	COMPOSE_DOCKER_CLI_BUILD=1 \
	DOCKER_BUILDKIT=1 \
	TAG=$(TAG) \
	docker compose -f docker-build/infrastructure.yml build

build-image-geoserver:
	./mvnw clean package -f src/apps/geoserver -DskipTests -T4 && \
	COMPOSE_DOCKER_CLI_BUILD=1 \
	DOCKER_BUILDKIT=1 \
	TAG=$(TAG) \
	docker compose -f docker-build/geoserver.yml build 
  
build-image: build-base-images build-image-infrastructure build-image-geoserver

push-image:
	TAG=$(TAG) \
	docker compose \
	-f docker-build/infrastructure.yml \
	-f docker-build/geoserver.yml \
	push
