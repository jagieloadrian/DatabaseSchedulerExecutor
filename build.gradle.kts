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
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("com.github.jsqlparser:jsqlparser:5.4")

    //jdbc drivers
    implementation("org.postgresql:postgresql:42.7.14")
    implementation("com.mysql:mysql-connector-j:26.10.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:4.0.0")
    implementation("com.microsoft.sqlserver:mssql-jdbc:13.6.1.jre11")
    implementation("com.oracle.database.jdbc:ojdbc11:23.26.3.0.0")

    //logging
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("org.slf4j:slf4j-simple:2.0.20")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter") // includes junit-jupiter-params
    testRuntimeOnly("org.junit.platform:junit-platform-launcher") // Gradle 9 no longer provides it
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.2.6")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")

    //testcontainers (E2E against real Postgres/MySQL/MariaDB/MSSQL/Oracle)
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-mariadb")
    testImplementation("org.testcontainers:testcontainers-mssqlserver")
    testImplementation("org.testcontainers:testcontainers-oracle-free")
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
