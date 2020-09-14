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

**NOTE** This is the first iteration of deployment process.
The next one will also include tagging by version and proper versioning of the api.

## API Versioning and usage example

Api versioning is done using request headers.

Headers used for specifying the version are:
- `Content-Type` - Specifies the version of the request content submitted to API (used for unmarshalling)
- `Accept` - Specifies the version for the response content expected from the API (used for marshalling)

Value of the header is in the following format:
```
<content-main-type>/moonlight.v<version-number>+<content-subtype>
```
E.g. like this:
```
application/moonlight.v1+json
```
If no version is specified, the latest one is returned:
```
application/json
```

So, the whole request example for adding a line can look like this:
```
curl --request POST 'http://localhost:8080/line' \
--header 'Content-Type: application/moonlight.v1+json' \
--header 'Accept: application/moonlight.v2+json' \
--data-raw '{
	"name": "test_aggregation",
	"io": [
		{
			"inputs": [
				{
					"name": "test_input_1",
					"storage": { "name": "test_storage_1" },
					"locationRelativePath": "test/ha/test_input_1"
				},
				{
					"name": "test_input_2",
					"storage": { "name": "test_storage_2" },
					"locationRelativePath": "test/test_input_2"
				}
			],
			"outputs": [
				{
					"name": "test_output_3",
					"storage": { "name": "test_storage_1" },
					"locationRelativePath": "test/test_input_1"
				}
			]
		}
	]
}'
```
In this request, line `test_aggregation` is added using the first version of the API (v1).
This means that the request data will be decoded using v1 unmarshaller. However, the results are
being coded back using v2 responses. For this route (`/line`) it would be just a message saying
that the line was added or updated.

For the complete documentation, check swagger (still a TODO :)).

## Tests

Currently, test coverage is quite poor due to high volatility of implementation choices in early versions.
As the project is getting into more stable shape, more tests are being added.

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

