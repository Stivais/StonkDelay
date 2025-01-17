package com.stonkdelay.features

import com.stonkdelay.*
import com.stonkdelay.StonkDelay.Companion.mc
import com.stonkdelay.events.BlockBreakEvent
import com.stonkdelay.events.BlockChangeEvent
import com.stonkdelay.events.BlockPlaceEvent
import com.stonkdelay.events.ChunkUpdateEvent
import com.stonkdelay.utils.Location
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.util.BlockPos
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object Delay {
    private val blocks = mutableMapOf<BlockPos, BlockData>()

    // Add chests to block map
    @SubscribeEvent
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!isChestEnabled()) return
        val block = event.stack?.item as? ItemBlock ?: return
        if (block.block != Blocks.chest || blocks.containsKey(event.pos)) return
        val state = mc.theWorld.getBlockState(event.pos)
        blocks[event.pos] = BlockData(
            state,
            System.currentTimeMillis(),
            queued = false,
            placed = true
        )
    }

    // Adds mined block to block map
    @SubscribeEvent
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isEnabled()) return
        val state = mc.theWorld.getBlockState(event.pos)
        blocks[event.pos] = BlockData(
            state,
            System.currentTimeMillis(),
            queued = false,
            placed = false
        )
    }

    // Queues and cancels blocks changed from PacketBlockChange and PacketMultiBlockChange
    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (!isEnabled()) return
        blocks[event.pos]?.let {
            it.state = event.state
            it.queued = true
            event.isCanceled = true
        }
    }

    // Cancel block changes from PacketChunkData
    @SubscribeEvent
    fun onChunkUpdate(event: ChunkUpdateEvent) {
        if (!isEnabled()) return
        val minX = event.packet.chunkX shl 4
        val minZ = event.packet.chunkZ shl 4
        val maxX = minX + 15
        val maxZ = minZ + 15

        blocks.forEach {
            if (it.key.x in minX..maxX && it.key.z in minZ..maxZ) {
                it.value.state = mc.theWorld.getBlockState(it.key)
                it.value.queued = true
                mc.theWorld.setBlockState(it.key, Blocks.air.defaultState)
            }
        }
    }

    // Resets expired queued blocks
    @SubscribeEvent
    fun onTick(event: TickEvent) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START) return
        val currentTime = System.currentTimeMillis()
        blocks.keys.removeAll {
            val blockData = blocks[it]!!
            val timeExisted = currentTime - blockData.time
            val shouldResetBlock = blockData.queued
                    && timeExisted >= StonkDelay.config.settings.delay
            if (shouldResetBlock) {
                mc.theWorld.setBlockState(it, blockData.state)
            }
            shouldResetBlock || (timeExisted >= 60000)
        }
    }

    // Stops tracking blocks when player right-clicks on adjacent block faces.
    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!isEnabled() || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return
        val affectedPos = event.pos.offset(event.face) ?: return
        if (blocks[affectedPos]?.placed == false) {
            blocks.remove(affectedPos)
        }
    }

    // Resets block list when world changes.
    @SubscribeEvent
    fun onWorldUnload(ignored: WorldEvent.Unload) {
        blocks.clear()
    }

    fun resetAll() {
        blocks.forEach { (pos, data) ->
            mc.theWorld.setBlockState(pos, data.state)
        }
        blocks.clear()
    }

    private fun isEnabled() = Location.inSkyblock && StonkDelay.config.settings.enabled
    private fun isChestEnabled() = isEnabled() && StonkDelay.config.settings.chestEnabled

    data class BlockData(var state: IBlockState, val time: Long, var queued: Boolean, var placed: Boolean)
}
