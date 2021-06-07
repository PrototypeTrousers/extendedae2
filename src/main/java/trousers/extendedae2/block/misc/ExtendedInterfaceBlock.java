package trousers.extendedae2.block.misc;

import appeng.api.util.IOrientable;
import appeng.block.AEBaseTileBlock;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.util.InteractionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import trousers.extendedae2.container.implementations.ExtendedInterfaceContainer;
import trousers.extendedae2.tile.misc.ExtendedInterfaceTileEntity;

import javax.annotation.Nullable;

public class ExtendedInterfaceBlock extends AEBaseTileBlock<ExtendedInterfaceTileEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final BooleanProperty OMNIDIRECTIONAL = BooleanProperty.create("omnidirectional");

    public ExtendedInterfaceBlock() {
        super(defaultProps(Material.IRON));
        this.setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(OMNIDIRECTIONAL);
        builder.add(FACING);
    }

    @Override
    protected BlockState updateBlockStateFromTileEntity(BlockState currentState, ExtendedInterfaceTileEntity te) {
        return currentState.with(OMNIDIRECTIONAL, te.isOmniDirectional()).with(FACING, te.getForward());
    }

    @Override
    public ActionResultType onActivated(final World w, final BlockPos pos, final PlayerEntity p, final Hand hand,
            final @Nullable ItemStack heldItem, final BlockRayTraceResult hit) {
        if (InteractionUtil.isInAlternateUseMode(p)) {
            return ActionResultType.PASS;
        }
        final ExtendedInterfaceTileEntity tg = this.getTileEntity(w, pos);
        if (tg != null) {
            if (!w.isRemote()) {
                ContainerOpener.openContainer(ExtendedInterfaceContainer.TYPE, p,
                        ContainerLocator.forTileEntitySide(tg, hit.getFace()));
            }
            return ActionResultType.func_233537_a_(w.isRemote());
        }
        return ActionResultType.PASS;
    }

    @Override
    protected boolean hasCustomRotation() {
        return true;
    }

    @Override
    protected void customRotateBlock(final IOrientable rotatable, final Direction axis) {
        if (rotatable instanceof ExtendedInterfaceTileEntity) {
            ((ExtendedInterfaceTileEntity) rotatable).setSide(axis);
        }
    }

}
