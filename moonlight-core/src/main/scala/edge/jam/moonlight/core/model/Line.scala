package edge.jam.moonlight.core.model

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
) extends Metadata

case class Storage(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata

case class Process(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    processingFramework: Option[ProcessingFramework],
    triggeredAt: Option[Long],
    triggeredBy: Option[String],
    locationRelativePath: Option[String]
) extends Metadata

case class ProcessingFramework(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata

case class Metric(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    metricFramework: Option[ProcessingFramework],
    locationRelativePath: Option[String]
) extends Metadata

case class MetricFramework(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata

case class Alert(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    alertsFramework: Option[AlertsFramework],
    locationRelativePath: Option[String]
) extends Metadata

case class AlertsFramework(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    locationPath: Option[String]
) extends Metadata

case class Code(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    remotePath: String,
    entryPointClass: String,
    entryPointArguments: Option[List[String]]
) extends Metadata
