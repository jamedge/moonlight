package com.github.jamedge.moonlight.core.api

case class ApiConfig(
    server: ServerConfig,
    routesLoadTimeoutsMs: RoutesLoadTimeoutsMsConfig)
case class ServerConfig(
    host: String,
    port: Int)
case class RoutesLoadTimeoutsMsConfig(
    report: Int
)
