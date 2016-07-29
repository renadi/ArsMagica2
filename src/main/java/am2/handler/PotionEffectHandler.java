package am2.handler;

import org.lwjgl.opengl.GL11;

import am2.api.event.EventPotionAdded;
import am2.api.event.SpellCastEvent;
import am2.buffs.BuffEffectTemporalAnchor;
import am2.buffs.BuffStatModifiers;
import am2.defs.ItemDefs;
import am2.defs.PotionEffectsDefs;
import am2.extensions.EntityExtension;
import am2.utils.SelectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class PotionEffectHandler {
	
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void playerPreDeathEvent(LivingDeathEvent e) {
		PotionEffect effect = e.getEntityLiving().getActivePotionEffect(PotionEffectsDefs.temporalAnchor);
		if (effect != null) {
			((BuffEffectTemporalAnchor)effect).stopEffect(e.getEntityLiving());
			e.getEntityLiving().removePotionEffect(PotionEffectsDefs.temporalAnchor);
			e.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void applyPotionEffect(EventPotionAdded e) {
		if (PotionEffectsDefs.getEffect(e.effect) != null)
			e.effect = PotionEffectsDefs.getEffect(e.effect);
	}
	
	@SubscribeEvent
	public void livingUpdate (LivingUpdateEvent e) {
		BuffStatModifiers.instance.applyStatModifiersBasedOnBuffs(e.getEntityLiving());
		if (e.getEntityLiving().isPotionActive(PotionEffectsDefs.slowfall)) {
			e.getEntityLiving().setPosition(e.getEntityLiving().posX, e.getEntityLiving().posY + (e.getEntityLiving().fallDistance / 1.1), e.getEntityLiving().posZ);
			e.getEntityLiving().fallDistance = 0;
		}
		if (e.getEntityLiving().isPotionActive(PotionEffectsDefs.gravityWell)&& e.getEntityLiving().motionY < 0) {
			e.getEntityLiving().motionY *= 2;
		}
		
		if (e.getEntityLiving().isPotionActive(PotionEffectsDefs.agility)) {
			e.getEntityLiving().stepHeight = 1.01f;
		}else if (e.getEntityLiving().stepHeight == 1.01f) {
			e.getEntityLiving().stepHeight = 0.5f;
		}
	}
	
	@SubscribeEvent
	public void playerJumpEvent(LivingJumpEvent event) {
		if (event.getEntityLiving().isPotionActive(PotionEffectsDefs.agility)){
			event.getEntityLiving().motionY *= 1.5f;
		}
		if (event.getEntityLiving().isPotionActive(PotionEffectsDefs.leap)){

			Entity velocityTarget = event.getEntityLiving();

			if (event.getEntityLiving().getRidingEntity() != null){
				if (event.getEntityLiving().getRidingEntity() instanceof EntityMinecart){
					event.getEntityLiving().getRidingEntity().setPosition(event.getEntityLiving().getRidingEntity().posX, event.getEntityLiving().getRidingEntity().posY + 1.5, event.getEntityLiving().getRidingEntity().posZ);
				}
				velocityTarget = event.getEntityLiving().getRidingEntity();
			}

			double yVelocity = 0;
			double xVelocity = 0;
			double zVelocity = 0;

			Vec3d vec = event.getEntityLiving().getLookVec().normalize();
			yVelocity = 0.4 + (event.getEntityLiving().getActivePotionEffect(PotionEffectsDefs.leap).getAmplifier() * 0.3);
			xVelocity = velocityTarget.motionX * (Math.pow(2, event.getEntityLiving().getActivePotionEffect(PotionEffectsDefs.leap).getAmplifier())) * Math.abs(vec.xCoord);
			zVelocity = velocityTarget.motionZ * (Math.pow(2, event.getEntityLiving().getActivePotionEffect(PotionEffectsDefs.leap).getAmplifier())) * Math.abs(vec.zCoord);

			float maxHorizontalVelocity = 1.45f;

			if (event.getEntityLiving().getRidingEntity() != null && (event.getEntityLiving().getRidingEntity() instanceof EntityMinecart || event.getEntityLiving().getRidingEntity() instanceof EntityBoat) || event.getEntityLiving().isPotionActive(PotionEffectsDefs.haste)){
				maxHorizontalVelocity += 25;
				xVelocity *= 2.5;
				zVelocity *= 2.5;
			}

			if (xVelocity > maxHorizontalVelocity){
				xVelocity = maxHorizontalVelocity;
			}else if (xVelocity < -maxHorizontalVelocity){
				xVelocity = -maxHorizontalVelocity;
			}

			if (zVelocity > maxHorizontalVelocity){
				zVelocity = maxHorizontalVelocity;
			}else if (zVelocity < -maxHorizontalVelocity){
				zVelocity = -maxHorizontalVelocity;
			}

			if (EntityExtension.For(event.getEntityLiving()).getIsFlipped()){
				yVelocity *= -1;
			}

			velocityTarget.addVelocity(xVelocity, yVelocity, zVelocity);
		}
		if (event.getEntityLiving().isPotionActive(PotionEffectsDefs.entangle)){
			event.getEntityLiving().motionY = 0;
		}
	}
	
	@SubscribeEvent
	public void livingFall (LivingFallEvent e) {
		if (e.getEntityLiving().isPotionActive(PotionEffectsDefs.agility)) {
			e.setDistance(e.getDistance() / 1.5F);
		}
		if (e.getEntityLiving().isPotionActive(PotionEffectsDefs.leap)) {
			if (e.getDistance() < (e.getEntityLiving().getActivePotionEffect(PotionEffectsDefs.leap).getAmplifier() + 1) * 10) {
				e.setCanceled(true);
			} else {
				e.setDistance(e.getDistance() - (e.getEntityLiving().getActivePotionEffect(PotionEffectsDefs.leap).getAmplifier() + 1) * 10);
			}
		}
	}
	
	@SubscribeEvent
	public void spellCast (SpellCastEvent.Pre e) {
		if (e.entityLiving.isPotionActive(PotionEffectsDefs.clarity)) {
			e.manaCost = 0;
			e.entityLiving.removePotionEffect(PotionEffectsDefs.clarity);
		}
	}
	
	@SubscribeEvent
	public void teleportEvent(EnderTeleportEvent e) {
		if (e.getEntityLiving().isPotionActive(PotionEffectsDefs.astralDistortion) || e.getEntity().isDead) {
			e.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void playerRender(RenderPlayerEvent.Pre e) {
		if (e.getEntityLiving().isPotionActive(PotionEffectsDefs.trueSight)) {
			GL11.glPushMatrix();
			GL11.glRotated(e.getEntityPlayer().rotationYawHead, 0, -1, 0);
			int[] runes = SelectionUtils.getRuneSet(e.getEntityPlayer());
			int numRunes = runes.length;
			double start = ((double)numRunes - 1) / 8D;
			GL11.glTranslated(start, 2.2, 0);
			for (int rune : runes) {
				GL11.glPushMatrix();
				GL11.glScaled(0.25, 0.25, 0.25);
				Minecraft.getMinecraft().getItemRenderer().renderItem(e.getEntityPlayer(), new ItemStack(ItemDefs.rune, 1, rune), TransformType.GUI);
				GL11.glPopMatrix();
				GL11.glTranslated(-0.25, 0, 0);			
			}
			GL11.glPopMatrix();
		}
	}
}
