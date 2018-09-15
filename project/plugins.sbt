logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

resolvers += Resolver.bintrayIvyRepo("kamon-io", "sbt-plugins")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.19")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")

addSbtPlugin("io.kamon" % "sbt-aspectj-runner-play-2.6" % "1.1.1")

