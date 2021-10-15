package com.willfp.eco.proxy.v1_16_R3

import com.willfp.eco.core.display.Display
import com.willfp.eco.proxy.ChatComponentProxy
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.v1_16_R3.IChatBaseComponent
import net.minecraft.server.v1_16_R3.MojangsonParser
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.entity.Player

@Suppress("UNCHECKED_CAST")
class ChatComponent : ChatComponentProxy {
    private val gsonComponentSerializer = GsonComponentSerializer.gson()

    override fun modifyComponent(obj: Any, player: Player): Any {
        if (obj !is IChatBaseComponent) {
            return obj
        }

        val component = gsonComponentSerializer.deserialize(
            IChatBaseComponent.ChatSerializer.a(
                obj
            )
        ).asComponent() as BuildableComponent<*, *>

        val newComponent = modifyBaseComponent(component, player)

        return IChatBaseComponent.ChatSerializer.a(
            gsonComponentSerializer.serialize(newComponent.asComponent())
        ) ?: obj
    }

    private fun modifyBaseComponent(baseComponent: Component, player: Player): Component {
        var component = baseComponent

        if (component is TranslatableComponent) {
            val args = mutableListOf<Component>()
            for (arg in component.args()) {
                args.add(modifyBaseComponent(arg, player))
            }
            component = component.args(args)
        }

        val children = mutableListOf<Component>()
        for (child in component.children()) {
            children.add(modifyBaseComponent(child, player))
        }
        component = component.children(children)

        val hoverEvent: HoverEvent<Any> = component.style().hoverEvent() as HoverEvent<Any>? ?: return component

        val showItem = hoverEvent.value()

        if (showItem !is HoverEvent.ShowItem) {
            return component
        }

        val newShowItem = showItem.nbt(
            BinaryTagHolder.of(
                CraftItemStack.asNMSCopy(
                    Display.display(
                        CraftItemStack.asBukkitCopy(
                            CraftItemStack.asNMSCopy(
                                org.bukkit.inventory.ItemStack(
                                    Material.matchMaterial(
                                        showItem.item()
                                            .toString()
                                    ) ?: return component,
                                    showItem.count()
                                )
                            ).apply {
                                this.tag = MojangsonParser.parse(
                                    showItem.nbt()?.string() ?: return component
                                ) ?: return component
                            }
                        ),
                        player
                    )
                ).orCreateTag.toString()
            )
        )

        val newHover = hoverEvent.value(newShowItem)
        val style = component.style().hoverEvent(newHover)
        return component.style(style)
    }
}