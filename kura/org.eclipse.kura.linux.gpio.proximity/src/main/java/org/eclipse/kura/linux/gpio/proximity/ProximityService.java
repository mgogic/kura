/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.linux.gpio.proximity;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
//proximity pins info :
//echo pin - only input
//trigger pin - only output
//echo pin mode - Input with pull down
//echo pin trigger - both edges
//trigger pin mode - push pull output
//trigger pin trigger both edges
import org.eclipse.kura.message.KuraPayload;
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

public class ProximityService implements ConfigurableComponent, CloudClientListener {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class GPIOServiceTrackerCustomizer implements ServiceTrackerCustomizer<GPIOService, GPIOService> {

        @Override
        public GPIOService addingService(final ServiceReference<GPIOService> reference) {
            ProximityService.this.gpioService = ProximityService.this.bundleContext.getService(reference);
            return ProximityService.this.gpioService;
        }

        @Override
        public void modifiedService(final ServiceReference<GPIOService> reference, final GPIOService service) {
            ProximityService.this.gpioService = ProximityService.this.bundleContext.getService(reference);
        }

        @Override
        public void removedService(final ServiceReference<GPIOService> reference, final GPIOService service) {
            ProximityService.this.gpioService = null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ProximityService.class);

    // Cloud Application identifier
    private static final String APP_ID = "DISTANCE";
    // cloud service and client
    private CloudService cloudService;
    private CloudClient cloudClient;

    private String cloudTopic = "proximity";
    private String metricName;
    // gpio directions
    private static final int INPUT_PIN_DIRECTION = 0;
    private static final int OUTPUT_PIN_DIRECTION = 1;
    private static final int BOTH_INIT_INPUT_MODE = 2;
    private static final int BOTH_INIT_OUTPUT_MODE = 3;
    // gpio modes
    private static final int DEFAULT_MODE = -1;
    private static final int INPUT_WITH_PULL_UP_MODE = 1;
    private static final int INPUT_WITH_PULL_DOWN_MODE = 2;
    private static final int PUSH_PULL_OUTPUT_MODE = 4;
    private static final int OPEN_DRAIN_OUTPUT_MODE = 8;
    // gpio triggers
    private static final int DEFAULT_TRIGGER = -1;
    private static final int BOTH_EDGES_TRIGGER = 3;
    private static final int FALLING_EDGES_TRIGGER = 1;
    private static final int RISING_EDGES_TRIGGER = 2;
    private static final int NO_TRIGGER = 0;
    // Proximity sensors pins
    private static final int FRONT_SENSOR_ECHO = 19;
    private static final int FRONT_SENSOR_TRIGGER = 13;

    private static final int RIGHT_SENSOR_ECHO = 24;
    private static final int RIGHT_SENSOR_TRIGGER = 23;

    private static final int LEFT_SENSOR_ECHO = 6;
    private static final int LEFT_SENSOR_TRIGGER = 5;

