import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    dependencies {
        classpath(libs.kotlin)
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }

    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    group = "io.github.hipporasy"
    version = "0.0.1"
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "GithubPackages"
                description = "Reachability library for Android and iOS"
                url = uri("https://maven.pkg.github.com/hipporasy/reachability")
                credentials {
                    username = gradleLocalProperties(rootDir).getProperty("githubUsername")
                    password = gradleLocalProperties(rootDir).getProperty("githubPassword")
                }
            }
        }
        publications {
            withType<MavenPublication> {
                pom {
                    name.set("Reachability")
                    description.set("Reachability library for Android and iOS")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }
                    url.set("https://github.com/hipporasy/reachability")
                    issueManagement {
                        system.set("Github")
                        url.set("https://github.com/hipporasy/reachability/issues")
                    }
                    scm {
                        connection.set("https://github.com/hipporasy/reachability.git")
                        url.set("https://github.com/hipporasy/reachability")
                    }
                    developers {
                        developer {
                            name.set("Marasy Phi")
                            email.set("hipporasy@gmail.com")
                        }
                    }
                }
            }
        }
    }

    val publishing = extensions.getByType<PublishingExtension>()
    extensions.configure<SigningExtension> {
        useInMemoryPgpKeys(
            gradleLocalProperties(rootDir).getProperty("gpgKeySecret"),
            gradleLocalProperties(rootDir).getProperty("gpgKeyPassword"),
        )
        sign(publishing.publications)
    }

    project.tasks.withType(AbstractPublishToMaven::class.java).configureEach {
        dependsOn(project.tasks.withType(Sign::class.java))
    }
}
