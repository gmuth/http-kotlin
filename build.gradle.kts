plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.22"
    id("org.sonarqube") version "4.3.0.3225" // https://plugins.gradle.org/plugin/org.sonarqube
    id("jacoco")
    id("maven-publish")
}

group = "de.gmuth"
version = "1.0"

repositories {
    mavenCentral()
}

// update gradle wrapper
// ./gradlew wrapper --gradle-version 7.6.2

val javaVersion = "11"

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

// gradlew clean -x test build publishToMavenLocal
defaultTasks("assemble")

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = javaVersion
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = javaVersion
    }
}
tasks.compileJava {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

java {
    withSourcesJar()
}

// ====== analyse code with SonarQube ======

// required for sonarqube code coverage
// https://docs.sonarqube.org/latest/analysis/test-coverage/java-test-coverage
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    // https://stackoverflow.com/questions/67725347/jacoco-fails-on-gradle-7-0-2-and-kotlin-1-5-10
    //version = "0.8.7"
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

// gradle test jacocoTestReport sonar
// https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/
// configure token with 'publish analysis' permission in file ~/.gradle/gradle.properties:
// systemProp.sonar.login=<token>
sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectKey", "gmuth_http-kotlin")
        property("sonar.organization", "gmuth")
    }
}

tasks.sonar {
    dependsOn(tasks.jacocoTestReport) // for coverage
}

publishing {
    publications {
        create<MavenPublication>("http-kotlin") {
            from(components["java"])
            pom {
                name.set("http-kotlin library")
                description.set("A http library for kotlin")
                url.set("https://github.com/gmuth/http-kotlin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/gmuth/http-kotlin/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("gmuth")
                        name.set("Gerhard Muth")
                        email.set("gerhard.muth@gmx.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/gmuth/http-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/gmuth/http-kotlin.git")
                    url.set("https://github.com/gmuth/http-kotlin")
                }
            }
        }
    }
}
