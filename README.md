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
                                                  .withMockHost("localhost")
                                                  .withMockPort(8097)
                                                  .withMockPath("/weather-change");

  @Test
  public void testMockRunner() throws Exception {
    assertTrue(runner.isRunning());
    System.out.println("do some testing against endpoint: " + runner.getMockEndpoint());
  }

}
```

### Configure SoapUI Version

Because the soapui starts in a classloader jail, one can also a) compile this libary against a different version:

```sh
mvn clean install -Dsoapui.version=5.1.2
```

or if the compiled version of `soapui-junit-mockrunner` is largely compatible with what you are trying to run, simply specify the version on the `SoapUIMockRunner`:

```java
new SoapUIMockRunner()
        .simpleBinding()
        .withProjectPath("embedded-soapui/TestSoapUIProject-soapui-project.xml")
        .withMockServiceName("WeatherMockService")
        .soapuiVersion("5.1.2");
```

The option is risky when you specify a runner version that is incompatible with the soapui classes that are loaded. 

I sucessfully ran a `soapui-junit-mockrunner` compiled against SoapUI 5.0.0 using a 5.1.2 runtime version (as above example).   


### TODO

Instead of the dodgy simple binding, should really use `com.eviware.soapui.tools.SoapUIMockServiceRunner.main()` with a set of standard SoapUI path parameters. 

### Limitiations

1. the reflection binding does not work with path, port and host settings (use simple binding)
2. searching for the local maven repo for cached artefact bindings is rather crude
3. need to improve exception handing, especially with the rule
4. not enough tests and examples
