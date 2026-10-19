plugins {
    application
    `maven-publish`
    id("com.gradleup.shadow") version "9.6.1"
    kotlin("plugin.serialization")
}

application {
    mainClass.set("org.darkan.lobby.MainKt")
    tasks.run.get().workingDir = rootProject.projectDir
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":core"))
    // The cache source tree stores index 12 as TypeScript; :cs2 carries the codec that
    // compiles it back (docs/cache-source.md). Headless - no LWJGL, GLFW or ImGui.
    implementation(project(":cs2"))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.darkan.lobby.MainKt"
    }
}

tasks.shadowJar {
    isZip64 = true
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    filesNotMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    archiveBaseName.set("lobby-server")
    // Published and downloaded as <name>-<version>.jar; no "-all" classifier.
    archiveClassifier.set("")
}

publishing {
    val ciProjectId: String? = System.getenv("CI_PROJECT_ID")
    val ciJobToken: String? = System.getenv("CI_JOB_TOKEN")
    val ciPipelineId: String? = System.getenv("CI_PIPELINE_ID")
    val ciBranch: String? = System.getenv("CI_COMMIT_REF_SLUG")
    val ciTag: String? = System.getenv("CI_COMMIT_TAG")
    if (ciProjectId == null || ciJobToken == null || ciPipelineId == null || ciBranch == null) {
        println("Failed to get project id, job token, pipeline id or branch.")
        return@publishing
    }

    publications.create<MavenPublication>("library") {
        // A tag is a release and is published under its own name; a branch push is
        // <version>-<branch>-<pipeline>, where the branch is the channel beta servers follow.
        version = ciTag ?: "${project.version}-${ciBranch}-${ciPipelineId}"
        artifactId = "lobby-server"
        artifact(tasks.shadowJar)
    }

    repositories {
        maven {
            name = "GitLab"
            url = uri("https://gitlab.com/api/v4/projects/${ciProjectId}/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = ciJobToken
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}