package WayofTime.alchemicalWizardry.common.rituals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import WayofTime.alchemicalWizardry.AlchemicalWizardry;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import WayofTime.alchemicalWizardry.ModBlocks;
import WayofTime.alchemicalWizardry.api.Int3;
import WayofTime.alchemicalWizardry.api.alchemy.energy.Reagent;
import WayofTime.alchemicalWizardry.api.alchemy.energy.ReagentRegistry;
import WayofTime.alchemicalWizardry.api.rituals.IMasterRitualStone;
import WayofTime.alchemicalWizardry.api.rituals.RitualComponent;
import WayofTime.alchemicalWizardry.api.rituals.RitualEffect;
import WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler;
import WayofTime.alchemicalWizardry.common.demonVillage.tileEntity.TEDemonPortal;
import WayofTime.alchemicalWizardry.api.tile.IBloodAltar;
import WayofTime.alchemicalWizardry.common.tileEntity.TEBellJar;

public class RitualEffectDemonPortal extends RitualEffect
{
	public static final int neededAmount = 16000;
	public static final Random rand = new Random();
	public static final int drainRate = 50;	
	public static final Reagent[] reagents = new Reagent[]{ReagentRegistry.aetherReagent, ReagentRegistry.aquasalusReagent, ReagentRegistry.terraeReagent, ReagentRegistry.incendiumReagent, ReagentRegistry.sanctusReagent, ReagentRegistry.tenebraeReagent, ReagentRegistry.magicalesReagent, ReagentRegistry.potentiaReagent};
	public static final Int3[] jarLocations = new Int3[]{new Int3(4, 5, 4), new Int3(-4, 5, 4), new Int3(4, 5, -4), new Int3(-4, 5, -4), new Int3(0, 5, 6), new Int3(0, 5, -6), new Int3(6, 5, 0), new Int3(-6, 5, 0)};
	
    @Override
    public void performEffect(IMasterRitualStone ritualStone)
    {
        String owner = ritualStone.getOwner();

        int currentEssence = SoulNetworkHandler.getCurrentEssence(owner);
        World world = ritualStone.getWorld();
        int x = ritualStone.getXCoord();
        int y = ritualStone.getYCoord();
        int z = ritualStone.getZCoord();

        if (currentEssence < this.getCostPerRefresh())
        {
            SoulNetworkHandler.causeNauseaToPlayer(owner);
        } else
        {
        	NBTTagCompound tag = ritualStone.getCustomRitualTag();
        	
        	boolean reagentsFulfilled = true;
        	
        	for(Reagent reagent : reagents)
        	{
        		int reagentAmount = tag.getInteger(ReagentRegistry.getKeyForReagent(reagent));
        		if(reagentAmount < neededAmount)
        		{
        			reagentsFulfilled = false;
//        			System.out.println("Reagents not fulfilled. Missing: " + ReagentRegistry.getKeyForReagent(reagent));
        			int drainAmount = Math.min(drainRate, neededAmount - reagentAmount);
        			
        			if(drainAmount <= 0)
        			{
        				continue;
        			}
        			
        			if(this.canDrainReagent(ritualStone, reagent, drainAmount, true))
        			{
        				if(rand.nextInt(10) == 0)
        				{
        					this.createRandomLightning(world, x, y, z);
        				}
        				reagentAmount += drainAmount;
        				
        				tag.setInteger(ReagentRegistry.getKeyForReagent(reagent), reagentAmount);
        				break;
        			}
        		}
        	}
        	
        	ritualStone.setCustomRitualTag(tag);
        	
        	if(reagentsFulfilled && checkCreatePortal(ritualStone))
        	{
        		world.setBlock(x, y+1, z, ModBlocks.blockDemonPortal);
        		
        		TEDemonPortal portal = (TEDemonPortal) world.getTileEntity(x, y + 1, z);
        		portal.start();
        		
        		ritualStone.setActive(false);
        	}
        	
            SoulNetworkHandler.syphonFromNetwork(owner, this.getCostPerRefresh());
        }
    }

