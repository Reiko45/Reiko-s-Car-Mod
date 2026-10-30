package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The scooter's shape, as Blockbench exports {@code blockbench/scooter.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one.
 * Keep the top-level bone names: body, trim, lights, fork, wheel_rl, signal_l, signal_r.
 * Its texture is {@code textures/entity/car/scooter.png}. See {@link CarModel} for what each bone is for.
 */
public final class ScooterModel {
    private ScooterModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.0F, -5.0F, -8.0F, 8.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(64, 32).addBox(-4.0F, -11.0F, 1.0F, 8.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 22).addBox(-5.5F, -10.0F, 2.0F, 11.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(84, 13).addBox(-4.5F, -12.0F, 2.0F, 9.0F, 7.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(36, 0).addBox(-6.0F, -10.0F, 4.0F, 12.0F, 5.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(72, 0).addBox(-4.5F, -12.0F, 4.0F, 9.0F, 7.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(100, 32).addBox(-5.0F, -9.0F, 10.0F, 10.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(46, 24).addBox(-4.0F, -11.0F, 10.0F, 8.0F, 5.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(10, 42).addBox(-3.0F, -9.0F, 12.0F, 6.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition leg_shield = body.addOrReplaceChild("leg_shield", CubeListBuilder.create()
            .texOffs(102, 0).addBox(-5.0F, -11.0F, -1.0F, 10.0F, 11.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(18, 33).addBox(-4.0F, -13.0F, -1.0F, 8.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(116, 37).addBox(4.0F, -9.0F, 1.0F, 1.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(122, 37).addBox(-5.0F, -9.0F, 1.0F, 1.0F, 9.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(24, 42).addBox(-2.0F, -9.0F, -1.5F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -5.0F, -8.0F, -0.1745F, -0.0F, 0.0F));
        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create()
            .texOffs(32, 11).addBox(-3.5F, -6.0F, -7.0F, 7.0F, 1.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(0, 11).addBox(-3.0F, -13.0F, 1.0F, 6.0F, 1.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 35).addBox(-3.0F, -14.0F, 8.0F, 6.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(24, 46).addBox(-3.0F, -14.0F, 11.0F, 6.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(92, 42).addBox(2.0F, -14.0F, 11.0F, 1.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(100, 42).addBox(-3.0F, -14.0F, 11.0F, 1.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(38, 46).addBox(-3.0F, -14.0F, 14.0F, 6.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(64, 13).addBox(-4.0F, -6.0F, 3.0F, 3.0F, 4.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(66, 24).addBox(-4.0F, -4.0F, 8.0F, 2.0F, 2.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(34, 42).addBox(-2.0F, -9.0F, 13.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(8, 49).addBox(3.0F, -11.0F, 11.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(12, 49).addBox(-4.0F, -11.0F, 11.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(32, 48).addBox(1.5F, -16.75F, -9.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(38, 48).addBox(-3.5F, -16.75F, -9.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(44, 48).addBox(3.5F, -17.0F, -9.25F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(72, 48).addBox(-5.0F, -17.0F, -9.25F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(98, 48).addBox(1.5F, -9.75F, 13.25F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(110, 48).addBox(-3.5F, -9.75F, 13.25F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(116, 48).addBox(3.5F, -10.0F, 13.0F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(122, 48).addBox(-5.0F, -10.0F, 13.0F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create()
            .texOffs(72, 45).addBox(-1.5F, -17.5F, -9.25F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(24, 48).addBox(-1.5F, -10.0F, 13.0F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create()
            .texOffs(84, 22).addBox(-2.0F, -5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 20.0F, -10.0F));
        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create()
            .texOffs(82, 36).addBox(-1.5F, 3.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(104, 25).addBox(-1.5F, 2.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(0, 39).addBox(-1.5F, -2.0F, -4.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(62, 39).addBox(-1.5F, -2.0F, 2.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 28).addBox(-1.5F, -3.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(18, 37).addBox(-1.5F, -4.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(106, 13).addBox(-0.5F, -3.0F, -3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(44, 42).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition fork_legs = fork.addOrReplaceChild("fork_legs", CubeListBuilder.create()
            .texOffs(68, 45).addBox(1.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(110, 37).addBox(-1.0F, -15.0F, -0.5F, 2.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(46, 38).addBox(-3.0F, -17.0F, -1.0F, 6.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(98, 46).addBox(3.0F, -16.5F, -0.5F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(80, 47).addBox(-6.0F, -16.5F, -0.5F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 49).addBox(6.0F, -16.5F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(4, 49).addBox(-7.0F, -16.5F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(106, 46).addBox(4.0F, -19.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(88, 47).addBox(-5.0F, -19.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(52, 46).addBox(4.0F, -21.0F, -0.5F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(92, 46).addBox(-6.0F, -21.0F, -0.5F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3142F, -0.0F, 0.0F));
        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create()
            .texOffs(32, 37).addBox(-1.5F, 3.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(82, 29).addBox(-1.5F, 2.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(72, 39).addBox(-1.5F, -2.0F, -4.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(82, 41).addBox(-1.5F, -2.0F, 2.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(46, 31).addBox(-1.5F, -3.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(96, 37).addBox(-1.5F, -4.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(32, 21).addBox(-0.5F, -3.0F, -3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(52, 42).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 20.0F, 9.0F));
        PartDefinition signal_l = partdefinition.addOrReplaceChild("signal_l", CubeListBuilder.create()
            .texOffs(0, 45).addBox(3.35F, -17.15F, -9.4F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(8, 45).addBox(3.35F, -10.15F, 12.85F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition signal_r = partdefinition.addOrReplaceChild("signal_r", CubeListBuilder.create()
            .texOffs(16, 45).addBox(-5.15F, -17.15F, -9.4F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(60, 45).addBox(-5.15F, -10.15F, 12.85F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
