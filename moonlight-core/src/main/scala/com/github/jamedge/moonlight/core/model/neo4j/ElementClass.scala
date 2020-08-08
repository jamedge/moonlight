package com.github.jamedge.moonlight.core.model.neo4j

sealed class ElementClass(val name: String, val elementType: ElementType)
sealed class NodeClass(override val name: String) extends ElementClass(name, ElementType.Node)
sealed class RelationshipRightClass(override val name: String) extends ElementClass(name, ElementType.RelationshipRight)

object ElementClass {
  // Nodes
  case object Line extends NodeClass("Line")
  case object IO extends NodeClass("IO")
  case object Details extends NodeClass("Details")
  case object Storage extends NodeClass("Storage")
  case object Process extends NodeClass("Process")
  case object ProcessingFramework extends NodeClass("ProcessingFramework")
  case object ProcessingHistory extends NodeClass("ProcessingHistory")
  case object Metric extends NodeClass("Metric")
  case object MetricsFramework extends NodeClass("MetricsFramework")
  case object Alert extends NodeClass("Alert")
  case object AlertsFramework extends NodeClass("AlertFramework")
  case object Code extends NodeClass("Code")

  // Relationships
  case object HasDetails extends RelationshipRightClass("HAS_DETAILS")
  case object HasInput extends RelationshipRightClass("HAS_INPUT")
  case object HasOutput extends RelationshipRightClass("HAS_OUTPUT")
  case object HasStorage extends RelationshipRightClass("HAS_STORAGE")
  case object IsProcessedBy extends RelationshipRightClass("IS_PROCESSED_BY")
  case object HasProcessingFramework extends RelationshipRightClass("HAS_PROCESSING_FRAMEWORK")
  case object HasProcessingHistory extends RelationshipRightClass("HAS_PROCESSING_HISTORY")
  case object HasProcessingHistoryRecord extends RelationshipRightClass("HAS_PROCESSING_HISTORY_RECORD")
  case object HasMetrics extends RelationshipRightClass("HAS_METRIC")
  case object HasMetricsFramework extends RelationshipRightClass("HAS_METRICS_FRAMEWORK")
  case object HasAlert extends RelationshipRightClass("HAS_ALERT")
  case object HasAlertsFramework extends RelationshipRightClass("HAS_ALERTS_FRAMEWORK")
  case object HasCode extends RelationshipRightClass("HAS_CODE")
}
