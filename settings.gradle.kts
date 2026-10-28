pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
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
