package edge.jam.moonlight.core.model

trait Metadata {
  def name: String
  def owner: Option[String]
  def purpose: Option[String]
  def notes: Option[List[String]]
  def details: Option[Map[String, String]]

  def fieldsMap(): Map[String, String] = Map(
    "name" -> name,
    "owner" -> owner.getOrElse(""),
    "purpose" -> purpose.getOrElse(""),
    "notes" -> s"[${notes.getOrElse(List()).map(el => s""""$el"""").mkString(", ")}]")
}
