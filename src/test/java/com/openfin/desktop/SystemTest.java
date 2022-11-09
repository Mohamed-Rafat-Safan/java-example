package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit tests for com.openfin.desktop.System class
 *
 * Since System is replaced by OpenFinRuntime, this class is testing backward compatibilities
 *
 * Created by wche on 1/26/16.
 *
 */
public class SystemTest {

    private static Logger logger = LoggerFactory.getLogger(SystemTest.class.getName());

    private static final String DESKTOP_UUID = SystemTest.class.getName();
    private static DesktopConnection desktopConnection;
    private static OpenFinRuntime runtime;

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
        if (desktopConnection != null) {
            runtime = new System(desktopConnection);
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

    @Test
    public void getMachineId() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getMachineId(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    String deviceId = ack.getData().toString();
                    logger.debug(String.format("Device ID %s", deviceId));
                    latch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("geDeviceId timeout", latch.getCount(), 0);
    }

    private String getRuntimeVersion() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> atomicReference = new AtomicReference<>();
        runtime.getVersion(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    String version = ack.getData().toString();
                    logger.debug(String.format("Runtime version %s", version));
                    atomicReference.set(version);
                    latch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getRuntimeVersion timeout", latch.getCount(), 0);
        return atomicReference.get();
    }

    @Test
    public void getProcessList() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getProcessList(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    // [{"cpuUsage":0,"workingSetSize":27725824,"processId":10292,"name":"Notifications Service","type":"application","uuid":"service:notifications"},{"cpuUsage":0,"workingSetSize":1486848,"processId":9116,"name":"Startup App","type":"application","uuid":"startup"}]
                    JSONArray data = (JSONArray) ack.getData();
                    int validCount = 0;
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        if (item.has("processId") && item.has("name")) {
                            validCount++;
                        }
                    }
                    if (validCount == data.length()) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getRuntimeVersion timeout", latch.getCount(), 0);
    }


