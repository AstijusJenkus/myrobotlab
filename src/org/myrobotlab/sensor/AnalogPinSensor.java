package org.myrobotlab.sensor;

import org.myrobotlab.service.data.SensorEvent;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.SensorEventListener;
import org.myrobotlab.service.interfaces.SensorDataPublisher;

public class AnalogPinSensor implements SensorDataPublisher {

  private final int pin;
  private final int sampleRate;
  
  public AnalogPinSensor(int pin, int sampleRate) {
    super();
    this.pin = pin;
    // TODO: fix the concept of sample rate! should be Hertz.. not number of skiped loops.
    this.sampleRate = sampleRate;
  }
  
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "A" + pin;
  }


  @Override
  public SensorEvent publishSensorData(SensorEvent data) {
    return data;
  }


  public int getPin() {
    return pin;
  }

  public int getSampleRate() {
    return sampleRate;
  }



}
