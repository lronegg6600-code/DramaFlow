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
    }
}

rootProject.name = "DramaFlow"

include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:player")
include(":core:billing")
include(":feature:feed")
include(":feature:detail")
include(":feature:player")
include(":feature:subscription")
include(":feature:profile")
include(":benchmark")
