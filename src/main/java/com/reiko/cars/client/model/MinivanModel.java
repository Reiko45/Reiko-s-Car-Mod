package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The minivan's shape, as Blockbench exports {@code blockbench/minivan.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/minivan.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class MinivanModel {
    private MinivanModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(72, 132).addBox(-14.0F, -16.0F, -32.0F, 28.0F, 10.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(182, 222).addBox(-14.0F, -18.0F, -22.0F, 28.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(148, 86).addBox(12.0F, -18.0F, -20.0F, 2.0F, 12.0F, 34.0F, CubeDeformation.NONE)
            .texOffs(0, 106).addBox(-14.0F, -18.0F, -20.0F, 2.0F, 12.0F, 34.0F, CubeDeformation.NONE)
            .texOffs(220, 86).addBox(12.0F, -18.0F, 14.0F, 2.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(220, 114).addBox(-14.0F, -18.0F, 14.0F, 2.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(98, 161).addBox(-14.0F, -18.0F, 30.0F, 28.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(248, 201).addBox(13.0F, -31.0F, -20.0F, 1.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(252, 201).addBox(-14.0F, -31.0F, -20.0F, 1.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(246, 215).addBox(13.0F, -31.0F, -4.0F, 1.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(250, 215).addBox(-14.0F, -31.0F, -4.0F, 1.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(246, 229).addBox(13.0F, -31.0F, 13.0F, 1.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(250, 229).addBox(-14.0F, -31.0F, 13.0F, 1.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(248, 171).addBox(13.0F, -31.0F, 30.0F, 1.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(248, 186).addBox(-14.0F, -31.0F, 30.0F, 1.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-14.0F, -33.0F, -20.0F, 28.0F, 2.0F, 52.0F, CubeDeformation.NONE)
            .texOffs(238, 231).addBox(14.0F, -21.0F, -19.0F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(138, 232).addBox(-16.0F, -21.0F, -19.0F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(158, 161).addBox(-13.0F, -31.0F, -20.0F, 26.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(98, 175).addBox(-13.0F, -31.0F, 31.0F, 26.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(34, 152).addBox(13.0F, -31.0F, -19.0F, 1.0F, 13.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(66, 153).addBox(-14.0F, -31.0F, -19.0F, 1.0F, 13.0F, 15.0F, CubeDeformation.NONE)
            .texOffs(150, 132).addBox(13.0F, -31.0F, -3.0F, 1.0F, 13.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(184, 132).addBox(-14.0F, -31.0F, -3.0F, 1.0F, 13.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(218, 142).addBox(13.0F, -31.0F, 14.0F, 1.0F, 13.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(0, 152).addBox(-14.0F, -31.0F, 14.0F, 1.0F, 13.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(0, 54).addBox(-12.0F, -8.0F, -20.0F, 24.0F, 2.0F, 50.0F, CubeDeformation.NONE)
            .texOffs(182, 211).addBox(-12.0F, -18.0F, -20.0F, 24.0F, 3.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(58, 229).addBox(5.0F, -18.0F, -17.0F, 5.0F, 5.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 221).addBox(2.0F, -10.0F, -15.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(138, 209).addBox(2.0F, -23.0F, -9.0F, 9.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(70, 231).addBox(4.5F, -26.0F, -9.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(122, 224).addBox(-11.0F, -10.0F, -15.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(160, 209).addBox(-11.0F, -23.0F, -9.0F, 9.0F, 13.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(82, 231).addBox(-8.5F, -26.0F, -9.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(152, 224).addBox(2.0F, -10.0F, -2.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(78, 217).addBox(2.0F, -22.0F, 4.0F, 9.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(94, 231).addBox(4.5F, -25.0F, 4.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(182, 226).addBox(-11.0F, -10.0F, -2.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(100, 217).addBox(-11.0F, -22.0F, 4.0F, 9.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(106, 231).addBox(-8.5F, -25.0F, 4.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(78, 209).addBox(-12.0F, -10.0F, 12.0F, 24.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(152, 175).addBox(-12.0F, -22.0F, 18.0F, 24.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(160, 0).addBox(10.0F, -34.0F, -14.0F, 1.0F, 1.0F, 42.0F, CubeDeformation.NONE)
            .texOffs(160, 43).addBox(-11.0F, -34.0F, -14.0F, 1.0F, 1.0F, 42.0F, CubeDeformation.NONE)
            .texOffs(182, 217).addBox(-14.0F, -9.0F, -33.0F, 28.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 229).addBox(-14.0F, -8.0F, 32.0F, 28.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(212, 226).addBox(-8.0F, -14.0F, -33.0F, 16.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(212, 231).addBox(-3.0F, -12.0F, 32.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(118, 232).addBox(9.0F, -15.0F, -33.0F, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(128, 232).addBox(-13.0F, -15.0F, -33.0F, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(226, 231).addBox(12.0F, -20.0F, 32.0F, 2.0F, 8.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(232, 231).addBox(-14.0F, -20.0F, 32.0F, 2.0F, 8.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(212, 171).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(102, 189).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(222, 191).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.5F, 18.0F, -21.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(0, 181).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(132, 189).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 201).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.5F, 18.0F, -21.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(34, 181).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(162, 189).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(26, 201).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(12.5F, 18.0F, 21.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(68, 189).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 8.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(192, 191).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(52, 209).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 12.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-12.5F, 18.0F, 21.0F));
        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
