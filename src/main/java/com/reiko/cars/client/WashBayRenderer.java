package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.fire.Firefighting;
import com.reiko.cars.wash.PressureWasherItem;
import com.reiko.cars.wash.WashBayBlock;
import com.reiko.cars.wash.WashBayBlockEntity;
import com.reiko.cars.wash.WashBayService;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Wash Bay's hoses: one from its reel to each player nearby holding a lance that draws from it, and the jet of water
 * from each one spraying (drawn as the fire hoses are: see {@link FireHoseLines}).
 */
public final class WashBayRenderer implements BlockEntityRenderer<WashBayBlockEntity> {
    private static final float[] BLACK_HOSE = {0.17F, 0.17F, 0.19F};

    public WashBayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WashBayBlockEntity bay, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Level level = bay.getLevel();
        if (level == null || !(bay.getBlockState().getBlock() instanceof WashBayBlock)) {
            return;
        }
        double reach = CarsConfig.get(CarsConfig.LANCE_REACH) + 4.0;
        Vec3 origin = Vec3.atLowerCornerOf(bay.getBlockPos());
        Vec3 outlet = WashBayBlock.outlet(bay.getBlockState(), bay.getBlockPos());
        for (Player player : level.players()) {
            if (player.isSpectator() || !holdsLance(player) || player.distanceToSqr(outlet) > reach * reach || WashBayService.bayFor(player) != bay) {
                continue;
            }
            Vec3 hand = FireHoseLines.hand(player, partialTick);
            FireHoseLines.hose(poseStack, buffers, origin, outlet, hand, FireHoseLines.light(level, outlet), FireHoseLines.light(level, hand), BLACK_HOSE);
            if (HoseSound.spraying(player)) {
                FireHoseLines.jet(poseStack, buffers, origin, Firefighting.trace(level, player, partialTick), hand);
            }
        }
    }

    private static boolean holdsLance(Player player) {
        return player.getMainHandItem().getItem() instanceof PressureWasherItem || player.getOffhandItem().getItem() instanceof PressureWasherItem;
    }

    @Override
    public boolean shouldRenderOffScreen(WashBayBlockEntity bay) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(WashBayBlockEntity bay) {
        return new AABB(bay.getBlockPos()).inflate(32.0);
    }
}
