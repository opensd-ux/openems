package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.Creator;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyComponentManager;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyDecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyIo;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyMeter;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.MeterCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NaProtection1On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NaProtection2On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.SyncBridgeOn;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.VoltageInRange;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OffGridAdjustParameter;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState;

public class TestOffGridAdjustParameter {

	private OffGridAdjustParameter sut;
	private DummyComponentManager manager = Creator.getDummyComponentManager();
	private static DummyDecisionTableCondition condition;
		
	@Before
	public void setUp() throws Exception {
		condition = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		sut = new OffGridAdjustParameter(//
				manager  
				, condition//
				, Creator.OUTPUT_SYNC_DEVICE_BRIDGE//
				, Creator.METER_ID//
				, Creator.DELTA_FREQUENCY//
				, Creator.DELTA_VOLTAGE//
				);
	}

	@Test
	public final void testGetState() {
		assertEquals(OnOffGridState.OFF_GRID_ADJUST_PARMETER, sut.getState());
	}
			
	@Test
	public void testGetNextStateOffGrid() {
		// According to the state machine the next state is "OFF_GRID" if condition is 0,0,0,0,-
		setCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID, sut.getNextState());

		setCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.OFF_GRID, sut.getNextState());
	}
		
	@Test
	public void testGetNextStateAdjustParameter() {
		// According to the state machine the next state is "OFF GRID ADJUST PARAMETER" if condition is 1,0,0,1,1
		setCondition(NaProtection1On.TRUE, NaProtection2On.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_ADJUST_PARMETER, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOnGrid() {
		// According to the state machine the next state is "ON GRID" if condition is 1,1,-,-,-
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.UNSET, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.UNSET, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.UNSET, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		

		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.UNSET, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.UNSET, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.UNSET, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.FALSE, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.TRUE, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.UNSET, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
				
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.UNSET, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, MeterCommunicationFailed.UNSET, VoltageInRange.UNSET, SyncBridgeOn.UNSET);
		assertEquals(OnOffGridState.ON_GRID, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOffGridDridBackInverterOff() {
		// According to the state machine the next state is "OFF GRID GRID BACK INVERTER OFF" if condition is 1,0,1,-,1
		setCondition(NaProtection1On.TRUE, NaProtection2On.FALSE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_GRID_BACK_INVERTER_OFF, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.FALSE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_GRID_BACK_INVERTER_OFF, sut.getNextState());
		
		setCondition(NaProtection1On.TRUE, NaProtection2On.FALSE, MeterCommunicationFailed.TRUE, VoltageInRange.UNSET, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_GRID_BACK_INVERTER_OFF, sut.getNextState());
	}
	
	@Test
	public void testAct() {
		// frequency preset for gridcon 0,2 Hz higher than grid, Voltage 5V higher than grid 
			
			try {
				sut.act();
				
				String channelName = DummyIo.adaptChannelAdress(Creator.OUTPUT_SYNC_DEVICE_BRIDGE);
				ChannelAddress adress = ChannelAddress.fromString(channelName);
				BooleanWriteChannel outputSyncDeviceBridgeChannel = this.manager.getChannel(adress);
				boolean expectedSyncBridge = true;
				boolean actualSyncBridge = outputSyncDeviceBridgeChannel.getNextWriteValue().get();
				
				assertEquals(expectedSyncBridge, actualSyncBridge);
				
				DummyMeter meter = this.manager.getComponent(Creator.METER_ID);
								
				float actualFrequency = meter.getFrequency().value().get() / 1000;
				float expectedFrequencyFactor = (actualFrequency + Creator.DELTA_FREQUENCY) / actualFrequency;
				
				float actualVoltage = meter.getVoltage().value().get() / 1000;
				float expectedVoltageFactor = (actualVoltage + Creator.DELTA_VOLTAGE) / actualVoltage;
				
				GridconSettings expectedSettings = GridconSettings.createRunningSettings(expectedVoltageFactor, expectedFrequencyFactor, Mode.VOLTAGE_CONTROL);
			
				GridconSettings actualSettings = sut.getGridconSettings();
				
				assertEquals(expectedSettings, actualSettings);				
				
			} catch (Exception e) {
				fail("Should not happen");
			}
	}
	
	private static void setCondition(NaProtection1On b, NaProtection2On c, MeterCommunicationFailed e, VoltageInRange f, SyncBridgeOn g) {
		condition.setNaProtection1On(b);
		condition.setNaProtection2On(c);
		condition.setMeterCommunicationFailed(e);
		condition.setVoltageInRange(f);
		condition.setSyncBridgeOn(g);
	}
}