package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The car hauler's shape, as Blockbench exports {@code blockbench/car_hauler.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_l1, wheel_r1, wheel_l2, wheel_r2). Its texture is {@code textures/entity/trailer/car_hauler.png}. See {@link TrailerModel}
 * for what each bone is for.
 */
public final class CarHaulerModel {
    private CarHaulerModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(488, 125).addBox(-17.0F, -30.0F, -72.0F, 34.0F, 12.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(604, 159).addBox(-17.0F, -44.0F, -52.0F, 34.0F, 14.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(300, 0).addBox(15.0F, -27.0F, -48.0F, 2.0F, 5.0F, 120.0F, CubeDeformation.NONE)
            .texOffs(544, 0).addBox(-17.0F, -27.0F, -48.0F, 2.0F, 5.0F, 120.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(0, 0).addBox(-15.0F, -22.0F, -48.0F, 30.0F, 4.0F, 120.0F, CubeDeformation.NONE)
            .texOffs(0, 124).addBox(15.0F, -18.0F, -48.0F, 2.0F, 4.0F, 120.0F, CubeDeformation.NONE)
            .texOffs(244, 125).addBox(-17.0F, -18.0F, -48.0F, 2.0F, 4.0F, 120.0F, CubeDeformation.NONE)
            .texOffs(688, 230).addBox(15.0F, -33.0F, -30.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(696, 230).addBox(-17.0F, -33.0F, -30.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(704, 230).addBox(15.0F, -33.0F, -6.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(712, 230).addBox(-17.0F, -33.0F, -6.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(736, 231).addBox(15.0F, -33.0F, 18.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(744, 231).addBox(-17.0F, -33.0F, 18.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(640, 232).addBox(15.0F, -33.0F, 42.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(648, 232).addBox(-17.0F, -33.0F, 42.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(578, 233).addBox(15.0F, -33.0F, 66.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(586, 233).addBox(-17.0F, -33.0F, 66.0F, 2.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(788, 0).addBox(15.0F, -34.0F, -48.0F, 2.0F, 1.0F, 116.0F, CubeDeformation.NONE)
            .texOffs(788, 117).addBox(-17.0F, -34.0F, -48.0F, 2.0F, 1.0F, 116.0F, CubeDeformation.NONE)
            .texOffs(640, 205).addBox(6.0F, -46.0F, 69.0F, 8.0F, 24.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(528, 206).addBox(-14.0F, -46.0F, 69.0F, 8.0F, 24.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(708, 209).addBox(-10.0F, -36.0F, -70.0F, 20.0F, 6.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(488, 189).addBox(-10.0F, -19.0F, -64.0F, 20.0F, 1.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(550, 206).addBox(12.0F, -18.0F, -38.0F, 2.0F, 17.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(726, 225).addBox(-14.0F, -18.0F, -38.0F, 2.0F, 17.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(736, 225).addBox(11.0F, -1.0F, -39.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(662, 230).addBox(-15.0F, -1.0F, -39.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(528, 233).addBox(-12.0F, -7.0F, -37.0F, 24.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(604, 125).addBox(6.0F, -18.0F, 36.0F, 4.0F, 4.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(672, 125).addBox(-10.0F, -18.0F, 36.0F, 4.0F, 4.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(768, 209).addBox(10.0F, -14.0F, 67.0F, 8.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(768, 222).addBox(-18.0F, -14.0F, 67.0F, 8.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(662, 225).addBox(-15.0F, -9.0F, 70.0F, 30.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(754, 225).addBox(10.0F, -18.0F, 70.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(680, 230).addBox(-12.0F, -18.0F, 70.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(594, 233).addBox(12.0F, -13.0F, 72.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(604, 233).addBox(-16.0F, -13.0F, 72.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(614, 233).addBox(17.0F, -20.0F, -60.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(622, 233).addBox(-18.0F, -20.0F, -60.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(630, 233).addBox(17.0F, -20.0F, -20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(488, 234).addBox(-18.0F, -20.0F, -20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(496, 234).addBox(17.0F, -20.0F, 20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(504, 234).addBox(-18.0F, -20.0F, 20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_l1 = partdefinition.addOrReplaceChild("wheel_l1", CubeListBuilder.create().texOffs(740, 125).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(536, 161).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(668, 187).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 44.0F));

        PartDefinition wheel_r1 = partdefinition.addOrReplaceChild("wheel_r1", CubeListBuilder.create().texOffs(740, 153).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(580, 177).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(560, 205).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 44.0F));

        PartDefinition wheel_l2 = partdefinition.addOrReplaceChild("wheel_l2", CubeListBuilder.create().texOffs(680, 159).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(624, 177).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(600, 205).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 58.0F));

        PartDefinition wheel_r2 = partdefinition.addOrReplaceChild("wheel_r2", CubeListBuilder.create().texOffs(488, 161).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(728, 181).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(488, 206).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 58.0F));
        return LayerDefinition.create(meshdefinition, 1024, 1024);
    }
}