    public boolean checkCreatePortal(IMasterRitualStone ritualStone)
    {
    	TileEntity entity = ritualStone.getWorld().getTileEntity(ritualStone.getXCoord(), ritualStone.getYCoord() + 1, ritualStone.getZCoord());
    	if(entity instanceof IBloodAltar)
    	{
    		IBloodAltar altar = (IBloodAltar)entity;
    		if(altar.hasDemonBlood() && ritualStone.getWorld().isAirBlock(ritualStone.getXCoord(), ritualStone.getYCoord() + 2, ritualStone.getZCoord()))
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    @Override
    public int getCostPerRefresh()
    {
        return AlchemicalWizardry.ritualCostConvocation[1];
    }
    
    public void createRandomLightning(World world, int x, int y, int z)
    {
    	world.addWeatherEffect(new EntityLightningBolt(world, x + rand.nextInt(10) - rand.nextInt(10), y + 1, z + rand.nextInt(10) - rand.nextInt(10)));
    }
    
    @Override
    public boolean startRitual(IMasterRitualStone ritualStone, EntityPlayer player) {

        String[] disallowedWords = {"world","Islands","DIM1","DIM-1"};

        if(!ritualStone.getWorld().isRemote){
            if(Arrays.asList(disallowedWords).contains(ritualStone.getWorld().provider.getDimensionName())){
                if(player != null){
                    player.addChatComponentMessage(new ChatComponentText("§cYou can't perform this ritual in this dimension."));
                    return false;
                }
            }
        }

        if(!checkJars(ritualStone))
    	{
    		player.addChatMessage(new ChatComponentTranslation("message.ritualdemonportal.missingjar"));
    		return false;
    	}
        return true;
    }
    
    public boolean checkJars(IMasterRitualStone ritualStone)
    {
    	int x = ritualStone.getXCoord();
    	int y = ritualStone.getYCoord();
    	int z = ritualStone.getZCoord();
    	
    	for(Int3 pos : jarLocations)
    	{
    		if(!(ritualStone.getWorld().getTileEntity(x + pos.xCoord, y + pos.yCoord, z + pos.zCoord) instanceof TEBellJar))
    		{
    			return false;
    		}
    	}
    	
    	return true;
    }

    @Override
    public List<RitualComponent> getRitualComponentList()
    {
        ArrayList<RitualComponent> demonRitual = new ArrayList();
        this.addParallelRunes(demonRitual, 3, 0, RitualComponent.FIRE);
        this.addParallelRunes(demonRitual, 5, 0, RitualComponent.FIRE);
        this.addCornerRunes(demonRitual, 2, 0, RitualComponent.AIR);
        this.addCornerRunes(demonRitual, 3, 0, RitualComponent.DUSK);
        this.addOffsetRunes(demonRitual, 3, 4, 0, RitualComponent.AIR);
        
        this.addCornerRunes(demonRitual, 4, 1, RitualComponent.BLANK);
        this.addCornerRunes(demonRitual, 4, 2, RitualComponent.EARTH);
        this.addCornerRunes(demonRitual, 4, 3, RitualComponent.EARTH);
        this.addCornerRunes(demonRitual, 4, 4, RitualComponent.DUSK);

        this.addParallelRunes(demonRitual, 6, 1, RitualComponent.BLANK);
        this.addParallelRunes(demonRitual, 6, 2, RitualComponent.WATER);
        this.addParallelRunes(demonRitual, 6, 3, RitualComponent.WATER);
        this.addParallelRunes(demonRitual, 6, 4, RitualComponent.DUSK);

        this.addOffsetRunes(demonRitual, 2, 6, 1, RitualComponent.FIRE);
        this.addOffsetRunes(demonRitual, 2, 7, 1, RitualComponent.BLANK);
        this.addOffsetRunes(demonRitual, 2, 8, 1, RitualComponent.FIRE);
        this.addOffsetRunes(demonRitual, 2, 9, 1, RitualComponent.BLANK);
        this.addOffsetRunes(demonRitual, 1, 9, 1, RitualComponent.AIR);
        this.addParallelRunes(demonRitual, 9, 2, RitualComponent.DUSK);
        
        this.addCornerRunes(demonRitual, 6, 3, RitualComponent.BLANK);
        this.addOffsetRunes(demonRitual, 6, 7, 3, RitualComponent.BLANK);
        this.addOffsetRunes(demonRitual, 5, 7, 3, RitualComponent.AIR);
        this.addOffsetRunes(demonRitual, 4, 7, 3, RitualComponent.AIR);

        return demonRitual;
    }
}
