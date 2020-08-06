package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.IOElement

object Nodes {

  import com.github.jamedge.moonlight.core.model.{
    Alert => ModelAlert,
    AlertsFramework => ModelAlertsFramework,
    Code => ModelCode,
    IOElement => ModelIOElement,
    Line => ModelLine,
    Metric => ModelMetric,
    MetricsFramework => ModelMetricsFramework,
    Process => ModelProcess,
    ProcessingFramework => ModelProcessingFramework,
    ProcessingHistory => ModelProcessingHistory,
    Storage => ModelStorage}

  case class Line(fieldPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.Line, GraphElementUtils.generateVariable(variablePrefix), fieldPairs)

  object Line {
    def apply(from: ModelLine, variablePrefix: String = "l"): Line = {
      val fields = from.fieldsMap()
      Line(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): Line = {
      Line(ModelLine("", None, None, None, None, List(), List(), List(), List(), None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): Line = {
      Line(ModelLine(name, None, None, None, None, List(), List(), List(), List(), None), anyObjectVariable)
    }
  }

  case class IO(fieldPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.IO, GraphElementUtils.generateVariable(variablePrefix), fieldPairs)

  object IO {
    def apply(from: ModelIOElement, variablePrefix: String = "io"): IO = {
      val fields = from.fieldsMap()
      IO(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): IO = {
      IO.apply(IOElement("", None, None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): IO = {
      IO.apply(IOElement(name, None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Storage(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.Storage, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object Storage {
    def apply(from: ModelStorage, variablePrefix: String = "s"): Storage = {
      val fields = from.fieldsMap()
      Storage(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): Storage = {
      Storage(ModelStorage("", None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): Storage = {
      Storage(ModelStorage(name, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Details(fieldsPairs: Map[String, _], variablePrefix: String = "d")
    extends Node(ElementClass.Details, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object Details {
    def apply(): Details = {
      Details(Map())
    }
  }

  case class Process(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.Process, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object Process {
    def apply(from: ModelProcess, variablePrefix: String = "p"): Process = {
      val fields = from.fieldsMap()
      Process(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): Process = {
      Process(ModelProcess("", None, None, None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): Process = {
      Process(ModelProcess(name, None, None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class ProcessingFramework(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.ProcessingFramework, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object ProcessingFramework {
    def apply(from: ModelProcessingFramework, variablePrefix: String = "pf"): ProcessingFramework = {
      val fields = from.fieldsMap()
      ProcessingFramework(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): ProcessingFramework = {
      ProcessingFramework(ModelProcessingFramework("", None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): ProcessingFramework = {
      ProcessingFramework(ModelProcessingFramework(name, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class ProcessingHistory(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.ProcessingHistory, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object ProcessingHistory {
    def apply(from: ModelProcessingHistory, variablePrefix: String = "ph"): ProcessingHistory = {
      val fields = from.fieldsMap()
      ProcessingHistory(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): ProcessingHistory = {
      ProcessingHistory(ModelProcessingHistory("", None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): ProcessingHistory = {
      ProcessingHistory(ModelProcessingHistory(name, None, None, None, None), anyObjectVariable)
    }
  }

  case class Metric(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.Metric, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object Metric {
    def apply(from: ModelMetric, variablePrefix: String = "m"): Metric = {
      val fields = from.fieldsMap()
      Metric(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): Metric = {
      Metric(ModelMetric("", None, None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): Metric = {
      Metric(ModelMetric(name, None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class MetricsFramework(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.MetricsFramework, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object MetricsFramework {
    def apply(from: ModelMetricsFramework, variablePrefix: String = "mf"): MetricsFramework = {
      val fields = from.fieldsMap()
      MetricsFramework(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): MetricsFramework = {
      MetricsFramework(ModelMetricsFramework("", None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): MetricsFramework = {
      MetricsFramework(ModelMetricsFramework(name, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Alert(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.Alert, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object Alert {
    def apply(from: ModelAlert, variablePrefix: String = "a"): Alert = {
      val fields = from.fieldsMap()
      Alert(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): Alert = {
      Alert(ModelAlert("", None, None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): Alert = {
      Alert(ModelAlert(name, None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class AlertsFramework(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.AlertsFramework, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object AlertsFramework {
    def apply(from: ModelAlertsFramework, variablePrefix: String = "af"): AlertsFramework = {
      val fields = from.fieldsMap()
      AlertsFramework(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): AlertsFramework = {
      AlertsFramework(ModelAlertsFramework("", None, None, None, None, None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): AlertsFramework = {
      AlertsFramework(ModelAlertsFramework(name, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Code(fieldsPairs: Map[String, _], variablePrefix: String)
    extends Node(ElementClass.Code, GraphElementUtils.generateVariable(variablePrefix), fieldsPairs)

  object Code {
    def apply(from: ModelCode, variablePrefix: String = "c"): Code = {
      val fields = from.fieldsMap()
      Code(fields, variablePrefix)
    }

    def apply(anyObjectVariable: String): Code = {
      Code(ModelCode("", None, None, None, None, "", "", None), anyObjectVariable)
    }

    def apply(name: String, anyObjectVariable: String): Code = {
      Code(ModelCode(name, None, None, None, None, "", "", None), anyObjectVariable)
    }
  }

}
