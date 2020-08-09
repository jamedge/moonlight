# moonlight-api

Module that contains data lineage REST API.

## Functionality

This module provides REST API for storage and retrieval of lineage metadata.
It provides endpoints for:
- adding and retrieving ETL jobs metadata such as name, purpose, owner, inputs, outputs, etc. as well as open
map for any additional details that might further describe the underlying job.
- retrieving lineage graph data in form of json or html
- generating html report describing the jobs and lineage

## Deployment

Running assembly of the project results in generation of a fat jar containing application logic and all its dependencies.
This jar is packed and pushed as docker image to DockerHub by CI/CO tool Travis each time when PRs are merged to master branch.
This means that merging of a new PR results in generation of separate docker image.
When pushed, image is tagged with commit id and `latest` tags.

**NOTE** This is the first iteration of deployment process. The next one will also include tagging by version and proper versioning of the api.

## Tests

Currently, tests exist only for API exception and rejection handlers. More tests for other functionalities will be added upon having more stable version of the application.

## Running

### `com.github.jamedge.moonlight.api.Api`

This is the entry point to the api.

Example of usage of the generated jar is below.
```
java -jar moonlight-api.jar
```

For the usage of the endpoints consult the generated swagger documentation. (Still a TODO)

## Configuration

For configuration Lightbend [Typesafe config](https://github.com/lightbend/config) is used.
Next to it, [PureConfig](https://github.com/pureconfig/pureconfig) library is used to make
loading of config runtime-safe by ingesting it to appropriate case classes that are created
to mimic the structure of used config.

## Code formatting style

Code is formatted following the [scala style guide](http://docs.scala-lang.org/style/).

## Resources

- Neo4j 3.4.0

## Local setup dependencies

- Java 8
- Sbt 1.3.7
- Scala 2.13

