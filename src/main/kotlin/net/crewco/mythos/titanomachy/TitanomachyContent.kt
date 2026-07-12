package net.crewco.mythos.titanomachy

import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.era.EraDefinition
import net.crewco.mythos.api.era.Objective
import net.crewco.mythos.api.role.ClaimResult
import net.crewco.mythos.api.role.ClaimRule
import net.crewco.mythos.api.role.ClaimRules
import net.crewco.mythos.api.role.RoleDefinition
import net.crewco.mythos.api.role.RoleTier
import net.crewco.mythos.api.role.Succession

/**
 * The cast of the war.
 *
 * Three different ways into this era, deliberately:
 *
 *  - **Olympians are borne by Rhea** (`/power bear`) — six seats, and a player
 *    decides who gets them. Same gate as Gaia's Titans, one age earlier.
 *  - **Cyclopes and Hekatoncheires are claimed** — but claiming one puts you at the
 *    bottom of Tartarus with no way out except an Olympian coming to get you. It is
 *    a role you take *knowing* it starts in a hole. Some players love that.
 *  - **The sworn are open to everyone.** Sixty seats a side, no gates, and they die
 *    constantly. On a hundred-player server this is where ninety of them live: not
 *    spirits, not gods — an army.
 */
class TitanomachyContent(private val mythos: Mythos) {

    /** You do not claim an Olympian. Rhea bears you, or Kronos eats you. */
    private val bornNotClaimed = ClaimRule { _, _ ->
        ClaimResult.Deny("Olympians are not claimed. Rhea bears them — and Kronos is watching her.")
    }

    /** The armies don't exist until the swallowed are back on their feet. */
    private val onlyOnceWarBegins = ClaimRule { _, _ ->
        if (mythos.eras.isComplete(ERA, "the_disgorging")) ClaimResult.Allow
        else ClaimResult.Deny("There is no war yet. There is only a father, and his stomach.")
    }

    val era = EraDefinition(
        id = ERA,
        displayName = "The Titanomachy",
        order = 1,
        next = "olympian-order",
        subtitle = "ten years, and no ground left flat",
        lore = listOf(
            "Kronos took the sickle to his father, and learned exactly one lesson from it:",
            "that a son does this. So he ate them, as they came. All but one.",
        ),
        objectives = listOf(
            Objective("the_swallowing", "Kronos devours his children"),
            Objective("the_stone", "A stone is swaddled and swallowed in a child's place", hidden = true),
            Objective("the_disgorging", "The swallowed are brought back up"),
            Objective("the_cyclopes", "The Cyclopes are freed, and they are grateful", optional = true),
            Objective("ten_years", "Ten years of war"),
            Objective("kronos_bound", "Kronos is thrown into Tartarus"),
        ),
    )

    // ---- the six ------------------------------------------------------------

    private fun olympian(id: String, name: String, domains: List<String>, lore: String, powers: List<String> = emptyList()) =
        RoleDefinition(
            id = id,
            displayName = name,
            tier = RoleTier.OLYMPIAN,
            era = ERA,
            domains = domains,
            color = "<yellow>",
            lore = listOf(lore),
            powers = powers,
            claimRules = listOf(bornNotClaimed),
            succession = Succession.QUEUE,
        )

    val olympians = listOf(
        olympian(
            "zeus", "Zeus", listOf("sky", "oath", "kingship"),
            "The one she hid. You owe a goat, a cave and a rock absolutely everything.",
            powers = listOf("draught", "free", "thunderbolt"),
        ),
        olympian("hera", "Hera", listOf("marriage", "sovereignty"), "Swallowed, disgorged, and never once forgiving."),
        olympian(
            "poseidon", "Poseidon", listOf("sea", "earthquake", "horses"),
            "The sea is not calm and neither are you.", powers = listOf("quake"),
        ),
        olympian(
            "hades", "Hades", listOf("the dead", "wealth"),
            "You will draw the worst lot, and rule it better than either of them rules theirs.",
            powers = listOf("unseen"),
        ),
        olympian("demeter", "Demeter", listOf("grain", "the seasons"), "Everything grows because you allow it. One day you will stop allowing it."),
        olympian("hestia", "Hestia", listOf("the hearth", "the centre"), "First swallowed, last brought up. The fire in the middle of every house."),
    )

    // ---- the things at the bottom of the pit --------------------------------

