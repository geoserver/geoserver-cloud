all: deps install test docker


PUSH?="false"

format-chek:
	./mvnw -f src/ fmt:check -DskipTests -ntp -T4

deps:
	./mvnw -f geoserver_submodule/ clean install -DskipTests -ntp -T4

install:
	./mvnw -f src/ clean install -DskipTests -ntp -T4 -P-docker,-docker-openj9

test:
	./mvnw -f src/ verify -ntp -T4 -P-docker,-docker-openj9

docker:
	./mvnw clean package -f src/apps -Ddockerfile.push.skip=$(PUSH) -ntp -Dfmt.skip -T4 -DskipTests -P-docker-openj9

docker-openj9:
	./mvnw clean package -f src/apps -Ddockerfile.push.skip=$(PUSH) -ntp -Dfmt.skip -T4 -DskipTests -P-docker
