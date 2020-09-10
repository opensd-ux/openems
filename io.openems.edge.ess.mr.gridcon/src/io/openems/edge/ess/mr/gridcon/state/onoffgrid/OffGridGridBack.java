package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.enums.Mode;

public class OffGridGridBack extends BaseState {

	private float targetFrequencyOffgrid;

	public OffGridGridBack(ComponentManager manager, DecisionTableCondition condition, String outputSyncBridge,
			String meterId, float targetFrequencyOffgrid) {
		super(manager, condition, outputSyncBridge, meterId);
		this.targetFrequencyOffgrid = targetFrequencyOffgrid;
	}

	@Override
	public IState getState() {
		return OnOffGridState.OFF_GRID_GRID_BACK;
	}

	@Override
	public IState getNextState() {
		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}

		if (DecisionTableHelper.isOffGridGridBack(condition)) {
			return OnOffGridState.OFF_GRID_GRID_BACK;
		}

		if (DecisionTableHelper.isOffGrid(condition)) {
			return OnOffGridState.OFF_GRID;
		}

		if (DecisionTableHelper.isOffGridAdjustParameter(condition)) {
			return OnOffGridState.OFF_GRID_ADJUST_PARMETER;
		}
		
		if (DecisionTableHelper.isOffGridStart(condition)) {
			return OnOffGridState.OFF_GRID_START;
		}

		return OnOffGridState.UNDEFINED;
	}

	@Override
	public void act() throws OpenemsNamedException {
		setSyncBridge(true);		
	}

	@Override
	public GridconSettings getGridconSettings() {
		float factor = targetFrequencyOffgrid / GridconPcs.DEFAULT_GRID_FREQUENCY;
		return GridconSettings.createRunningSettings(1, factor, Mode.VOLTAGE_CONTROL);
	}

}