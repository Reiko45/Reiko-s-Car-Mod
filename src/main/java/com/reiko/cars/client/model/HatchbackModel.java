package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The hatchback's shape, as Blockbench exports {@code blockbench/hatchback.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/hatchback.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class HatchbackModel {
    private HatchbackModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(120, 33).addBox(-13.0F, -13.0F, -24.0F, 26.0F, 9.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(154, 113).addBox(-13.0F, -15.0F, -12.0F, 26.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(200, 33).addBox(11.0F, -15.0F, -10.0F, 2.0F, 11.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(0, 36).addBox(-13.0F, -15.0F, -10.0F, 2.0F, 11.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(52, 56).addBox(-13.0F, -15.0F, 14.0F, 26.0F, 11.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(68, 106).addBox(12.0F, -26.0F, -10.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(72, 106).addBox(-13.0F, -26.0F, -10.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(244, 114).addBox(12.0F, -26.0F, 2.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(248, 114).addBox(-13.0F, -26.0F, 2.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(248, 68).addBox(12.0F, -26.0F, 20.0F, 1.0F, 11.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(248, 82).addBox(-13.0F, -26.0F, 20.0F, 1.0F, 11.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-13.0F, -28.0F, -11.0F, 26.0F, 2.0F, 34.0F, CubeDeformation.NONE)
            .texOffs(154, 117).addBox(-12.0F, -27.0F, 23.0F, 24.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(104, 118).addBox(13.0F, -17.0F, -9.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(112, 118).addBox(-15.0F, -17.0F, -9.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(196, 68).addBox(-12.0F, -26.0F, -10.0F, 24.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(44, 77).addBox(-12.0F, -25.0F, 22.0F, 24.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(94, 77).addBox(12.0F, -26.0F, -9.0F, 1.0F, 11.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(196, 80).addBox(-13.0F, -26.0F, -9.0F, 1.0F, 11.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(124, 56).addBox(12.0F, -26.0F, 3.0F, 1.0F, 11.0F, 17.0F, CubeDeformation.NONE)
            .texOffs(160, 56).addBox(-13.0F, -26.0F, 3.0F, 1.0F, 11.0F, 17.0F, CubeDeformation.NONE)
            .texOffs(120, 0).addBox(-11.0F, -5.0F, -10.0F, 22.0F, 1.0F, 32.0F, CubeDeformation.NONE)
            .texOffs(152, 102).addBox(-11.0F, -16.0F, -10.0F, 22.0F, 3.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(244, 98).addBox(4.0F, -16.0F, -7.0F, 5.0F, 5.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(40, 106).addBox(2.0F, -7.0F, -4.0F, 8.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 102).addBox(2.0F, -19.0F, 2.0F, 8.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(244, 104).addBox(4.0F, -22.0F, 2.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(40, 114).addBox(-10.0F, -7.0F, -4.0F, 8.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(20, 106).addBox(-10.0F, -19.0F, 2.0F, 8.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(244, 109).addBox(-8.0F, -22.0F, 2.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(100, 102).addBox(-10.0F, -7.0F, 8.0F, 20.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 71).addBox(-10.0F, -18.0F, 14.0F, 20.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(152, 108).addBox(-13.0F, -7.0F, -25.0F, 26.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(100, 113).addBox(-13.0F, -7.0F, 24.0F, 26.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(210, 116).addBox(-7.0F, -11.0F, -25.0F, 14.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 116).addBox(-3.0F, -11.0F, 24.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(76, 117).addBox(8.0F, -12.0F, -25.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(86, 117).addBox(-12.0F, -12.0F, -25.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(68, 118).addBox(10.0F, -14.0F, 24.0F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(96, 118).addBox(-13.0F, -14.0F, 24.0F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(220, 80).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(28, 88).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.0F, 19.0F, -15.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(0, 84).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(52, 88).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.0F, 19.0F, -15.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(118, 84).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(220, 98).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.0F, 19.0F, 15.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(146, 84).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(76, 99).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.0F, 19.0F, 15.0F));
        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