    @Test
    public void getLogList() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getLogList(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONArray data = (JSONArray) ack.getData();
                    int validCount = 0;
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        if (item.has("date") && item.has("name") && item.has("size")) {
                            validCount++;
                        }
                    }
                    if (validCount == data.length()) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getLogList timeout", latch.getCount(), 0);
    }

    @Test
    public void writeAndReadLog() throws Exception {
        CountDownLatch writeLatch = new CountDownLatch(1);
        String text = UUID.randomUUID().toString();  // text to write
        runtime.log("info", text, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    writeLatch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        writeLatch.await(5, TimeUnit.SECONDS);
        assertEquals("writing log timeout", writeLatch.getCount(), 0);
        // text should be written to debug.log
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getLog("debug.log", new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    String data = (String) ack.getData();
                    if (data.contains(text) && data.contains("INFO")) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        latch.await(10, TimeUnit.SECONDS);  // longer wait time since debug.log can be big
        assertEquals("getLog timeout", latch.getCount(), 0);
    }

    @Test
    public void getMonitorInfo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getMonitorInfo(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONObject data = (JSONObject) ack.getData();
                    logger.debug(String.format("Monitor info %s", data.toString()));
                    if (data.has("deviceScaleFactor") && data.has("primaryMonitor")) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getMonitorInfo timeout", latch.getCount(), 0);
    }

    @Test
    public void getAllWindows() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getAllWindows(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONArray data = (JSONArray) ack.getData();
                    logger.debug(String.format("All windows info %s", data.toString()));
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject window = data.getJSONObject(i);
                        if (window.has("uuid") && window.has("mainWindow") && window.getString("uuid").equals(options.getUUID())) {
                            latch.countDown();
                        }
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getAllWindows timeout", latch.getCount(), 0);
    }

    @Test
    public void getAllApplications() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getAllApplications(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONArray data = (JSONArray) ack.getData();
                    logger.debug(String.format("All applications info %s", data.toString()));
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject window = data.getJSONObject(i);
                        if (window.has("uuid") && window.has("isRunning") && window.getString("uuid").equals(options.getUUID())) {
                            latch.countDown();
                        }
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getAllApplications timeout", latch.getCount(), 0);
    }

    @Test
    public void getMousePosition() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getMousePosition(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONObject data = (JSONObject) ack.getData();
                    logger.debug(String.format("getMousePosition info %s", data.toString()));
                    JSONObject position = (JSONObject) ack.getData();
                    if (position.has("left") && position.has("top")) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getMousePosition timeout", latch.getCount(), 0);
    }

    @Test
    public void getConfig() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getConfig(null, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONObject config = (JSONObject) ack.getData();
                    logger.debug(String.format("getConfig info %s", config.toString()));
                    if (config.has("runtime")) {
                        JSONObject runtime = config.getJSONObject("runtime");
                        logger.debug(String.format("Runtime version %s", runtime.getString("version")));
                        if (runtime.getString("version").equals(TestUtils.getRuntimeVersion())) {
                            latch.countDown();
                        }
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getConfig timeout", latch.getCount(), 0);
    }

    @Test
    public void showDeveloperTools() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        // show dev tool for main window of the app.  Name of the main window is same as UUID
        runtime.showDeveloperTools(options.getUUID(), options.getUUID(), new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("showDeveloperTools timeout", latch.getCount(), 0);
    }

    @Test
    public void addAndRemoveEventListeners() throws Exception {
        String[] events = {"desktop-icon-clicked", "idle-state-changed", "monitor-info-changed", "session-changed"};
        EventListener listener = (actionEvent -> {});
        for (String eventType : events) {
            CountDownLatch latch = new CountDownLatch(1);
            runtime.addEventListener(eventType, listener, new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    if (ack.isSuccessful()) {
                        latch.countDown();
                    }
                }

                @Override
                public void onError(Ack ack) {
                    logger.error(String.format("onError %s", ack.getReason()));
                }
            });
            latch.await(3, TimeUnit.SECONDS);
            assertEquals(String.format("removeEventListener %s timeout", eventType), latch.getCount(), 0);
        }
        for (String eventType : events) {
            CountDownLatch latch = new CountDownLatch(1);
            runtime.removeEventListener(eventType, listener, new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    if (ack.isSuccessful()) {
                        latch.countDown();
                    }
                }

                @Override
                public void onError(Ack ack) {
                    logger.error(String.format("onError %s", ack.getReason()));
                }
            });
            latch.await(3, TimeUnit.SECONDS);
            assertEquals(String.format("removeEventListener %s timeout", eventType), latch.getCount(), 0);
        }
    }

    @Test
    @Ignore
    public void startAndTerminateExternalProcess() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<String> processUuid = new AtomicReference<>();
        runtime.launchExternalProcess("notepad.exe", "", result -> {
            processUuid.set(result.getProcessUuid());
            logger.debug(String.format("launch process %s", processUuid.get()));
            startLatch.countDown();
        }, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }

            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        startLatch.await(10, TimeUnit.SECONDS);
        assertEquals("launchExternalProcess timeout", startLatch.getCount(), 0);
        CountDownLatch terminateLatch = new CountDownLatch(1);
        runtime.terminateExternalProcess(processUuid.get(), 2000, false, result -> {
            logger.debug(String.format("terminate process %s %s", processUuid.get(), result.getProcessUuid()));
            if (result.getProcessUuid().equals(processUuid.get())) {
                logger.debug(String.format("External process exit code %s", result.getResult()));
                terminateLatch.countDown();
                logger.debug(String.format("countdown terminateLatch %d", terminateLatch.getCount()));
            }
        }, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }

            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        logger.debug("waiting on terminateLatch");
        terminateLatch.await(10, TimeUnit.SECONDS);
        assertEquals("terminateExternalProcess timeout", terminateLatch.getCount(), 0);
    }

    @Test
    public void getEnvironmentVariables() throws Exception {
        String[] envVarNames = {"LOCALAPPDATA", "USERNAME"};
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getEnvironmentVariables(envVarNames, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONObject data = (JSONObject) ack.getData();
                    for (String name : envVarNames) {
                        if (!data.has(name)) {
                            fail(String.format("Missing env variable %s", name));
                        }
                    }
                    latch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getEnvironmentVariables timeout", latch.getCount(), 0);
    }

    @Test
    public void deleteCacheOnRestart() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.deleteCacheOnRestart(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("deleteCacheOnRestart timeout", latch.getCount(), 0);
        // @TODO need to create some windows and move them around, the restart OpenFin Runtime to verify windows are open at default bounds
    }

    @Test
    public void clearCache() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.clearCache(true, true, true, true, true, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("clearCache timeout", latch.getCount(), 0);
        // @TODO need to create an OpenFin app that verify these caches are actually being cleared
    }

    @Test
    public void restartRuntime() throws Exception {
        String version1 = getRuntimeVersion();
        TestUtils.teardownDesktopConnection(desktopConnection);
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
        runtime = new OpenFinRuntime(desktopConnection);
        String version2 = getRuntimeVersion();
        assertEquals(version1, version2);
    }

    @Test
    public void getRuntimeInfo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getRuntimeInfo(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    // { "manifestUrl":"http://localhost:5090/getManifest","port":9696,"version":"8.56.30.37","architecture":"x64"}
                    JSONObject data = (JSONObject) ack.getData();
                    if (data.has("manifestUrl") && data.has("port") && data.has("version")) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getRuntimeInfo timeout", latch.getCount(), 0);
    }

    @Test
    public void getRvmInfo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getRvmInfo(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    // { "action":"get-rvm-info","path":"C:\\Users\\abc\\AppData\\Local\\OpenFin\\OpenFinRVM.exe","start-time":"2018-04-12 14:01:41","version":"4.0.1.1","working-dir":"C:\\Users\\abc\\AppData\\Local\\OpenFin"}
                    JSONObject data = (JSONObject) ack.getData();
                    if (data.has("action") && data.has("path") && data.has("start-time")) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void onError(Ack ack) {
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getRvmInfo timeout", latch.getCount(), 0);
    }


}
