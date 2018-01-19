package local.development.restfulharmony;

import static java.lang.String.format;
import static spark.Spark.*;

import javax.inject.Inject;

import local.development.restfulharmony.api.DevModeResponse;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import local.development.restfulharmony.api.HarmonyRest;
import com.google.inject.Guice;
import com.google.inject.Injector;

import net.whistlingfish.harmony.ActivityChangeListener;
import net.whistlingfish.harmony.HarmonyClient;
import net.whistlingfish.harmony.HarmonyClientModule;
import net.whistlingfish.harmony.config.Activity;
import spark.Spark;
import spark.servlet.SparkApplication;

import java.util.List;

public class HarmonyRestServer implements SparkApplication {
    @Inject
    private HarmonyClient harmonyClient;
    
    private HarmonyRest harmonyApi;
    
    private DevModeResponse devResponse;
    
    private static Boolean devMode;
    private static Boolean noopCalls;
    private static Logger log;

    private String harmonyHubIp = "192.168.1.170";

    @Override
    public void init() {

        log = LoggerFactory.getLogger(HarmonyRestServer.class);
        Version theVersion;

        theVersion = new Version();
        devMode = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));
        noopCalls = Boolean.parseBoolean(System.getProperty("noop.calls", "false"));
        String modeString = "";
        if(devMode)
            modeString = " (development mode)";
        if(noopCalls)
            modeString = " (no op calls to harmony)";
        log.info("Harmony v" + theVersion.getVersion() + " rest server running" + modeString + "....");
        Injector injector = null;
        if(!devMode)
            injector = Guice.createInjector(new HarmonyClientModule());
        HarmonyRestServer mainObject = new HarmonyRestServer();
        if(!devMode)
            injector.injectMembers(mainObject);
        mainObject.execute(harmonyHubIp);
    }

    private void execute(String harmonyHubIp)  {
        if(devMode)
        {
        	harmonyClient = null;
        	devResponse = new DevModeResponse();
        }
        else {
        	devResponse = null;
	        harmonyClient.addListener(new ActivityChangeListener() {
	            @Override
	            public void activityStarted(Activity activity) {
	                log.info(format("activity changed: [%d] %s", activity.getId(), activity.getLabel()));
	            }
	        });
	        harmonyClient.connect(harmonyHubIp);
        }
        //port(Integer.valueOf(System.getProperty("server.port", "8081")));
        int sleepTime = Integer.parseInt(System.getProperty("button.sleep", "500"));
        harmonyApi = new HarmonyRest(harmonyClient, noopCalls, sleepTime, devResponse);
        harmonyApi.setupServer();
    }
}
