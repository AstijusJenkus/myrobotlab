package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

/**
 * This test will iterate all possible services (except for the blacklisted
 * ones) it will create an instance of that service and pass the service to the
 * json serializer to ensure it doesn't blow up.
 * 
 * @author kwatters
 *
 */

public class ServiceSmokeTest extends AbstractTest {

  transient public final static Logger log = LoggerFactory.getLogger(ServiceSmokeTest.class);

  @Test
  public void testAllServiceSerialization() {
    try {
    installAll();

    // known problematic services?! TODO: fix them and remove from the following
    // list.
    ArrayList<String> blacklist = new ArrayList<String>();
    blacklist.add("org.myrobotlab.service.OpenNi");
    blacklist.add("org.myrobotlab.service.LeepMotion");
    blacklist.add("org.myrobotlab.service.Runtime");
  
    // the service data!
    ServiceData serviceData = ServiceData.getLocalInstance();

    // we need to load a service for each service type we have.
    String[] x = serviceData.getServiceTypeNames();

    for (String serviceType : x) {
      log.info("Service Type: {}", serviceType);
    }
    log.info("Press any key to continue");
    // System.in.read();
    for (String serviceType : x) {

      long start = System.currentTimeMillis();

      if (blacklist.contains(serviceType)) {
        log.warn("Skipping known problematic service {}", serviceType);
        continue;
      }
      log.warn("Testing service type {}", serviceType);
      String serviceName = serviceType.toLowerCase();
      ServiceInterface s = Runtime.create(serviceName, serviceType);            
      assertNotNull(String.format("could not create %s",  serviceName), s);
      s.setVirtual(true);
      s = Runtime.start(serviceName, serviceType);
      assertNotNull(String.format("could not start %s",  serviceName), s);
      // log.error("serviceType {}", s.getName());
      testSerialization(s);
      // TODO: validate the service is released!
      s.releaseService();

      long delta = System.currentTimeMillis() - start;
      log.info("Done testing serialization of {} in {} ms", serviceType, delta);

    }

    Runtime.releaseAll();

    log.info("Done with tests..");

    } catch(Exception e) {
      log.error("ServiceSmokeTest threw", e);
      fail("ServiceSmokeTest threw");
    }
  }

  public void testSerialization(ServiceInterface s) {

    // TODO: perhaps some extra service type specific initialization?!
    String res = CodecUtils.toJson(s);
    assertNotNull(res);
    log.info("Serialization successful for {}", s.getType());

    // ServiceInterface s = CodecUtils.fromJson(res, clazz)
    // assertNotNull(res);
  }

}