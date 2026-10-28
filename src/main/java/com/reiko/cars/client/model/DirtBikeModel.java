package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The dirt bike's shape, as Blockbench exports {@code blockbench/dirt_bike.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, fork with wheel_fl inside it, wheel_rl). Its texture is
 * {@code textures/entity/car/dirt_bike.png}. See {@link CarModel} for what each bone is for.
 */
public final class DirtBikeModel {
    private DirtBikeModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(108, 55).addBox(3.0F, -18.0F, -9.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(20, 60).addBox(-4.0F, -18.0F, -9.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(60, 57).addBox(-3.0F, -18.0F, -7.0F, 6.0F, 4.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(34, 60).addBox(2.5F, -17.0F, 3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 66).addBox(-3.5F, -17.0F, 3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 43).addBox(-2.0F, -17.0F, 9.0F, 4.0F, 1.0F, 11.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(88, 24).addBox(-2.5F, -18.0F, -3.0F, 5.0F, 1.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(88, 38).addBox(-2.5F, -12.0F, -6.0F, 5.0F, 8.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(56, 66).addBox(-2.0F, -14.0F, -6.0F, 4.0F, 2.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(48, 60).addBox(-1.0F, -16.0F, -10.0F, 2.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(60, 48).addBox(-3.0F, -4.0F, -6.0F, 6.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(0, 55).addBox(1.5F, -8.0F, 2.0F, 1.0F, 2.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(88, 55).addBox(-2.5F, -8.0F, 2.0F, 1.0F, 2.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(86, 66).addBox(-1.0F, -15.0F, 1.0F, 2.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 24).addBox(-4.5F, -14.0F, 0.0F, 2.0F, 3.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(14, 72).addBox(2.5F, -8.0F, 0.0F, 2.5F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(23, 72).addBox(-5.0F, -8.0F, 0.0F, 2.5F, 1.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(32, 72).addBox(-1.5F, -18.0F, -16.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(56, 72).addBox(-1.0F, -16.0F, 19.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create().texOffs(122, 38).addBox(1.5F, -11.0F, -2.0F, 1.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(122, 51).addBox(-2.5F, -11.0F, -2.0F, 1.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(94, 67).addBox(-3.0F, -12.0F, -1.0F, 6.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(40, 72).addBox(-1.5F, -13.0F, 1.0F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(94, 71).addBox(-8.0F, -14.0F, 1.0F, 16.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(30, 48).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 1.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(72, 66).addBox(-3.0F, -12.0F, -3.0F, 6.0F, 5.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 17.0F, -12.0F));

        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -5.0F, -7.0F, 3.0F, 10.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(68, 0).addBox(-1.5F, -6.0F, -6.0F, 3.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(36, 24).addBox(-1.5F, -7.0F, -5.0F, 3.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, -1.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(34, 0).addBox(-1.5F, -5.0F, -7.0F, 3.0F, 10.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(98, 0).addBox(-1.5F, -6.0F, -6.0F, 3.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(62, 24).addBox(-1.5F, -7.0F, -5.0F, 3.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 17.0F, 11.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
