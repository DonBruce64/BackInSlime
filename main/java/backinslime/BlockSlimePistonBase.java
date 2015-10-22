package backinslime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.Facing;
import net.minecraft.world.World;

public class BlockSlimePistonBase extends BlockPistonBase{	
	private boolean isSticky;
	private List<int[]> pushingBlockCoords = new ArrayList<int[]>();
	private List<Block> pushedBlockList = new ArrayList<Block>();
	private List<int[]> pushedBlockData = new ArrayList<int[]>();
	
	public BlockSlimePistonBase(boolean isSticky) {
		super(isSticky);
		this.isSticky=isSticky;
	}
	
	@Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack){
        world.setBlockMetadataWithNotify(x, y, z, determineOrientation(world, x, y, z, entity), 2);
        if(!world.isRemote){
            this.updatePistonState(world, x, y, z);
        }
    }

	@Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block){
        if(!world.isRemote){
            this.updatePistonState(world, x, y, z);
        }
    }

    public void onBlockAdded(World world, int x, int y, int z){
        if(!world.isRemote && world.getTileEntity(x, y, z) == null){
            this.updatePistonState(world, x, y, z);
        }
    }
    
	private boolean isIndirectlyPowered(World world, int x, int y, int z, int metadata){
		return metadata != 0 && world.getIndirectPowerOutput(x, y - 1, z, 0) ? true : (metadata != 1 && world.getIndirectPowerOutput(x, y + 1, z, 1) ? true : (metadata != 2 && world.getIndirectPowerOutput(x, y, z - 1, 2) ? true : (metadata != 3 && world.getIndirectPowerOutput(x, y, z + 1, 3) ? true : (metadata != 5 && world.getIndirectPowerOutput(x + 1, y, z, 5) ? true : (metadata != 4 && world.getIndirectPowerOutput(x - 1, y, z, 4) ? true : (world.getIndirectPowerOutput(x, y, z, 0) ? true : (world.getIndirectPowerOutput(x, y + 2, z, 1) ? true : (world.getIndirectPowerOutput(x, y + 1, z - 1, 2) ? true : (world.getIndirectPowerOutput(x, y + 1, z + 1, 3) ? true : (world.getIndirectPowerOutput(x - 1, y + 1, z, 4) ? true : world.getIndirectPowerOutput(x + 1, y + 1, z, 5)))))))))));
	}
	

    /**
     * Called during changes to see if an update block event needs to go to the server.
     * Actual update is handled in onBlockEventReceived.
     * @param world
     * @param x
     * @param y
     * @param z
     */
    private void updatePistonState(World world, int x, int y, int z){
        int pistonMetadata = world.getBlockMetadata(x, y, z);
        int side = getPistonOrientation(pistonMetadata);
        if(side != 7){ //should never be 7, but just checking
            boolean isPowered = this.isIndirectlyPowered(world, x, y, z, side);
            if(isPowered && !isExtended(pistonMetadata)){
            	clearBlockLists();
                if(getPushableBlocks(world, x+Facing.offsetsXForSide[side], y+Facing.offsetsYForSide[side], z+Facing.offsetsZForSide[side], side, x, y, z)<=12){
                    world.addBlockEvent(x, y, z, this, 0, side); //push piston
                }
            }else if(!isPowered && isExtended(pistonMetadata)){
            	if(this.isSticky){
            		clearBlockLists();
            		if(getPushableBlocks(world, x+Facing.offsetsXForSide[side]*2, y+Facing.offsetsYForSide[side]*2, z+Facing.offsetsZForSide[side]*2, Facing.oppositeSide[side], x+Facing.offsetsXForSide[side], y+Facing.offsetsYForSide[side], z+Facing.offsetsZForSide[side])>12){
            			return;
            		}
            	}
            	world.addBlockEvent(x, y, z, this, 1, side); //pull piston
            }
        }
    }
	

	
	@Override
	public boolean onBlockEventReceived(World world, int x, int y, int z, int extend, int side){
        if(!world.isRemote){
            boolean hasPower = this.isIndirectlyPowered(world, x, y, z, side);
            if(hasPower && extend == 1){
                world.setBlockMetadataWithNotify(x, y, z, side | 8, 2);
                return false;
            }
            if(!hasPower && extend == 0){
                return false;
            }
        }
        
        if(extend == 0){
        	clearBlockLists();
        	if(getPushableBlocks(world, x+Facing.offsetsXForSide[side], y+Facing.offsetsYForSide[side], z+Facing.offsetsZForSide[side], side, x, y, z)<=12){
        		pushBlocks(world, x, y, z, side, true);
    			world.setBlock(x + Facing.offsetsXForSide[side], y + Facing.offsetsYForSide[side], z + Facing.offsetsZForSide[side], Blocks.piston_extension, side | (this.isSticky ? 8 : 0), 4);
                world.setTileEntity(x + Facing.offsetsXForSide[side], y + Facing.offsetsYForSide[side], z + Facing.offsetsZForSide[side], BlockPistonMoving.getTileEntity(BIS.slimePistonHead, side | (this.isSticky ? 8 : 0), side, true, false));
                //world.notifyBlocksOfNeighborChange(x + Facing.offsetsXForSide[side], y + Facing.offsetsYForSide[side], z + Facing.offsetsZForSide[side], BIS.slimePistonMoving);
        	}else{
        		return false;
        	}
            world.setBlockMetadataWithNotify(x, y, z, side | 8, 2);
            world.playSoundEffect((double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D, "tile.piston.out", 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);
        
        }else if(extend == 1){
        	TileEntity tileentity = world.getTileEntity(x + Facing.offsetsXForSide[side], y + Facing.offsetsYForSide[side], z + Facing.offsetsZForSide[side]);
            if(tileentity instanceof TileEntityPiston){
                ((TileEntityPiston)tileentity).clearPistonTileEntity();
            }
            world.setBlock(x, y, z, Blocks.piston_extension, side, 3);
            world.setTileEntity(x, y, z, BlockPistonMoving.getTileEntity(this, side, side, false, true));

        	if(this.isSticky){
                int pulledBlockX = x + Facing.offsetsXForSide[side] * 2;
                int pulledBlockY = y + Facing.offsetsYForSide[side] * 2;
                int pulledBlockZ = z + Facing.offsetsZForSide[side] * 2;
                Block pulledBlock = world.getBlock(pulledBlockX, pulledBlockY, pulledBlockZ);
                int pulledBlockMetadata = world.getBlockMetadata(pulledBlockX, pulledBlockY, pulledBlockZ);

                if(pulledBlock.getMaterial() != Material.air && canPushBlock(pulledBlock, world, pulledBlockX, pulledBlockY, pulledBlockZ, false) && (pulledBlock.getMobilityFlag() == 0 || pulledBlock == BIS.slimePiston || pulledBlock == BIS.stickySlimePiston)){
                	clearBlockLists();
                	if(!pulledBlock.equals(BIS.slimeBlock)){
                		pushingBlockCoords.add(new int[] {pulledBlockX, pulledBlockY, pulledBlockZ});
                	}                	
                    if(getPushableBlocks(world, pulledBlockX, pulledBlockY, pulledBlockZ, Facing.oppositeSide[side], x + Facing.offsetsXForSide[side], y + Facing.offsetsYForSide[side], z + Facing.offsetsZForSide[side])<=12){
                     	pushBlocks(world, x, y, z, Facing.oppositeSide[side], false);
                    }else{
                    	return false;
                    }
                	x += Facing.offsetsXForSide[side];
                    y += Facing.offsetsYForSide[side];
                    z += Facing.offsetsZForSide[side];
                    world.setBlock(x, y, z, Blocks.piston_extension, pulledBlockMetadata, 3);
                    world.setTileEntity(x, y, z, BlockPistonMoving.getTileEntity(pulledBlock, pulledBlockMetadata, side, false, false));
                }else{
                    world.setBlockToAir(x + Facing.offsetsXForSide[side], y + Facing.offsetsYForSide[side], z + Facing.offsetsZForSide[side]);
                }
        	}else{
        		world.setBlockToAir(x + Facing.offsetsXForSide[side], y + Facing.offsetsYForSide[side], z + Facing.offsetsZForSide[side]);
        	}
        	world.playSoundEffect((double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D, "tile.piston.in", 0.5F, world.rand.nextFloat() * 0.15F + 0.6F);
        }
        return true;
    }  

    /**
     * Recursive, internal method to calculates the blocks that constitute a piston system.
     * This includes all attached slime blocks.  It populates the pushedBlockCoordinate List pushingBlockCoordinate list,
     * so use those to determine what blocks need pushing/ are pushers. 
     * Called from populatePushableBlocks.
     * @param world The current world
     * @param x X-coordinate of push chain
     * @param y Y-coordinate of push chain
     * @param z Z-coordinate of push chain
     * @param side Direction piston or block is facing, determines extension direction
     * @return The number of blocks that will be pushed by this piston system.
     */
    private int getPushableBlocks(World world, int x, int y, int z, final int side, final int pistonX, final int pistonY, final int pistonZ){    	
    	Block thisBlock=world.getBlock(x, y, z);
    	Block pushedBlock=thisBlock;
    	int pushedBlockX=x;
        int pushedBlockY=y;
        int pushedBlockZ=z;
        int pushedBlockMeta=world.getBlockMetadata(pushedBlockX, pushedBlockY, pushedBlockZ);
        int blocksPushed = 0;
        while(true){
        	if(pushedBlock.isAir(world, pushedBlockX, pushedBlockY, pushedBlockZ)){
            	return blocksPushed;
            }
        	if(pushedBlockY <= 0 || pushedBlockY >= world.getHeight()){
                return 13;
            }
            if(!canPushBlock(pushedBlock, world, pushedBlockX, pushedBlockY, pushedBlockZ, true)){
            	if(pushedBlockX==pistonX && pushedBlockY==pistonY && pushedBlockZ==pistonZ){
            		return blocksPushed;
            	}else{
            		return 13;
            	}
            }
            if(pushedBlock.getMobilityFlag() != 1){
            	for(int i=0; i<pushedBlockData.size(); ++i){
            		if(Arrays.equals(pushedBlockData.get(i), new int[] {pushedBlockX, pushedBlockY, pushedBlockZ, pushedBlockMeta})){
            			return blocksPushed;
            		}
            	}
                ++blocksPushed;
                pushedBlockList.add(pushedBlock);
                pushedBlockData.add(new int[] {pushedBlockX, pushedBlockY, pushedBlockZ, pushedBlockMeta});
            }else{
                float chance = pushedBlock instanceof BlockSnow ? -1.0f : 1.0f;
                pushedBlock.dropBlockAsItemWithChance(world, pushedBlockX, pushedBlockY, pushedBlockZ, world.getBlockMetadata(pushedBlockX, pushedBlockY, pushedBlockZ), chance, 0);
                world.setBlockToAir(pushedBlockX, pushedBlockY, pushedBlockZ);
            }
            
            if(pushedBlock.equals(BIS.slimeBlock)){
        		for(int i=0; i<6; ++i){
        			int attachedX=pushedBlockX+Facing.offsetsXForSide[i];
    				int attachedY=pushedBlockY+Facing.offsetsYForSide[i];
    				int attachedZ=pushedBlockZ+Facing.offsetsZForSide[i];
    				int attachedMeta=world.getBlockMetadata(attachedX, attachedY, attachedZ);
    				Block attachedBlock=world.getBlock(attachedX, attachedY, attachedZ);
        			
        			
    				if(!(attachedX==pistonX && attachedY==pistonY && attachedZ==pistonZ) && i != side){
    					if(attachedBlock.isAir(world, attachedX, attachedY, attachedZ)){
    						if(i==Facing.oppositeSide[side]){
	        					pushingBlockCoords.add(new int[] {pushedBlockX, pushedBlockY, pushedBlockZ});
	        				}	
    					}else if(canPushBlock(attachedBlock, world, attachedX, attachedY, attachedZ, false)){
    						blocksPushed+=getPushableBlocks(world, attachedX, attachedY, attachedZ, side, pistonX, pistonY, pistonZ);
    						if(attachedBlock != BIS.slimeBlock){
    							int rearX=attachedX-Facing.offsetsXForSide[side];
        						int rearY=attachedY-Facing.offsetsYForSide[side];
        						int rearZ=attachedZ-Facing.offsetsZForSide[side];
        						int rearMeta=world.getBlockMetadata(rearX, rearY, rearZ);
        						Block rearBlock=world.getBlock(rearX, rearY, rearZ);
        						
        						//don't push if a pushed block is behind it
        						boolean needsPusher=true;
        				    	for(int[] coordArray : pushedBlockData){
        		            		if(Arrays.equals(coordArray, new int[] {rearX, rearY, rearZ, rearMeta})){
        		            			needsPusher=false;
        		            		}
        		            	}
        				    	if(needsPusher){
        				    		pushingBlockCoords.add(new int[] {attachedX, attachedY, attachedZ});
        				    	}
    						}
    					}else if(i==Facing.oppositeSide[side]){
        					pushingBlockCoords.add(new int[] {pushedBlockX, pushedBlockY, pushedBlockZ});
        				}	
    				}
            	}
            }
        	if(blocksPushed>=13){
        		return 13;
        	}
        	pushedBlockX += Facing.offsetsXForSide[side];
            pushedBlockY += Facing.offsetsYForSide[side];
            pushedBlockZ += Facing.offsetsZForSide[side];
            pushedBlockMeta=world.getBlockMetadata(pushedBlockX, pushedBlockY, pushedBlockZ);
            pushedBlock = world.getBlock(pushedBlockX, pushedBlockY, pushedBlockZ);
        }
    }

    /**
     * Pushes all blocks in the pushedBlocks list and pushingBlocks list
     * Also launches entities if needed
     * @param world World
     * @param x X-coordinate of piston pushing
     * @param y Y-coordinate of piston pushing
     * @param z Z-coordinate of piston pushing
     * @param side Side the block is moving towards.
     * @param extending Whether the piston is extending or retracting
     */
    private void pushBlocks(World world, int x, int y, int z, int side, boolean extending){
    	int blockX;
    	int blockY;
    	int blockZ;
    	int blockMeta;
    	Block block;
  
    	for(int i=0; i<pushedBlockList.size(); ++i){	
    		block = pushedBlockList.get(i);
    		blockX=pushedBlockData.get(i)[0] + Facing.offsetsXForSide[side];
    		blockY=pushedBlockData.get(i)[1] + Facing.offsetsYForSide[side];
    		blockZ=pushedBlockData.get(i)[2] + Facing.offsetsZForSide[side];
    		blockMeta=pushedBlockData.get(i)[3];
    		world.setBlock(blockX, blockY, blockZ, Blocks.piston_extension, blockMeta, 4);
            world.setTileEntity(blockX, blockY, blockZ, BlockPistonMoving.getTileEntity(block, blockMeta, side, true, false));
            world.notifyBlocksOfNeighborChange(blockX, blockY, blockZ, block);
            
            if(extending){
            	Iterator entityIterator=world.getEntitiesWithinAABBExcludingEntity(null, this.getCollisionBoundingBoxFromPool(world, blockX, blockY, blockZ)).iterator();
            	Entity entity;
            	while(entityIterator.hasNext()){
            		entity=(Entity) entityIterator.next();
            		entity.motionX += Facing.offsetsXForSide[side]*.5;
            		entity.motionY += Facing.offsetsYForSide[side]*.5;
            		entity.motionZ += Facing.offsetsZForSide[side]*.5;
            	}
            }
    	}
    	
    	for(int i=0; i<pushingBlockCoords.size(); ++i){
    		blockX=pushingBlockCoords.get(i)[0];
    		blockY=pushingBlockCoords.get(i)[1];
    		blockZ=pushingBlockCoords.get(i)[2];
    		world.setBlockToAir(blockX, blockY, blockZ);
    		world.notifyBlocksOfNeighborChange(blockX, blockY, blockZ, Blocks.air);
    	}
    }
    
    private boolean canPushBlock(Block block, World world, int x, int y, int z, boolean p_150080_5_){
        if(block == Blocks.obsidian){
            return false;
        }else{
            if(block != BIS.slimePiston && block != BIS.stickySlimePiston){
                if(block.getBlockHardness(world, x, y, z) == -1.0F){
                    return false;
                }else if(block.getMobilityFlag() == 2){
                    return false;
                }else if(block.getMobilityFlag() == 1){
                    return !p_150080_5_;
                }
            }else if(isExtended(world.getBlockMetadata(x, y, z))){
                return false;
            }
            return !(world.getBlock(x, y, z).hasTileEntity(world.getBlockMetadata(x, y, z)));
        }
    }
    
    private void clearBlockLists(){
    	pushedBlockList.clear();
    	pushedBlockData.clear();
    	pushingBlockCoords.clear();
    }
}

