package com.github.jamedge.moonlight.core.model.neo4j.queries

import com.github.jamedge.moonlight.core.model.neo4j.Nodes
import neotypes.DeferredQueryBuilder

import neotypes.implicits.all._

object LineQueriesConstructor
  extends BaseQueriesConstructor((name: String) => Nodes.Line(name, "l"))
    with DeleteQueriesConstructor
    with CreateQueriesConstructor {
  // TODO: fix a bug which happens when user_data storage tries to be deleted

  def constructGetAllLineNames(): DeferredQueryBuilder = {
    c"MATCH (l:Line) RETURN l.name"
  }
}
