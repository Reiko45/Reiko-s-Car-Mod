package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The scooter's shape, as Blockbench exports {@code blockbench/scooter.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, fork with wheel_fl inside it, wheel_rl). Its texture is
 * {@code textures/entity/car/scooter.png}. See {@link CarModel} for what each bone is for.
 */
public final class ScooterModel {
    private ScooterModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(36, 12).addBox(-4.0F, -18.0F, -10.0F, 8.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-4.0F, -12.0F, 2.0F, 8.0F, 7.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(36, 27).addBox(-3.0F, -11.0F, 12.0F, 6.0F, 5.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(36, 0).addBox(-3.0F, -5.0F, -8.0F, 6.0F, 2.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(90, 14).addBox(-4.0F, -6.0F, 2.0F, 2.5F, 4.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(56, 25).addBox(2.0F, -4.0F, 5.0F, 2.0F, 2.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(68, 0).addBox(-3.0F, -13.0F, 1.0F, 6.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(14, 31).addBox(-3.0F, -14.0F, 11.0F, 6.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(28, 31).addBox(-1.5F, -20.0F, -13.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(76, 32).addBox(-1.5F, -10.0F, 14.0F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create().texOffs(8, 31).addBox(1.5F, -6.0F, -1.0F, 1.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 31).addBox(-1.0F, -14.0F, -1.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(76, 26).addBox(-6.0F, -16.0F, -2.0F, 12.0F, 2.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(52, 27).addBox(5.5F, -19.0F, 0.0F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(52, 31).addBox(-6.5F, -19.0F, 0.0F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(108, 26).addBox(-2.0F, -5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 20.0F, -10.0F));

        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(100, 0).addBox(-1.5F, -3.0F, -4.0F, 3.0F, 6.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(0, 17).addBox(-1.5F, -4.0F, -3.0F, 3.0F, 8.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(68, 11).addBox(-1.5F, -3.0F, -4.0F, 3.0F, 6.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(18, 17).addBox(-1.5F, -4.0F, -3.0F, 3.0F, 8.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 20.0F, 9.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
