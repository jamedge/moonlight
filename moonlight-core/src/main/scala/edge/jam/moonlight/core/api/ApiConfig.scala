package edge.jam.moonlight.core.api

case class ApiConfig(server: ServerConfig)
case class ServerConfig(
    host: String,
    port: Int)
