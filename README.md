# SoapUI JUnit MockRunner

### Overview

Ever tried to start a soapui mock service from within your junit test? Sure the soapui-maven-plugin make a good job when you just run from command line ... but if you develop in an IDE that is not always the case. 

This module can be used to execute a soapui mock service within a classloader jail. This will ensure the project under test is not dependent on SoapUI itself. The classloader jail will resolve all required dependencies from the official soapui maven repository.

### Example

The most simple example on how to fire up a Junit test with a SoapUI mock service running in the background. 

```java
import static org.junit.Assert.*;
import org.junit.ClassRule;
import org.junit.Test;

public class SomeMockServiceTest {

  @ClassRule
  public static SoapUIMockRunner runner = new SoapUIMockRunner()
                                                  .simpleBinding()
                                                  .withProjectPath("embedded-soapui/TestSoapUIProject-soapui-project.xml")
                                                  .withMockServiceName("WeatherMockService")
                                                  .withMockHost("localhost").withMockPort(8097)
                                                  .withMockPath("/weather-change");

  @Test
  public void testMockRunner() throws Exception {
    assertTrue(runner.isRunning());
    System.out.println("do some testing against endpoint: " + runner.getMockEndpoint());
  }

}
```


### Limitiations

1) the reflection binding does not work with path, port and host settings (use simple binding)
2) searching for the local maven repo for cached artefact bindings is rather crude
3) need to improve exception handing, especially with the rule
3) not enough tests and examples