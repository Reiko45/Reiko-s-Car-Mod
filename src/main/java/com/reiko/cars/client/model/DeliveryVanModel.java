package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The delivery van's shape, as Blockbench exports {@code blockbench/delivery_van.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/delivery_van.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class DeliveryVanModel {
    private DeliveryVanModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(136, 98).addBox(-15.0F, -18.0F, -35.0F, 30.0F, 11.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(104, 190).addBox(-15.0F, -21.0F, -26.0F, 30.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(216, 98).addBox(13.0F, -22.0F, -24.0F, 2.0F, 15.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(82, 122).addBox(-15.0F, -22.0F, -24.0F, 2.0F, 15.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(13.0F, -41.0F, -9.0F, 2.0F, 34.0F, 44.0F, CubeDeformation.NONE)
            .texOffs(92, 0).addBox(-15.0F, -41.0F, -9.0F, 2.0F, 34.0F, 44.0F, CubeDeformation.NONE)
            .texOffs(184, 0).addBox(-13.0F, -39.0F, 33.0F, 26.0F, 32.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(230, 190).addBox(14.0F, -36.0F, -24.0F, 1.0F, 14.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(234, 190).addBox(-15.0F, -36.0F, -24.0F, 1.0F, 14.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(238, 190).addBox(14.0F, -36.0F, -10.0F, 1.0F, 14.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(242, 190).addBox(-15.0F, -36.0F, -10.0F, 1.0F, 14.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(136, 78).addBox(-15.0F, -41.0F, -24.0F, 30.0F, 5.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(0, 78).addBox(-13.0F, -41.0F, -9.0F, 26.0F, 2.0F, 42.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(190, 128).addBox(-14.0F, -36.0F, -24.0F, 28.0F, 14.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 139).addBox(14.0F, -36.0F, -23.0F, 1.0F, 14.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(28, 139).addBox(-15.0F, -36.0F, -23.0F, 1.0F, 14.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(0, 122).addBox(-13.0F, -9.0F, -24.0F, 26.0F, 2.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(136, 119).addBox(-13.0F, -39.0F, -10.0F, 26.0F, 30.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(56, 172).addBox(-13.0F, -22.0F, -24.0F, 26.0F, 4.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(230, 183).addBox(5.0F, -22.0F, -21.0F, 6.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 186).addBox(2.0F, -16.0F, -18.0F, 10.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(56, 179).addBox(2.0F, -28.0F, -12.0F, 10.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 194).addBox(5.0F, -31.0F, -12.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(32, 193).addBox(-12.0F, -16.0F, -18.0F, 10.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(80, 179).addBox(-12.0F, -28.0F, -12.0F, 10.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(12, 194).addBox(-9.0F, -31.0F, -12.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(244, 163).addBox(15.0F, -28.0F, -22.0F, 3.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(244, 171).addBox(-18.0F, -28.0F, -22.0F, 3.0F, 6.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(168, 191).addBox(-15.0F, -10.0F, -36.0F, 30.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(168, 183).addBox(-10.0F, -17.0F, -36.0F, 20.0F, 7.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(64, 195).addBox(-13.0F, -8.0F, 35.0F, 26.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(120, 195).addBox(-3.0F, -12.0F, 35.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(246, 190).addBox(10.0F, -17.0F, -36.0F, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(134, 195).addBox(-14.0F, -17.0F, -36.0F, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(244, 179).addBox(13.0F, -20.0F, 35.0F, 2.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(250, 179).addBox(-15.0F, -20.0F, 35.0F, 2.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(24, 194).addBox(10.0F, -41.0F, 35.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(144, 195).addBox(-12.0F, -41.0F, 35.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(190, 143).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(224, 143).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(90, 152).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.5F, 18.0F, -23.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(116, 150).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(184, 163).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(30, 166).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.5F, 18.0F, -23.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(150, 150).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(214, 163).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(116, 170).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.5F, 18.0F, 22.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(56, 152).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(0, 166).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(142, 170).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.5F, 18.0F, 22.0F));
        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
