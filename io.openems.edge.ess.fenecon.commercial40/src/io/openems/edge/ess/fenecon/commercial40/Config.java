package io.openems.edge.ess.fenecon.commercial40;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "FENECON Commercial 40 ESS", //
		description = "Implements the FENECON Commercial 40 energy storage system.")
@interface Config {
	String service_pid();

	String id();

	boolean enabled();

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus brige.")
	String modbus_id();

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "";

	String webconsole_configurationFactory_nameHint() default "FENECON Commercial 40 ESS [{id}]";
}