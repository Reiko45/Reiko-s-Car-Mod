package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The box trailer's shape, as Blockbench exports {@code blockbench/box_trailer.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one
 * (keep the bone names: body, trim, lights, wheel_l1, wheel_r1, wheel_l2, wheel_r2). Its texture is {@code textures/entity/trailer/box_trailer.png}. See {@link TrailerModel}
 * for what each bone is for.
 */
public final class BoxTrailerModel {
    private BoxTrailerModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-17.0F, -60.0F, -72.0F, 34.0F, 36.0F, 144.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(0, 180).addBox(-17.0F, -24.0F, -72.0F, 34.0F, 6.0F, 144.0F, CubeDeformation.NONE)
            .texOffs(508, 0).addBox(16.0F, -58.0F, 72.0F, 1.0F, 32.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(508, 33).addBox(-17.0F, -58.0F, 72.0F, 1.0F, 32.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(506, 274).addBox(11.0F, -44.0F, 72.0F, 2.0F, 8.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(506, 283).addBox(-13.0F, -44.0F, 72.0F, 2.0F, 8.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(356, 0).addBox(16.0F, -18.0F, -30.0F, 1.0F, 4.0F, 60.0F, CubeDeformation.NONE)
            .texOffs(356, 64).addBox(-17.0F, -18.0F, -30.0F, 1.0F, 4.0F, 60.0F, CubeDeformation.NONE)
            .texOffs(356, 246).addBox(-10.0F, -19.0F, -64.0F, 20.0F, 1.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(496, 274).addBox(12.0F, -18.0F, -38.0F, 2.0F, 17.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(396, 279).addBox(-14.0F, -18.0F, -38.0F, 2.0F, 17.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(406, 279).addBox(11.0F, -1.0F, -39.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(424, 279).addBox(-15.0F, -1.0F, -39.0F, 4.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(406, 285).addBox(-12.0F, -7.0F, -37.0F, 24.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(356, 128).addBox(6.0F, -18.0F, 36.0F, 4.0F, 4.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(424, 128).addBox(-10.0F, -18.0F, 36.0F, 4.0F, 4.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(460, 274).addBox(10.0F, -14.0F, 67.0F, 8.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(478, 274).addBox(-18.0F, -14.0F, 67.0F, 8.0F, 12.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(396, 274).addBox(-15.0F, -9.0F, 70.0F, 30.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(406, 287).addBox(10.0F, -18.0F, 70.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(414, 287).addBox(-12.0F, -18.0F, 70.0F, 2.0F, 9.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(422, 287).addBox(12.0F, -13.0F, 72.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(432, 287).addBox(-16.0F, -13.0F, 72.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(442, 287).addBox(17.0F, -22.0F, -60.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(450, 287).addBox(-18.0F, -22.0F, -60.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(458, 287).addBox(17.0F, -22.0F, -20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(466, 287).addBox(-18.0F, -22.0F, -20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(474, 287).addBox(17.0F, -22.0F, 20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(482, 287).addBox(-18.0F, -22.0F, 20.0F, 1.0F, 2.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(490, 287).addBox(14.0F, -59.0F, 72.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(490, 289).addBox(-16.0F, -59.0F, 72.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheel_l1 = partdefinition.addOrReplaceChild("wheel_l1", CubeListBuilder.create().texOffs(356, 162).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(404, 190).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(444, 218).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 44.0F));

        PartDefinition wheel_r1 = partdefinition.addOrReplaceChild("wheel_r1", CubeListBuilder.create().texOffs(404, 162).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(448, 190).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(428, 246).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 44.0F));

        PartDefinition wheel_l2 = partdefinition.addOrReplaceChild("wheel_l2", CubeListBuilder.create().texOffs(452, 162).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(356, 218).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(468, 246).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(14.0F, 16.0F, 58.0F));

        PartDefinition wheel_r2 = partdefinition.addOrReplaceChild("wheel_r2", CubeListBuilder.create().texOffs(356, 190).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 12.0F, 16.0F, CubeDeformation.NONE)
            .texOffs(400, 218).addBox(-4.0F, -7.0F, -7.0F, 8.0F, 14.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(356, 263).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 16.0F, 12.0F, CubeDeformation.NONE), PartPose.offset(-14.0F, 16.0F, 58.0F));
        return LayerDefinition.create(meshdefinition, 512, 512);
    }
}
