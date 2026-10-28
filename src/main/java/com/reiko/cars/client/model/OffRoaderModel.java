package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The off-roader's shape, exported by Blockbench from {@code blockbench/off_roader.bbmodel}. To change it, edit that
 * file in Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()}
 * over this one (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is
 * {@code textures/entity/car/off_roader.png}. See {@link CarModel} for what each bone is for.
 */
public final class OffRoaderModel {
    private OffRoaderModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 83).addBox(-14.0F, -19.0F, -28.0F, 28.0F, 12.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(138, 0).addBox(12.0F, -19.0F, -12.0F, 2.0F, 12.0F, 38.0F, CubeDeformation.NONE)
        .texOffs(124, 50).addBox(-14.0F, -19.0F, -12.0F, 2.0F, 12.0F, 38.0F, CubeDeformation.NONE)
        .texOffs(0, 168).addBox(-14.0F, -19.0F, 26.0F, 28.0F, 12.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(252, 217).addBox(13.0F, -30.0F, -12.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(252, 229).addBox(-14.0F, -30.0F, -12.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(26, 240).addBox(13.0F, -30.0F, 3.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(156, 240).addBox(-14.0F, -30.0F, 3.0F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(246, 217).addBox(13.0F, -30.0F, 26.0F, 1.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(246, 230).addBox(-14.0F, -30.0F, 26.0F, 1.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(0, 0).addBox(-14.0F, -32.0F, -13.0F, 28.0F, 2.0F, 41.0F, CubeDeformation.NONE)
        .texOffs(30, 241).addBox(14.0F, -21.0F, -11.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(38, 241).addBox(-16.0F, -21.0F, -11.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(76, 205).addBox(-13.0F, -30.0F, -12.0F, 26.0F, 11.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(130, 206).addBox(-13.0F, -30.0F, 26.0F, 26.0F, 11.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(156, 181).addBox(13.0F, -30.0F, -11.0F, 1.0F, 11.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(186, 181).addBox(-14.0F, -30.0F, -11.0F, 1.0F, 11.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(0, 111).addBox(13.0F, -30.0F, 4.0F, 1.0F, 11.0F, 22.0F, CubeDeformation.NONE)
        .texOffs(46, 133).addBox(-14.0F, -30.0F, 4.0F, 1.0F, 11.0F, 22.0F, CubeDeformation.NONE)
        .texOffs(0, 43).addBox(-12.0F, -9.0F, -12.0F, 24.0F, 2.0F, 38.0F, CubeDeformation.NONE)
        .texOffs(158, 221).addBox(-12.0F, -20.0F, -12.0F, 24.0F, 3.0F, 3.0F, CubeDeformation.NONE)
        .texOffs(220, 238).addBox(4.0F, -20.0F, -9.0F, 5.0F, 5.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(216, 230).addBox(2.0F, -11.0F, -6.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(80, 226).addBox(2.0F, -22.0F, 0.0F, 9.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(232, 238).addBox(4.5F, -25.0F, 0.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(0, 232).addBox(-11.0F, -11.0F, -6.0F, 9.0F, 2.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(102, 226).addBox(-11.0F, -22.0F, 0.0F, 9.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(0, 240).addBox(-8.5F, -25.0F, 0.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(80, 218).addBox(-11.0F, -11.0F, 8.0F, 22.0F, 2.0F, 6.0F, CubeDeformation.NONE)
        .texOffs(0, 208).addBox(-11.0F, -22.0F, 14.0F, 22.0F, 11.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(216, 181).addBox(14.0F, -16.0F, -26.0F, 3.0F, 2.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(0, 182).addBox(-17.0F, -16.0F, -26.0F, 3.0F, 2.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(38, 190).addBox(14.0F, -16.0F, 9.0F, 3.0F, 2.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(216, 199).addBox(-17.0F, -16.0F, 9.0F, 3.0F, 2.0F, 16.0F, CubeDeformation.NONE)
        .texOffs(184, 206).addBox(14.0F, -7.0F, -8.0F, 2.0F, 1.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(48, 217).addBox(-16.0F, -7.0F, -8.0F, 2.0F, 1.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(158, 227).addBox(-14.0F, -10.0F, -29.0F, 28.0F, 4.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(124, 236).addBox(-11.0F, -10.0F, -31.0F, 22.0F, 2.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(172, 238).addBox(-11.0F, -17.0F, -31.0F, 22.0F, 1.0F, 2.0F, CubeDeformation.NONE)
        .texOffs(62, 241).addBox(8.0F, -16.0F, -31.0F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(66, 241).addBox(-9.0F, -16.0F, -31.0F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(30, 232).addBox(-8.0F, -17.0F, -29.0F, 16.0F, 6.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(158, 232).addBox(-14.0F, -9.0F, 28.0F, 28.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(12, 240).addBox(-3.0F, -12.0F, 28.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(88, 100).addBox(10.0F, -33.0F, -8.0F, 1.0F, 1.0F, 32.0F, CubeDeformation.NONE)
        .texOffs(154, 100).addBox(-11.0F, -33.0F, -8.0F, 1.0F, 1.0F, 32.0F, CubeDeformation.NONE)
        .texOffs(30, 239).addBox(-10.0F, -33.0F, -4.0F, 20.0F, 1.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(72, 239).addBox(-10.0F, -33.0F, 8.0F, 20.0F, 1.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(114, 240).addBox(-10.0F, -33.0F, 20.0F, 20.0F, 1.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(136, 218).addBox(-4.0F, -25.0F, 28.0F, 8.0F, 12.0F, 3.0F, CubeDeformation.NONE)
        .texOffs(216, 217).addBox(-5.0F, -24.0F, 28.0F, 10.0F, 10.0F, 3.0F, CubeDeformation.NONE)
        .texOffs(0, 221).addBox(-6.0F, -23.0F, 28.0F, 12.0F, 8.0F, 3.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(46, 241).addBox(9.0F, -16.0F, -29.0F, 3.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(54, 241).addBox(-12.0F, -16.0F, -29.0F, 3.0F, 3.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(160, 240).addBox(12.0F, -17.0F, 28.0F, 2.0F, 5.0F, 1.0F, CubeDeformation.NONE)
        .texOffs(166, 240).addBox(-14.0F, -17.0F, 28.0F, 2.0F, 5.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(92, 133).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(0, 144).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(200, 157).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 17.0F, -18.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(132, 133).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(92, 157).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(60, 166).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 17.0F, -18.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(172, 133).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(128, 157).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(92, 181).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 17.0F, 17.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(212, 133).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
        .texOffs(164, 157).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
        .texOffs(124, 181).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 17.0F, 17.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
