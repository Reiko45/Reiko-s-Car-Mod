package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The sport bike's shape, as Blockbench exports {@code blockbench/sport_bike.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one.
 * Keep the top-level bone names: body, trim, lights, fork, wheel_rl, signal_l, signal_r.
 * Its texture is {@code textures/entity/car/sport_bike.png}. See {@link CarModel} for what each bone is for.
 */
public final class SportBikeModel {
    private SportBikeModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(44, 73).addBox(-2.5F, -16.0F, -17.0F, 5.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(34, 54).addBox(-3.5F, -17.0F, -16.0F, 7.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(28, 0).addBox(-4.5F, -18.0F, -14.0F, 9.0F, 5.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(88, 53).addBox(3.5F, -13.0F, -9.0F, 1.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(52, 54).addBox(-4.5F, -13.0F, -9.0F, 1.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(22, 22).addBox(4.0F, -15.0F, -5.0F, 1.0F, 9.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(38, 22).addBox(-5.0F, -15.0F, -5.0F, 1.0F, 9.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(86, 0).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 3.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(0, 38).addBox(-4.0F, -18.0F, -7.0F, 8.0F, 5.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(100, 44).addBox(-4.0F, -17.0F, -3.0F, 8.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(46, 46).addBox(-3.0F, -14.0F, 7.0F, 6.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(62, 60).addBox(-2.5F, -16.0F, 13.0F, 5.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(10, 61).addBox(-2.0F, -17.0F, 15.0F, 4.0F, 3.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create()
            .texOffs(28, 12).addBox(-3.0F, -15.0F, -1.0F, 6.0F, 2.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(100, 52).addBox(-2.0F, -15.0F, 7.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(74, 78).addBox(4.5F, -18.0F, -11.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(124, 78).addBox(-5.5F, -18.0F, -11.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(98, 75).addBox(5.5F, -19.5F, -11.5F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(36, 76).addBox(-7.5F, -19.5F, -11.5F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(92, 77).addBox(5.5F, -19.0F, -10.75F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(16, 78).addBox(-7.5F, -19.0F, -10.75F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-3.0F, -10.0F, -6.0F, 6.0F, 7.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(18, 54).addBox(-3.5F, -13.0F, -5.0F, 7.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(56, 13).addBox(2.0F, -7.0F, 2.0F, 1.0F, 2.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 15).addBox(-3.0F, -7.0F, 2.0F, 1.0F, 2.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(76, 26).addBox(3.0F, -7.0F, 2.0F, 1.0F, 2.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(32, 74).addBox(-0.5F, -12.0F, 4.0F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(60, 0).addBox(-3.0F, -4.0F, -6.0F, 1.0F, 1.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(112, 77).addBox(3.0F, -9.0F, 1.0F, 2.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 78).addBox(-5.5F, -9.0F, 1.0F, 2.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(22, 78).addBox(3.0F, -10.0F, 8.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(68, 78).addBox(-5.0F, -10.0F, 8.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(120, 77).addBox(-0.5F, -15.0F, -17.25F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(28, 78).addBox(-0.5F, -14.0F, 15.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(114, 73).addBox(-2.0F, -12.0F, 16.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(116, 79).addBox(2.5F, -13.0F, 15.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(6, 80).addBox(-3.5F, -13.0F, 15.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(98, 78).addBox(2.5F, -14.75F, -17.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(36, 79).addBox(-4.5F, -14.75F, -17.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(60, 79).addBox(4.5F, -15.0F, -17.25F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(84, 79).addBox(-6.0F, -15.0F, -17.25F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(90, 79).addBox(1.5F, -15.75F, 17.25F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(104, 79).addBox(-3.5F, -15.75F, 17.25F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(110, 79).addBox(3.5F, -16.0F, 17.0F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 80).addBox(-5.0F, -16.0F, 17.0F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition windscreen = trim.addOrReplaceChild("windscreen", CubeListBuilder.create()
            .texOffs(98, 59).addBox(-3.5F, -4.0F, -0.5F, 7.0F, 4.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -18.0F, -14.0F, -0.6981F, -0.0F, 0.0F));
        PartDefinition muffler = trim.addOrReplaceChild("muffler", CubeListBuilder.create()
            .texOffs(54, 25).addBox(-1.5F, -2.0F, 0.0F, 3.0F, 3.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(42, 77).addBox(-1.0F, -1.5F, 8.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-4.5F, -7.0F, 6.0F, 0.2618F, -0.0F, 0.0F));
        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create()
            .texOffs(48, 77).addBox(0.5F, -15.5F, -17.25F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(78, 77).addBox(-2.5F, -15.5F, -17.25F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(68, 75).addBox(-2.0F, -20.0F, -8.25F, 4.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(8, 78).addBox(-1.5F, -16.0F, 17.0F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create()
            .texOffs(54, 37).addBox(-2.0F, -7.0F, -5.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 18.0F, -10.0F));
        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create()
            .texOffs(70, 53).addBox(-1.5F, 5.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(24, 38).addBox(-1.5F, 4.0F, -4.0F, 3.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(74, 66).addBox(-1.5F, 3.0F, 2.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(86, 68).addBox(-1.5F, 3.0F, -5.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(98, 68).addBox(-1.5F, 2.0F, -6.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(52, 69).addBox(-1.5F, 2.0F, 3.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(76, 60).addBox(-1.5F, -2.0F, 4.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 61).addBox(-1.5F, -2.0F, -6.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(110, 69).addBox(-1.5F, -3.0F, -6.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(0, 70).addBox(-1.5F, -3.0F, 3.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(12, 70).addBox(-1.5F, -4.0F, 2.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(24, 70).addBox(-1.5F, -4.0F, -5.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(78, 44).addBox(-1.5F, -5.0F, -4.0F, 3.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(0, 54).addBox(-1.5F, -6.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(86, 10).addBox(-0.5F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(0, 74).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, -1.0F));
        PartDefinition fork_legs = fork.addOrReplaceChild("fork_legs", CubeListBuilder.create()
            .texOffs(124, 69).addBox(1.5F, -11.0F, -0.5F, 1.0F, 8.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(56, 73).addBox(-2.5F, -11.0F, -0.5F, 1.0F, 8.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(64, 69).addBox(1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(36, 70).addBox(-3.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(72, 72).addBox(-3.0F, -8.0F, -1.0F, 6.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(88, 72).addBox(-3.0F, -11.0F, -1.0F, 6.0F, 1.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(78, 75).addBox(3.0F, -11.0F, -0.5F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(88, 75).addBox(-7.0F, -11.0F, -0.5F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(84, 77).addBox(4.0F, -11.0F, -1.5F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(104, 77).addBox(-7.0F, -11.0F, -1.5F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.4189F, -0.0F, 0.0F));
        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create()
            .texOffs(0, 47).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(96, 26).addBox(-2.0F, 4.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(46, 62).addBox(-2.0F, 3.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(86, 64).addBox(-2.0F, 3.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(100, 64).addBox(-2.0F, 2.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(60, 65).addBox(-2.0F, 2.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(114, 59).addBox(-2.0F, -2.0F, 4.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(34, 60).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(114, 65).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(10, 66).addBox(-2.0F, -3.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(24, 66).addBox(-2.0F, -4.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(38, 66).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(96, 35).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(20, 47).addBox(-2.0F, -6.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(104, 10).addBox(-0.5F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(104, 73).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 18.0F, 11.0F));
        PartDefinition signal_l = partdefinition.addOrReplaceChild("signal_l", CubeListBuilder.create()
            .texOffs(8, 74).addBox(4.35F, -15.15F, -17.4F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(16, 74).addBox(3.35F, -16.15F, 16.85F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition signal_r = partdefinition.addOrReplaceChild("signal_r", CubeListBuilder.create()
            .texOffs(24, 74).addBox(-6.15F, -15.15F, -17.4F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(60, 75).addBox(-5.15F, -16.15F, 16.85F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
