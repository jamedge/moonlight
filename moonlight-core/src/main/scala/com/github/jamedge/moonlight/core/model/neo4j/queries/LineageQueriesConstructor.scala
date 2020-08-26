package com.github.jamedge.moonlight.core.model.neo4j.queries

import neotypes.DeferredQueryBuilder

import neotypes.implicits.all._

object LineageQueriesConstructor {
  def constructGetLineageRawEdgesQuery(rootIOElementName: String): DeferredQueryBuilder = {
    c"""MATCH p = (i:IO {name: $rootIOElementName}) -[:HAS_OUTPUT *1..]-> (n:IO)
      WITH p
      MATCH (a) -[r]-> (b) WHERE r IN relationships(p)
      RETURN DISTINCT a AS left, r.fromLines AS properties, b AS right"""
  }

  def constructGetAllIODetailsQuery(): DeferredQueryBuilder = {
    c"""MATCH (i:IO) -[:HAS_DETAILS]-> (d)
      RETURN DISTINCT i.name, d {.*}"""
  }

  def constructGetAllIOStoragesQuery(): DeferredQueryBuilder = {
    c"""MATCH (i:IO) -[:HAS_STORAGE]-> (s)
      RETURN i.name, s"""
  }

  def constructGetAllStorageDetailsQuery(): DeferredQueryBuilder = {
    c"""MATCH (s:Storage) -[:HAS_DETAILS]-> (d)
      RETURN DISTINCT s.name, d {.*}"""
  }

  def constructGetAllIONamesQuery(): DeferredQueryBuilder = {
    c"""MATCH (i:IO) RETURN i.name"""
  }
}
