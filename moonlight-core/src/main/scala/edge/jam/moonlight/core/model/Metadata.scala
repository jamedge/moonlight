package edge.jam.moonlight.core.model

trait Metadata {
  def name: String
  def owner: Option[String]
  def purpose: Option[String]
  def notes: Option[List[String]]
  def details: Option[Map[String, String]]

  def fieldsMap(): Map[String, _] = {
    val nameMap: Map[String, String] = Map("name" -> name)
    val ownerMap = owner.map(o => Map("owner" -> o)).getOrElse(Map())
    val purposeMap = purpose.map(p => Map("purpose" -> p)).getOrElse(Map())
    val notesMap = notes.map(n => Map("notes" -> n)).getOrElse(Map())
    nameMap ++ ownerMap ++ purposeMap ++ notesMap
  }
}

trait HistoryRecord {
  def triggeredAt: String
  def triggeredBy: String

  def fieldsMap(): Map[String, _] = {
    Map(
      "triggeredAt" -> triggeredAt,
      "triggeredBy" -> triggeredBy
    )
  }
}
