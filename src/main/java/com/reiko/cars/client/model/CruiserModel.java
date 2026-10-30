package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The cruiser's shape, as Blockbench exports {@code blockbench/cruiser.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one.
 * Keep the top-level bone names: body, trim, lights, fork, wheel_rl, signal_l, signal_r.
 * Its texture is {@code textures/entity/car/cruiser.png}. See {@link CarModel} for what each bone is for.
 */
public final class CruiserModel {
    private CruiserModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 26).addBox(-3.5F, -17.0F, -8.0F, 7.0F, 4.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(44, 81).addBox(-2.5F, -18.0F, -7.0F, 5.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(42, 88).addBox(-2.5F, -21.0F, -13.0F, 5.0F, 5.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(52, 23).addBox(-3.5F, -14.0F, 6.0F, 7.0F, 2.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(58, 92).addBox(-3.5F, -13.0F, 17.0F, 7.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(92, 106).addBox(-3.0F, -11.0F, 19.0F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(66, 82).addBox(3.0F, -11.0F, -1.0F, 1.0F, 4.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(80, 82).addBox(-4.0F, -11.0F, -1.0F, 1.0F, 4.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create()
            .texOffs(68, 106).addBox(-1.0F, -19.0F, -6.0F, 2.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(98, 0).addBox(-3.0F, -9.0F, -7.0F, 6.0F, 6.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(0, 92).addBox(-5.0F, -13.0F, -4.0F, 2.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(54, 36).addBox(3.0F, -8.0F, -7.0F, 1.0F, 5.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(114, 83).addBox(-1.0F, -16.0F, -12.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 38).addBox(2.5F, -7.0F, 2.0F, 1.0F, 2.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(76, 49).addBox(-3.5F, -7.0F, 2.0F, 1.0F, 2.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(30, 62).addBox(3.5F, -7.0F, 2.0F, 1.0F, 2.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(8, 114).addBox(2.75F, -12.0F, 10.0F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(102, 116).addBox(-3.75F, -12.0F, 10.0F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-5.5F, -8.0F, -6.0F, 2.0F, 2.0F, 24.0F, CubeDeformation.NONE)
            .texOffs(52, 0).addBox(-5.5F, -5.0F, -4.0F, 2.0F, 2.0F, 21.0F, CubeDeformation.NONE)
            .texOffs(42, 117).addBox(-5.5F, -8.0F, 18.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(78, 118).addBox(-5.5F, -5.0F, 17.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(22, 83).addBox(4.0F, -5.0F, -10.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(94, 83).addBox(-8.0F, -5.0F, -10.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 61).addBox(-4.0F, -12.0F, -1.0F, 8.0F, 2.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(68, 111).addBox(-3.0F, -13.0F, 4.0F, 6.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 86).addBox(-3.0F, -13.0F, 6.0F, 6.0F, 1.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(84, 111).addBox(2.5F, -22.0F, 11.0F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(88, 111).addBox(-3.5F, -22.0F, 11.0F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(92, 114).addBox(-3.5F, -23.0F, 11.0F, 7.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(80, 106).addBox(-2.5F, -21.0F, 10.75F, 5.0F, 4.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(88, 35).addBox(4.0F, -13.0F, 8.0F, 4.0F, 6.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(30, 36).addBox(-8.0F, -13.0F, 8.0F, 4.0F, 6.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(52, 62).addBox(4.25F, -13.0F, 7.75F, 4.0F, 1.0F, 8.5F, CubeDeformation.NONE)
            .texOffs(78, 65).addBox(-8.25F, -13.0F, 7.75F, 4.0F, 1.0F, 8.5F, CubeDeformation.NONE)
            .texOffs(48, 119).addBox(7.5F, -12.0F, 10.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(52, 119).addBox(-8.5F, -12.0F, 10.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(56, 119).addBox(7.5F, -12.0F, 14.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(60, 119).addBox(-8.5F, -12.0F, 14.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(98, 119).addBox(3.0F, -19.0F, -12.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(8, 120).addBox(-4.0F, -19.0F, -12.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(38, 113).addBox(-2.0F, -10.0F, 19.25F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(92, 119).addBox(2.0F, -18.75F, -13.25F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(32, 120).addBox(-4.0F, -18.75F, -13.25F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(38, 120).addBox(4.0F, -19.0F, -13.5F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(64, 120).addBox(-5.5F, -19.0F, -13.5F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(70, 120).addBox(1.5F, -11.75F, 20.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(106, 120).addBox(-3.5F, -11.75F, 20.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(120, 120).addBox(3.5F, -12.0F, 19.75F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 121).addBox(-5.0F, -12.0F, 19.75F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition cylinder_front = trim.addOrReplaceChild("cylinder_front", CubeListBuilder.create()
            .texOffs(26, 74).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(76, 92).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 1.0F, 4.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -9.0F, -3.0F, 0.4189F, -0.0F, 0.0F));
        PartDefinition cylinder_rear = trim.addOrReplaceChild("cylinder_rear", CubeListBuilder.create()
            .texOffs(104, 74).addBox(-2.5F, -5.0F, -1.5F, 5.0F, 5.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(40, 96).addBox(-2.5F, -6.0F, -1.5F, 5.0F, 1.0F, 4.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -9.0F, -1.0F, -0.4189F, -0.0F, 0.0F));
        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create()
            .texOffs(58, 112).addBox(-2.0F, -20.0F, -13.5F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(120, 118).addBox(-1.5F, -12.0F, 19.75F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create()
            .texOffs(104, 65).addBox(-2.0F, -8.0F, -7.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(48, 112).addBox(-2.0F, -7.0F, -9.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 17.0F, -13.0F));
        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create()
            .texOffs(22, 90).addBox(-1.5F, 6.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(24, 50).addBox(-1.5F, 5.0F, -5.0F, 3.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(58, 97).addBox(-1.5F, 4.0F, -6.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(72, 97).addBox(-1.5F, 4.0F, 2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(48, 116).addBox(-1.5F, 3.0F, 4.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(58, 116).addBox(-1.5F, 3.0F, -6.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(106, 106).addBox(-1.5F, 2.0F, -7.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(36, 109).addBox(-1.5F, 2.0F, 4.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(48, 106).addBox(-1.5F, -2.0F, -7.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(58, 106).addBox(-1.5F, -2.0F, 5.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 110).addBox(-1.5F, -3.0F, -7.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(92, 110).addBox(-1.5F, -3.0F, 4.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(92, 116).addBox(-1.5F, -4.0F, 4.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(32, 117).addBox(-1.5F, -4.0F, -6.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(86, 97).addBox(-1.5F, -5.0F, -6.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(100, 97).addBox(-1.5F, -5.0F, 2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(50, 51).addBox(-1.5F, -6.0F, -5.0F, 3.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(94, 90).addBox(-1.5F, -7.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(98, 15).addBox(-0.5F, -5.0F, -5.0F, 1.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(120, 111).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition fork_legs = fork.addOrReplaceChild("fork_legs", CubeListBuilder.create()
            .texOffs(12, 110).addBox(1.5F, -15.0F, -0.5F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(116, 110).addBox(-2.5F, -15.0F, -0.5F, 1.0F, 11.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(120, 97).addBox(1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(120, 104).addBox(-3.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 107).addBox(-3.5F, -12.0F, -1.0F, 7.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(18, 109).addBox(-3.5F, -15.0F, -1.0F, 7.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(16, 119).addBox(1.0F, -17.0F, 0.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(20, 119).addBox(-2.0F, -17.0F, 0.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(16, 113).addBox(-5.0F, -18.0F, 0.0F, 10.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(68, 118).addBox(5.0F, -18.0F, 0.0F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(106, 118).addBox(-9.0F, -18.0F, 0.0F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 114).addBox(9.0F, -18.0F, 0.0F, 1.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(68, 114).addBox(-10.0F, -18.0F, 0.0F, 1.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(24, 119).addBox(6.0F, -20.0F, 0.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(28, 119).addBox(-7.0F, -20.0F, 0.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(120, 115).addBox(5.0F, -22.0F, 0.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 118).addBox(-8.0F, -22.0F, 0.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, -0.5236F, -0.0F, 0.0F));
        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create()
            .texOffs(78, 75).addBox(-2.5F, 5.0F, -3.0F, 5.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 70).addBox(-2.5F, 4.0F, -4.0F, 5.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(40, 101).addBox(-2.5F, 3.0F, 2.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(56, 102).addBox(-2.5F, 3.0F, -5.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(72, 102).addBox(-2.5F, 2.0F, -6.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(88, 102).addBox(-2.5F, 2.0F, 3.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(12, 97).addBox(-2.5F, -2.0F, 4.0F, 5.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(26, 97).addBox(-2.5F, -2.0F, -6.0F, 5.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(104, 102).addBox(-2.5F, -3.0F, -6.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(0, 103).addBox(-2.5F, -3.0F, 3.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(16, 103).addBox(-2.5F, -4.0F, 2.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(32, 105).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(52, 72).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(0, 79).addBox(-2.5F, -6.0F, -3.0F, 5.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(100, 49).addBox(-0.5F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(104, 110).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 18.0F, 13.0F));
        PartDefinition signal_l = partdefinition.addOrReplaceChild("signal_l", CubeListBuilder.create()
            .texOffs(76, 114).addBox(3.85F, -19.15F, -13.65F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(108, 114).addBox(3.35F, -12.15F, 19.6F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition signal_r = partdefinition.addOrReplaceChild("signal_r", CubeListBuilder.create()
            .texOffs(16, 115).addBox(-5.65F, -19.15F, -13.65F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(24, 115).addBox(-5.15F, -12.15F, 19.6F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
