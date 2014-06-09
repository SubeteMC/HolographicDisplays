package com.gmail.filoghost.holograms.object;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.gmail.filoghost.holograms.Configuration;
import com.gmail.filoghost.holograms.HolographicDisplays;
import com.gmail.filoghost.holograms.api.Hologram;
import com.gmail.filoghost.holograms.api.TouchHandler;
import com.gmail.filoghost.holograms.commands.CommandValidator;
import com.gmail.filoghost.holograms.exception.CommandException;
import com.gmail.filoghost.holograms.exception.SpawnFailedException;
import com.gmail.filoghost.holograms.utils.Validator;
import com.gmail.filoghost.holograms.utils.VisibilityManager;

/**
 * This class is only used by the plugin itself. Other plugins should just use the API.
 */

public class CraftHologram extends HologramBase implements Hologram {
	
	private List<FloatingDoubleEntity> linesEntities;
	private List<String> textLines;
	
	private VisibilityManager visibilityManager;
	
	private long creationTimestamp;
	
	private FloatingTouchSlime touchSlimeEntity;
	private TouchHandler touchHandler;

	public CraftHologram(String name, Location source) {
		super(name, source);
		linesEntities = new ArrayList<FloatingDoubleEntity>();
		textLines = new ArrayList<String>();
		touchSlimeEntity = new FloatingTouchSlime();
		creationTimestamp = System.currentTimeMillis();
	}
	
	public void addLine(String message) {
		if (message == null) {
			message = "";
		}

		textLines.add(message);
	}
	
	public void insertLine(int index, String message) {
		if (message == null) {
			message = "";
		}

		textLines.add(index, message);
	}

	public String[] getLines() {
		return textLines.toArray(new String[textLines.size()]);
	}
	
	public void setLine(int index, String text) {
		if (text == null) {
			text = "";
		}
		
		textLines.set(index, text);
	}
	
	public void clearLines() {
		textLines.clear();
	}
	
	public void removeLine(int index) {
		textLines.remove(index);
	}
	
	public int getLinesLength() {
		return textLines.size();
	}
	
	@Override
	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setVisibilityManager(VisibilityManager visibilityManager) {
		this.visibilityManager = visibilityManager;
	}
	
	public boolean hasVisibilityManager() {
		return visibilityManager != null;
	}
	
	public VisibilityManager getVisibilityManager() {
		return visibilityManager;
	}
	
	public void setTouchHandler(TouchHandler touchHandler) {
		this.touchHandler = touchHandler;
		
		if (touchHandler != null && !touchSlimeEntity.isSpawned()) {
			try {
				touchSlimeEntity.spawn(this, bukkitWorld, x, y, z);
			} catch (SpawnFailedException e) { }
		} else if (touchHandler == null && touchSlimeEntity.isSpawned()) {
			touchSlimeEntity.despawn();
		}
	}

	public boolean hasTouchHandler() {
		return touchHandler != null;
	}

	public TouchHandler getTouchHandler() {
		return touchHandler;
	}
	
	public boolean update() {
		if (isInLoadedChunk()) {
			return forceUpdate();
		}
		
		return true;
	}

	/**
	 *  Updates the hologram without checking if it's in a loaded chunk.
	 */
	public boolean forceUpdate() {
		
		Validator.checkState(!isDeleted(), "Hologram already deleted");
	
		// Remove previous entities.
		hide();
		
		try {
			
			double lineSpacing = Configuration.verticalLineSpacing;
			
			// While iterating we change this var.
			double currentY = this.y;
			
			for (String text : textLines) {
				
				if (text.length() >= 5 && text.substring(0, 5).toLowerCase().equals("icon:")) {
					
					// It's a floating icon!
					ItemStack icon;
					try {
						icon = CommandValidator.matchItemStack(text.substring(5).trim());
					} catch (CommandException e) {
						icon = new ItemStack(Material.BEDROCK);
					}
					
					// If the current Y has been changed, the item is NOT on top of the hologram.
					if (currentY != this.y) {
						// Extra space for the floating item...
						currentY -= 0.27;
					}
					
					FloatingItem lineEntity = new FloatingItem(icon);
					lineEntity.spawn(this, bukkitWorld, x, currentY, z);
					linesEntities.add(lineEntity);
					
					// And some more space below.
					currentY -= 0.05;
					
				} else {
				
					HologramLine lineEntity = new HologramLine(text);
					lineEntity.spawn(this, bukkitWorld, x, currentY, z);
					linesEntities.add(lineEntity);
				
					// Placeholders.
					HolographicDisplays.getPlaceholderManager().trackIfNecessary(lineEntity.getHorse());
				}
				
				currentY -= lineSpacing;
			}
			
			if (touchHandler != null) {
				touchSlimeEntity.spawn(this, bukkitWorld, x, y, z);
			}
			
		} catch (SpawnFailedException ex) {
			// Kill the entities and return false.
			hide();
			return false;
		}
		
		return true;
	}

	public void hide() {
		for (FloatingDoubleEntity lineEntity : linesEntities) {
			lineEntity.despawn();
		}
		linesEntities.clear();
		
		if (touchSlimeEntity.isSpawned()) {
			touchSlimeEntity.despawn();
		}
	}

	@Override
	public void onDeleteEvent() {
		hide();
		HologramManager.remove(this);
		APIHologramManager.remove(this);
	}
	
	public String toString() {
		return "CraftHologram{lines=" + textLines.toString() + ",x=" + x + ",y=" + y + ",z=" + z + ",world=" + bukkitWorld.getName() + "}";
	}
}