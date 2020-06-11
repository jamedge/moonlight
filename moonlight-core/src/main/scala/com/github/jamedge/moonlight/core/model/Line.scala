package com.github.jamedge.moonlight.core.model

case class Line(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    io: List[IO],
    processedBy: List[Process],
    metrics: List[Metric],
    alerts: List[Alert],
    code: Option[Code]
) extends Metadata

case class IO(
    inputs: List[IOElement],
    outputs: List[IOElement])

case class IOElement(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    storage: Option[Storage],
    locationRelativePath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationRelativePathMap = locationRelativePath.map(e => Map("locationRelativePath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationRelativePathMap
  }
}

case class Storage(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationPathMap = locationPath.map(e => Map("locationPath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationPathMap
  }
}

case class Process(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    processingFramework: Option[ProcessingFramework],
    triggered: Option[ProcessingHistoryRecord],
    locationRelativePath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationRelativePathMap = locationRelativePath.map(e => Map("locationRelativePath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationRelativePathMap
  }
}

case class ProcessingFramework(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationPathMap = locationPath.map(e => Map("locationPath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationPathMap
  }
}

case class ProcessingHistoryRecord(
    triggeredAt: String,
    triggeredBy: String,
    processingHistory: ProcessingHistory = ProcessingHistory(
      name = "processing_history",
      purpose = Some("Enables saving of processing history."),
      owner = None,
      details = None,
      notes = None
    )
) extends HistoryRecord

case class ProcessingHistory(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
) extends Metadata

case class Metric(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    metricFramework: Option[MetricsFramework],
    locationRelativePath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationRelativePathMap = locationRelativePath.map(e => Map("locationRelativePath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationRelativePathMap
  }
}

case class MetricsFramework(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationPathMap = locationPath.map(e => Map("locationPath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationPathMap
  }
}

case class Alert(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    alertsFramework: Option[AlertsFramework],
    locationRelativePath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationRelativePathMap = locationRelativePath.map(e => Map("locationRelativePath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationRelativePathMap
  }
}

case class AlertsFramework(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata {
  override def fieldsMap(): Map[String, _] = {
    val locationPathMap = locationPath.map(e => Map("locationPath" -> e)).getOrElse(Map())
    super.fieldsMap() ++ locationPathMap
  }
}

case class Code(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    remotePath: String,
    entryPointClass: String,
    entryPointArguments: Option[List[String]]
) extends Metadata{
  override def fieldsMap(): Map[String, _] = {
    val entryPointArgumentsMap = entryPointArguments.map(e => Map("entryPointArguments" -> e)).getOrElse(Map())
    super.fieldsMap() ++ Map("remotePath" -> remotePath, "entryPointClass" -> entryPointClass) ++ entryPointArgumentsMap
  }
}
