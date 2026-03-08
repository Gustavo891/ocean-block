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
    ":system:craft:",
    ":system:currencies:",
    ":system:fly-timer:",
    ":system:islands:",
    ":system:trader",
    ":system:minions:",
    ":system:mob-drops:",
    ":system:offhand-pets:",
    ":system:playtime:",
    ":system:sell:",
    ":system:shop:",
    ":system:starter-kit:",
    ":system:tops:",
    ":system:worlds:",
    ":system:xp-bottle:",
)