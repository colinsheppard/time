enablePlugins(org.nlogo.build.NetLogoExtension, org.nlogo.build.ExtensionDocumentationPlugin)
netLogoVersion := "6.1.0-RC1"
netLogoClassManager := "org.nlogo.extensions.time.TimeExtension"
scalaVersion := "2.12.4"
netLogoExtName := "time"
netLogoZipSources := false

netLogoTarget := org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value / "time")
publishMavenStyle in ThisBuild := false
lazy val src = (project in file("."))
  .settings(
    scalaSource in Compile := baseDirectory.value / "src",
    scalaSource in Test    := baseDirectory.value / "test",
    scalacOptions ++= "-deprecation -unchecked -Xfatal-warnings".split(" ").toSeq
  )
