plugins {
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

val centralPortalUsername = providers.gradleProperty("centralPortalUsername")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_USERNAME")).getOrElse("")
val centralPortalPassword = providers.gradleProperty("centralPortalPassword")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_PASSWORD")).getOrElse("")

nmcpSettings {
    centralPortal {
        username = centralPortalUsername
        password = centralPortalPassword
        publishingType = "USER_MANAGED"
        publicationName = "top.skyeyefast:mcr-mahjong:0.1.0"
    }
}

rootProject.name = "mcr-mahjong"
