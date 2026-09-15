package com.example.effects;

import com.example.ExampleMod;
import com.example.item.JointItem;
import com.example.item.CuredJointItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;

public final class SmokingEffectGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 80)
    public void curedPuffsAndDeath(TestContext context) {
        var player = context.createMockCreativeServerPlayerInWorld();
        var world = context.getWorld();
        ItemStack cured = new ItemStack(ExampleMod.CURED_JOINT);
        player.setStackInHand(Hand.MAIN_HAND, cured);
        player.setCurrentHand(Hand.MAIN_HAND);
        ExampleMod.CURED_JOINT.onStoppedUsing(cured, world, player, 30);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.CURED_JOINT) == 0,
                "Interrupted cured puff must not apply an effect");
        ExampleMod.CURED_JOINT.finishUsing(cured, world, player);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.CURED_JOINT) == CuredJointItem.EFFECT_PER_PUFF,
                "Cured puff applies its stronger independent profile");
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT) == 0,
                "Cured puff must not start the ordinary profile");
        check(cured.getOrCreateNbt().getBoolean("lit"), "Cured joint must light normally");
        ExampleMod.JOINT.finishUsing(new ItemStack(ExampleMod.JOINT), world, player);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.CURED_JOINT) == CuredJointItem.EFFECT_PER_PUFF,
                "Ordinary puffs must not overwrite cured strength or its fade parameters");
        for (int i = 0; i < 5; i++) ExampleMod.CURED_JOINT.finishUsing(cured, world, player);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.CURED_JOINT) == JointItem.EFFECT_MAX,
                "Cured puffs must respect the cap");
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.DESATURATION) == 0,
                "Neither joint applies grayscale");
        context.waitAndRun(25, () -> {
            float value = EffectManager.getEffectValue(player, EffectManager.EffectType.CURED_JOINT);
            check(value > 0 && value < JointItem.EFFECT_MAX, "Cured effect decays on server ticks");
            player.kill();
            context.waitAndRun(2, () -> {
                check(EffectManager.getEffectValue(player, EffectManager.EffectType.CURED_JOINT) == 0,
                        "Death clears cured effects");
                check(EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT) == 0,
                        "Death clears both joint profiles");
                world.getServer().getPlayerManager().remove(player);
                context.complete();
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 80)
    public void completedPuffsDecayAndDeath(TestContext context) {
        var player = context.createMockCreativeServerPlayerInWorld();
        var world = context.getWorld();
        ItemStack joint = new ItemStack(ExampleMod.JOINT);
        player.setStackInHand(Hand.MAIN_HAND, joint);
        player.setCurrentHand(Hand.MAIN_HAND);
        ExampleMod.JOINT.onStoppedUsing(joint, world, player, 30);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT) == 0,
                "Interrupted smoking must not apply the effect");

        ExampleMod.JOINT.finishUsing(joint, world, player);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT) == JointItem.EFFECT_PER_PUFF,
                "A completed Joint puff must apply Cannabis");
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.DESATURATION) == 0,
                "Joint must not apply cigarette desaturation");
        check(joint.getOrCreateNbt().getBoolean("lit"), "Smoking still lights the Joint");

        ItemStack cigarette = new ItemStack(ExampleMod.CIGARETTE);
        player.setStackInHand(Hand.MAIN_HAND, cigarette);
        ExampleMod.CIGARETTE.finishUsing(cigarette, world, player);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.DESATURATION) > 0,
                "Cigarettes must retain grayscale");
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT) == JointItem.EFFECT_PER_PUFF,
                "Cigarette and Joint effects coexist independently");

        for (int i = 0; i < 5; i++) ExampleMod.JOINT.finishUsing(joint, world, player);
        check(EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT) == JointItem.EFFECT_MAX,
                "Repeated puffs must stop at the maximum");

        context.waitAndRun(25, () -> {
            float value = EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT);
            check(value > 0 && value < JointItem.EFFECT_MAX, "Server ticks must decay the effect");
            player.kill();
            context.waitAndRun(2, () -> {
                check(EffectManager.getEffectValue(player, EffectManager.EffectType.JOINT) == 0,
                        "Non-combat death must clear Joint");
                check(EffectManager.getEffectValue(player, EffectManager.EffectType.DESATURATION) == 0,
                        "Death must clear cigarette effects too");
                world.getServer().getPlayerManager().remove(player);
                context.complete();
            });
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
