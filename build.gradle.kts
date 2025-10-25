import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.gradleup.shadow") version "9.2.0"
    id("maven-publish")
    java
    application
    kotlin("jvm") version "2.2.21"
}

group = "darkan"
version = "2.0.0"
description = "Darkan Client"

application {
    mainClass.set("com.jagex.game.launch.Loader")
}

java {
	toolchain.languageVersion = JavaLanguageVersion.of(19)
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
	compilerOptions {
		//jvmTarget.set(JvmTarget.JVM_1_8)
        jvmTarget.set(JvmTarget.JVM_17)
	}
}

sourceSets {
	main {
		java {
			setSrcDirs(listOf("src/main/java", "src/main/kotlin"))
		}
		kotlin {
			setSrcDirs(listOf("src/main/java", "src/main/kotlin"))
		}
	}
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitLab"
            url = uri("https://gitlab.com/api/v4/projects/42378995/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    jar {
        manifest {
            attributes(mutableMapOf("Main-Class" to "com.jagex.game.launch.Loader"))
        }
    }

    shadowJar {
        archiveBaseName.set("darkan-client-shaded")
    }
}

tasks {
	build {
		dependsOn(shadowJar)
	}
}
