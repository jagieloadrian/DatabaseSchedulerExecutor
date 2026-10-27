plugins {
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
}

group = project.property("group") as String
version = project.property("version") as String

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.github.jsqlparser:jsqlparser:5.1")

    //jdbc drivers
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.10")
    implementation("com.microsoft.sqlserver:mssql-jdbc:13.6.0.jre11")
    implementation("com.oracle.database.jdbc:ojdbc11:23.8.0.25.04")

    //logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter") // includes junit-jupiter-params
    testRuntimeOnly("org.junit.platform:junit-platform-launcher") // Gradle 9 no longer provides it
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

    //testcontainers (E2E against real Postgres/MySQL/MariaDB/MSSQL/Oracle)
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
    testImplementation("org.testcontainers:mariadb:1.21.4")
    testImplementation("org.testcontainers:mssqlserver:1.21.4")
    testImplementation("org.testcontainers:oracle-free:1.21.4")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

// Fat jar: merges META-INF/services (one java.sql.Driver entry per JDBC driver)
// and drops dependency signatures (mssql-jdbc's MSFTSIG.SF broke class loading).
tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "com.anjo.MainKt"
    }
    mergeServiceFiles()
    // Shadow 9 drops duplicates (EXCLUDE) before transformers run; files they merge must keep every copy
    filesMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

// shadowJar writes the same file name, the thin jar would clash with it
tasks.jar {
    enabled = false
}

// Release bundle matching the documented Usage flow exactly: fat jar +
// example config + the run/kill/install scripts, flat, ready to unzip and run.
val releaseZip = tasks.register<Zip>("releaseZip") {
    archiveBaseName.set(rootProject.name)
    archiveVersion.set(version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(tasks.shadowJar)
    from("src/main/resources/app.properties")
    from("scripts") {
        exclude("test/**")
    }
}

tasks.named("assemble") {
    dependsOn(releaseZip)
}
