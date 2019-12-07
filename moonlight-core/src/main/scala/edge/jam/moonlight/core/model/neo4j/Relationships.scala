package edge.jam.moonlight.core.model.neo4j

import edge.jam.moonlight.core.model.neo4j.GraphElements.{ElementClass, ElementType, GraphElement}

object Relationships {
  case class HasDetails(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "hd", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasDetails(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasInput(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "hi", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasInput(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasOutput(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "ho", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasOutput(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasStorage(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "hs", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasStorage(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class IsProcessedBy(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "ipb", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.IsProcessedBy(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingFramework(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "hpf", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasProcessingFramework(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetrics(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "hm", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasMetrics(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetricsFramework(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "hmf", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasMetricsFramework(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlert(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "ha", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasAlert(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlertsFramework(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "haf", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasAlertsFramework(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasCode(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "hc", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasCode(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)
}
