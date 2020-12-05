SHELL := /usr/bin/env bash

# login to docker hub
.PHONY: docker-login
docker-login:
	@docker login -u ${DOCKERHUB_USERNAME} -p "${DOCKERHUB_PASSWORD}"

# start the environment stack
.PHONY: start-environment
start-environment:
	docker-compose -f docker-compose.yaml up

# start the environment stack in background
.PHONY: start-environment-daemon
start-environment-daemon:
	docker-compose -f docker-compose.yaml up -d

# stop the environment stack
.PHONY: stop-environment
stop-environment:
	docker-compose -f docker-compose.yaml down --remove-orphans

# run tests
.PHONY: test
test:
	sbt -J-Xms1024M -J-Xmx2200M -J-Xss8M test

# build and push docker image for moonlight-api
.PHONY: moonlight-api-docker-build-and-push
moonlight-api-docker-build-and-push:
	sbt -J-Xms1024M -J-Xmx2200M -J-Xss8M moonlight-api/dockerBuildAndPush
