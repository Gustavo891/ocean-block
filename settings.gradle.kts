enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "one-block"

include(
    ":plugin",
    ":lib:connection:",
    ":lib:coordinate:",
    ":lib:formatter",
    ":lib:instance:",
    ":lib:item-builder:",
    ":lib:translation",

    ":system:special-items:",
    ":system:arena:",
    ":system:auction-house:",
    ":system:chats:",
    ":system:coinflip:",
    ":system:combat-log:",
    ":system:currencies:",
    ":system:islands:",
    ":system:minions:",
    ":system:playtime:",
    ":system:sell:",
    ":system:shop:",
    ":system:starter-kit:",
    ":system:tops:",
    ":system:worlds:",
)