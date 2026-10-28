package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The tanker trailer's shape, as Blockbench exports {@code blockbench/tanker_trailer.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_l1, wheel_r1, wheel_l2, wheel_r2). Its texture is {@code textures/entity/trailer/tanker_trailer.png}. See {@link TrailerModel}
 * for what each bone is for.
 */
public final class TankerTrailerModel {
    private TankerTrailerModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(324, 0).addBox(-16.0F, -49.0F, -68.0F, 32.0F, 18.0F, 134.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-14.0F, -53.0F, -68.0F, 28.0F, 26.0F, 134.0F, CubeDeformation.NONE)
            .texOffs(656, 0).addBox(-9.0F, -56.0F, -68.0F, 18.0F, 32.0F, 134.0F, CubeDeformation.NONE)
            .texOffs(760, 297).addBox(-12.0F, -47.0F, -70.0F, 24.0F, 14.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(322, 302).addBox(-12.0F, -47.0F, 66.0F, 24.0F, 14.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(978, 224).addBox(-10.0F, -50.0F, -70.0F, 20.0F, 20.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(978, 246).addBox(-10.0F, -50.0F, 66.0F, 20.0F, 20.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(978, 268).addBox(-7.0F, -52.0F, -70.0F, 14.0F, 24.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(978, 294).addBox(-7.0F, -52.0F, 66.0F, 14.0F, 24.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(324, 152).addBox(-6.0F, -24.0F, -72.0F, 12.0F, 6.0F, 144.0F, CubeDeformation.NONE)
            .texOffs(216, 160).addBox(-14.0F, -26.0F, -72.0F, 28.0F, 8.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(636, 269).addBox(-14.0F, -26.0F, 34.0F, 28.0F, 8.0F, 34.0F, CubeDeformation.NONE)
            .texOffs(0, 160).addBox(-4.0F, -57.0F, -50.0F, 8.0F, 1.0F, 100.0F, CubeDeformation.NONE)
            .texOffs(502, 302).addBox(-3.0F, -58.0F, -40.0F, 6.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(526, 302).addBox(-3.0F, -58.0F, -3.0F, 6.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(550, 302).addBox(-3.0F, -58.0F, 34.0F, 6.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(636, 166).addBox(4.0F, -61.0F, -50.0F, 1.0F, 3.0F, 100.0F, CubeDeformation.NONE)
            .texOffs(0, 261).addBox(-5.0F, -61.0F, -50.0F, 1.0F, 3.0F, 100.0F, CubeDeformation.NONE)
            .texOffs(1010, 268).addBox(4.0F, -56.0F, 68.0F, 1.0F, 32.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1016, 268).addBox(-5.0F, -56.0F, 68.0F, 1.0F, 32.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(300, 303).addBox(-4.0F, -29.0F, 68.0F, 8.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(250, 305).addBox(-4.0F, -35.0F, 68.0F, 8.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(270, 305).addBox(-4.0F, -41.0F, 68.0F, 8.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(290, 306).addBox(-4.0F, -47.0F, 68.0F, 8.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(832, 306).addBox(-4.0F, -53.0F, 68.0F, 8.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(374, 302).addBox(13.0F, -26.0F, 20.0F, 4.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(406, 302).addBox(-17.0F, -26.0F, 20.0F, 4.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(838, 166).addBox(15.0F, -22.0F, -48.0F, 2.0F, 2.0F, 68.0F, CubeDeformation.NONE)
            .texOffs(838, 236).addBox(-17.0F, -22.0F, -48.0F, 2.0F, 2.0F, 68.0F, CubeDeformation.NONE)
            .texOffs(250, 286).addBox(-10.0F, -19.0F, -64.0F, 20.0F, 1.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(812, 295).addBox(12.0F, -18.0F, -38.0F, 2.0F, 17.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(822, 295).addBox(-14.0F, -18.0F, -38.0F, 2.0F, 17.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(574, 302).addBox(11.0F, -1.0F, -39.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(592, 302).addBox(-15.0F, -1.0F, -39.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(250, 303).addBox(-12.0F, -7.0F, -37.0F, 24.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(216, 190).addBox(6.0F, -18.0F, 36.0F, 4.0F, 4.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(216, 224).addBox(-10.0F, -18.0F, 36.0F, 4.0F, 4.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(812, 269).addBox(10.0F, -14.0F, 67.0F, 8.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(812, 282).addBox(-18.0F, -14.0F, 67.0F, 8.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(438, 302).addBox(-15.0F, -9.0F, 70.0F, 30.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(610, 302).addBox(10.0F, -18.0F, 70.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(618, 302).addBox(-12.0F, -18.0F, 70.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(626, 302).addBox(12.0F, -13.0F, 72.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(1010, 302).addBox(-16.0F, -13.0F, 72.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(310, 306).addBox(17.0F, -24.0F, -60.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(626, 306).addBox(-18.0F, -24.0F, -60.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(852, 306).addBox(17.0F, -24.0F, -20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(860, 306).addBox(-18.0F, -24.0F, -20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(868, 306).addBox(17.0F, -24.0F, 20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(876, 306).addBox(-18.0F, -24.0F, 20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_l1 = partdefinition.addOrReplaceChild("wheel_l1", CubeListBuilder.create().texOffs(216, 258).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(978, 0).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(978, 112).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 44.0F));

        PartDefinition wheel_r1 = partdefinition.addOrReplaceChild("wheel_r1", CubeListBuilder.create().texOffs(264, 258).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(978, 28).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(978, 140).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 44.0F));

        PartDefinition wheel_l2 = partdefinition.addOrReplaceChild("wheel_l2", CubeListBuilder.create().texOffs(760, 269).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(978, 56).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(978, 168).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 58.0F));

        PartDefinition wheel_r2 = partdefinition.addOrReplaceChild("wheel_r2", CubeListBuilder.create().texOffs(202, 286).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(978, 84).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(978, 196).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 58.0F));
        return LayerDefinition.create(meshdefinition, 1024, 1024);
    }
}
