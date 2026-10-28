package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The pickup truck's shape, exported by Blockbench from {@code blockbench/pickup_truck.bbmodel}. To change it, edit that
 * file in Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()}
 * over this one (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is
 * {@code textures/entity/car/pickup_truck.png}. See {@link CarModel} for what each bone is for.
 */
public final class PickupTruckModel {
    private PickupTruckModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -18.0F, -30.0F, 28.0F, 12.0F, 18.0F, CubeDeformation.NONE)
        .texOffs(186, 37).addBox(12.0F, -18.0F, -12.0F, 2.0F, 12.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(132, 46).addBox(-14.0F, -18.0F, -12.0F, 2.0F, 12.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(168, 65).addBox(-14.0F, -18.0F, 4.0F, 28.0F, 12.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(64, 111).addBox(13.0F, -30.0F, -12.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(242, 123).addBox(-14.0F, -30.0F, -12.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(52, 111).addBox(13.0F, -30.0F, 4.0F, 1.0F, 12.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(58, 111).addBox(-14.0F, -30.0F, 4.0F, 1.0F, 12.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(92, 25).addBox(-14.0F, -32.0F, -13.0F, 28.0F, 2.0F, 19.0F, CubeDeformation.NONE)
        .texOffs(182, 126).addBox(14.0F, -20.0F, -11.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(190, 126).addBox(-16.0F, -20.0F, -11.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(92, 0).addBox(-12.0F, -9.0F, 6.0F, 24.0F, 3.0F, 22.0F, CubeDeformation.NONE)
        .texOffs(186, 0).addBox(12.0F, -19.0F, 6.0F, 2.0F, 13.0F, 24.0F, CubeDeformation.NONE)
        .texOffs(0, 30).addBox(-14.0F, -19.0F, 6.0F, 2.0F, 13.0F, 24.0F, CubeDeformation.NONE)
        .texOffs(0, 67).addBox(-12.0F, -19.0F, 28.0F, 24.0F, 13.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(84, 74).addBox(-13.0F, -30.0F, -12.0F, 26.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(138, 79).addBox(-13.0F, -30.0F, 4.0F, 26.0F, 12.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(222, 37).addBox(13.0F, -30.0F, -11.0F, 1.0F, 12.0F, 15.0F, CubeDeformation.NONE)
        .texOffs(52, 64).addBox(-14.0F, -30.0F, -11.0F, 1.0F, 12.0F, 15.0F, CubeDeformation.NONE)
        .texOffs(52, 46).addBox(-12.0F, -8.0F, -12.0F, 24.0F, 2.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(94, 112).addBox(-12.0F, -19.0F, -12.0F, 24.0F, 3.0F, 3.0F, CubeDeformation.NONE)
        .texOffs(242, 112).addBox(4.0F, -19.0F, -9.0F, 5.0F, 5.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(212, 119).addBox(2.0F, -10.0F, -4.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(234, 99).addBox(2.0F, -21.0F, 2.0F, 9.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(242, 118).addBox(4.5F, -24.0F, 2.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(94, 123).addBox(-11.0F, -10.0F, -4.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(152, 112).addBox(-11.0F, -21.0F, 2.0F, 9.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(0, 122).addBox(-8.5F, -24.0F, 2.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(94, 118).addBox(-14.0F, -9.0F, -31.0F, 28.0F, 4.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(174, 119).addBox(-9.0F, -16.0F, -31.0F, 18.0F, 6.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(124, 126).addBox(-14.0F, -8.0F, 30.0F, 28.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(12, 122).addBox(-3.0F, -12.0F, 30.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(58, 125).addBox(10.0F, -16.0F, -31.0F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(12, 126).addBox(-13.0F, -16.0F, -31.0F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(246, 123).addBox(12.0F, -17.0F, 30.0F, 2.0F, 6.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(52, 125).addBox(-14.0F, -17.0F, 30.0F, 2.0F, 6.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(192, 79).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(226, 79).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(208, 99).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(13.5F, 18.0F, -20.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(0, 82).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(118, 92).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(0, 102).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-13.5F, 18.0F, -20.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(84, 87).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(148, 92).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(68, 107).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(13.5F, 18.0F, 18.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(34, 91).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(178, 99).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
        .texOffs(26, 111).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-13.5F, 18.0F, 18.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
