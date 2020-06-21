package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement, RelationshipRight}

object Relationships {

  case class HasDetails(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hd")
    extends RelationshipRight(ElementClass.HasDetails, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasInput(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hi")
    extends RelationshipRight(ElementClass.HasInput, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasOutput(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ho")
    extends RelationshipRight(ElementClass.HasOutput, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasStorage(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hs")
    extends RelationshipRight(ElementClass.HasStorage, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class IsProcessedBy(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ipb")
    extends RelationshipRight(ElementClass.IsProcessedBy, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hpf")
    extends RelationshipRight(ElementClass.HasProcessingFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingHistory(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hph")
    extends RelationshipRight(ElementClass.HasProcessingHistory, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingHistoryRecord(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hphr")
    extends RelationshipRight(ElementClass.HasProcessingHistoryRecord, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetrics(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hm")
    extends RelationshipRight(ElementClass.HasMetrics, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetricsFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hmf")
    extends RelationshipRight(ElementClass.HasMetricsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlert(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ha")
    extends RelationshipRight(ElementClass.HasAlert, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlertsFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "haf")
    extends RelationshipRight(ElementClass.HasAlertsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasCode(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hc")
    extends RelationshipRight(ElementClass.HasCode, GraphElements.generateVariable(variablePrefix), fieldsPairs)

}
