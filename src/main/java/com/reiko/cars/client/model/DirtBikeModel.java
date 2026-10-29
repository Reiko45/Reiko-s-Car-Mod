package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The dirt bike's shape, as Blockbench exports {@code blockbench/dirt_bike.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one.
 * Keep the top-level bone names: body, trim, lights, fork, wheel_rl.
 * Its texture is {@code textures/entity/car/dirt_bike.png}. See {@link CarModel} for what each bone is for.
 */
public final class DirtBikeModel {
    private DirtBikeModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(98, 41).addBox(-3.0F, -18.0F, -7.0F, 6.0F, 4.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(46, 42).addBox(3.0F, -18.0F, -9.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(60, 42).addBox(-4.0F, -18.0F, -9.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(20, 50).addBox(2.5F, -17.0F, 3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(98, 50).addBox(-3.5F, -17.0F, 3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(56, 20).addBox(-2.0F, -17.0F, 9.0F, 4.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(114, 81).addBox(-1.5F, -17.5F, 19.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create()
            .texOffs(88, 59).addBox(2.75F, -16.0F, 4.0F, 1.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(52, 61).addBox(-3.75F, -16.0F, 4.0F, 1.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-2.5F, -18.0F, -4.0F, 5.0F, 1.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(0, 15).addBox(-2.5F, -10.0F, -6.0F, 5.0F, 6.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(112, 50).addBox(-2.0F, -14.0F, -6.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(34, 61).addBox(-2.5F, -15.0F, -6.0F, 5.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(112, 58).addBox(-1.0F, -17.0F, -10.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(84, 20).addBox(-3.0F, -4.0F, -7.0F, 6.0F, 1.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(26, 39).addBox(1.5F, -8.0F, 2.0F, 1.0F, 2.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(0, 40).addBox(-2.5F, -8.0F, 2.0F, 1.0F, 2.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(78, 41).addBox(2.5F, -8.0F, 2.0F, 1.0F, 2.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(98, 62).addBox(-1.0F, -15.0F, 1.0F, 2.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 51).addBox(-4.0F, -12.0F, -8.0F, 2.0F, 2.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(76, 82).addBox(3.0F, -8.0F, 0.0F, 2.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(84, 82).addBox(-5.0F, -8.0F, 0.0F, 2.0F, 1.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition silencer = trim.addOrReplaceChild("silencer", CubeListBuilder.create()
            .texOffs(38, 0).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 3.0F, 13.0F, CubeDeformation.NONE)
            .texOffs(124, 81).addBox(-0.5F, -1.5F, 13.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-3.5F, -12.0F, 0.0F, 0.1745F, -0.0F, 0.0F));
        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create()
            .texOffs(8, 83).addBox(-1.5F, -15.0F, -16.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(68, 83).addBox(-1.0F, -16.5F, 19.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create()
            .texOffs(26, 16).addBox(-2.0F, -10.0F, -8.0F, 4.0F, 1.0F, 11.0F, CubeDeformation.NONE)
            .texOffs(104, 81).addBox(-1.5F, -9.0F, -9.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 17.0F, -12.0F));
        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create()
            .texOffs(74, 52).addBox(-1.5F, 6.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(26, 28).addBox(-1.5F, 5.0F, -5.0F, 3.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(70, 66).addBox(-1.5F, 4.0F, -6.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(0, 67).addBox(-1.5F, 4.0F, 2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(106, 78).addBox(-1.5F, 3.0F, 4.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(46, 79).addBox(-1.5F, 3.0F, -6.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(46, 75).addBox(-1.5F, 2.0F, -7.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(58, 75).addBox(-1.5F, 2.0F, 4.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(98, 71).addBox(-1.5F, -2.0F, -7.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 72).addBox(-1.5F, -2.0F, 5.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(10, 76).addBox(-1.5F, -3.0F, -7.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(70, 76).addBox(-1.5F, -3.0F, 4.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(56, 79).addBox(-1.5F, -4.0F, 4.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(8, 80).addBox(-1.5F, -4.0F, -6.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(84, 67).addBox(-1.5F, -5.0F, -6.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(14, 68).addBox(-1.5F, -5.0F, 2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(0, 29).addBox(-1.5F, -6.0F, -5.0F, 3.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(34, 54).addBox(-1.5F, -7.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(68, 0).addBox(-0.5F, -5.0F, -5.0F, 1.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(118, 77).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, -1.0F));
        PartDefinition fork_legs = fork.addOrReplaceChild("fork_legs", CubeListBuilder.create()
            .texOffs(120, 67).addBox(1.5F, -15.0F, -0.5F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(124, 67).addBox(-2.5F, -15.0F, -0.5F, 1.0F, 9.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(120, 58).addBox(1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(62, 61).addBox(-3.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(10, 73).addBox(-3.5F, -13.0F, -1.0F, 7.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(28, 74).addBox(-3.5F, -16.0F, -1.0F, 7.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(60, 83).addBox(-1.5F, -17.0F, -0.5F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(14, 66).addBox(-9.0F, -18.0F, -0.5F, 18.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(38, 82).addBox(-3.0F, -19.0F, -0.5F, 6.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 82).addBox(7.0F, -18.0F, -1.0F, 2.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(52, 82).addBox(-9.0F, -18.0F, -1.0F, 2.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 60).addBox(-3.0F, -15.0F, -2.0F, 6.0F, 6.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.4538F, -0.0F, 0.0F));
        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create()
            .texOffs(52, 54).addBox(-1.5F, 6.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(84, 30).addBox(-1.5F, 5.0F, -5.0F, 3.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(28, 68).addBox(-1.5F, 4.0F, -6.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(42, 69).addBox(-1.5F, 4.0F, 2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(66, 80).addBox(-1.5F, 3.0F, 4.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(18, 81).addBox(-1.5F, 3.0F, -6.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(22, 77).addBox(-1.5F, 2.0F, -7.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(34, 77).addBox(-1.5F, 2.0F, 4.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(84, 72).addBox(-1.5F, -2.0F, -7.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(108, 72).addBox(-1.5F, -2.0F, 5.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(94, 77).addBox(-1.5F, -3.0F, -7.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(82, 78).addBox(-1.5F, -3.0F, 4.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(28, 81).addBox(-1.5F, -4.0F, 4.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(94, 81).addBox(-1.5F, -4.0F, -6.0F, 3.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(56, 70).addBox(-1.5F, -5.0F, -6.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(70, 71).addBox(-1.5F, -5.0F, 2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(52, 31).addBox(-1.5F, -6.0F, -5.0F, 3.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(70, 59).addBox(-1.5F, -7.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(90, 0).addBox(-0.5F, -5.0F, -5.0F, 1.0F, 10.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 78).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 17.0F, 11.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
