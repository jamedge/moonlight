# moonlight-client

This is a Moonlight Scala client library. Its main intention is to provide easy functionality for adding lines directly
from the target Scala project.

## Functionality

Current functionality of this module only includes a simple client class with a fuction to
persist line to Neo4j graph db. 

## Deployment

This module is still not included into CI/CD pipeline. 
The idea is to add building of the versioned library deployed to some public package registry. (TODO item) 

## Tests

At the moment, only a simple test for line client exists. 
More of them will be added as this library is gaining shape.

## Running

This module is intended to be used as a library, so it won't contain any class with `main` method. 

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

