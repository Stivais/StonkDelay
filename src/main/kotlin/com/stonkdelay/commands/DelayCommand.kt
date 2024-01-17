package com.stonkdelay.commands

import com.stonkdelay.StonkDelay
import com.stonkdelay.features.Delay
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.event.ClickEvent
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle

class DelayCommand : CommandBase() {
    override fun getCommandName(): String {
        return "stonkdelay"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "sd"
        )
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "/$commandName <delay in milliseconds|reset>"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<out String>, pos: BlockPos): List<String> {
        return if (args.size == 1) getListOfStringsMatchingLastWord(args, "reset", "chest") else listOf()
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            StonkDelay.config.settings.enabled = !StonkDelay.config.settings.enabled
            StonkDelay.config.save()
            sender.addChatMessage(ChatComponentText(if (StonkDelay.config.settings.enabled) {
                "§aStonkDelay enabled. (delay: §b${StonkDelay.config.settings.delay} §ams)"
            } else {
                "§cStonkDelay disabled."
            }))
        } else if (args.size == 1) {
            when (val arg = args[0]) {
                "reset", "rs" -> {
                    Delay.resetAll()
                    sender.addChatMessage(ChatComponentText("§aMade all blocks reappear"))
                }
                "chest" -> {
                    StonkDelay.config.settings.chestEnabled = !StonkDelay.config.settings.chestEnabled
                    StonkDelay.config.save()
                    sender.addChatMessage(ChatComponentText(if (StonkDelay.config.settings.chestEnabled) {
                        "§aStonkDelay enabled for chests. (delay: §b${StonkDelay.config.settings.delay} §ams)"
                    } else {
                        "§cStonkDelay disabled for chests."
                    }))
                }
                else -> {
                    val delay = arg.toIntOrNull() ?: return sender.addChatMessage(ChatComponentText("§cNot a valid delay!"))
                    StonkDelay.config.settings.delay = delay
                    StonkDelay.config.save()
                    sender.addChatMessage(ChatComponentText("§aDelay set to $delay ms."))
                    if (!StonkDelay.config.settings.enabled) {
                        sender.addChatMessage(ChatComponentText("§e§lWarning: §bStonkDelay is currently disabled. ")
                            .appendSibling(ChatComponentText("§7[§a§lENABLE§7]").setChatStyle(
                                ChatStyle().setChatClickEvent(
                                    ClickEvent(ClickEvent.Action.RUN_COMMAND, "/stonkdelay")
                                )
                            ))
                        )
                    }
                }
            }
        }
    }
}
