package tech.thatgravyboat.craftify.platform

import gg.essential.universal.UKeyboard
import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.fml.client.registry.ClientRegistry

typealias MCKeyBinding = net.minecraft.client.settings.KeyBinding
typealias MCEscMenu = net.minecraft.client.gui.GuiIngameMenu
typealias MCInventoryMenu = net.minecraft.client.gui.inventory.GuiInventory
typealias MCChatMenu = net.minecraft.client.gui.GuiChat

fun registerKeybinding(name: String, category: String, type: UKeybind.Type, code: Int): MCKeyBinding {
    val bind = MCKeyBinding(name, if (type == UKeybind.Type.MOUSE) code - 100 else code, category)
    ClientRegistry.registerKeyBinding(bind)
    return bind
}

fun isPressed(bind: UKeybind): Boolean {
    return bind.getBinding().keyCode != UKeyboard.KEY_NONE && bind.getBinding().isPressed
}

fun runOnMcThread(block: () -> Unit) {
    Minecraft.getMinecraft().addScheduledTask(block)
}

fun isGuiHidden(): Boolean {
    return !Minecraft.isGuiEnabled()
}

fun registerCommand(name: String, commands: Map<String, Runnable>) {
    ClientCommandHandler.instance.registerCommand(SimpleCommand(
        name,
        { commands[""]?.run() },
        commands.filter { it.key != "" }
    ))
}

class SimpleCommand(private val name: String, private val default: Runnable, private val commands: Map<String, Runnable>): CommandBase() {

    override fun getCommandName() = name

    override fun getCommandUsage(sender: ICommandSender) = "/$name"

    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            default.run()
        } else {
            commands[args[0]]?.run()
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender) = true

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<out String>, pos: BlockPos): MutableList<String> {
        if (args.size == 1) {
            val arg = args[0]
            return commands.keys.filter { it.startsWith(arg) }.toMutableList()
        } else if (args.isEmpty()) {
            return commands.keys.toMutableList()
        }
        return mutableListOf()
    }
}
