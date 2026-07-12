package net.crewco.mythos.titanomachy

import net.crewco.mythos.addon.AddonBase
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.ERA
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.IMPRISONED
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.SWALLOWED
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Addon #2 — and the proof the architecture works.
 *
 * It never imports EraOfCreation. It doesn't check whether it's installed. All it
 * does is declare that an era called "titanomachy" exists and that something called
 * "kronos" had better be registered by the time anyone needs it — and if nobody has,
 * it registers its own. Delete the Creation jar and this still runs, standalone,
 * from `/mythos advance titanomachy`.
 *
 * The one thing it takes from upstream: a single `EraAdvancedEvent`.
 */
class TitanomachyAddon : AddonBase() {

    override fun onEnable() {
        val mythos = Mythos.from(context)
        val config = loadConfig()

        val content = TitanomachyContent(mythos)
        val places = Places(context, mythos, config)
        val war = WarState(File(context.dataFolder, "war.yml"))

        mythos.eras.register(content.era)

        content.olympians.forEach(mythos.roles::register)
        content.cyclopes.forEach(mythos.roles::register)
        content.hekatoncheires.forEach(mythos.roles::register)
        content.armies.forEach(mythos.roles::register)

        mythos.powers.register(BearPower(mythos, context, content))
        mythos.powers.register(DevourPower(mythos, context, content, places))
        mythos.powers.register(StonePower(mythos, context, content, places))
        mythos.powers.register(DraughtPower(mythos, context))
        mythos.powers.register(FreePower(mythos, context, content, places))
        mythos.powers.register(ForgePower(mythos, context, war))
        mythos.powers.register(ThunderboltPower(mythos, context))
        mythos.powers.register(QuakePower(context))
        mythos.powers.register(UnseenPower(context))
        mythos.powers.register(HundredfoldPower(context))

        context.registerListener(
            TitanomachyListener(
                mythos, context, content, places, war,
                killsToEnd = config.getInt("war.kills-to-end", 200),
                announceEvery = config.getInt("war.announce-every", 25).coerceAtLeast(1),
            ),
        )

        // One tick later every other addon has registered its cast, so "does Kronos
        // already exist?" finally has a truthful answer. This is the soft-dependency
        // pattern — use it any time an addon wants to *extend* a myth it doesn't own.
        context.schedulers.globalDelayed(1) {
            content.registerFallbacks()
            if (mythos.eras.currentId() == ERA) {
                content.grantWarPowers()
                // Put everyone back in the hole they were in when the server stopped.
                Bukkit.getOnlinePlayers().forEach { player ->
                    val profile = mythos.profiles.profile(player.uniqueId)
                    when {
                        profile.hasFlag(SWALLOWED) -> places.leash(player, places.stomach(), SWALLOWED)
                        profile.hasFlag(IMPRISONED) -> places.leash(player, places.tartarus(), IMPRISONED)
                    }
                }
            }
        }

        context.logger.info("The Titanomachy is ready. It does not know who ended the last age, and does not need to.")
    }

    private fun loadConfig(): YamlConfiguration {
        val file = File(context.dataFolder, "config.yml")
        if (!file.exists()) {
            javaClass.getResourceAsStream("/config.yml")?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return YamlConfiguration.loadConfiguration(file)
    }
}
