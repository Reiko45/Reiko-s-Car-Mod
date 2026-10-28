package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The semi truck's shape, as Blockbench exports {@code blockbench/semi_truck.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr, wheel_rl2, wheel_rr2). Its texture is {@code textures/entity/car/semi_truck.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class SemiTruckModel {
    private SemiTruckModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(400, 58).addBox(-14.0F, -32.0F, -46.0F, 28.0F, 16.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(0, 152).addBox(14.0F, -19.0F, -44.0F, 4.0F, 3.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(362, 155).addBox(-18.0F, -19.0F, -44.0F, 4.0F, 3.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(70, 180).addBox(14.0F, -19.0F, -46.0F, 4.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(70, 189).addBox(-18.0F, -19.0F, -46.0F, 4.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(370, 197).addBox(-17.0F, -34.0F, -22.0F, 34.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(198, 101).addBox(15.0F, -34.0F, -21.0F, 2.0F, 20.0F, 19.0F, CubeDeformation.NONE)
            .texOffs(240, 101).addBox(-17.0F, -34.0F, -21.0F, 2.0F, 20.0F, 19.0F, CubeDeformation.NONE)
            .texOffs(82, 180).addBox(16.0F, -48.0F, -22.0F, 1.0F, 14.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(82, 195).addBox(-17.0F, -48.0F, -22.0F, 1.0F, 14.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(480, 190).addBox(16.0F, -48.0F, -2.0F, 1.0F, 14.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(444, 197).addBox(-17.0F, -48.0F, -2.0F, 1.0F, 14.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(400, 98).addBox(-17.0F, -50.0F, -22.0F, 34.0F, 2.0F, 22.0F, CubeDeformation.NONE)
            .texOffs(370, 202).addBox(-17.0F, -48.0F, -25.0F, 34.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(102, 101).addBox(-16.0F, -53.0F, -16.0F, 32.0F, 3.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(256, 176).addBox(-17.0F, -56.0F, -8.0F, 34.0F, 3.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(400, 0).addBox(-17.0F, -56.0F, 0.0F, 34.0F, 42.0F, 16.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(340, 178).addBox(-16.0F, -47.0F, -22.0F, 32.0F, 13.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(378, 122).addBox(16.0F, -48.0F, -21.0F, 1.0F, 14.0F, 19.0F, CubeDeformation.NONE)
            .texOffs(418, 122).addBox(-17.0F, -48.0F, -21.0F, 1.0F, 14.0F, 19.0F, CubeDeformation.NONE)
            .texOffs(0, 101).addBox(-15.0F, -16.0F, -21.0F, 30.0F, 2.0F, 21.0F, CubeDeformation.NONE)
            .texOffs(406, 190).addBox(-15.0F, -34.0F, -21.0F, 30.0F, 4.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(230, 196).addBox(5.0F, -36.0F, -18.0F, 8.0F, 8.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(194, 196).addBox(3.0F, -25.0F, -15.0F, 12.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(34, 180).addBox(3.0F, -40.0F, -9.0F, 12.0F, 15.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(450, 197).addBox(7.0F, -43.0F, -9.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(34, 197).addBox(-15.0F, -25.0F, -15.0F, 12.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(256, 187).addBox(-15.0F, -40.0F, -9.0F, 12.0F, 15.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(70, 198).addBox(-11.0F, -43.0F, -9.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(120, 176).addBox(-11.0F, -31.0F, -48.0F, 22.0F, 17.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(406, 179).addBox(-18.0F, -13.0F, -50.0F, 36.0F, 7.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(6.0F, -13.0F, -46.0F, 4.0F, 5.0F, 96.0F, CubeDeformation.NONE)
            .texOffs(200, 0).addBox(-10.0F, -13.0F, -46.0F, 4.0F, 5.0F, 96.0F, CubeDeformation.NONE)
            .texOffs(120, 195).addBox(-10.0F, -12.0F, 46.0F, 20.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(410, 155).addBox(11.0F, -14.0F, -20.0F, 6.0F, 7.0F, 17.0F, CubeDeformation.NONE)
            .texOffs(172, 168).addBox(-17.0F, -14.0F, -20.0F, 6.0F, 7.0F, 17.0F, CubeDeformation.NONE)
            .texOffs(168, 192).addBox(17.0F, -7.0F, -18.0F, 1.0F, 3.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(344, 192).addBox(-18.0F, -7.0F, -18.0F, 1.0F, 3.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(502, 122).addBox(18.0F, -68.0F, 14.0F, 2.0F, 54.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(486, 178).addBox(-20.0F, -68.0F, 14.0F, 2.0F, 54.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(472, 190).addBox(19.0F, -46.0F, -22.0F, 2.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(248, 196).addBox(-21.0F, -46.0F, -22.0F, 2.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(506, 202).addBox(17.0F, -41.0F, -22.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(10, 203).addBox(-19.0F, -41.0F, -22.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(284, 192).addBox(-12.0F, -14.0F, 16.0F, 24.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(282, 101).addBox(-10.0F, -18.0F, 24.0F, 20.0F, 3.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(494, 178).addBox(10.0F, -13.0F, 48.0F, 8.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(494, 190).addBox(-18.0F, -13.0F, 48.0F, 8.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(284, 199).addBox(-12.0F, -12.0F, 49.0F, 24.0F, 4.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(450, 202).addBox(12.0F, -22.0F, -47.0F, 5.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(494, 202).addBox(-17.0F, -22.0F, -47.0F, 5.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(16, 203).addBox(8.0F, -48.0F, -26.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(22, 203).addBox(3.0F, -48.0F, -26.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(28, 203).addBox(-5.0F, -48.0F, -26.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(70, 203).addBox(-10.0F, -48.0F, -26.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(462, 197).addBox(18.0F, -18.0F, -42.0F, 1.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(334, 199).addBox(-19.0F, -18.0F, -42.0F, 1.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(462, 202).addBox(8.0F, -12.0F, 50.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 203).addBox(-12.0F, -12.0F, 50.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(88, 148).addBox(-2.5F, -6.0F, -8.0F, 5.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(48, 152).addBox(-2.5F, -7.0F, -7.0F, 5.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(0, 175).addBox(-2.5F, -8.0F, -6.0F, 5.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(15.5F, 16.0F, -34.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(130, 148).addBox(-2.5F, -6.0F, -8.0F, 5.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(218, 168).addBox(-2.5F, -7.0F, -7.0F, 5.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(86, 176).addBox(-2.5F, -8.0F, -6.0F, 5.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-15.5F, 16.0F, -34.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(102, 120).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(458, 122).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(242, 140).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 24.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(150, 120).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(0, 124).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(282, 148).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 24.0F));

        PartDefinition wheel_rl2 = partdefinition.addOrReplaceChild("wheel_rl2", CubeListBuilder.create().texOffs(282, 120).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(44, 124).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(322, 148).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 40.0F));

        PartDefinition wheel_rr2 = partdefinition.addOrReplaceChild("wheel_rr2", CubeListBuilder.create().texOffs(330, 120).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(198, 140).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(458, 150).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 40.0F));
        return LayerDefinition.create(meshdefinition, 512, 512);
    }
}
