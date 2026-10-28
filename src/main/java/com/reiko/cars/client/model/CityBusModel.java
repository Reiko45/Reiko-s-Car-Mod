package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The city bus's shape, as Blockbench exports {@code blockbench/city_bus.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_fl, wheel_fr, wheel_rl, wheel_rr). Its texture is {@code textures/entity/car/city_bus.png}. See {@link CarModel}
 * for what each bone is for.
 */
public final class CityBusModel {
    private CityBusModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(344, 256).addBox(-16.0F, -22.0F, -60.0F, 32.0F, 16.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 122).addBox(15.0F, -22.0F, -58.0F, 1.0F, 16.0F, 116.0F, CubeDeformation.NONE)
            .texOffs(1006, 194).addBox(-16.0F, -22.0F, -58.0F, 1.0F, 16.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 254).addBox(-16.0F, -22.0F, -42.0F, 1.0F, 16.0F, 100.0F, CubeDeformation.NONE)
            .texOffs(234, 122).addBox(-16.0F, -50.0F, 58.0F, 32.0F, 44.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1006, 26).addBox(15.0F, -48.0F, -58.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 26).addBox(-16.0F, -48.0F, -58.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1018, 26).addBox(15.0F, -48.0F, -44.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1006, 54).addBox(-16.0F, -48.0F, -44.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 54).addBox(15.0F, -48.0F, -30.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1018, 54).addBox(-16.0F, -48.0F, -30.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1006, 82).addBox(15.0F, -48.0F, -16.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 82).addBox(-16.0F, -48.0F, -16.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1018, 82).addBox(15.0F, -48.0F, -2.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1006, 110).addBox(-16.0F, -48.0F, -2.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 110).addBox(15.0F, -48.0F, 12.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1018, 110).addBox(-16.0F, -48.0F, 12.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1006, 138).addBox(15.0F, -48.0F, 26.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 138).addBox(-16.0F, -48.0F, 26.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1018, 138).addBox(15.0F, -48.0F, 40.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1006, 166).addBox(-16.0F, -48.0F, 40.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 166).addBox(15.0F, -48.0F, 54.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1018, 166).addBox(-16.0F, -48.0F, 54.0F, 1.0F, 26.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(529, 138).addBox(15.0F, -50.0F, -58.0F, 1.0F, 2.0F, 116.0F, CubeDeformation.NONE)
            .texOffs(763, 138).addBox(-16.0F, -50.0F, -58.0F, 1.0F, 2.0F, 116.0F, CubeDeformation.NONE)
            .texOffs(532, 280).addBox(-16.0F, -50.0F, -60.0F, 32.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-16.0F, -52.0F, -60.0F, 32.0F, 2.0F, 120.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(282, 256).addBox(-15.0F, -46.0F, -60.0F, 30.0F, 24.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(596, 0).addBox(15.0F, -48.0F, -56.0F, 0.5F, 26.0F, 112.0F, CubeDeformation.NONE)
            .texOffs(304, 118).addBox(-15.5F, -48.0F, -56.0F, 0.5F, 26.0F, 112.0F, CubeDeformation.NONE)
            .texOffs(997, 0).addBox(-16.0F, -22.0F, -56.0F, 0.5F, 14.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(304, 0).addBox(-15.0F, -10.0F, -58.0F, 30.0F, 2.0F, 116.0F, CubeDeformation.NONE)
            .texOffs(776, 274).addBox(-15.0F, -22.0F, -58.0F, 30.0F, 4.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(1006, 214).addBox(7.0F, -23.0F, -54.0F, 6.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(300, 281).addBox(6.0F, -18.0F, -52.0F, 8.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(752, 280).addBox(6.0F, -30.0F, -46.0F, 8.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(600, 280).addBox(2.0F, -18.0F, -33.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(976, 256).addBox(2.0F, -30.0F, -27.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(638, 280).addBox(-15.0F, -18.0F, -33.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(976, 270).addBox(-15.0F, -30.0F, -27.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(676, 280).addBox(2.0F, -18.0F, -17.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(412, 272).addBox(2.0F, -30.0F, -11.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(714, 280).addBox(-15.0F, -18.0F, -17.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(442, 272).addBox(-15.0F, -30.0F, -11.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(842, 280).addBox(2.0F, -18.0F, -1.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(202, 277).addBox(2.0F, -30.0F, 5.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(880, 280).addBox(-15.0F, -18.0F, -1.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(232, 277).addBox(-15.0F, -30.0F, 5.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(918, 280).addBox(2.0F, -18.0F, 15.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(472, 280).addBox(2.0F, -30.0F, 21.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(262, 281).addBox(-15.0F, -18.0F, 15.0F, 13.0F, 3.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(502, 280).addBox(-15.0F, -30.0F, 21.0F, 13.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(776, 256).addBox(-14.0F, -18.0F, 40.0F, 28.0F, 3.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(412, 256).addBox(-14.0F, -32.0F, 48.0F, 28.0F, 14.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(202, 254).addBox(-10.0F, -55.0F, -10.0F, 20.0F, 3.0F, 20.0F, CubeDeformation.NONE)
            .texOffs(776, 267).addBox(-16.0F, -9.0F, -62.0F, 32.0F, 5.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(344, 274).addBox(-16.0F, -9.0F, 60.0F, 32.0F, 5.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 194).addBox(17.0F, -44.0F, -62.0F, 3.0F, 8.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1012, 204).addBox(-20.0F, -44.0F, -62.0F, 3.0F, 8.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1016, 234).addBox(15.0F, -45.0F, -61.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(1016, 236).addBox(-17.0F, -45.0F, -61.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(1006, 230).addBox(-3.0F, -13.0F, 60.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(328, 281).addBox(-12.0F, -49.0F, -61.0F, 24.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(1006, 234).addBox(11.0F, -14.0F, -61.0F, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(1006, 239).addBox(-15.0F, -14.0F, -61.0F, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(1006, 221).addBox(13.0F, -18.0F, 60.0F, 3.0F, 8.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(1014, 221).addBox(-16.0F, -18.0F, 60.0F, 3.0F, 8.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_fl = partdefinition.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(472, 256).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(632, 256).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(848, 256).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(13.0F, 17.0F, -40.0F));

        PartDefinition wheel_fr = partdefinition.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(512, 256).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(668, 256).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(880, 256).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(-13.0F, 17.0F, -40.0F));

        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(552, 256).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(704, 256).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(912, 256).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(13.0F, 17.0F, 36.0F));

        PartDefinition wheel_rr = partdefinition.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(592, 256).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 10.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(740, 256).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(944, 256).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 14.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(-13.0F, 17.0F, 36.0F));
        return LayerDefinition.create(meshdefinition, 1024, 1024);
    }
}