    private fun prisoner(id: String, name: String, lore: String, powers: List<String>) = RoleDefinition(
        id = id,
        displayName = name,
        tier = RoleTier.MONSTER,
        era = ERA,
        domains = listOf("the forge", "the pit"),
        color = "<dark_aqua>",
        lore = listOf(lore, "You begin at the bottom of Tartarus. Someone will have to come down for you."),
        powers = powers,
        claimRules = listOf(ClaimRules.duringEra(ERA)),
    )

    val cyclopes = listOf(
        prisoner("brontes", "Brontes", "Thunder. You make it; you don't throw it.", listOf("forge")),
        prisoner("steropes", "Steropes", "Lightning. Uranus buried you for being useful.", listOf("forge")),
        prisoner("arges", "Arges", "Brightness. Three brothers, one eye each, and a grudge.", listOf("forge")),
    )

    val hekatoncheires = listOf(
        prisoner("cottus", "Cottus", "A hundred hands. Fifty heads. Nobody has ever been glad to see you.", listOf("hundredfold")),
        prisoner("briareos", "Briareos", "The strong one. Even the gods will call you up when they're frightened.", listOf("hundredfold")),
        prisoner("gyges", "Gyges", "You throw mountains. That is the whole of it.", listOf("hundredfold")),
    )

    // ---- everyone else ------------------------------------------------------

    private fun sworn(id: String, name: String, color: String, lore: String) = RoleDefinition(
        id = id,
        displayName = name,
        tier = RoleTier.MORTAL,
        era = ERA,
        domains = listOf("the war"),
        maxHolders = 60,
        color = color,
        lore = listOf(lore, "You will die. Probably more than once. The war does not notice."),
        claimRules = listOf(onlyOnceWarBegins, ClaimRules.duringEra(ERA)),
    )

    val armies = listOf(
        sworn("titan-sworn", "Titan-sworn", "<gold>", "The old order. It fed you, and it was not kind, and it was yours."),
        sworn("olympian-sworn", "Olympian-sworn", "<yellow>", "The new gods promise better. They are lying, but they are younger."),
    )

    /**
     * SOFT DEPENDENCY.
     *
     * Kronos and Rhea belong to EraOfCreation. This addon doesn't depend on it and
     * doesn't import it — so if that jar isn't in the folder, we register our own
     * copies, and the Titanomachy still runs standalone from `/mythos advance titanomachy`.
     *
     * Called one tick after enable, when every other addon has finished registering,
     * so "does kronos already exist?" has a truthful answer.
     */
    fun registerFallbacks() {
        if (mythos.roles.definition("kronos") == null) {
            mythos.roles.register(
                RoleDefinition(
                    id = "kronos", displayName = "Kronos", tier = RoleTier.TITAN, era = ERA,
                    domains = listOf("time", "harvest"), color = "<gold>",
                    lore = listOf("You cut down your father. You already know what your children are for."),
                    powers = listOf("devour"), claimRules = listOf(ClaimRules.duringEra(ERA)),
                ),
            )
        }
        if (mythos.roles.definition("rhea") == null) {
            mythos.roles.register(
                RoleDefinition(
                    id = "rhea", displayName = "Rhea", tier = RoleTier.TITAN, era = ERA,
                    domains = listOf("flow", "motherhood"), color = "<gold>",
                    lore = listOf("Six times you carry a child to term and watch him swallow it. The sixth time, you lie."),
                    powers = listOf("bear", "stone"), claimRules = listOf(ClaimRules.duringEra(ERA)),
                ),
            )
        }
    }

    /**
     * Kronos and Rhea already exist (Creation registered them) but with no powers —
     * they were just Titans then. The war is what gives them something to do, so we
     * re-register them with the same ids and the powers this era grants.
     */
    fun grantWarPowers() {
        mythos.roles.definition("kronos")?.let {
            mythos.roles.register(it.copy(powers = (it.powers + "devour").distinct()))
        }
        mythos.roles.definition("rhea")?.let {
            mythos.roles.register(it.copy(powers = (it.powers + listOf("bear", "stone")).distinct()))
        }
    }

    companion object {
        const val ERA = "titanomachy"
        const val SWALLOWED = "titanomachy.swallowed"
        const val HIDDEN = "titanomachy.hidden"
        const val IMPRISONED = "titanomachy.imprisoned"
    }
}
