package com.github.jamedge.moonlight.api

case class ApiConfig(
    server: ServerConfig,
    routesLoadTimeoutsMs: RoutesLoadTimeoutsMsConfig)
case class ServerConfig(
    host: String,
    port: Int)
case class RoutesLoadTimeoutsMsConfig(
    report: Int
)
