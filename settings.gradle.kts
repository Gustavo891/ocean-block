enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "one-block"

include(
    ":plugins:oneblock:plugin",
    ":plugins:textura",

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

    ":plugins:oneblock:system:custom-items",
    ":plugins:oneblock:system:auction-house",
    ":plugins:oneblock:system:coinflip",
    ":plugins:oneblock:system:collections",
    ":plugins:oneblock:system:craft",
    ":plugins:oneblock:system:currencies",
    ":plugins:oneblock:system:enchants",
    ":plugins:oneblock:system:islands",
    ":plugins:oneblock:system:trader",
    ":plugins:oneblock:system:battle-pass",
    ":plugins:oneblock:system:playtime",
    ":plugins:oneblock:system:sell",
    ":plugins:oneblock:system:shop",
    ":plugins:oneblock:system:scoreboard",
    ":plugins:oneblock:system:skills",
    ":plugins:oneblock:system:spawn",
    ":plugins:oneblock:system:starter-kit",
    ":plugins:oneblock:system:tops",
    ":plugins:oneblock:system:worlds",
)
