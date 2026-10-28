package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The convertible's shape, as Blockbench exports {@code blockbench/convertible.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/convertible.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class ConvertibleModel {
    private ConvertibleModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-13.0F, -11.0F, -28.0F, 26.0F, 8.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(108, 63).addBox(-13.0F, -12.0F, -9.0F, 26.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(180, 0).addBox(11.0F, -12.0F, -7.0F, 2.0F, 9.0F, 17.0F, CubeDeformation.NONE)
            .texOffs(218, 0).addBox(-13.0F, -12.0F, -7.0F, 2.0F, 9.0F, 17.0F, CubeDeformation.NONE)
            .texOffs(92, 0).addBox(-13.0F, -12.0F, 10.0F, 26.0F, 9.0F, 18.0F, CubeDeformation.NONE)
            .texOffs(250, 26).addBox(12.0F, -21.0F, -8.0F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(250, 36).addBox(-13.0F, -21.0F, -8.0F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(164, 63).addBox(-13.0F, -22.0F, -8.0F, 26.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(164, 65).addBox(13.0F, -14.0F, -6.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(172, 65).addBox(-15.0F, -14.0F, -6.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(198, 35).addBox(-12.0F, -21.0F, -8.0F, 24.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(92, 27).addBox(-11.0F, -4.0F, -7.0F, 22.0F, 1.0F, 17.0F, CubeDeformation.NONE)
            .texOffs(0, 46).addBox(-11.0F, -12.0F, -7.0F, 22.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(222, 53).addBox(4.0F, -12.0F, -5.0F, 5.0F, 5.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(222, 45).addBox(2.0F, -6.0F, 0.0F, 8.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 51).addBox(2.0F, -15.0F, 6.0F, 8.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(234, 53).addBox(4.0F, -18.0F, 6.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(48, 46).addBox(-10.0F, -6.0F, 0.0F, 8.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(20, 51).addBox(-10.0F, -15.0F, 6.0F, 8.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(234, 58).addBox(-8.0F, -18.0F, 6.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(180, 26).addBox(-11.0F, -15.0F, 12.0F, 22.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 62).addBox(-13.0F, -5.0F, -29.0F, 26.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(54, 63).addBox(-13.0F, -5.0F, 28.0F, 26.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(164, 53).addBox(-6.0F, -9.0F, -29.0F, 12.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(218, 63).addBox(-3.0F, -9.0F, 28.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(246, 53).addBox(8.0F, -9.0F, -29.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(246, 57).addBox(-12.0F, -9.0F, -29.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(246, 61).addBox(8.0F, -10.0F, 28.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(232, 63).addBox(-12.0F, -10.0F, 28.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(0, 28).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(84, 45).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.0F, 19.0F, -17.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(28, 28).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(108, 45).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.0F, 19.0F, -17.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(56, 28).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(132, 45).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.0F, 19.0F, 17.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(170, 35).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(198, 45).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.0F, 19.0F, 17.0F));
        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
