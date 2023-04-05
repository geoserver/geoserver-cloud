all: deps install test build-image

# run `make build-image[-*] SKIP_PUSH=false` to push the images to dockerhub
SKIP_PUSH?="true"

lint:
	./mvnw -P-geoserver sortpom:verify fmt:check -ntp

format:
	./mvnw -P-geoserver sortpom:sort fmt:format -ntp

deps:
	./mvnw -U -f geoserver_submodule/ clean install -DskipTests -ntp -T4

install:
	./mvnw clean install -P-geoserver -DskipTests -ntp -T4

test:
	./mvnw verify -P-geoserver -ntp -T4

build-image: build-image-infrastructure build-image-geoserver

build-image-openj9: build-image-infrastructure-openj9 build-image-geoserver-openj9

build-image-infrastructure:
	./mvnw clean package -f src/apps/infrastructure \
	-Ddocker -P-geoserver -Ddockerfile.push.skip=$(SKIP_PUSH) -ntp -Dfmt.skip -DskipTests

build-image-infrastructure-openj9:
	./mvnw clean package -f src/apps/infrastructure \
	-Dopenj9 -P-geoserver -Ddockerfile.push.skip=$(SKIP_PUSH) -ntp -Dfmt.skip -DskipTests

build-image-geoserver:
	./mvnw clean package -f src/apps/geoserver \
	-Ddocker -P-geoserver -Ddockerfile.push.skip=$(SKIP_PUSH) -ntp -Dfmt.skip -DskipTests
  
build-image-geoserver-openj9:
	./mvnw clean package -f src/apps/geoserver \
	-Dopenj9 -P-geoserver -Ddockerfile.push.skip=$(SKIP_PUSH) -ntp -Dfmt.skip -DskipTests

build-config-native-image:
	./mvnw -pl :gs-cloud-config-service install -am -Dfmt.action=check -ntp -P-geoserver
	./mvnw -pl :gs-cloud-config-service spring-boot:build-image -Pnative,-geoserver -ntp -DskipTests -Dfmt.skip

