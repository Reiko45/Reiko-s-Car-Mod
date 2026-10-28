package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The muscle car's shape, as Blockbench exports {@code blockbench/muscle_car.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/muscle_car.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class MuscleCarModel {
    private MuscleCarModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -12.0F, -30.0F, 28.0F, 8.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(26, 74).addBox(-4.0F, -14.0F, -24.0F, 8.0F, 2.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(196, 0).addBox(12.0F, -12.0F, -8.0F, 2.0F, 8.0F, 18.0F, CubeDeformation.NONE)
            .texOffs(196, 26).addBox(-14.0F, -12.0F, -8.0F, 2.0F, 8.0F, 18.0F, CubeDeformation.NONE)
            .texOffs(100, 0).addBox(-14.0F, -13.0F, 10.0F, 28.0F, 9.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(144, 96).addBox(12.0F, -22.0F, -8.0F, 1.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(148, 96).addBox(-13.0F, -22.0F, -8.0F, 1.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 92).addBox(12.0F, -22.0F, 8.0F, 1.0F, 9.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(10, 92).addBox(-13.0F, -22.0F, 8.0F, 1.0F, 9.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(100, 29).addBox(-13.0F, -24.0F, -8.0F, 26.0F, 2.0F, 19.0F, CubeDeformation.NONE)
            .texOffs(26, 86).addBox(-13.0F, -16.0F, 26.0F, 26.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(152, 99).addBox(14.0F, -14.0F, -6.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(160, 99).addBox(-16.0F, -14.0F, -6.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(64, 70).addBox(-12.0F, -22.0F, -8.0F, 24.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(174, 72).addBox(-12.0F, -22.0F, 10.0F, 24.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 49).addBox(12.0F, -22.0F, -7.0F, 1.0F, 10.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(32, 49).addBox(-13.0F, -22.0F, -7.0F, 1.0F, 10.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(0, 30).addBox(-12.0F, -5.0F, -8.0F, 24.0F, 1.0F, 18.0F, CubeDeformation.NONE)
            .texOffs(196, 90).addBox(-12.0F, -13.0F, -8.0F, 24.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(212, 95).addBox(4.0F, -14.0F, -6.0F, 6.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(26, 91).addBox(2.0F, -6.0F, -1.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(86, 81).addBox(2.0F, -17.0F, 5.0F, 9.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(244, 95).addBox(4.5F, -20.0F, 5.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(56, 91).addBox(-11.0F, -6.0F, -1.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(174, 82).addBox(-11.0F, -17.0F, 5.0F, 9.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(38, 99).addBox(-8.5F, -20.0F, 5.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(180, 99).addBox(9.0F, -15.0F, 27.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(184, 99).addBox(-10.0F, -15.0F, 27.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(154, 95).addBox(-14.0F, -6.0F, -31.0F, 28.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(86, 96).addBox(-14.0F, -6.0F, 30.0F, 28.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(108, 90).addBox(-11.0F, -11.0F, -31.0F, 22.0F, 5.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(50, 99).addBox(-3.0F, -10.0F, 30.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(64, 99).addBox(6.0F, -5.0F, 30.0F, 3.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(74, 99).addBox(-9.0F, -5.0F, 30.0F, 3.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(168, 99).addBox(11.0F, -10.0F, -31.0F, 2.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(174, 99).addBox(-13.0F, -10.0F, -31.0F, 2.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(226, 95).addBox(5.0F, -11.0F, 30.0F, 8.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(20, 99).addBox(-13.0F, -11.0F, 30.0F, 8.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(114, 70).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(224, 72).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.5F, 19.0F, -19.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(144, 72).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 74).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.5F, 19.0F, -19.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(64, 49).addBox(-3.0F, -4.0F, -6.0F, 6.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(136, 50).addBox(-3.0F, -5.0F, -5.0F, 6.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(200, 52).addBox(-3.0F, -6.0F, -4.0F, 6.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(13.0F, 18.0F, 19.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(100, 50).addBox(-3.0F, -4.0F, -6.0F, 6.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(168, 52).addBox(-3.0F, -5.0F, -5.0F, 6.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(228, 52).addBox(-3.0F, -6.0F, -4.0F, 6.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-13.0F, 18.0F, 19.0F));
        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
