package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The farm tractor's shape, as Blockbench exports {@code blockbench/farm_tractor.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/farm_tractor.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class FarmTractorModel {
    private FarmTractorModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(82, 0).addBox(-6.0F, -22.0F, -28.0F, 12.0F, 12.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(110, 123).addBox(-7.0F, -22.0F, -6.0F, 14.0F, 6.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(176, 103).addBox(10.0F, -24.0F, 4.0F, 7.0F, 2.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(0, 114).addBox(-17.0F, -24.0F, 4.0F, 7.0F, 2.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(138, 40).addBox(-12.0F, -47.0F, -2.0F, 24.0F, 1.0F, 22.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(210, 141).addBox(-5.0F, -21.0F, -29.0F, 10.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 136).addBox(-8.0F, -9.0F, -31.0F, 16.0F, 5.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(132, 143).addBox(-10.0F, -6.0F, -20.0F, 20.0F, 2.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-5.0F, -10.0F, -27.0F, 10.0F, 4.0F, 31.0F, CubeDeformation.NONE)
            .texOffs(230, 103).addBox(6.0F, -16.0F, -22.0F, 1.0F, 7.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(230, 122).addBox(-7.0F, -16.0F, -22.0F, 1.0F, 7.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(100, 103).addBox(-10.0F, -18.0F, 2.0F, 20.0F, 2.0F, 18.0F, CubeDeformation.NONE)
            .texOffs(54, 123).addBox(-10.0F, -13.0F, 9.0F, 20.0F, 4.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(102, 137).addBox(-4.0F, -24.0F, 6.0F, 8.0F, 3.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(232, 141).addBox(-4.0F, -33.0F, 12.0F, 8.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(8, 145).addBox(-1.0F, -28.0F, -3.0F, 2.0F, 10.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(180, 143).addBox(-4.0F, -29.0F, -5.0F, 8.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(40, 136).addBox(10.0F, -46.0F, 16.0F, 1.0F, 28.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(46, 136).addBox(-11.0F, -46.0F, 16.0F, 1.0F, 28.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(102, 149).addBox(-11.0F, -46.0F, 16.0F, 22.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 145).addBox(-7.0F, -40.0F, -14.0F, 2.0F, 18.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(16, 145).addBox(-7.5F, -41.0F, -14.5F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(252, 141).addBox(5.0F, -18.0F, -29.0F, 1.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(36, 145).addBox(-6.0F, -18.0F, -29.0F, 1.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(28, 145).addBox(7.0F, -46.0F, -3.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(28, 148).addBox(-10.0F, -46.0F, -3.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(16, 149).addBox(14.0F, -24.0F, 24.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(22, 149).addBox(-16.0F, -24.0F, 24.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(154, 125).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(54, 135).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(8.0F, 19.0F, -18.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(182, 125).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 8.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(78, 135).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-8.0F, 19.0F, -18.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(150, 0).addBox(-3.0F, -9.0F, -11.0F, 6.0F, 18.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(0, 35).addBox(-3.0F, -10.0F, -10.0F, 6.0F, 20.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(190, 63).addBox(-3.0F, -11.0F, -9.0F, 6.0F, 22.0F, 18.0F, CubeDeformation.NONE), PartPose.offset(13.0F, 13.0F, 13.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(82, 34).addBox(-3.0F, -9.0F, -11.0F, 6.0F, 18.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(138, 63).addBox(-3.0F, -10.0F, -10.0F, 6.0F, 20.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(52, 74).addBox(-3.0F, -11.0F, -9.0F, 6.0F, 22.0F, 18.0F, CubeDeformation.NONE), PartPose.offset(-13.0F, 13.0F, 13.0F));
        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
