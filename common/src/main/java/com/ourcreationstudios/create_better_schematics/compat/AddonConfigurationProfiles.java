package com.ourcreationstudios.create_better_schematics.compat;

/**
 * Additional configuration omitted by the audited addons' safe-NBT writers.
 * Class names keep every integration optional and include derived block variants.
 * Inventories, machine progress, ownership and physical network identities are deliberately absent.
 */
final class AddonConfigurationProfiles {
    private AddonConfigurationProfiles() {
    }

    static void register() {
        block("com.kipti.bnb.content.trinkets.nixie.foundation.GenericNixieDisplayBlockEntity",
                "CustomTextComponentTop", "CustomTextComponentBottom", "CustomTextIndex", "CurrentDisplayOption");
        behaviour("com.kipti.bnb.foundation.behaviour.drag.DragInteractionBehaviour", "DragValue");
        block("fr.iglee42.createqualityoflife.blockentitites.InventoryLinkerBlockEntity", "mode");
        block("plus.dragons.createenchantmentindustry.common.processing.forger.BlazeForgerBlockEntity", "ForgingMode");

        block("de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity", "BeeReturnModeEnabled", "FilterMode");
        block("dev.ithundxr.createnumismatics.content.bank.blaze_banker.BlazeBankerBlockEntity", "Label");

        behaviour("com.finchy.pipeorgans.content.midi.MidiSourceBehaviour", "frequencyItems");
        block("com.finchy.pipeorgans.content.midi.trackerBar.TrackerBarBlockEntity", "ButtonsEnabled");
        block("com.finchy.pipeorgans.content.pipes.generic.GenericPipeBlockEntity", "Pitch");

        behaviour("org.patryk3211.powergrid.electricity.info.customdisplay.CustomDisplayBehaviour",
                "Fmt.Eq", "Fmt.Unit", "Fmt.UnitS", "Fmt.Prefix");
        block("org.patryk3211.powergrid.electricity.electricswitch.SwitchBlockEntity", "Timeout", "NormallyClosed");
        behaviour("com.happysg.radar.block.radar.behavior.RadarScanningBlockBehavior", "fov", "yRange", "range", "trackExpiration");

        block("dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity", "DyeId");
        block("dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity", "plumeType");
        block("dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity", "plumeType");
        block("dev.propulsionteam.propulsionsimulated.content.tilt_adapter.AdvancedTiltAdapterBlockEntity",
                "LeftAngle", "RightAngle", "SharedAngles", "AngleStateVersion");

        block("com.maxenonyme.createsubmarine.submarine.block.entity.IndustrialAlarmBlockEntity", "Period");
        block("com.maxenonyme.createsubmarine.submarine.block.entity.BarometerBlockEntity", "CustomName");
        block("com.maxenonyme.highseas.block.entity.BoatEngineBlockEntity", "Enabled", "Cap");
        block("com.cbc_more_content.block.SirenBlockEntity", "Settings");

        // Transmission & Linkage's typed controls opt out of safe NBT; the owner stores their settings.
        block("com.enxv.aerouniversaljoint.content.DampingStressBearingBlockEntity", "ResistanceValue");
        block("com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadBlockEntity",
                "StretchResistance", "FreeMode", "ExpectedLengthTenths", "RedstoneMinLengthTenths",
                "RedstoneMaxLengthTenths", "ReturnForce", "HingeMinAngle", "HingeMaxAngle");

        block("com.railwayteam.railways.content.buffer.DyeableBlockEntity", "Color");
        block("com.railwayteam.railways.content.buffer.headstock.HeadstockBlockEntity", "Material");
        block("com.railwayteam.railways.content.buffer.WoodVariantTrackBufferBlockEntity", "Material");
        block("com.railwayteam.railways.content.buffer.headstock.CopycatHeadstockBlockEntity", "Color");
        block("com.railwayteam.railways.content.smokestack.block.be.SmokeStackBlockEntity", "color", "isSoul");
        block("com.railwayteam.railways.content.conductor.whistle.ConductorWhistleFlagBlockEntity", "SelectedColor");

        block("dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity", "high_signal", "low_signal");
        block("dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity", "LaserColor", "Rainbow");
        block("dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity", "Opacity");
        block("dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity", "TextColor", "Glow", "Waxed", "Name");
        block("dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity",
                "MinRange", "MaxRange");
        block("dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity", "Keys");

        block("com.rieno.gadgetsandgizmos.content.CameraBlockEntity", "CameraMode", "CameraUiMode", "CameraFlashlight",
                "CameraAutoTracking", "CameraLockTarget", "CameraUiTracking", "CameraUiTargetLock", "CameraFov",
                "MaxRayLength", "RaysPerSecond", "FilterAllowlist", "CameraFilter");
        block("com.rieno.gadgetsandgizmos.content.UniversalDisplayAdapterBlockEntity", "DisplayWidth", "DisplayHeight");
        block("com.rieno.gadgetsandgizmos.content.ShipDockBlockEntity", "DockName", "Refuel", "Restock", "Packages",
                "DoorControlEnabled", "DoorControlMask");
        block("com.rieno.gadgetsandgizmos.content.ShipCouplerBlockEntity", "RedstoneInverted");
        block("com.rieno.gadgetsandgizmos.content.ScissorPistonBlockEntity", "MaxRange");

        block("com.hlysine.create_connected.content.dashboard.DashboardBlockEntity", "text");
        String pulseGenerator = "com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlockEntity";
        block(pulseGenerator, "Instructions.*.ID", "Instructions.*.Signal", "Instructions.*.Value");
        net.minecraft.nbt.CompoundTag idle = new net.minecraft.nbt.CompoundTag();
        idle.putInt("InstructionIndex", -1);
        SchematicConfigurationRegistry.registerPlacementDefaults(pulseGenerator, idle);
        behaviour("org.antarcticgardens.cna.content.energising.EnergiserBehaviour", "capacitorMode");
        AddonInstalledComponents.registerProfiles();
        PopularModConfigurationProfiles.register();
    }

    private static void block(String className, String... fields) {
        SchematicConfigurationRegistry.registerClass(className, fields);
    }

    private static void behaviour(String className, String... fields) {
        SchematicConfigurationRegistry.registerBehaviour(className, fields);
    }
}
