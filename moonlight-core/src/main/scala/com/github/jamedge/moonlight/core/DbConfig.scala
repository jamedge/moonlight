package com.github.jamedge.moonlight.core

case class DbConfig(neo4j: Neo4jConfig)
case class Neo4jConfig(
    uri: String,
    username: String,
    password: String)
