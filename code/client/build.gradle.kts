import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.gradleup.shadow") version "9.2.0"
    id("maven-publish")
    java
    application
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("jvm") version "2.2.21"
}

group = "darkan"
version = providers.gradleProperty("releaseVersion").orElse("2.0.0").get()
description = "Darkan Client"

application {
    mainClass.set("com.darkan.bot.BotKt")
    applicationDefaultJvmArgs = listOf("-XX:+UseZGC", "-Xmx4g", "-Xms1g", "--add-opens=java.desktop/java.awt=ALL-UNNAMED", "--add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED")
}

tasks.named<JavaExec>("run") {
    val forwarded = listOf("bot.", "render.", "toolkit", "autologin", "window.", "prod")
    System.getProperties().stringPropertyNames()
        .filter { name -> forwarded.any { name.startsWith(it) } }
        .forEach { systemProperty(it, System.getProperty(it)) }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17)
	}
}

repositories {
    mavenLocal()
    mavenCentral()
}

val lwjglVersion = "3.3.3"
val hostLwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else if (arch.startsWith("ppc"))
                "natives-linux-ppc64le"
            else if (arch.startsWith("riscv"))
                "natives-linux-riscv64"
            else
                "natives-linux"
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64"))
                "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            else
                "natives-windows-x86"
        else -> throw Error("Unrecognized or unsupported platform: $name")
    }
}

val bundleAllNatives = (providers.gradleProperty("allNatives").orNull ?: System.getenv("CI"))?.toBoolean() ?: false
val lwjglNativesList = if (bundleAllNatives)
    listOf("natives-linux", "natives-linux-arm64", "natives-macos", "natives-macos-arm64", "natives-windows", "natives-windows-arm64")
else
    listOf(hostLwjglNatives)

val gitCommit: String = System.getenv("CI_COMMIT_SHORT_SHA")
    ?: runCatching {
        providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }.standardOutput.asText.get().trim()
    }.getOrDefault("unknown")

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.github.classgraph:classgraph:4.8.177")
    implementation("com.github.weisj:darklaf-core:3.0.2")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.8.0.202609011348-r")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.17")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-stb")
    lwjglNativesList.forEach { natives ->
        runtimeOnly("org.lwjgl", "lwjgl", classifier = natives)
        runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = natives)
        runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = natives)
        runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = natives)
    }

    implementation("org.joml:joml:1.10.5")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.2.21")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    systemProperty("java.io.tmpdir", temporaryDir)
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

    val manifestAttributes = mapOf(
        "Main-Class" to "com.darkan.bot.BotKt",
        "Implementation-Title" to "Darkan client",
        "Implementation-Version" to project.version.toString(),
        "Git-Commit" to gitCommit,
        "Add-Opens" to "java.desktop/java.awt",
        "Add-Exports" to "java.desktop/sun.awt.X11",
    )

    jar {
        manifest { attributes(manifestAttributes) }
    }

    shadowJar {
        archiveBaseName.set("darkan-client-shaded")
        manifest { attributes(manifestAttributes) }
    }
}

tasks {
	build {
		dependsOn(shadowJar)
	}
}

tasks.register<JavaExec>("runRenderTest") {
    group = "application"
    description = "Run the RuneTek5 Rendering Engine Test Application"
    mainClass.set("com.darkan.rendertest.RenderingTestAppKt")
    classpath = sourceSets["main"].runtimeClasspath

}
