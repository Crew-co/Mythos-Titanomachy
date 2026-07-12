package net.crewco.mythos.titanomachy

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Two holes in the world: a stomach and a prison. Plus the leash that keeps people
 * in them.
 *
 * The leash is a per-entity repeating task, which is the only Folia-legal way to do
 * this — a global task can't read another region's player position, and a move
 * listener at 100 players is a bad idea for something that affects six of them.
 */
class Places(
    private val context: AddonContext,
    private val mythos: Mythos,
    config: FileConfiguration,
) {
    private val worldName = config.getString("places.world", "")!!
    private val stomachOffset = config.getIntegerList("places.stomach-offset").ifEmpty { listOf(0, 0) }
    private val tartarusOffset = config.getIntegerList("places.tartarus-offset").ifEmpty { listOf(200, 0) }
    private val leashRadius = config.getDouble("places.leash-radius", 24.0)

    private fun world(): World =
        (if (worldName.isNotEmpty()) Bukkit.getWorld(worldName) else null) ?: Bukkit.getWorlds().first()

    fun stomach(): Location = world().let { w ->
        w.spawnLocation.clone().apply {
            x += stomachOffset[0]
            z += stomachOffset[1]
            y = w.minHeight + 4.0
        }
    }

    fun tartarus(): Location = world().let { w ->
        w.spawnLocation.clone().apply {
            x += tartarusOffset[0]
            z += tartarusOffset[1]
            y = w.minHeight + 4.0
        }
    }

    /** Somewhere very far away, where a father won't look. */
    fun crete(): Location = world().let { w ->
        val angle = Math.random() * Math.PI * 2
        val distance = 1500 + Math.random() * 1500
        val x = w.spawnLocation.x + Math.cos(angle) * distance
        val z = w.spawnLocation.z + Math.sin(angle) * distance
        Location(w, x, w.getHighestBlockYAt(x.toInt(), z.toInt()) + 1.0, z)
    }

    /** Put someone in a hole, and keep them there while [flag] is set on them. */
    fun imprison(player: Player, where: Location, flag: String, message: String) {
        mythos.profiles.profile(player.uniqueId).setFlag(flag, true)

        player.teleportAsync(where).thenRun {
            context.schedulers.entity(player) {
                player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false))
                player.sendMessage(mm(message))
            }
        }
        leash(player, where, flag)
    }

    /** Restart the leash on rejoin, or after a reload. */
    fun leash(player: Player, where: Location, flag: String) {
        context.schedulers.entityRepeating(player, 40, 40, retired = null) { task ->
            val profile = mythos.profiles.profile(player.uniqueId)
            if (!profile.hasFlag(flag) || !player.isOnline) {
                task.cancel()
                return@entityRepeating
            }
            val location = player.location
            val strayed = location.world != where.world || location.distanceSquared(where) > leashRadius * leashRadius
            if (strayed) {
                player.teleportAsync(where)
                player.sendMessage(mm("<dark_gray><i>There is no way out that way."))
            }
        }
    }

    fun release(player: Player, flag: String, to: Location, message: String) {
        mythos.profiles.profile(player.uniqueId).setFlag(flag, null)
        player.teleportAsync(to).thenRun {
            context.schedulers.entity(player) {
                player.removePotionEffect(PotionEffectType.BLINDNESS)
                player.sendMessage(mm(message))
            }
        }
    }

    fun surface(): Location = world().spawnLocation
}