    private static final int BACK_SENSOR_ECHO = 27;
    private static final int BACK_SENSOR_TRIGGER = 17;
    // kura pins for motors
    private KuraGPIOPin frontSensorEcho = getPin(Integer.toString(FRONT_SENSOR_ECHO),
            getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));
    private KuraGPIOPin frontSensorTrigger = getPin(Integer.toString(FRONT_SENSOR_TRIGGER),
            getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));

    private KuraGPIOPin backSensorEcho = getPin(Integer.toString(BACK_SENSOR_ECHO),
            getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));

    private KuraGPIOPin backSensorTrigger = getPin(Integer.toString(BACK_SENSOR_TRIGGER),
            getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));

    private KuraGPIOPin leftSensorEcho = getPin(Integer.toString(LEFT_SENSOR_ECHO),
            getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));

    private KuraGPIOPin leftSensorTrigger = getPin(Integer.toString(LEFT_SENSOR_TRIGGER),
            getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));

    private KuraGPIOPin rightSensorEcho = getPin(Integer.toString(RIGHT_SENSOR_ECHO),
            getPinDirection(INPUT_PIN_DIRECTION), getPinMode(INPUT_WITH_PULL_DOWN_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));

    private KuraGPIOPin rightSensorTrigger = getPin(Integer.toString(RIGHT_SENSOR_TRIGGER),
            getPinDirection(OUTPUT_PIN_DIRECTION), getPinMode(PUSH_PULL_OUTPUT_MODE),
            getPinTrigger(BOTH_EDGES_TRIGGER));

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
    private long startTime = 0;
    private long stop = 0;
    private long start = 0;
    private double distance = 0.0;

    private final int SPEED_OF_SOUND = 34029;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating {}", APP_ID);

        this.bundleContext = componentContext.getBundleContext();

        this.gpioComponentOptions = new GpioComponentOptions(properties);

        this.gpioServiceTrackerCustomizer = new GPIOServiceTrackerCustomizer();
        initGPIOServiceTracking();

        try {
            cloudClient = this.cloudService.newCloudClient(APP_ID);
            this.setCloudService(cloudService);
            cloudClient.addCloudClientListener(this);
        } catch (KuraException e) {
            e.printStackTrace();
        }

        doUpdate(properties);

        logger.info("Activating {}... Done.", APP_ID);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating {}", APP_ID);

        stopTasks();
        releasePins();

        if (nonNull(this.gpioServiceTracker)) {
            this.gpioServiceTracker.close();
        }

        this.executor.shutdownNow();
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
        acquirePins();
        // List<KuraGPIOPin> outputAndInputPins = new ArrayList<KuraGPIOPin>(acquiredOutputPins);
        // outputAndInputPins.addAll(acquiredInputPins);
        // logger.info("ALL PINS : {}", outputAndInputPins.toString());
        // if (!outputAndInputPins.isEmpty()) {
        // this.blinkTask = this.executor.scheduleWithFixedDelay(() -> {
        // for (KuraGPIOPin pin : outputAndInputPins) {
        // try {
        // // if (outputPin.getIndex() == RIGHT_SENSOR_ECHO) {
        // logger.info("TRIGGER PIN {} ", outputAndInputPins.get(0).getIndex());
        // KuraGPIOPin triggerPin = outputAndInputPins.get(0);
        // logger.info("TRIGGER PIN INDEX : {} ", triggerPin.getIndex());
        // KuraGPIOPin echoPin = outputAndInputPins.get(1);
        // logger.info("TRIGGER PIN DIRECTION {} ", triggerPin.getDirection());
        // logger.info("TRIGGER PIN DIRECTION {} ", triggerPin.getMode());
        // logger.info("TRIGGER PIN DIRECTION {} ", triggerPin.getTrigger());
        //
        // logger.info("ECHO PIN DIRECTION {} ", echoPin.getDirection());
        // logger.info("ECHO PIN DIRECTION {} ", echoPin.getMode());
        // logger.info("ECHO PIN DIRECTION {} ", echoPin.getTrigger());
        // // logger.info("Setting GPIO pin {} to {}", outputPin, this.value);
        // logger.info("Iznad set value");
        // triggerPin.setValue(true);
        // Thread.sleep(0, 10000);
        // triggerPin.setValue(false);
        // logger.info("Ispd set value");
        //
        // startTime = System.nanoTime();
        // start = startTime;
        // stop = startTime;
        // logger.info("Trigger pin value {} ", triggerPin.getValue());
        // logger.info("ECHO pin value {} ", echoPin.getValue());
        //
        // while ((!echoPin.getValue()) && (start < startTime + 1000000000L * 2)) {
        // start = System.nanoTime();
        // }
        //
        // while ((echoPin.getValue()) && (stop < startTime + 1000000000L * 2)) {
        // stop = System.nanoTime();
        // }
        // long delta = stop - start;
        // // distance = delta * SPEED_OF_SOUND;
        // // long realDistance = (long) (distance / 2.0 / (1000000000L));
        // distance = Math.ceil(delta / 1000000000.0 * 17150);
        // logger.info("DISTANCE FROM FRONT SENSOR = {} cm :", distance);
        // // }
        // } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException
        // | InterruptedException e) {
        // logException(pin, e);
        // }
        // }
        // this.value = !this.value;
        // }, 0, 2000, TimeUnit.MILLISECONDS);
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

    // method for measuring distance with
    public void measureDistance(int echoPinNumber, int triggerPinNumber) {
        stopTasks();
        releasePins();

        this.value = false;
        acquirePins();
        this.blinkTask = this.executor.scheduleWithFixedDelay(() -> {

            // if (outputPin.getIndex() == RIGHT_SENSOR_ECHO) {
            KuraGPIOPin echoPin = getPin(Integer.toString(echoPinNumber), getPinDirection(INPUT_PIN_DIRECTION),
                    getPinMode(INPUT_WITH_PULL_DOWN_MODE), getPinTrigger(BOTH_EDGES_TRIGGER));
            KuraGPIOPin triggerPin = getPin(Integer.toString(triggerPinNumber), getPinDirection(OUTPUT_PIN_DIRECTION),
                    getPinMode(PUSH_PULL_OUTPUT_MODE), getPinTrigger(BOTH_EDGES_TRIGGER));

            calculateDistance(echoPin, triggerPin);

        }, 0, 2000, TimeUnit.MILLISECONDS);

    }

    private void calculateDistance(KuraGPIOPin echoPin, KuraGPIOPin triggerPin) {
        try {
            // if (outputPin.getIndex() == RIGHT_SENSOR_ECHO) {
            // logger.info("TRIGGER PIN {} ", outputAndInputPins.get(0).getIndex());
            // logger.info("TRIGGER PIN INDEX : {} ", triggerPin.getIndex());
            // logger.info("TRIGGER PIN DIRECTION {} ", triggerPin.getDirection());
            // logger.info("TRIGGER PIN DIRECTION {} ", triggerPin.getMode());
            // logger.info("TRIGGER PIN DIRECTION {} ", triggerPin.getTrigger());

            triggerPin.setValue(true);
            Thread.sleep(0, 10000);
            triggerPin.setValue(false);
            logger.info("Ispd set  value");

            startTime = System.nanoTime();
            start = startTime;
            stop = startTime;
            logger.info("Trigger pin value {} ", triggerPin.getValue());
            logger.info("ECHO pin value {} ", echoPin.getValue());

            while ((!echoPin.getValue()) && (start < startTime + 1000000000L * 2)) {
                start = System.nanoTime();
            }

            while ((echoPin.getValue()) && (stop < startTime + 1000000000L * 2)) {
                stop = System.nanoTime();
            }
            long delta = stop - start;
            // distance = delta * SPEED_OF_SOUND;
            // long realDistance = (long) (distance / 2.0 / (1000000000L));
            distance = Math.ceil(delta / 1000000000.0 * 17150);
            logger.info("DISTANCE FROM  SENSOR = {} cm :", distance);
            // }
        } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException | InterruptedException e) {
            e.printStackTrace();
        }

        this.value = !this.value;
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
            getPins();
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
        try {
            int terminal = Integer.parseInt(resource);
            if (terminal > 0 && terminal < 1255) {
                pin = this.gpioService.getPinByTerminal(Integer.parseInt(resource), pinDirection, pinMode, pinTrigger);
            }
        } catch (NumberFormatException e) {
            pin = this.gpioService.getPinByName(resource, pinDirection, pinMode, pinTrigger);
        }
        return pin;
    }

    // private static final int FRONT_SENSOR_ECHO = 19;
    // private static final int FRONT_SENSOR_TRIGGER = 13;
    //
    // private static final int RIGHT_SENSOR_ECHO = 24;
    // private static final int RIGHT_SENSOR_TRIGGER = 23;
    //
    // private static final int LEFT_SENSOR_ECHO = 6;
    // private static final int LEFT_SENSOR_TRIGGER = 5;
    //
    // private static final int BACK_SENSOR_ECHO = 27;
    // private static final int BACK_SENSOR_TRIGGER = 17;

    private void submitBlinkTask(long delayMs, final List<KuraGPIOPin> outputPins) {

        // logger.info("SUBIMT BLINK TASK");
        // this.blinkTask = this.executor.scheduleWithFixedDelay(() -> {
        // for (KuraGPIOPin outputPin : outputPins) {
        // try {
        // logger.info("PIN INDEX 1 {} ", outputPin.getIndex());
        // if (outputPin.getIndex() == FRONT_SENSOR_TRIGGER) {
        // logger.info("PIN INDEX 2 {} ", outputPin.getIndex());
        //
        // KuraGPIOPin triggerPin = outputPins.get(RIGHT_SENSOR_TRIGGER);
        // KuraGPIOPin echoPin = outputPins.get(RIGHT_SENSOR_ECHO);
        // logger.info("PIN INDEX 3 {} ", outputPin.getIndex());
        //
        // // logger.info("Setting GPIO pin {} to {}", outputPin, this.value);
        // logger.info("Iznad set value");
        // triggerPin.setValue(true);
        // Thread.sleep(0, 10000);
        // triggerPin.setValue(false);
        // logger.info("Ispd set value");
        //
        // startTime = System.nanoTime();
        // start = startTime;
        // stop = startTime;
        // logger.info("Trigger pin value {} ", triggerPin.getValue());
        // logger.info("ECHO pin value {} ", echoPin.getValue());
        //
        // while (!echoPin.getValue() && (start < startTime + 1000000000L * 2)) {
        // start = System.nanoTime();
        // logger.info("U PRVOM WHILE");
        // }
        //
        // while (echoPin.getValue() && (stop < startTime + 1000000000L * 2)) {
        // stop = System.nanoTime();
        // logger.info("U drugom whileu");
        // }
        // long delta = stop - start;
        // distance = delta * SPEED_OF_SOUND;
        // long realDistance = (long) (distance / 2.0 / (1000000000L));
        // logger.info("DISTANCE FROM FRONT SENSOR = {} cm :", realDistance);
        // }
        // } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException
        // | InterruptedException e) {
        // logException(outputPin, e);
        // }
        // }
        // this.value = !this.value;
        // }, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    private void submitPollTask(long delayMs, final List<KuraGPIOPin> inputPins) {
        this.pollTask = this.executor.scheduleWithFixedDelay(() -> {
            for (KuraGPIOPin inputPin : inputPins) {
                try {
                    logger.info("input pin {} value {}", inputPin, inputPin.getValue());
                } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException e) {
                    logException(inputPin, e);
                }
            }
        }, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    private void attachPinListeners(final List<KuraGPIOPin> inputPins) {
        for (final KuraGPIOPin pin : inputPins) {
            logger.info("Attaching Pin Listener to GPIO pin {}", pin);
            try {
                pin.addPinStatusListener(value -> logger.info(""));
            } catch (Exception e) {
                logException(pin, e);
            }
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

    private void releasePins() {
        Stream.concat(acquiredInputPins.stream(), acquiredOutputPins.stream()).forEach(pin -> {
            try {
                logger.warn("Closing GPIO pin {}", pin);
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

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionLost() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionEstablished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        // TODO Auto-generated method stub

    }
}
