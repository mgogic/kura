/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Clean up kura properties handling
 *******************************************************************************/
package org.eclipse.kura.linux.excavator;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.excavator.ExcavatorService;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcavatorServiceImpl implements ConfigurableComponent, ExcavatorService {

    private static final Logger logger = LoggerFactory.getLogger(ExcavatorServiceImpl.class);
    // paramtehers
    // gpio directions only output
    // gpio mode push-pull output
    // gpio trigger - rising edge

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class GPIOServiceTrackerCustomizer implements ServiceTrackerCustomizer<GPIOService, GPIOService> {

        @Override
        public GPIOService addingService(final ServiceReference<GPIOService> reference) {
            ExcavatorServiceImpl.this.gpioService = ExcavatorServiceImpl.this.bundleContext.getService(reference);
            return ExcavatorServiceImpl.this.gpioService;
        }

        @Override
        public void modifiedService(final ServiceReference<GPIOService> reference, final GPIOService service) {
            ExcavatorServiceImpl.this.gpioService = ExcavatorServiceImpl.this.bundleContext.getService(reference);
        }

        @Override
        public void removedService(final ServiceReference<GPIOService> reference, final GPIOService service) {
            ExcavatorServiceImpl.this.gpioService = null;
        }
    }

    // Cloud Application identifier
    private static final String APP_ID = "EXCAVATOR_COMPONENT";

    private static final int EXCAVATOR_MOTOR_START_PIN = 4;
    private static final int EXCAVATOR_MOTOR_FRONT_DIRECTION_PIN = 8;
    private static final int EXCAVATOR_MOTOR_BACK_DIRECTION_PIN = 25;

    private static final int PLATFORM_MOTOR_START_PIN = 11;
    private static final int PLATFORM_MOTOR_START_PIN_MOTOR_FRONT_DIRECTION_PIN = 10;
    private static final int PLATFORM_MOTOR_START_PIN_MOTOR_BACK_DIRECTION_PIN = 9;

    private static final int WHEEL_MOTOR_START_PIN = 26;
    private static final int WHEEL_MOTOR_FRONT_DIRECTION_PIN = 20;
    private static final int WHEEL_MOTOR_BACK_DIRECTION_PIN = 21;
    // gpio directions
    public static final int INPUT_PIN_DIRECTION = 0;
    public static final int OUTPUT_PIN_DIRECTION = 1;
    public static final int BOTH_INIT_INPUT_MODE = 2;
    public static final int BOTH_INIT_OUTPUT_MODE = 3;
    // gpio modes
    public static final int DEFAULT_MODE = -1;
    public static final int INPUT_WITH_PULL_UP_MODE = 1;
    public static final int INPUT_WITH_PULL_DOWN_MODE = 2;
    public static final int PUSH_PULL_OUTPUT_MODE = 4;
    public static final int OPEN_DRAIN_OUTPUT_MODE = 8;
    // gpio triggers
    public static final int DEFAULT_TRIGGER = -1;
    public static final int BOTH_EDGES_TRIGGER = 3;
    public static final int FALLING_EDGES_TRIGGER = 1;
    public static final int RISING_EDGES_TRIGGER = 2;
    public static final int NO_TRIGGER = 0;

    private GpioComponentOptions gpioComponentOptions;
    private ServiceTrackerCustomizer<GPIOService, GPIOService> gpioServiceTrackerCustomizer;
    private ServiceTracker<GPIOService, GPIOService> gpioServiceTracker;

    private BundleContext bundleContext;
    private GPIOService gpioService;

    private List<KuraGPIOPin> acquiredOutputPins = new ArrayList<>();
    private List<KuraGPIOPin> acquiredInputPins = new ArrayList<>();

    private ScheduledFuture<?> blinkTask = null;
    private ScheduledFuture<?> pollTask = null;

    private boolean value;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    //excavator motors pins
    private KuraGPIOPin excavatorFrontDirectionPin;
    private KuraGPIOPin excavatorBackDirectionPin;
    private KuraGPIOPin excavatorPowerPin;

    private KuraGPIOPin frontDirectionPlatformPin;
    private KuraGPIOPin backDirectionPlatformPin;
    private KuraGPIOPin powerPlatformPin;

    private KuraGPIOPin frontDirectionWheelPin;
    private KuraGPIOPin backDirectionWheelPin;
    private KuraGPIOPin powerWheelPin;
    
    private List<KuraGPIOPin> motorPins = new ArrayList<KuraGPIOPin>();
    private List<KuraGPIOPin> pinsForOpening = new ArrayList<KuraGPIOPin>();
    
    // Proximity sensors pins
    private static final int FRONT_SENSOR_ECHO = 19;
    private static final int FRONT_SENSOR_TRIGGER = 13;

    private static final int BACK_SENSOR_ECHO = 24;
    private static final int BACK_SENSOR_TRIGGER = 23;

    private static final int RIGHT_SENSOR_ECHO = 6;
    private static final int RIGHT_SENSOR_TRIGGER = 5;

    private static final int LEFT_SENSOR_ECHO = 27;
    private static final int LEFT_SENSOR_TRIGGER = 17;
   
    private KuraGPIOPin frontSensorEcho;
    private KuraGPIOPin frontSensorTrigger ;

    private KuraGPIOPin backSensorEcho;
    private KuraGPIOPin backSensorTrigger;

    private KuraGPIOPin leftSensorEcho;
    private KuraGPIOPin leftSensorTrigger;

    private KuraGPIOPin rightSensorEcho;
    private KuraGPIOPin rightSensorTrigger;
    
    private long startTime;
    private long stop;
    private long start;
    private long distance;

    private final int SPEED_OF_SOUND = 34029;

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating {}", APP_ID);

        this.bundleContext = componentContext.getBundleContext();

        this.gpioComponentOptions = new GpioComponentOptions(properties);

        this.gpioServiceTrackerCustomizer = new GPIOServiceTrackerCustomizer();
        initGPIOServiceTracking();

        

        logger.info("Activating {}... Done.", APP_ID);
        excavatorBackDirectionPin = getPin(Integer.toString(EXCAVATOR_MOTOR_BACK_DIRECTION_PIN),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE), getPinTrigger(2));
        excavatorPowerPin = getPin(Integer.toString(EXCAVATOR_MOTOR_START_PIN), getPinDirection(OUTPUT_PIN_DIRECTION),
                getPinMode(PUSH_PULL_OUTPUT_MODE), getPinTrigger(2));
        
        excavatorFrontDirectionPin = getPin(Integer.toString(EXCAVATOR_MOTOR_FRONT_DIRECTION_PIN), getPinDirection(OUTPUT_PIN_DIRECTION),
                getPinMode(PUSH_PULL_OUTPUT_MODE), getPinTrigger(2));
        
        frontDirectionPlatformPin = getPin(Integer.toString(PLATFORM_MOTOR_START_PIN_MOTOR_FRONT_DIRECTION_PIN),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(RISING_EDGES_TRIGGER));
        backDirectionPlatformPin = getPin(Integer.toString(PLATFORM_MOTOR_START_PIN_MOTOR_BACK_DIRECTION_PIN),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(RISING_EDGES_TRIGGER));

        powerPlatformPin = getPin(Integer.toString(PLATFORM_MOTOR_START_PIN), getPinDirection(OUTPUT_PIN_DIRECTION),
                getPinMode(PUSH_PULL_OUTPUT_MODE), getPinTrigger(RISING_EDGES_TRIGGER));

        frontDirectionWheelPin = getPin(Integer.toString(WHEEL_MOTOR_FRONT_DIRECTION_PIN),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(RISING_EDGES_TRIGGER));

        backDirectionWheelPin = getPin(Integer.toString(WHEEL_MOTOR_BACK_DIRECTION_PIN),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(RISING_EDGES_TRIGGER));

        powerWheelPin = getPin(Integer.toString(WHEEL_MOTOR_START_PIN), getPinDirection(OUTPUT_PIN_DIRECTION),
                getPinMode(PUSH_PULL_OUTPUT_MODE), getPinTrigger(RISING_EDGES_TRIGGER));
        motorPins.clear();
        
        frontSensorEcho = getPin(Integer.toString(FRONT_SENSOR_ECHO),
                getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));
        frontSensorTrigger = getPin(Integer.toString(FRONT_SENSOR_TRIGGER),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));

        backSensorEcho = getPin(Integer.toString(BACK_SENSOR_ECHO),
                getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));


        backSensorTrigger = getPin(Integer.toString(BACK_SENSOR_TRIGGER),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));

        leftSensorEcho = getPin(Integer.toString(LEFT_SENSOR_ECHO),
                getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));

        leftSensorTrigger = getPin(Integer.toString(LEFT_SENSOR_TRIGGER),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));

        rightSensorEcho = getPin(Integer.toString(RIGHT_SENSOR_ECHO),
                getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));

        rightSensorTrigger = getPin(Integer.toString(RIGHT_SENSOR_TRIGGER),
                getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
                getPinTrigger(BOTH_EDGES_TRIGGER));
        
        pinsForOpening.add(excavatorBackDirectionPin);
        pinsForOpening.add(excavatorPowerPin);
        pinsForOpening.add(excavatorFrontDirectionPin);
        pinsForOpening.add(frontDirectionPlatformPin);
        pinsForOpening.add(backDirectionPlatformPin);
        pinsForOpening.add(powerPlatformPin);
        pinsForOpening.add(frontDirectionWheelPin);
        pinsForOpening.add(backDirectionWheelPin);
        pinsForOpening.add(powerWheelPin);
        
        pinsForOpening.add(frontSensorEcho);
        pinsForOpening.add(frontSensorTrigger);
        pinsForOpening.add(backSensorEcho);
        pinsForOpening.add(backSensorTrigger);
        pinsForOpening.add(leftSensorEcho);
        pinsForOpening.add(leftSensorTrigger);
        pinsForOpening.add(rightSensorEcho);
        pinsForOpening.add(rightSensorTrigger);

        openPins();

        doUpdate(properties);
        
        measureDistance(frontSensorEcho, frontSensorTrigger, "FRONT ");
        measureDistance(backSensorEcho, backSensorTrigger, "BACK ");
        measureDistance(leftSensorEcho, leftSensorTrigger, "LEFT ");
        measureDistance(rightSensorEcho, rightSensorTrigger, "RIGHT ");

    }

    public void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating {}", APP_ID);

        stopTasks();
        releasePins();

        if (nonNull(this.gpioServiceTracker)) {
            this.gpioServiceTracker.close();
        }

        this.executor.shutdown();
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated...");

        this.gpioComponentOptions = new GpioComponentOptions(properties);

        if (nonNull(this.gpioServiceTracker)) {
            this.gpioServiceTracker.close();
        }
        initGPIOServiceTracking();

        doUpdate(properties);
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate(Map<String, Object> properties) {
        stopTasks();
        releasePins();

        this.value = false;
      //  acquirePins();
        logger.info("New set of properties has been configured for motors");
       
        // if (!acquiredOutputPins.isEmpty()) {
        // submitBlinkTask(2000, acquiredOutputPins);
        // }
        //
        // if (!acquiredInputPins.isEmpty()) {
        // String inputReadMode = this.gpioComponentOptions.getInputReadMode();
        //
        // if (GpioComponentOptions.INPUT_READ_MODE_PIN_STATUS_LISTENER.equals(inputReadMode)) {
        // attachPinListeners(acquiredInputPins);
        // } else if (GpioComponentOptions.INPUT_READ_MODE_POLLING.equals(inputReadMode)) {
        // submitPollTask(500, acquiredInputPins);
        // }
        // }

    }

    private void acquirePins() {
        if (this.gpioService != null) {
            logger.info("______________________________");
            logger.info("Available GPIOs on the system:");
            Map<Integer, String> gpios = this.gpioService.getAvailablePins();
            for (Entry<Integer, String> e : gpios.entrySet()) {
                logger.info("#{} - [{}]", e.getKey(), e.getValue());
            }
            logger.info("______________________________");
            // getPins();
        }
    }

    private void getPins() {
        String[] pins = this.gpioComponentOptions.getPins();
        Integer[] directions = this.gpioComponentOptions.getDirections();
        Integer[] modes = this.gpioComponentOptions.getModes();
        Integer[] triggers = this.gpioComponentOptions.getTriggers();
        for (int i = 0; i < pins.length; i++) {
            try {
                logger.info("Acquiring GPIO pin {} with params:", pins[i]);
                logger.info("   Direction....: {}", directions[i]);
                logger.info("   Mode.........: {}", modes[i]);
                logger.info("   Trigger......: {}", triggers[i]);
                KuraGPIOPin p = getPin(pins[i], getPinDirection(directions[i]), getPinMode(modes[i]),
                        getPinTrigger(triggers[i]));
                if (p != null) {
                    p.open();
                    logger.info("GPIO pin {} acquired", pins[i]);
                    if (p.getDirection() == KuraGPIODirection.OUTPUT) {
                        acquiredOutputPins.add(p);
                    } else {
                        acquiredInputPins.add(p);
                    }
                } else {
                    logger.info("GPIO pin {} not found", pins[i]);
                }
            } catch (IOException e) {
                logger.error("I/O Error occurred!", e);
            } catch (Exception e) {
                logger.error("got errror", e);
            }
        }
    }

    private KuraGPIOPin getPin(String resource, KuraGPIODirection pinDirection, KuraGPIOMode pinMode,
            KuraGPIOTrigger pinTrigger) {
        KuraGPIOPin pin = null;
        // this.gpioService = new GPIOService();

        try {
            int terminal = Integer.parseInt(resource);
            if (terminal > 0 && terminal < 1255) {
                pin = this.gpioService.getPinByTerminal(terminal, pinDirection, pinMode, pinTrigger);
            }
        } catch (NumberFormatException e) {
            pin = this.gpioService.getPinByName(resource, pinDirection, pinMode, pinTrigger);
        }
        return pin;
    }

    //
    // private static final int EXCAVATOR_MOTOR_START_PIN = 4;
    // private static final int EXCAVATOR_MOTOR_FRONT_DIRECTION_PIN = 8;
    // private static final int EXCAVATOR_MOTOR_BACK_DIRECTION_PIN = 25;
    //
    // private static final int PLATFORM_MOTOR_START_PIN = 11;
    // private static final int PLATFORM_MOTOR_START_PIN_MOTOR_FRONT_DIRECTION_PIN = 10;
    // private static final int PLATFORM_MOTOR_START_PIN_MOTOR_BACK_DIRECTION_PIN = 9;
    //
    // private static final int WHEEL_MOTOR_START_PIN = 26;
    // private static final int WHEEL_MOTOR_FRONT_DIRECTION_PIN = 20;
    // private static final int WHEEL_MOTOR_BACK_DIRECTION_PIN = 21;
    // method for starting motors
    // 1 - excavator motor
    // 2 - platform motor
    // 3 - bucket wheel command
    @Override
    public void startMotor(int motorNumber, String direction) {

        if ("front".equals(direction)) {
            switch (motorNumber) {
            case 1:
                try {
                    excavatorPowerPin.setValue(true);
                    excavatorFrontDirectionPin.setValue(true);
                    excavatorBackDirectionPin.setValue(false);
                    // adding pins to list, in order to attach pin listeners to them
                    motorPins.add(excavatorFrontDirectionPin);
                    motorPins.add(excavatorBackDirectionPin);
                    motorPins.add(excavatorPowerPin);
              //      attachPinListeners(motorPins);

                } catch (KuraUnavailableDeviceException e) {
                    e.printStackTrace();
                } catch (KuraClosedDeviceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case 3:
                try {
                    frontDirectionWheelPin.setValue(true);
                    backDirectionWheelPin.setValue(false);
                    powerWheelPin.setValue(true);
                    // adding pins to list, in order to attach pin listeners to them
                    motorPins.add(frontDirectionWheelPin);
                    motorPins.add(backDirectionWheelPin);
                    motorPins.add(powerWheelPin);
                    //attachPinListeners(motorPins);
                } catch (KuraUnavailableDeviceException e) {
                    e.printStackTrace();
                } catch (KuraClosedDeviceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                logger.error("Wrong motor selected.");
            }
        }	else if("right".equals(direction)) {
	            try {
	                frontDirectionPlatformPin.setValue(true);
	                backDirectionPlatformPin.setValue(false);
	                powerPlatformPin.setValue(true);
	                // adding pins to list, in order to attach pin listeners to them
	                motorPins.add(frontDirectionPlatformPin);
	                motorPins.add(backDirectionPlatformPin);
	                motorPins.add(powerPlatformPin);
	         //       attachPinListeners(motorPins);
	
	            } catch (KuraUnavailableDeviceException e) {
	                e.printStackTrace();
	            } catch (KuraClosedDeviceException e) {
	                e.printStackTrace();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
        }
        else if ("back".equals(direction)) {
            // if ("front".equals(direction)) {
            switch (motorNumber) {
            case 1:
                try {
                    excavatorFrontDirectionPin.setValue(false);
                    excavatorBackDirectionPin.setValue(true);
                    excavatorPowerPin.setValue(true);
                    // adding pins to list, in order to attach pin listeners to them
                    motorPins.add(excavatorFrontDirectionPin);
                    motorPins.add(excavatorBackDirectionPin);
                    motorPins.add(excavatorPowerPin);
                //    attachPinListeners(motorPins);
                } catch (KuraUnavailableDeviceException e) {
                    e.printStackTrace();
                } catch (KuraClosedDeviceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    frontDirectionPlatformPin.setValue(false);
                    backDirectionPlatformPin.setValue(true);
                    powerPlatformPin.setValue(true);
                    // adding pins to list, in order to attach pin listeners to them
                    motorPins.add(frontDirectionPlatformPin);
                    motorPins.add(backDirectionPlatformPin);
                    motorPins.add(powerPlatformPin);
              //      attachPinListeners(motorPins);
                } catch (KuraUnavailableDeviceException e) {
                    e.printStackTrace();
                } catch (KuraClosedDeviceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 3:
                try {
                    frontDirectionWheelPin.setValue(true);
                    backDirectionWheelPin.setValue(false);
                    powerWheelPin.setValue(true);
                    // adding pins to list, in order to attach pin listeners to them
                    motorPins.add(frontDirectionWheelPin);
                    motorPins.add(backDirectionWheelPin);
                    motorPins.add(powerWheelPin);
              //      attachPinListeners(motorPins);
                } catch (KuraUnavailableDeviceException e) {
                    e.printStackTrace();
                } catch (KuraClosedDeviceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                logger.error("Wrong motor selected.");
            }
            // }
        } else if("left".equals(direction)) {
            try {
                frontDirectionPlatformPin.setValue(false);
                backDirectionPlatformPin.setValue(true);
                powerPlatformPin.setValue(true);
                // adding pins to list, in order to attach pin listeners to them
                motorPins.add(frontDirectionPlatformPin);
                motorPins.add(backDirectionPlatformPin);
                motorPins.add(powerPlatformPin);
          //      attachPinListeners(motorPins);
	            } catch (KuraUnavailableDeviceException e) {
	                e.printStackTrace();
	            } catch (KuraClosedDeviceException e) {
	                e.printStackTrace();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }

        }
        else {
            logger.error("Wrong direction command sent.");
        }
    }

    // Method for stopping specific motor
    @Override
    public void stopMotor(int motorNumber) {
        switch (motorNumber) {
        case 1:
            try {
                excavatorFrontDirectionPin.setValue(false);
                excavatorBackDirectionPin.setValue(false);
                excavatorPowerPin.setValue(false);
            } catch (KuraUnavailableDeviceException e) {
                e.printStackTrace();
            } catch (KuraClosedDeviceException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;
        case 2:
            try {
                frontDirectionPlatformPin.setValue(false);
                backDirectionPlatformPin.setValue(false);
                powerPlatformPin.setValue(false);
            } catch (KuraUnavailableDeviceException e) {
                e.printStackTrace();
            } catch (KuraClosedDeviceException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;
        case 3:
            try {
                frontDirectionWheelPin.setValue(false);
                backDirectionWheelPin.setValue(false);
                powerWheelPin.setValue(false);
            } catch (KuraUnavailableDeviceException e) {
                e.printStackTrace();
            } catch (KuraClosedDeviceException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;
        default:
            logger.error("Wrong motor selected.");
        }
    }



    private void logException(KuraGPIOPin pin, Exception e) {
        if (e instanceof KuraUnavailableDeviceException) {
            logger.warn("GPIO pin {} is not available for export.", pin);
        } else if (e instanceof KuraClosedDeviceException) {
            logger.warn("GPIO pin {} has been closed.", pin);
        } else {
            logger.error("I/O Error occurred!", e);
        }
    }

    private void stopTasks() {
        if (this.blinkTask != null) {
            this.blinkTask.cancel(true);
        }
        if (this.pollTask != null) {
            this.pollTask.cancel(true);
        }
    }

    public void releasePins() {
        Stream.concat(acquiredInputPins.stream(), acquiredOutputPins.stream()).forEach(pin -> {
            try {
                logger.warn("Closing GPIO pin {}", pin);
                try {
                    pin.setValue(false);
                } catch (KuraUnavailableDeviceException e) {
                    e.printStackTrace();
                } catch (KuraClosedDeviceException e) {
                    e.printStackTrace();
                }
                pin.close();
            } catch (IOException e) {
                logger.warn("Cannot close pin!");
            }
        });
        acquiredInputPins.clear();
        acquiredOutputPins.clear();
    }

    private KuraGPIODirection getPinDirection(int direction) {
        switch (direction) {
        case 0:
        case 2:
            return KuraGPIODirection.INPUT;
        case 1:
        case 3:
            return KuraGPIODirection.OUTPUT;
        default:
            return KuraGPIODirection.OUTPUT;
        }
    }

    private KuraGPIOMode getPinMode(int mode) {
        switch (mode) {
        case 2:
            return KuraGPIOMode.INPUT_PULL_DOWN;
        case 1:
            return KuraGPIOMode.INPUT_PULL_UP;
        case 8:
            return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
        case 4:
            return KuraGPIOMode.OUTPUT_PUSH_PULL;
        default:
            return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
        }
    }

    private KuraGPIOTrigger getPinTrigger(int trigger) {
        switch (trigger) {
        case 0:
            return KuraGPIOTrigger.NONE;
        case 2:
            return KuraGPIOTrigger.RAISING_EDGE;
        case 3:
            return KuraGPIOTrigger.BOTH_EDGES;
        case 1:
            return KuraGPIOTrigger.FALLING_EDGE;
        default:
            return KuraGPIOTrigger.NONE;
        }
    }

    private void initGPIOServiceTracking() {
        String selectedGPIOServicePid = this.gpioComponentOptions.getGpioServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                GPIOService.class.getName(), selectedGPIOServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.gpioServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.gpioServiceTrackerCustomizer);
        this.gpioServiceTracker.open();
    }
    
    private void openPins() {
    	for(KuraGPIOPin pin : pinsForOpening) {
    		if(!pin.isOpen()) {
    			try {
					pin.open();
				} catch (KuraGPIODeviceException e) {
					e.printStackTrace();
				} catch (KuraUnavailableDeviceException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    }
    
    // method for measuring distance with
    private void measureDistance(KuraGPIOPin echoPin, KuraGPIOPin triggerPin, String sensorSide) {
      //  stopTasks();
      //  releasePins();

       // this.value = false;
      //  acquirePins();
    	ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
    	
        this.blinkTask = singleThreadExecutor.scheduleWithFixedDelay(() -> {

            // if (outputPin.getIndex() == RIGHT_SENSOR_ECHO) {
           
          //  long distance = calculateDistance(echoPin, triggerPin, sensorSide);
            CompletableFuture.supplyAsync(() -> {
            	return calculateDistance(echoPin, triggerPin, sensorSide);
            }, this.executor).thenAccept(distance -> {
            	logger.info("DISTANCE FROM {} SENSOR = {} cm ", sensorSide, distance);
            	if(distance < 20) {
            		stopMotor(1);
            		stopMotor(2);
            		logger.info("OBSTACLE DETECTED - EXCAVATOR STOPPED.");
            	}
            });
            
//            logger.info("DISTANCE FROM {} SENSOR = {} cm :",sensorSide, distance);
//            if(distance < 20) {
//            	stopMotor(1);
//            	stopMotor(2);
//            	logger.info("OBSTACLE DETECTED - EXCAVATOR STOPPED.");
//            }
            
        }, 20, 1, TimeUnit.SECONDS);
        
    }
    @Override
    public long calculateDistance(KuraGPIOPin echoPin, KuraGPIOPin triggerPin, String sensorSide) {
    	long realDistance = 0;
        try {

            triggerPin.setValue(true);
            Thread.sleep(0, 100);
            triggerPin.setValue(false);
           // logger.info("Ispd set  value");

            startTime = System.nanoTime();
            stop = startTime;
            start = startTime;
       //     logger.info("Trigger pin value {} ", triggerPin.getValue());
         //   logger.info("ECHO pin value {} ", echoPin.getValue());

            while ((!echoPin.getValue()) && (start < startTime + 1000000000L * 2)) {
                start = System.nanoTime();
            }

            while ((echoPin.getValue()) && (stop < startTime + 1000000000L * 2)) {
                stop = System.nanoTime();
            }
            long delta = (stop - start);
            distance = delta * SPEED_OF_SOUND;
            realDistance = (long) (distance / 2.0 / (1000000000L));
            
          //  logger.info("DISTANCE FROM {} SENSOR = {} cm :",sensorSide, realDistance);
            Thread.sleep(2000);
            // }
        } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return realDistance;
    }
    
    @Override
	public KuraGPIOPin getFrontSensorEcho() {
		return frontSensorEcho;
	}


    @Override
	public KuraGPIOPin getFrontSensorTrigger() {
		return frontSensorTrigger;
	}

    @Override
	public KuraGPIOPin getBackSensorEcho() {
		return backSensorEcho;
	}


    @Override
	public KuraGPIOPin getBackSensorTrigger() {
		return backSensorTrigger;
	}


    @Override
	public KuraGPIOPin getLeftSensorEcho() {
		return leftSensorEcho;
	}

    @Override
	public KuraGPIOPin getLeftSensorTrigger() {
		return leftSensorTrigger;
	}


    @Override
	public KuraGPIOPin getRightSensorEcho() {
		return rightSensorEcho;
	}

    @Override
	public KuraGPIOPin getRightSensorTrigger() {
		return rightSensorTrigger;
	}

}
