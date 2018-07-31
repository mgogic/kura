/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.excavator;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.osgi.service.component.ComponentContext;
import org.osgi.annotation.versioning.ProviderType;

/**
 * 
 * 
 */
@ProviderType
public interface ExcavatorService {


  //  public void activate(ComponentContext componentContext, Map<String, Object> properties);


  //  public void deactivate(ComponentContext componentContext);


 //   public void updated(Map<String, Object> properties);

    public void startMotor(int motorNumber, String direction);

    public void stopMotor(int motorNumber);
    
    public long calculateDistance(KuraGPIOPin echoPin, KuraGPIOPin triggerPin, String sensorSide);
    
	public KuraGPIOPin getFrontSensorEcho(); 

	public KuraGPIOPin getFrontSensorTrigger() ;

	public KuraGPIOPin getBackSensorEcho();
	
	public KuraGPIOPin getBackSensorTrigger();

	public KuraGPIOPin getLeftSensorEcho();

	public KuraGPIOPin getLeftSensorTrigger();
	
	public KuraGPIOPin getRightSensorEcho();
	
	public KuraGPIOPin getRightSensorTrigger();
	
}
