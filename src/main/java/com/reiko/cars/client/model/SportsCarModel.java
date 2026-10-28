package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The sports car's shape, exported by Blockbench from {@code blockbench/sports_car.bbmodel}. To change it, edit that
 * file in Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()}
 * over this one (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is
 * {@code textures/entity/car/sports_car.png}. See {@link CarModel} for what each bone is for.
 */
public final class SportsCarModel {
    private SportsCarModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-13.0F, -11.0F, -28.0F, 26.0F, 8.0F, 22.0F, CubeDeformation.NONE)
        .texOffs(96, 22).addBox(-13.0F, -13.0F, 14.0F, 26.0F, 10.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(84, 46).addBox(11.0F, -12.0F, -6.0F, 2.0F, 9.0F, 20.0F, CubeDeformation.NONE)
        .texOffs(128, 46).addBox(-13.0F, -12.0F, -6.0F, 2.0F, 9.0F, 20.0F, CubeDeformation.NONE)
        .texOffs(38, 97).addBox(12.0F, -24.0F, -6.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(42, 97).addBox(-13.0F, -24.0F, -6.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(46, 97).addBox(12.0F, -24.0F, 13.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(50, 97).addBox(-13.0F, -24.0F, 13.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(96, 0).addBox(-12.0F, -26.0F, -6.0F, 24.0F, 2.0F, 20.0F, CubeDeformation.NONE)
        .texOffs(234, 97).addBox(13.0F, -14.0F, -5.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(138, 98).addBox(-15.0F, -14.0F, -5.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(0, 86).addBox(-12.0F, -17.0F, 25.0F, 24.0F, 1.0F, 3.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(172, 52).addBox(-12.0F, -24.0F, -6.0F, 24.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(172, 65).addBox(-12.0F, -24.0F, 13.0F, 24.0F, 11.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(0, 51).addBox(12.0F, -24.0F, -5.0F, 1.0F, 12.0F, 18.0F, CubeDeformation.NONE)
        .texOffs(38, 51).addBox(-13.0F, -24.0F, -5.0F, 1.0F, 12.0F, 18.0F, CubeDeformation.NONE)
        .texOffs(0, 30).addBox(-11.0F, -4.0F, -6.0F, 22.0F, 1.0F, 20.0F, CubeDeformation.NONE)
        .texOffs(0, 81).addBox(-11.0F, -14.0F, -6.0F, 22.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(244, 93).addBox(4.0F, -14.0F, -4.0F, 5.0F, 5.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(54, 93).addBox(2.0F, -5.0F, 1.0F, 8.0F, 1.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(210, 80).addBox(2.0F, -16.0F, 7.0F, 8.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(0, 97).addBox(4.0F, -19.0F, 7.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(82, 93).addBox(-10.0F, -5.0F, 1.0F, 8.0F, 1.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(230, 80).addBox(-10.0F, -16.0F, 7.0F, 8.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(12, 97).addBox(-8.0F, -19.0F, 7.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(110, 95).addBox(-13.0F, -3.0F, -29.0F, 26.0F, 1.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(0, 94).addBox(-13.0F, -5.0F, -29.0F, 26.0F, 2.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(210, 93).addBox(-8.0F, -8.0F, -29.0F, 16.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(0, 90).addBox(-13.0F, -6.0F, 28.0F, 26.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(166, 95).addBox(-10.0F, -3.0F, 27.0F, 20.0F, 1.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(24, 97).addBox(-3.0F, -9.0F, 28.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(146, 98).addBox(6.0F, -5.0F, 28.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(154, 98).addBox(-8.0F, -5.0F, 28.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(162, 98).addBox(8.0F, -16.0F, 25.0F, 1.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(168, 98).addBox(-9.0F, -16.0F, 25.0F, 1.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(184, 0).addBox(13.0F, -5.0F, -12.0F, 1.0F, 2.0F, 24.0F, CubeDeformation.NONE)
        .texOffs(176, 26).addBox(-14.0F, -5.0F, -12.0F, 1.0F, 2.0F, 24.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(110, 98).addBox(6.0F, -10.0F, -29.0F, 6.0F, 2.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(124, 98).addBox(-12.0F, -10.0F, -29.0F, 6.0F, 2.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(210, 97).addBox(7.0F, -12.0F, 28.0F, 5.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(222, 97).addBox(-12.0F, -12.0F, 28.0F, 5.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(226, 26).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(106, 75).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.5F, 19.0F, -17.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(226, 44).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(132, 75).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.5F, 19.0F, -17.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(222, 62).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(158, 77).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.5F, 19.0F, 18.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(76, 75).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(184, 77).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.5F, 19.0F, 18.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
