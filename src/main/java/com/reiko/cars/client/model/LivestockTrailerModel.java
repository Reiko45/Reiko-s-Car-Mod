package com.reiko.cars.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The livestock trailer's shape, as Blockbench exports {@code blockbench/livestock_trailer.bbmodel}. To change it, edit that file in
 * Blockbench, then File &gt; Export &gt; Export Java Entity and paste the new {@code createBodyLayer()} over this one.
 * Keep the top-level bone names: body, trim, lights, wheel_l1, wheel_r1, wheel_l2, wheel_r2, ball_mount, ramp, signal_l, signal_r.
 * Its texture is {@code textures/entity/trailer/livestock_trailer.png}. See {@link TrailerModel} for what each bone is for.
 */
public final class LivestockTrailerModel {
    private LivestockTrailerModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(452, 0).addBox(15.0F, -22.0F, -40.0F, 1.0F, 10.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(614, 0).addBox(-16.0F, -22.0F, -40.0F, 1.0F, 10.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(394, 90).addBox(15.0F, -27.0F, -40.0F, 1.0F, 2.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(556, 90).addBox(-16.0F, -27.0F, -40.0F, 1.0F, 2.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(0, 166).addBox(15.0F, -32.0F, -40.0F, 1.0F, 2.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(162, 166).addBox(-16.0F, -32.0F, -40.0F, 1.0F, 2.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(718, 167).addBox(15.0F, -37.0F, -40.0F, 1.0F, 2.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(324, 172).addBox(-16.0F, -37.0F, -40.0F, 1.0F, 2.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(0, 83).addBox(15.0F, -42.0F, -40.0F, 1.0F, 3.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(776, 84).addBox(-16.0F, -42.0F, -40.0F, 1.0F, 3.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(1018, 193).addBox(15.5F, -42.0F, -40.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(706, 195).addBox(-16.5F, -42.0F, -40.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(712, 195).addBox(15.5F, -42.0F, -24.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(534, 202).addBox(-16.5F, -42.0F, -24.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(540, 202).addBox(15.5F, -42.0F, -8.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(900, 203).addBox(-16.5F, -42.0F, -8.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(906, 203).addBox(15.5F, -42.0F, 8.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(912, 203).addBox(-16.5F, -42.0F, 8.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(918, 203).addBox(15.5F, -42.0F, 24.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(924, 203).addBox(-16.5F, -42.0F, 24.0F, 1.0F, 30.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(0, 0).addBox(-16.5F, -43.0F, -41.0F, 33.0F, 1.0F, 82.0F, CubeDeformation.NONE)
            .texOffs(940, 0).addBox(-16.0F, -42.0F, -41.0F, 32.0F, 30.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(938, 93).addBox(-14.0F, -40.0F, -43.0F, 28.0F, 27.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(486, 172).addBox(-11.0F, -38.0F, -44.0F, 22.0F, 24.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(930, 191).addBox(15.0F, -43.0F, 39.5F, 2.0F, 31.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(938, 191).addBox(-17.0F, -43.0F, 39.5F, 2.0F, 31.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(622, 172).addBox(-15.0F, -43.0F, 39.5F, 30.0F, 2.5F, 1.5F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition trim = partdefinition.addOrReplaceChild("trim", CubeListBuilder.create()
            .texOffs(586, 203).addBox(-12.5F, -7.0F, 1.0F, 25.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(988, 149).addBox(11.0F, -8.5F, -4.0F, 2.0F, 1.5F, 12.0F, CubeDeformation.NONE)
            .texOffs(988, 163).addBox(-13.0F, -8.5F, -4.0F, 2.0F, 1.5F, 12.0F, CubeDeformation.NONE)
            .texOffs(640, 204).addBox(-12.5F, -7.0F, 15.0F, 25.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(532, 172).addBox(11.0F, -8.5F, 10.0F, 2.0F, 1.5F, 12.0F, CubeDeformation.NONE)
            .texOffs(560, 172).addBox(-13.0F, -8.5F, 10.0F, 2.0F, 1.5F, 12.0F, CubeDeformation.NONE)
            .texOffs(776, 0).addBox(11.0F, -12.0F, -40.0F, 2.0F, 3.5F, 80.0F, CubeDeformation.NONE)
            .texOffs(230, 82).addBox(-13.0F, -12.0F, -40.0F, 2.0F, 3.5F, 80.0F, CubeDeformation.NONE)
            .texOffs(880, 193).addBox(-11.0F, -11.5F, -36.0F, 22.0F, 2.5F, 1.5F, CubeDeformation.NONE)
            .texOffs(946, 193).addBox(-11.0F, -11.5F, -20.0F, 22.0F, 2.5F, 1.5F, CubeDeformation.NONE)
            .texOffs(586, 198).addBox(-11.0F, -11.5F, -4.0F, 22.0F, 2.5F, 1.5F, CubeDeformation.NONE)
            .texOffs(634, 198).addBox(-11.0F, -11.5F, 12.0F, 22.0F, 2.5F, 1.5F, CubeDeformation.NONE)
            .texOffs(880, 198).addBox(-11.0F, -11.5F, 28.0F, 22.0F, 2.5F, 1.5F, CubeDeformation.NONE)
            .texOffs(550, 186).addBox(-2.0F, -12.5F, -61.5F, 4.0F, 3.5F, 7.5F, CubeDeformation.NONE)
            .texOffs(646, 208).addBox(-2.5F, -13.5F, -61.0F, 5.0F, 1.5F, 5.0F, CubeDeformation.NONE)
            .texOffs(958, 226).addBox(-0.5F, -14.5F, -59.0F, 1.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(666, 208).addBox(-1.5F, -11.5F, -55.0F, 3.0F, 3.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(682, 208).addBox(2.5F, -9.0F, -57.0F, 0.5F, 2.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(1004, 211).addBox(-3.0F, -9.0F, -57.0F, 0.5F, 2.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(574, 186).addBox(-1.2F, -22.0F, -53.2F, 2.4F, 19.0F, 2.4F, CubeDeformation.NONE)
            .texOffs(556, 227).addBox(-0.8F, -3.0F, -52.8F, 1.6F, 2.0F, 1.6F, CubeDeformation.NONE)
            .texOffs(486, 216).addBox(-2.0F, -1.0F, -54.0F, 4.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(968, 226).addBox(-0.5F, -23.0F, -52.5F, 1.0F, 1.0F, 4.0F, CubeDeformation.NONE)
            .texOffs(614, 227).addBox(-0.5F, -23.0F, -49.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(588, 172).addBox(-6.0F, -16.0F, -48.0F, 12.0F, 5.0F, 5.0F, CubeDeformation.NONE)
            .texOffs(678, 188).addBox(-6.25F, -16.75F, -48.25F, 12.5F, 0.75F, 5.5F, CubeDeformation.NONE)
            .texOffs(230, 0).addBox(-15.5F, -14.0F, -40.0F, 31.0F, 2.0F, 80.0F, CubeDeformation.NONE)
            .texOffs(606, 217).addBox(3.0F, -35.0F, -44.25F, 5.0F, 5.0F, 0.5F, CubeDeformation.NONE)
            .texOffs(618, 217).addBox(-8.0F, -35.0F, -44.25F, 5.0F, 5.0F, 0.5F, CubeDeformation.NONE)
            .texOffs(486, 213).addBox(-11.0F, -16.0F, -44.5F, 22.0F, 2.0F, 0.5F, CubeDeformation.NONE)
            .texOffs(622, 177).addBox(-15.0F, -14.0F, 39.5F, 30.0F, 3.0F, 1.5F, CubeDeformation.NONE)
            .texOffs(940, 31).addBox(16.0F, -12.5F, -6.0F, 1.6F, 1.0F, 30.0F, CubeDeformation.NONE)
            .texOffs(940, 62).addBox(-17.6F, -12.5F, -6.0F, 1.6F, 1.0F, 30.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition tongue_l = trim.addOrReplaceChild("tongue_l", CubeListBuilder.create()
            .texOffs(938, 149).addBox(-1.0F, -3.0F, -21.406F, 2.0F, 3.0F, 22.406F, CubeDeformation.NONE), PartPose.offsetAndRotation(10.0F, -8.5F, -40.0F, -0.0F, 0.4412F, 0.0F));
        PartDefinition tongue_r = trim.addOrReplaceChild("tongue_r", CubeListBuilder.create()
            .texOffs(880, 167).addBox(-1.0F, -3.0F, -21.406F, 2.0F, 3.0F, 22.406F, CubeDeformation.NONE), PartPose.offsetAndRotation(-10.0F, -8.5F, -40.0F, -0.0F, -0.4412F, 0.0F));
        PartDefinition lights = partdefinition.addOrReplaceChild("lights", CubeListBuilder.create()
            .texOffs(1016, 225).addBox(15.5F, -19.0F, 40.0F, 1.75F, 4.0F, 1.25F, CubeDeformation.NONE)
            .texOffs(998, 226).addBox(-17.25F, -19.0F, 40.0F, 1.75F, 4.0F, 1.25F, CubeDeformation.NONE)
            .texOffs(564, 227).addBox(15.5F, -42.5F, 40.0F, 1.75F, 1.5F, 1.25F, CubeDeformation.NONE)
            .texOffs(572, 227).addBox(-17.25F, -42.5F, 40.0F, 1.75F, 1.5F, 1.25F, CubeDeformation.NONE)
            .texOffs(894, 224).addBox(16.0F, -21.5F, -38.0F, 0.75F, 1.5F, 2.0F, CubeDeformation.NONE)
            .texOffs(658, 226).addBox(-16.75F, -21.5F, -38.0F, 0.75F, 1.5F, 2.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition wheel_l1 = partdefinition.addOrReplaceChild("wheel_l1", CubeListBuilder.create()
            .texOffs(486, 206).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(994, 193).addBox(-2.0F, 4.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(946, 218).addBox(-2.0F, 3.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(960, 218).addBox(-2.0F, 3.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(974, 218).addBox(-2.0F, 2.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(988, 218).addBox(-2.0F, 2.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(682, 215).addBox(-2.0F, -2.0F, 4.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(502, 216).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(1002, 218).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(880, 220).addBox(-2.0F, -3.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(486, 221).addBox(-2.0F, -4.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(674, 221).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(682, 195).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(546, 207).addBox(-2.0F, -6.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(686, 172).addBox(0.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(528, 216).addBox(0.75F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(930, 175).addBox(-1.5F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(948, 175).addBox(-2.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(14.5F, 18.0F, 2.0F));
        PartDefinition wheel_r1 = partdefinition.addOrReplaceChild("wheel_r1", CubeListBuilder.create()
            .texOffs(586, 207).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(486, 197).addBox(-2.0F, 4.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(500, 222).addBox(-2.0F, 3.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(514, 222).addBox(-2.0F, 3.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(650, 222).addBox(-2.0F, 2.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(946, 222).addBox(-2.0F, 2.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(514, 216).addBox(-2.0F, -2.0F, 4.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(662, 216).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(960, 222).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(974, 222).addBox(-2.0F, -3.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(988, 222).addBox(-2.0F, -4.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(1002, 222).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(550, 198).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(606, 207).addBox(-2.0F, -6.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(966, 175).addBox(-1.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(528, 220).addBox(-1.75F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(984, 177).addBox(0.5F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(1002, 177).addBox(1.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-14.5F, 18.0F, 2.0F));
        PartDefinition wheel_l2 = partdefinition.addOrReplaceChild("wheel_l2", CubeListBuilder.create()
            .texOffs(946, 207).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(946, 198).addBox(-2.0F, 4.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(546, 223).addBox(-2.0F, 3.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(560, 223).addBox(-2.0F, 3.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(574, 223).addBox(-2.0F, 2.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(588, 223).addBox(-2.0F, 2.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(546, 217).addBox(-2.0F, -2.0F, 4.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(558, 217).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(602, 223).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(616, 223).addBox(-2.0F, -3.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(630, 224).addBox(-2.0F, -4.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(880, 224).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(970, 198).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(966, 207).addBox(-2.0F, -6.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(588, 182).addBox(0.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(894, 220).addBox(0.75F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(606, 182).addBox(-1.5F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(624, 182).addBox(-2.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(14.5F, 18.0F, 16.0F));
        PartDefinition wheel_r2 = partdefinition.addOrReplaceChild("wheel_r2", CubeListBuilder.create()
            .texOffs(566, 208).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(510, 202).addBox(-2.0F, 4.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(930, 224).addBox(-2.0F, 3.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(486, 225).addBox(-2.0F, 3.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(664, 225).addBox(-2.0F, 2.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(678, 225).addBox(-2.0F, 2.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(570, 217).addBox(-2.0F, -2.0F, 4.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(582, 217).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(500, 226).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(514, 226).addBox(-2.0F, -3.0F, 3.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(644, 226).addBox(-2.0F, -4.0F, 2.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(944, 226).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 1.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(994, 202).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 1.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(626, 208).addBox(-2.0F, -6.0F, -3.0F, 4.0F, 1.0F, 6.0F, CubeDeformation.NONE)
            .texOffs(642, 182).addBox(-1.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(528, 224).addBox(-1.75F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(660, 182).addBox(0.5F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE)
            .texOffs(532, 186).addBox(1.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(-14.5F, 18.0F, 16.0F));
        PartDefinition ball_mount = partdefinition.addOrReplaceChild("ball_mount", CubeListBuilder.create()
            .texOffs(594, 217).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(580, 227).addBox(-0.8F, 2.0F, -0.8F, 1.6F, 0.5F, 1.6F, CubeDeformation.NONE)
            .texOffs(880, 213).addBox(-1.5F, 2.5F, -3.0F, 3.0F, 1.5F, 5.0F, CubeDeformation.NONE)
            .texOffs(880, 203).addBox(-1.0F, 2.5F, -11.0F, 2.0F, 1.5F, 8.0F, CubeDeformation.NONE)
            .texOffs(588, 227).addBox(-1.6F, 2.75F, -8.0F, 3.2F, 1.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-0.0F, 14.0F, -58.0F));
        PartDefinition ramp = partdefinition.addOrReplaceChild("ramp", CubeListBuilder.create()
            .texOffs(938, 122).addBox(-14.5F, -26.0F, 0.0F, 29.0F, 26.0F, 0.75F, CubeDeformation.NONE)
            .texOffs(694, 204).addBox(14.0F, -26.0F, 0.0F, 1.0F, 26.0F, 1.75F, CubeDeformation.NONE)
            .texOffs(700, 204).addBox(-15.0F, -26.0F, 0.0F, 1.0F, 26.0F, 1.75F, CubeDeformation.NONE)
            .texOffs(946, 214).addBox(-14.0F, -5.0F, 0.75F, 28.0F, 1.0F, 0.75F, CubeDeformation.NONE)
            .texOffs(546, 215).addBox(-14.0F, -11.0F, 0.75F, 28.0F, 1.0F, 0.75F, CubeDeformation.NONE)
            .texOffs(604, 215).addBox(-14.0F, -17.0F, 0.75F, 28.0F, 1.0F, 0.75F, CubeDeformation.NONE)
            .texOffs(946, 216).addBox(-14.0F, -23.0F, 0.75F, 28.0F, 1.0F, 0.75F, CubeDeformation.NONE)
            .texOffs(598, 227).addBox(10.0F, -24.0F, 1.5F, 3.0F, 1.0F, 0.75F, CubeDeformation.NONE)
            .texOffs(606, 227).addBox(-13.0F, -24.0F, 1.5F, 3.0F, 1.0F, 0.75F, CubeDeformation.NONE), PartPose.offset(-0.0F, 10.0F, 40.0F));
        PartDefinition signal_l = partdefinition.addOrReplaceChild("signal_l", CubeListBuilder.create()
            .texOffs(630, 217).addBox(15.35F, -19.15F, 39.85F, 2.05F, 4.3F, 1.55F, CubeDeformation.NONE)
            .texOffs(1006, 226).addBox(15.35F, -42.65F, 39.85F, 2.05F, 1.8F, 1.55F, CubeDeformation.NONE)
            .texOffs(978, 226).addBox(15.85F, -21.65F, -38.15F, 1.05F, 1.8F, 2.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        PartDefinition signal_r = partdefinition.addOrReplaceChild("signal_r", CubeListBuilder.create()
            .texOffs(640, 217).addBox(-17.4F, -19.15F, 39.85F, 2.05F, 4.3F, 1.55F, CubeDeformation.NONE)
            .texOffs(546, 227).addBox(-17.4F, -42.65F, 39.85F, 2.05F, 1.8F, 1.55F, CubeDeformation.NONE)
            .texOffs(988, 226).addBox(-16.9F, -21.65F, -38.15F, 1.05F, 1.8F, 2.3F, CubeDeformation.NONE), PartPose.offset(-0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 1024, 512);
    }
}
