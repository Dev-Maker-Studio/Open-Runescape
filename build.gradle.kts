plugins {
    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("maven-publish")
    java
    application
}

group = "darkan"
version = "2.0.0"
description = "Darkan Client"

application {
    mainClass.set("com.jagex.game.launch.Loader")
}

java {
	toolchain.languageVersion = JavaLanguageVersion.of(19)
}

project.setProperty("mainClassName", "com.jagex.game.launch.Loader")

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {

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
