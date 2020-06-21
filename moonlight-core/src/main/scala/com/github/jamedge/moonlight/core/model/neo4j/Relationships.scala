package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}

object Relationships {

  case class HasDetails(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hd")
    extends GraphElement(ElementClass.HasDetails, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasInput(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hi")
    extends GraphElement(ElementClass.HasInput, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasOutput(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ho")
    extends GraphElement(ElementClass.HasOutput, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasStorage(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hs")
    extends GraphElement(ElementClass.HasStorage, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class IsProcessedBy(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ipb")
    extends GraphElement(ElementClass.IsProcessedBy, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hpf")
    extends GraphElement(ElementClass.HasProcessingFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingHistory(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hph")
    extends GraphElement(ElementClass.HasProcessingHistory, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingHistoryRecord(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hphr")
    extends GraphElement(ElementClass.HasProcessingHistoryRecord, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetrics(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hm")
    extends GraphElement(ElementClass.HasMetrics, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetricsFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hmf")
    extends GraphElement(ElementClass.HasMetricsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlert(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "ha")
    extends GraphElement(ElementClass.HasAlert, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlertsFramework(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "haf")
    extends GraphElement(ElementClass.HasAlertsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasCode(fieldsPairs: Map[String, _] = Map("relationshipType" -> "mutable"), variablePrefix: String = "hc")
    extends GraphElement(ElementClass.HasCode, GraphElements.generateVariable(variablePrefix), fieldsPairs)

}
