package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class SplicingTableBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    HexDebugBlockEntities.SPLICING_TABLE.value, pos, state
), ISplicingTable, BaseContainer, MenuProvider {
    override val stacks = BaseContainer.withSize(ISplicingTable.CONTAINER_SIZE)

    var listHolder by ContainerSlotDelegate(ISplicingTable.LIST_INDEX)
    var clipboardHolder by ContainerSlotDelegate(ISplicingTable.CLIPBOARD_INDEX)

    val analogOutputSignal get() = if (!listHolder.isEmpty) 15 else 0

    private val undoStack = mutableListOf<UndoState>()
    private var undoIndex = -1

    override fun load(tag: CompoundTag) {
        super.load(tag)
        ContainerHelper.loadAllItems(tag, stacks)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, stacks)
    }

    override fun createMenu(i: Int, inventory: Inventory, player: Player) = SplicingTableMenu(i, inventory, this)

    override fun getDisplayName() = Component.translatable(blockState.block.descriptionId)

    // FIXME: placeholder
    override fun getClientView() = SplicingTableClientView(
        iotas = List(18) { CompoundTag() },
        clipboard = null,
        isWritable = true,
        isClipboardWritable = false,
    )

    /** Called on the server. */
    override fun runAction(action: Action, selection: Selection?): Selection? {
        HexDebug.LOGGER.info("Got action: $action") // FIXME: remove

        when (action) {
            Action.UNDO -> return applyUndoState(-1, selection)
            Action.REDO -> return applyUndoState(1, selection)
            else -> {}
        }

        if (selection == null) return null

        @Suppress("KotlinConstantConditions")
        return when (action) {
            Action.NUDGE_LEFT -> {

                selection.moveBy(-1)
            }
            Action.NUDGE_RIGHT -> {

                selection.moveBy(1)
            }
            Action.DUPLICATE -> {

                selection.expandRight(selection.size)
            }
            Action.DELETE -> {

                null
            }
            Action.CUT -> {

                null
            }
            Action.COPY -> {

                selection
            }
            Action.PASTE -> {

                Selection.withSize(selection.lastIndex + 1, 1)
            }
            Action.PASTE_SPLAT -> {

                Selection.withSize(selection.lastIndex + 1, 1) // FIXME: replace 1 with size of clipboard
            }
            Action.UNDO, Action.REDO -> throw AssertionError("unreachable")
        }
    }

    private fun applyUndoState(delta: Int, currentSelection: Selection?): Selection? {
        val newIndex = undoIndex + delta
        val state = undoStack.getOrNull(newIndex) ?: return currentSelection
        undoIndex = newIndex
        // FIXME: implement
        return state.selection
    }

    private fun pushUndoState() {}

    data class UndoState(
        val list: List<Iota>,
        val clipboard: List<Iota>?,
        val selection: Selection?,
    )
}
