name := """cajuthorizer"""
organization := "com.rods"


// This line prevents sbt from breaking sometimes because it expects courier when updatingSbtClassifiers.
// For more information see https://github.com/sbt/sbt/issues/5263#issuecomment-626462593
updateSbtClassifiers / useCoursier := true
// This line fixes the common "Unknown Artifact" problem in intellij.
// For more information see https://stackoverflow.com/a/58456468
ThisBuild / useCoursier := false


version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.6"

libraryDependencies += guice
libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "5.0.1",
//  "org.reactivemongo" %% "play2-reactivemongo" % "1.0.4-play28",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
//  "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % Test,
  "org.mockito" % "mockito-scala-scalatest_2.13" % "1.16.37" % Test
)
