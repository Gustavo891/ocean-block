enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "ocean-block"

include(
    ":plugins:oceanblock:plugin",
    ":plugins:textura",
    ":tools:textura-studio",

    ":lib:actionbar",
    ":lib:connection",
    ":lib:coordinate",
    ":lib:formatter",
    ":lib:instance",
    ":lib:item-builder",
    ":lib:rewards",
    ":lib:scoreboard",
    ":lib:toast",
    ":lib:translation",

    ":plugins:oceanblock:system:custom-items",
    ":plugins:oceanblock:system:auction-house",
    ":plugins:oceanblock:system:coinflip",
    ":plugins:oceanblock:system:collections",
    ":plugins:oceanblock:system:craft",
    ":plugins:oceanblock:system:drift",
    ":plugins:oceanblock:system:currencies",
    ":plugins:oceanblock:system:enchants",
    ":plugins:oceanblock:system:islands",
    ":plugins:oceanblock:system:trader",
    ":plugins:oceanblock:system:battle-pass",
    ":plugins:oceanblock:system:playtime",
    ":plugins:oceanblock:system:sell",
    ":plugins:oceanblock:system:shop",
    ":plugins:oceanblock:system:scoreboard",
    ":plugins:oceanblock:system:spawn",
    ":plugins:oceanblock:system:starter-kit",
    ":plugins:oceanblock:system:tops",
    ":plugins:oceanblock:system:worlds",
)
