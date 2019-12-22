package psx.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import docking.WindowPosition;
import ghidra.app.services.GoToService;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.AddressFactory;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryBlockType;
import ghidra.util.Msg;
import resources.ResourceManager;

public class DebuggerProvider extends ComponentProviderAdapter {

	private final static ImageIcon ICON = ResourceManager.loadImage("images/debug_icon.png");
	private final DebuggerGui gui;
	private DebuggerCore core;
	private final Program program;
	
	public DebuggerProvider(PluginTool tool, String name, Program program) {
		super(tool, name, name);
		
		this.program = program;

		gui = new DebuggerGui();
		gui.initButtonsState();
		setBtnActions();
		
		setIcon(ICON);
		setDefaultWindowPosition(WindowPosition.RIGHT);
		setTitle("PSX Debugger");
		setVisible(true);
	}
	
	@Override
	public void componentActivated() {
		super.componentActivated();
		updateAddressSpaces();
	}
	
	private void startDebugger() {
		try {
			core = new DebuggerCore("localhost");
			showPcReg();
		} catch (IOException e) {
			gui.initButtonsState();
			Msg.showError(this, gui, "Error", "Cannot connect to debugger server!", e);
		}
	}
	
	private void stopDebugger() {
		if (core == null) {
			return;
		}
		
		try {
			core.closeSocket();
		} catch (IOException e) {
			Msg.showError(this, gui, "Error", "Cannot close debugger connection!", e);
		}
	}
	
	private void showPcReg() {
		long pc = 0L;
		
		try {
			pc = core.getPcRegister();
			gui.setPcRegDisplay(pc);
		} catch (IOException e) {
			Msg.showError(this, gui, "Error", "Cannot get PC value!", e);
			return;
		}
		
		GoToService gotoService = tool.getService(GoToService.class);
		
		AddressFactory af = program.getAddressFactory();
		AddressSpace as = af.getAddressSpace(gui.getAddressSpace());
		
		if (as == null) {
			as = af.getDefaultAddressSpace();
		}
		
		if (gotoService != null) {
			gotoService.goTo(as.getAddress(pc));
		}
	}
	
	private void stepInto() {
		if (core == null) {
			return;
		}
		
		try {
			if (!core.stepInto()) {
				throw new IOException();
			}
		} catch (IOException e) {
			Msg.showWarn(this, gui, "Warning", "Cannot step into!");
		}
		
		showPcReg();
	}
	
	private void stepOver() {
		if (core == null) {
			return;
		}
		
		try {
			if (!core.stepOver()) {
				throw new IOException();
			}
		} catch (IOException e) {
			Msg.showWarn(this, gui, "Warning", "Cannot step over!");
		}
		
		showPcReg();
	}
	
	private void pause() {
		if (core == null) {
			return;
		}
		
		try {
			core.pause();
		} catch (IOException e) {
			Msg.showWarn(this, gui, "Warning", "Cannot pause!");
		}
		
		showPcReg();
	}
	
	private void resume() {
		if (core == null) {
			return;
		}
		
		try {
			core.resume();
		} catch (IOException e) {
			Msg.showWarn(this, gui, "Warning", "Cannot resume!");
		}
	}
	
	private void setBtnActions() {
		if (gui == null) {
			return;
		}
		
		gui.setStartDebuggerAction(a -> startDebugger());
		gui.setStopDebuggerAction(a -> stopDebugger());
		gui.setStepIntoAction(a -> stepInto());
		gui.setStepOverAction(a -> stepOver());
		gui.setPauseAction(a -> pause());
		gui.setRunAction(a -> resume());
	}
	
	private void updateAddressSpaces() {
		List<String> overlays = new ArrayList<>();
		MemoryBlock[] memBlocks = program.getMemory().getBlocks();
		for (MemoryBlock block : memBlocks) {
			if (block.getType() == MemoryBlockType.OVERLAY) {
				overlays.add(block.getName());
			}
		}
		
		gui.updateAddressSpacesList(overlays.toArray(String[]::new));
	}
	
	public void close() {
		stopDebugger();
	}

	@Override
	public JComponent getComponent() {
		return gui;
	}
}