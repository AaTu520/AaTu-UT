package cn.blockforge.aatu_ut.block;

import cn.blockforge.aatu_ut.menu.DismantlingMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class DismantlingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DismantlingBlock> CODEC = simpleCodec(DismantlingBlock::new);
    public DismantlingBlock(Properties properties) {
        super(properties);
        // The unrotated top faces west; this keeps blocks from older worlds unchanged.
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.WEST));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public MapCodec<DismantlingBlock> codec() { return CODEC; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) server.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new DismantlingMenu(id, inventory, pos), Component.translatable("block.aatu_ut.dismantling_table")), data -> data.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
