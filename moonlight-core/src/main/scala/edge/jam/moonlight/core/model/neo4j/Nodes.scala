package edge.jam.moonlight.core.model.neo4j

import edge.jam.moonlight.core.model.IOElement
import edge.jam.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}

object Nodes {
  import edge.jam.moonlight.core.model.{
    Alert => ModelAlert,
    AlertsFramework => ModelAlertsFramework,
    Code => ModelCode,
    IOElement => ModelIOElement,
    Line => ModelLine,
    Metric => ModelMetric,
    MetricsFramework => ModelMetricsFramework,
    Process => ModelProcess,
    ProcessingFramework => ModelProcessingFramework,
    Storage => ModelStorage
  }

  case class Line(fieldPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Line, GraphElements.generateVariable(variablePrefix), fieldPairs)
  object Line {
    def apply(from: ModelLine, variablePrefix: String): Line = {
      val fields = from.fieldsMap()
      Line(fields, variablePrefix)
    }
  }

  case class IO(fieldPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.IO, GraphElements.generateVariable(variablePrefix), fieldPairs)
  object IO {
    def apply(from: ModelIOElement, variablePrefix: String): IO = {
      val fields = from.fieldsMap()
      IO(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): IO = {
      IO(IOElement("", None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Storage(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Storage, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object Storage {
    def apply(from: ModelStorage, variablePrefix: String): Storage = {
      val fields = from.fieldsMap()
      Storage(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): Storage = {
      Storage(ModelStorage("", None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Details(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Process(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Process, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object Process {
    def apply(from: ModelProcess, variablePrefix: String): Process = {
      val fields = from.fieldsMap()
      Process(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): Process = {
      Process(ModelProcess("", None, None, None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class ProcessingFramework(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.ProcessingFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object ProcessingFramework {
    def apply(from: ModelProcessingFramework, variablePrefix: String): ProcessingFramework = {
      val fields = from.fieldsMap()
      ProcessingFramework(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): ProcessingFramework = {
      ProcessingFramework(ModelProcessingFramework("", None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Metric(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Metric, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object Metric {
    def apply(from: ModelMetric, variablePrefix: String): Metric = {
      val fields = from.fieldsMap()
      Metric(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): Metric = {
      Metric(ModelMetric("", None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class MetricsFramework(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.MetricsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object MetricsFramework {
    def apply(from: ModelMetricsFramework, variablePrefix: String): MetricsFramework = {
      val fields = from.fieldsMap()
      MetricsFramework(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): MetricsFramework = {
      MetricsFramework(ModelMetricsFramework("", None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Alert(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Alert, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object Alert {
    def apply(from: ModelAlert, variablePrefix: String): Alert = {
      val fields = from.fieldsMap()
      Alert(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): Alert = {
      Alert(ModelAlert("", None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class AlertsFramework(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.AlertsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object AlertsFramework {
    def apply(from: ModelAlertsFramework, variablePrefix: String): AlertsFramework = {
      val fields = from.fieldsMap()
      AlertsFramework(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): AlertsFramework = {
      AlertsFramework(ModelAlertsFramework("", None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Code(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Code, GraphElements.generateVariable(variablePrefix), fieldsPairs)
  object Code {
    def apply(from: ModelCode, variablePrefix: String): Code = {
      val fields = from.fieldsMap()
      Code(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): Code = {
      Code(ModelCode("", None, None, None, None, "", "", None), anyObjectVariable)
    }
  }
}
