val Http4sVersion = "0.21.31"
val CirceVersion = "0.13.0"
val DoobieVersion = "0.13.4"
val PureConfigVersion = "0.17.2"
val LogbackVersion = "1.3.11"
val CatsEffectVersion = "2.2.0"
val KafkaVersion = "2.8.0"
val JedisVersion = "4.4.3"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    organization := "com.example",
    name := "event-api",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.12.18",
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-blaze-server"  % Http4sVersion,
      "org.http4s"            %% "http4s-circe"         % Http4sVersion,
      "org.http4s"            %% "http4s-dsl"           % Http4sVersion,
      "io.circe"              %% "circe-generic"        % CirceVersion,
      "io.circe"              %% "circe-core"           % CirceVersion,
      "io.circe"              %% "circe-parser"         % CirceVersion,
      "org.tpolecat"          %% "doobie-core"          % DoobieVersion,
      "org.tpolecat"          %% "doobie-postgres"      % DoobieVersion,
      "org.tpolecat"          %% "doobie-hikari"        % DoobieVersion,
      "com.github.pureconfig" %% "pureconfig"           % PureConfigVersion,
      "ch.qos.logback"        %  "logback-classic"      % LogbackVersion,
      "org.typelevel"         %% "cats-effect"          % CatsEffectVersion,
      "org.apache.kafka"      % "kafka-clients"         % KafkaVersion,
      "org.apache.kafka"      %% "kafka-streams-scala"  % KafkaVersion,
      "redis.clients"         % "jedis"                 % JedisVersion
    ),

    // BuildInfo settings
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildTime") {
        java.time.OffsetDateTime.now().toString
      }
    ),
    buildInfoPackage := "com.example",
    buildInfoOptions += BuildInfoOption.ToJson
  )