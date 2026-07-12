package net.crewco.mythos.titanomachy

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * The three gifts of the Cyclopes. These are the objects the rest of Greek myth is
 * *about*, so they're built to outlive this addon: a PDC tag and an owning role id,
 * which any future addon can read without importing anything from here.
 *
 * A power that needs one checks `Regalia.held(player, THUNDERBOLT, context)`. Holding
 * the item without the role does nothing; holding the role without the item does
 * nothing. You need both, which is the entire point of the war.
 */
object Regalia {

    const val THUNDERBOLT = "thunderbolt"
    const val TRIDENT = "trident"
    const val HELM = "helm"

    /** Which brother each gift is for. */
    val OWNERS = mapOf(THUNDERBOLT to "zeus", TRIDENT to "poseidon", HELM to "hades")

    private fun key(context: AddonContext) = NamespacedKey(context.plugin, "regalia")

    fun item(kind: String, context: AddonContext): ItemStack {
        val material = when (kind) {
            THUNDERBOLT -> Material.BLAZE_ROD
            TRIDENT -> Material.TRIDENT
            else -> Material.NETHERITE_HELMET
        }
        val name = when (kind) {
            THUNDERBOLT -> "<yellow><b>The Thunderbolt"
            TRIDENT -> "<aqua><b>The Trident"
            else -> "<dark_gray><b>The Helm of Darkness"
        }
        val lore = when (kind) {
            THUNDERBOLT -> listOf("<dark_gray><i>Brontes made the sound.", "<dark_gray><i>Steropes made the flash.", "<dark_gray><i>Arges made it bright.")
            TRIDENT -> listOf("<dark_gray><i>It splits rock as easily as water.", "<dark_gray><i>The horses came out of a crack it made.")
            else -> listOf("<dark_gray><i>Not invisibility. Something worse.", "<dark_gray><i>You are simply not there to be looked at.")
        }
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(mm(name))
                meta.lore(lore.map { mm(it) } + listOf(mm(""), mm("<gray>Only ${OWNERS[kind]?.replaceFirstChar(Char::uppercase)} may wield it.")))
                meta.isUnbreakable = true
                meta.persistentDataContainer.set(key(context), PersistentDataType.STRING, kind)
            }
        }
    }

    fun kindOf(item: ItemStack?, context: AddonContext): String? =
        item?.itemMeta?.persistentDataContainer?.get(key(context), PersistentDataType.STRING)

    /** True if this player is holding (main hand, or wearing, for the helm) the gift. */
    fun held(player: org.bukkit.entity.Player, kind: String, context: AddonContext): Boolean {
        if (kindOf(player.inventory.itemInMainHand, context) == kind) return true
        if (kind == HELM && kindOf(player.inventory.helmet, context) == kind) return true
        return false
    }

    fun emetic(context: AddonContext): ItemStack = ItemStack(Material.POTION).apply {
        editMeta { meta ->
            meta.displayName(mm("<dark_green><b>The Emetic Draught"))
            meta.lore(
                listOf(
                    mm("<dark_gray><i>Metis mixed it. She is cleverer than all of you"),
                    mm("<dark_gray><i>and it will not save her."),
                    mm(""),
                    mm("<gray>Right-click Kronos with it."),
                ),
            )
            meta.persistentDataContainer.set(NamespacedKey(context.plugin, "emetic"), PersistentDataType.BYTE, 1)
        }
    }

    fun isEmetic(item: ItemStack?, context: AddonContext): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(NamespacedKey(context.plugin, "emetic"), PersistentDataType.BYTE) == true

    fun stone(context: AddonContext): ItemStack = ItemStack(Material.STONE).apply {
        editMeta { meta ->
            meta.displayName(mm("<gray><b>A Swaddled Stone"))
            meta.lore(
                listOf(
                    mm("<dark_gray><i>Wrapped carefully, and handed over"),
                    mm("<dark_gray><i>by a woman who is not crying."),
                ),
            )
            meta.persistentDataContainer.set(NamespacedKey(context.plugin, "swaddled"), PersistentDataType.BYTE, 1)
        }
    }

    fun isStone(item: ItemStack?, context: AddonContext): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(NamespacedKey(context.plugin, "swaddled"), PersistentDataType.BYTE) == true
}
