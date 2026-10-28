package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The heavy-duty pickup's shape, as Blockbench exports {@code blockbench/heavy_pickup.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/heavy_pickup.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class HeavyPickupModel {
    private HeavyPickupModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(120, 30).addBox(-15.0F, -23.0F, -34.0F, 30.0F, 14.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(0, 32).addBox(13.0F, -23.0F, -18.0F, 2.0F, 14.0F, 28.0F, CubeDeformation.NONE)
            .texOffs(60, 32).addBox(-15.0F, -23.0F, -18.0F, 2.0F, 14.0F, 28.0F, CubeDeformation.NONE)
            .texOffs(80, 136).addBox(-15.0F, -23.0F, 10.0F, 30.0F, 14.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(32, 198).addBox(14.0F, -35.0F, -18.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(252, 200).addBox(-15.0F, -35.0F, -18.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(32, 211).addBox(14.0F, -35.0F, -4.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(252, 213).addBox(-15.0F, -35.0F, -4.0F, 1.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(248, 170).addBox(14.0F, -35.0F, 9.0F, 1.0F, 12.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(246, 185).addBox(-15.0F, -35.0F, 9.0F, 1.0F, 12.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-15.0F, -37.0F, -18.0F, 30.0F, 2.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(120, 60).addBox(-13.0F, -12.0F, 12.0F, 26.0F, 3.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(0, 74).addBox(13.0F, -25.0F, 12.0F, 2.0F, 16.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(48, 74).addBox(-15.0F, -25.0F, 12.0F, 2.0F, 16.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(0, 140).addBox(-13.0F, -25.0F, 32.0F, 26.0F, 16.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(112, 164).addBox(-14.0F, -35.0F, -18.0F, 28.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 167).addBox(-14.0F, -35.0F, 10.0F, 28.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(58, 167).addBox(14.0F, -35.0F, -17.0F, 1.0F, 12.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(170, 170).addBox(-15.0F, -35.0F, -17.0F, 1.0F, 12.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(36, 192).addBox(14.0F, -35.0F, -3.0F, 1.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(62, 195).addBox(-15.0F, -35.0F, -3.0F, 1.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(120, 0).addBox(-13.0F, -11.0F, -18.0F, 26.0F, 2.0F, 28.0F, CubeDeformation.NONE)
            .texOffs(88, 204).addBox(-13.0F, -24.0F, -18.0F, 26.0F, 4.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(132, 211).addBox(5.0F, -24.0F, -15.0F, 6.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(88, 211).addBox(2.0F, -15.0F, -11.0F, 10.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 198).addBox(2.0F, -28.0F, -5.0F, 10.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(146, 214).addBox(5.0F, -31.0F, -5.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 213).addBox(-12.0F, -15.0F, -11.0F, 10.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(222, 201).addBox(-12.0F, -28.0F, -5.0F, 10.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(158, 214).addBox(-9.0F, -31.0F, -5.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(88, 195).addBox(-13.0F, -15.0F, 0.0F, 26.0F, 2.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(56, 152).addBox(-13.0F, -28.0F, 7.0F, 26.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(210, 208).addBox(15.0F, -30.0F, -17.0F, 4.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(120, 211).addBox(-19.0F, -30.0F, -17.0F, 4.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(86, 177).addBox(15.0F, -19.0F, -32.0F, 2.0F, 2.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(122, 177).addBox(-17.0F, -19.0F, -32.0F, 2.0F, 2.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(0, 180).addBox(15.0F, -19.0F, 14.0F, 2.0F, 2.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(198, 183).addBox(-17.0F, -19.0F, 14.0F, 2.0F, 2.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(96, 83).addBox(15.0F, -8.0F, -16.0F, 2.0F, 1.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(148, 83).addBox(-17.0F, -8.0F, -16.0F, 2.0F, 1.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(198, 170).addBox(-12.0F, -22.0F, -35.0F, 24.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(154, 201).addBox(-16.0F, -11.0F, -36.0F, 32.0F, 5.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(146, 208).addBox(-15.0F, -10.0F, 34.0F, 30.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(170, 214).addBox(-3.0F, -13.0F, 34.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(184, 214).addBox(12.0F, -20.0F, -35.0F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(192, 214).addBox(-15.0F, -20.0F, -35.0F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(246, 200).addBox(13.0F, -22.0F, 34.0F, 2.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(246, 210).addBox(-15.0F, -22.0F, 34.0F, 2.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(200, 214).addBox(6.0F, -38.0F, -17.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(36, 216).addBox(2.0F, -38.0F, -17.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(42, 216).addBox(-4.0F, -38.0F, -17.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(48, 216).addBox(-8.0F, -38.0F, -17.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(212, 30).addBox(-3.0F, -6.0F, -8.0F, 6.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(140, 108).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(220, 114).addBox(-3.0F, -8.0F, -6.0F, 6.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, -24.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(212, 58).addBox(-3.0F, -6.0F, -8.0F, 6.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(0, 112).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(144, 136).addBox(-3.0F, -8.0F, -6.0F, 6.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, -24.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(200, 86).addBox(-3.0F, -6.0F, -8.0F, 6.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(40, 112).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(180, 142).addBox(-3.0F, -8.0F, -6.0F, 6.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 22.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(96, 108).addBox(-3.0F, -6.0F, -8.0F, 6.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(180, 114).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(216, 142).addBox(-3.0F, -8.0F, -6.0F, 6.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 22.0F));
        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
