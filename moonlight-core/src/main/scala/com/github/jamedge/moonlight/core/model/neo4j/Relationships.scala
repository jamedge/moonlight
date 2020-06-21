package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.RelationshipRight

object Relationships {

  case class HasDetails(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hd")
    extends RelationshipRight(ElementClass.HasDetails, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasInput(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hi")
    extends RelationshipRight(ElementClass.HasInput, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasOutput(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ho")
    extends RelationshipRight(ElementClass.HasOutput, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasStorage(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hs")
    extends RelationshipRight(ElementClass.HasStorage, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class IsProcessedBy(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ipb")
    extends RelationshipRight(ElementClass.IsProcessedBy, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hpf")
    extends RelationshipRight(ElementClass.HasProcessingFramework, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingHistory(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hph")
    extends RelationshipRight(ElementClass.HasProcessingHistory, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingHistoryRecord(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hphr")
    extends RelationshipRight(ElementClass.HasProcessingHistoryRecord, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetrics(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hm")
    extends RelationshipRight(ElementClass.HasMetrics, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetricsFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hmf")
    extends RelationshipRight(ElementClass.HasMetricsFramework, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlert(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ha")
    extends RelationshipRight(ElementClass.HasAlert, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlertsFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "haf")
    extends RelationshipRight(ElementClass.HasAlertsFramework, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  case class HasCode(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hc")
    extends RelationshipRight(ElementClass.HasCode, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

}
