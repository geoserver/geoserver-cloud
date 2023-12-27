all: install test build-image

# run `make build-image[-*] SKIP_PUSH=false` to push the images to dockerhub
SKIP_PUSH?="true"

lint:
	./mvnw fmt:check sortpom:verify -Dsort.verifyFailOn=strict -Dsort.verifyFail=stop -ntp

format:
	./mvnw sortpom:sort fmt:format -ntp

install:
	./mvnw clean install -DskipTests -ntp -T4 -U

test:
	./mvnw verify -ntp -T4

build-image: build-base-images build-image-infrastructure build-image-geoserver

build-image-openj9: build-image-infrastructure-openj9 build-image-geoserver-openj9

build-base-images:
	./mvnw clean package -f src/apps/base-images \
	-Ddocker -Ddockerfile.push.skip=true -ntp -Dfmt.skip -DskipTests

build-image-infrastructure:
	./mvnw clean package -f src/apps/infrastructure \
	-Ddocker -Ddockerfile.push.skip=$(SKIP_PUSH) -ntp -Dfmt.skip -DskipTests

build-image-geoserver:
	./mvnw clean package -f src/apps/geoserver \
	-Ddocker -Ddockerfile.push.skip=$(SKIP_PUSH) -ntp -Dfmt.skip -DskipTests
  
