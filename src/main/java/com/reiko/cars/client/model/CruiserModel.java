package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The cruiser's shape, as Blockbench exports {@code blockbench/cruiser.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, fork with wheel_fl inside it, wheel_rl). Its texture is
 * {@code textures/entity/car/cruiser.png}. See {@link CarModel} for what each bone is for.
 */
public final class CruiserModel {
    private CruiserModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 67).addBox(-3.5F, -17.0F, -9.0F, 7.0F, 4.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(86, 44).addBox(-3.5F, -14.0F, 6.0F, 7.0F, 2.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(0, 94).addBox(-3.5F, -12.0F, 18.0F, 7.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(90, 87).addBox(3.0F, -11.0F, -1.0F, 1.0F, 4.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(104, 87).addBox(-4.0F, -11.0F, -1.0F, 1.0F, 4.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(40, 90).addBox(-2.5F, -19.0F, -17.0F, 5.0F, 4.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(82, 59).addBox(-3.0F, -9.0F, -7.0F, 6.0F, 6.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(54, 81).addBox(-2.5F, -13.0F, -8.0F, 5.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(72, 87).addBox(-2.5F, -13.0F, -2.0F, 5.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(118, 87).addBox(-1.0F, -13.0F, -12.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(78, 74).addBox(2.5F, -7.0F, 2.0F, 1.0F, 2.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(102, 74).addBox(-3.5F, -7.0F, 2.0F, 1.0F, 2.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(54, 89).addBox(4.0F, -5.0F, -9.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(22, 90).addBox(-8.0F, -5.0F, -9.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-5.5F, -7.0F, -6.0F, 2.0F, 2.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(52, 0).addBox(-5.5F, -5.0F, -4.0F, 2.0F, 2.0F, 21.0F, CubeDeformation.NONE)
            .texOffs(0, 79).addBox(-4.0F, -12.0F, -1.0F, 8.0F, 2.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(0, 88).addBox(-3.0F, -13.0F, 6.0F, 6.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(70, 97).addBox(2.5F, -22.0F, 11.0F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(74, 97).addBox(-3.5F, -22.0F, 11.0F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(18, 96).addBox(-2.5F, -21.0F, 11.0F, 5.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(30, 67).addBox(4.0F, -13.0F, 8.0F, 4.0F, 6.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(54, 67).addBox(-8.0F, -13.0F, 8.0F, 4.0F, 6.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(78, 97).addBox(-2.0F, -18.5F, -18.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(104, 97).addBox(-1.5F, -14.0F, 19.0F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create().texOffs(30, 96).addBox(1.5F, -6.0F, -3.0F, 1.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(36, 96).addBox(-2.5F, -6.0F, -3.0F, 1.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(42, 96).addBox(1.5F, -12.0F, -2.0F, 1.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(48, 96).addBox(-2.5F, -12.0F, -2.0F, 1.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(54, 97).addBox(-3.0F, -13.0F, -1.0F, 6.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(88, 97).addBox(1.0F, -16.0F, 0.0F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(92, 97).addBox(-2.0F, -16.0F, 0.0F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(54, 95).addBox(-8.0F, -17.0F, 0.0F, 16.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(96, 97).addBox(7.0F, -16.0F, 0.0F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(100, 97).addBox(-8.0F, -16.0F, 0.0F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(112, 97).addBox(8.0F, -14.0F, 0.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(118, 98).addBox(-10.0F, -14.0F, 0.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(30, 81).addBox(-2.0F, -8.0F, -6.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 17.0F, -13.0F));

        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(52, 23).addBox(-1.5F, -5.0F, -7.0F, 3.0F, 10.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(98, 0).addBox(-1.5F, -6.0F, -6.0F, 3.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(0, 26).addBox(-1.5F, -7.0F, -5.0F, 3.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, -2.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(86, 24).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(26, 47).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(56, 47).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 18.0F, 13.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
