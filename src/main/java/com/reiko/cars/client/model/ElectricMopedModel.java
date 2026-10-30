package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The electric moped's shape, as Blockbench exports {@code blockbench/electric_moped.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one.
 * Keep the top-level bone names: body, trim, lights, fork, wheel_rl, signal_l, signal_r.
 * Its texture is {@code textures/entity/car/electric_moped.png}. See {@link CarModel} for what each bone is for.
 */
public final class ElectricMopedModel {
    private ElectricMopedModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(78, 0).addBox(-4.0F, -5.0F, -9.0F, 8.0F, 1.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(74, 36).addBox(-4.0F, -11.0F, 2.0F, 8.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-5.0F, -12.0F, 3.0F, 10.0F, 7.0F, 9.0F, CubeDeformation.NONE)
            .texOffs(36, 29).addBox(-4.0F, -11.0F, 12.0F, 8.0F, 5.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 36).addBox(-3.0F, -10.0F, 14.0F, 6.0F, 3.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition leg_shield = body.addOrReplaceChild("leg_shield", CubeListBuilder.create()
            .texOffs(38, 15).addBox(-5.0F, -12.0F, -1.0F, 10.0F, 12.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(120, 25).addBox(4.0F, -10.0F, 1.0F, 1.0F, 10.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(92, 36).addBox(-5.0F, -10.0F, 1.0F, 1.0F, 10.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(0, 50).addBox(-3.0F, -9.0F, -1.25F, 6.0F, 1.0F, 0.25F, CubeDeformation.NONE)
            .texOffs(0, 52).addBox(3.5F, -11.0F, -1.25F, 1.5F, 1.0F, 0.5F, CubeDeformation.NONE)
            .texOffs(6, 52).addBox(-5.0F, -11.0F, -1.25F, 1.5F, 1.0F, 0.5F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -5.0F, -9.0F, -0.1745F, -0.0F, 0.0F));
        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create()
            .texOffs(78, 13).addBox(-3.5F, -5.5F, -8.0F, 7.0F, 0.5F, 10.0F, CubeDeformation.NONE)
            .texOffs(0, 16).addBox(-3.0F, -8.0F, 3.0F, 6.0F, 3.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(98, 45).addBox(3.0F, -7.0F, 5.0F, 0.25F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(38, 0).addBox(-3.0F, -13.0F, 2.0F, 6.0F, 1.0F, 14.0F, CubeDeformation.NONE)
            .texOffs(62, 24).addBox(-3.0F, -13.5F, 9.0F, 6.0F, 0.5F, 7.0F, CubeDeformation.NONE)
            .texOffs(108, 45).addBox(3.0F, -13.5F, 12.0F, 1.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(118, 45).addBox(-4.0F, -13.5F, 12.0F, 1.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(102, 25).addBox(-2.5F, -6.0F, 9.0F, 5.0F, 4.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(0, 27).addBox(-2.5F, -5.0F, 4.0F, 1.0F, 2.0F, 7.0F, CubeDeformation.NONE)
            .texOffs(124, 52).addBox(3.0F, -10.5F, 15.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(36, 53).addBox(-4.0F, -10.5F, 15.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(10, 46).addBox(-2.0F, -9.0F, 15.0F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(54, 45).addBox(-2.0F, -9.0F, 14.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(12, 52).addBox(2.0F, -19.25F, -11.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(18, 52).addBox(-4.0F, -19.25F, -11.0F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(24, 52).addBox(4.0F, -19.5F, -11.25F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(86, 52).addBox(-5.5F, -19.5F, -11.25F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(96, 52).addBox(2.5F, -10.75F, 15.75F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(112, 52).addBox(-4.5F, -10.75F, 15.75F, 2.0F, 0.5F, 0.5F, CubeDeformation.NONE)
            .texOffs(118, 52).addBox(4.5F, -11.0F, 15.5F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(30, 53).addBox(-6.0F, -11.0F, 15.5F, 1.5F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create()
            .texOffs(0, 46).addBox(-2.0F, -20.5F, -11.25F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(14, 50).addBox(-2.5F, -11.0F, 16.0F, 5.0F, 1.0F, 0.5F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition fork = partdefinition.addOrReplaceChild("fork", CubeListBuilder.create()
            .texOffs(16, 27).addBox(-2.0F, -5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 20.0F, -11.0F));
        PartDefinition wheel_fl = fork.addOrReplaceChild("wheel_fl", CubeListBuilder.create()
            .texOffs(98, 40).addBox(-1.5F, 3.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(56, 32).addBox(-1.5F, 2.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(34, 43).addBox(-1.5F, -2.0F, -4.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(72, 43).addBox(-1.5F, -2.0F, 2.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(102, 33).addBox(-1.5F, -3.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(112, 40).addBox(-1.5F, -4.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(112, 13).addBox(-0.5F, -3.0F, -3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(20, 46).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition fork_legs = fork.addOrReplaceChild("fork_legs", CubeListBuilder.create()
            .texOffs(92, 48).addBox(1.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(28, 41).addBox(-1.0F, -15.0F, -0.5F, 2.0F, 10.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(52, 39).addBox(-3.5F, -18.0F, -1.5F, 7.0F, 3.0F, 2.5F, CubeDeformation.NONE)
            .texOffs(96, 50).addBox(-2.0F, -18.25F, 0.75F, 4.0F, 0.75F, 0.5F, CubeDeformation.NONE)
            .texOffs(112, 50).addBox(3.5F, -17.0F, -0.5F, 2.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(120, 50).addBox(-6.0F, -17.0F, -0.5F, 2.5F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(50, 51).addBox(6.0F, -17.0F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(102, 52).addBox(-7.0F, -17.0F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(42, 51).addBox(4.0F, -19.5F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(46, 51).addBox(-5.0F, -19.5F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(86, 49).addBox(4.0F, -21.5F, -0.5F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(106, 50).addBox(-6.0F, -21.5F, -0.5F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3142F, -0.0F, 0.0F));
        PartDefinition wheel_rl = partdefinition.addOrReplaceChild("wheel_rl", CubeListBuilder.create()
            .texOffs(0, 41).addBox(-1.5F, 3.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(16, 34).addBox(-1.5F, 2.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(82, 43).addBox(-1.5F, -2.0F, -4.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(44, 45).addBox(-1.5F, -2.0F, 2.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(34, 36).addBox(-1.5F, -3.0F, -3.0F, 3.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(14, 41).addBox(-1.5F, -4.0F, -2.0F, 3.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(88, 24).addBox(-0.5F, -3.0F, -3.0F, 1.0F, 6.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(34, 49).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 20.0F, 11.0F));
        PartDefinition signal_l = partdefinition.addOrReplaceChild("signal_l", CubeListBuilder.create()
            .texOffs(54, 49).addBox(3.85F, -19.65F, -11.4F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(62, 49).addBox(4.35F, -11.15F, 15.35F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition signal_r = partdefinition.addOrReplaceChild("signal_r", CubeListBuilder.create()
            .texOffs(70, 49).addBox(-5.65F, -19.65F, -11.4F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE)
            .texOffs(78, 49).addBox(-6.15F, -11.15F, 15.35F, 1.8F, 1.3F, 1.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
