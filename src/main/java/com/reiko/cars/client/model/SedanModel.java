package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The sedan's shape, exported by Blockbench from {@code blockbench/sedan.bbmodel}. To change it, edit that
 * file in Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()}
 * over this one (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is
 * {@code textures/entity/car/sedan.png}. See {@link CarModel} for what each bone is for.
 */
public final class SedanModel {
    private SedanModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(110, 28).addBox(-13.0F, -13.0F, -28.0F, 26.0F, 9.0F, 17.0F, CubeDeformation.NONE)
        .texOffs(102, 111).addBox(-13.0F, -15.0F, -13.0F, 26.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(58, 54).addBox(-13.0F, -14.0F, 16.0F, 26.0F, 10.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(196, 28).addBox(11.0F, -15.0F, -11.0F, 2.0F, 11.0F, 27.0F, CubeDeformation.NONE)
        .texOffs(0, 31).addBox(-13.0F, -15.0F, -11.0F, 2.0F, 11.0F, 27.0F, CubeDeformation.NONE)
        .texOffs(248, 66).addBox(12.0F, -27.0F, -11.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(252, 66).addBox(-13.0F, -27.0F, -11.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(248, 79).addBox(12.0F, -27.0F, 2.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(252, 79).addBox(-13.0F, -27.0F, 2.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(248, 92).addBox(12.0F, -27.0F, 15.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(252, 92).addBox(-13.0F, -27.0F, 15.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(0, 0).addBox(-13.0F, -29.0F, -12.0F, 26.0F, 2.0F, 29.0F, CubeDeformation.NONE)
        .texOffs(248, 105).addBox(13.0F, -17.0F, -10.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(248, 109).addBox(-15.0F, -17.0F, -10.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(134, 54).addBox(-12.0F, -27.0F, -11.0F, 24.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(184, 66).addBox(-12.0F, -27.0F, 15.0F, 24.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(134, 67).addBox(12.0F, -27.0F, -10.0F, 1.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(0, 69).addBox(-13.0F, -27.0F, -10.0F, 1.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(26, 69).addBox(12.0F, -27.0F, 3.0F, 1.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(52, 76).addBox(-13.0F, -27.0F, 3.0F, 1.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(110, 0).addBox(-11.0F, -5.0F, -11.0F, 22.0F, 1.0F, 27.0F, CubeDeformation.NONE)
        .texOffs(106, 100).addBox(-11.0F, -16.0F, -11.0F, 22.0F, 3.0F, 3.0F, CubeDeformation.NONE)
        .texOffs(236, 111).addBox(4.0F, -16.0F, -8.0F, 5.0F, 5.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(0, 111).addBox(2.0F, -7.0F, -5.0F, 8.0F, 2.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(208, 97).addBox(2.0F, -19.0F, 1.0F, 8.0F, 12.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(54, 113).addBox(4.0F, -22.0F, 1.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(208, 111).addBox(-10.0F, -7.0F, -5.0F, 8.0F, 2.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(228, 97).addBox(-10.0F, -19.0F, 1.0F, 8.0F, 12.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(66, 113).addBox(-8.0F, -22.0F, 1.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(106, 91).addBox(-10.0F, -7.0F, 6.0F, 20.0F, 2.0F, 7.0F, CubeDeformation.NONE)
        .texOffs(78, 76).addBox(-10.0F, -19.0F, 13.0F, 20.0F, 12.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(106, 106).addBox(-13.0F, -7.0F, -29.0F, 26.0F, 4.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(48, 108).addBox(-13.0F, -7.0F, 28.0F, 26.0F, 4.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(28, 113).addBox(-6.0F, -12.0F, -29.0F, 12.0F, 4.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(78, 113).addBox(-3.0F, -10.0F, 28.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(92, 115).addBox(7.0F, -12.0F, -29.0F, 5.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(104, 115).addBox(-12.0F, -12.0F, -29.0F, 5.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(116, 115).addBox(8.0F, -13.0F, 28.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(126, 115).addBox(-12.0F, -13.0F, 28.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(160, 79).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(0, 93).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(13.0F, 19.0F, -18.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(188, 79).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(24, 93).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-13.0F, 19.0F, -18.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(216, 79).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(160, 97).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(13.0F, 19.0F, 18.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(78, 90).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(184, 97).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-13.0F, 19.0F, 18.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
