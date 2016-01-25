package backinslime;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = BIS.MODID, name = BIS.MODNAME, version = BIS.MODVER)
public class BIS {
	public static final String MODID="bis";
	public static final String MODNAME="Back in Slime Mod";
	public static final String MODVER="3.1.0";
	
	public static final BlockSlime slimeBlock = new BlockSlime();
	public static final BlockSlimePistonBase slimePiston = new BlockSlimePistonBase(false);
	public static final BlockSlimePistonBase stickySlimePiston = new BlockSlimePistonBase(true);
	public static final BlockSlimePistonHead slimePistonHead = new BlockSlimePistonHead();
	
	
	@EventHandler
	public void Init(FMLInitializationEvent event){
		GameRegistry.registerBlock(slimeBlock, "SlimeBlock");
		GameRegistry.registerBlock(slimePiston, "SlimePistonBase").setBlockName("SlimePiston");
		GameRegistry.registerBlock(stickySlimePiston, "StickySlimePistonBase").setBlockName("StickySlimePiston");
		GameRegistry.registerBlock(slimePistonHead, "SlimePistonHead").setBlockName("SlimePistonHead");
		this.initRecipies();
	}
	
	private void initRecipies(){
		GameRegistry.addShapedRecipe(new ItemStack(Item.getItemFromBlock(slimeBlock)),
		"AAA",
		"AAA",
		"AAA",
		'A', new ItemStack(Items.slime_ball));
		GameRegistry.addShapelessRecipe(new ItemStack(Items.slime_ball, 9), new ItemStack(slimeBlock));
		GameRegistry.addShapelessRecipe(new ItemStack(stickySlimePiston), new ItemStack(slimePiston), new ItemStack(Items.slime_ball));		
		GameRegistry.addShapelessRecipe(new ItemStack(slimePiston), new ItemStack(Blocks.piston));
		GameRegistry.addShapelessRecipe(new ItemStack(stickySlimePiston), new ItemStack(Blocks.sticky_piston));
		GameRegistry.addShapelessRecipe(new ItemStack(Blocks.piston), new ItemStack(slimePiston));
		GameRegistry.addShapelessRecipe(new ItemStack(Blocks.sticky_piston), new ItemStack(stickySlimePiston));
	}
}

