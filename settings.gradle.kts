pluginManagement {
    repositories {
        // Finix Nexus mirror (Maven Central is blocked on Finix machines). Only added when the
        // NEXUS_TOKEN_* env vars are set, so external users fall back to the public repos below.
        if (!System.getenv("NEXUS_TOKEN_USER").isNullOrBlank()) {
            maven {
                url = uri("https://finix.repo.sonatype.app/repository/finix-maven/")
                credentials {
                    username = System.getenv("NEXUS_TOKEN_USER")
                    password = System.getenv("NEXUS_TOKEN_PASSWORD")
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Finix Nexus mirror (Maven Central is blocked on Finix machines). Only added when the
        // NEXUS_TOKEN_* env vars are set, so external users fall back to the public repos below.
        if (!System.getenv("NEXUS_TOKEN_USER").isNullOrBlank()) {
            maven {
                url = uri("https://finix.repo.sonatype.app/repository/finix-maven/")
                credentials {
                    username = System.getenv("NEXUS_TOKEN_USER")
                    password = System.getenv("NEXUS_TOKEN_PASSWORD")
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        google()
        mavenCentral()
        maven {
            name = "CentralPortalSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            // Only look here for snapshots (and optionally for a specific group)
            mavenContent {
                snapshotsOnly()
                includeGroup("com.finix")   // optional but recommended
            }
        }
    }
}

rootProject.name = "mposSampleApplication"
include(":app")
