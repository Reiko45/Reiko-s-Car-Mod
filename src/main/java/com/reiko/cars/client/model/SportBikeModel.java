package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The sport bike's shape, as Blockbench exports {@code blockbench/sport_bike.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, fork with wheel_fl inside it, wheel_rl). Its texture is
 * {@code textures/entity/car/sport_bike.png}. See {@link CarModel} for what each bone is for.
 */
public final class SportBikeModel {
    private SportBikeModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 36).addBox(-4.0F, -16.0F, -17.0F, 8.0F, 7.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(76, 53).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 3.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(54, 36).addBox(4.0F, -14.0F, -10.0F, 1.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 40).addBox(-5.0F, -14.0F, -10.0F, 1.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(86, 20).addBox(-4.0F, -17.0F, -10.0F, 8.0F, 4.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(32, 0).addBox(-3.0F, -14.0F, 0.0F, 6.0F, 3.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(108, 48).addBox(-2.0F, -15.0F, 13.0F, 4.0F, 2.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(120, 68).addBox(4.0F, -18.0F, -13.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(108, 69).addBox(-6.0F, -18.0F, -13.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(108, 33).addBox(-3.0F, -20.0F, -15.0F, 6.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(58, 20).addBox(-3.0F, -12.0F, -7.0F, 6.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(48, 54).addBox(2.0F, -7.0F, 1.0F, 1.0F, 2.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 58).addBox(-3.0F, -7.0F, 1.0F, 1.0F, 2.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(120, 58).addBox(3.0F, -7.0F, 1.0F, 2.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(120, 61).addBox(-5.0F, -7.0F, 1.0F, 2.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(22, 50).addBox(-5.0F, -9.0F, 3.0F, 2.0F, 3.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(120, 64).addBox(-1.0F, -13.0F, 15.0F, 2.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(98, 64).addBox(-2.0F, -11.0F, 16.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(70, 64).addBox(-3.0F, -15.0F, -1.0F, 6.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(108, 41).addBox(-2.0F, -16.0F, 7.0F, 4.0F, 2.0F, 5.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(46, 66).addBox(-3.0F, -14.0F, -18.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(98, 68).addBox(-2.0F, -15.0F, 17.0F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create().texOffs(108, 58).addBox(1.5F, -9.0F, -2.0F, 1.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(114, 58).addBox(-2.5F, -9.0F, -2.0F, 1.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(108, 54).addBox(-3.0F, -10.0F, -2.0F, 6.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(60, 66).addBox(3.0F, -10.0F, 0.0F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(60, 68).addBox(-7.0F, -10.0F, 0.0F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(22, 64).addBox(-2.0F, -7.0F, -5.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 18.0F, -10.0F));

        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(70, 0).addBox(-1.5F, -4.0F, -6.0F, 3.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(32, 16).addBox(-1.5F, -5.0F, -5.0F, 3.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(86, 33).addBox(-1.5F, -6.0F, -4.0F, 3.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, -1.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -4.0F, -6.0F, 4.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(100, 0).addBox(-2.0F, -5.0F, -5.0F, 4.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 20).addBox(-2.0F, -6.0F, -4.0F, 4.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 18.0F, 11.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
