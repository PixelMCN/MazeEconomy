plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.pixelmcn"
version = "1.0.1"
description = "MazeEconomy - Multi-currency cross-server economy plugin by PixelMCN"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // Paper API (1.21.x)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Triumph GUI — shaded
    implementation("dev.triumphteam:triumph-gui:3.1.10") {
        exclude(group = "net.kyori") // Paper already provides Adventure
    }

    // MariaDB JDBC Driver (shaded)
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")

    // SQLite JDBC Driver — DO NOT relocate (native lib loading)
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // HikariCP connection pool (shaded)
    implementation("com.zaxxer:HikariCP:6.2.1")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("MazeEconomy-${project.version}.jar")

        relocate("org.mariadb.jdbc",    "com.pixelmcn.mazeeconomy.libs.mariadb")
        relocate("com.zaxxer.hikari",   "com.pixelmcn.mazeeconomy.libs.hikari")
        relocate("dev.triumphteam.gui", "com.pixelmcn.mazeeconomy.libs.gui")

        minimize {
            exclude(dependency("org.mariadb.jdbc:.*"))
            exclude(dependency("org.xerial:.*"))
            exclude(dependency("dev.triumphteam:.*"))
        }
    }

    build { dependsOn(shadowJar) }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
