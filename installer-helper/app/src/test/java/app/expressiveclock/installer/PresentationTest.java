package app.expressiveclock.installer;
import org.junit.Test;
import static org.junit.Assert.*;
import static app.expressiveclock.installer.Presentation.*;

public class PresentationTest {
    @Test public void connectedIdleHasNoDisabledPrimary(){assertEquals(Action.NONE,primary(Phase.READY,true,false,false,false,false));}
    @Test public void disconnectedOffersConnection(){assertEquals(Action.CONNECT,primary(Phase.READY,false,false,false,false,false));}
    @Test public void authorizationRemainsAvailableWhilePolling(){assertEquals(Action.AUTHORIZE,primary(Phase.CONNECTING,false,true,false,false,true));}
    @Test public void connectingBeforeCodeHasNoFakeAction(){assertEquals(Action.NONE,primary(Phase.CONNECTING,false,true,false,false,false));}
    @Test public void busyCannotInstall(){assertEquals(Action.NONE,primary(Phase.READY_TO_INSTALL,true,true,true,true,false));}
    @Test public void readinessRequiresControllerApproval(){assertEquals(Action.NONE,primary(Phase.READY_TO_INSTALL,true,false,false,true,false));}
    @Test public void verifiedBuildCanInstall(){assertEquals(Action.INSTALL,primary(Phase.READY_TO_INSTALL,true,false,true,true,false));}
    @Test public void cancelledInstallerCanRetryWithoutRedownload(){assertEquals(Action.INSTALL,primary(Phase.FAILED,true,false,true,true,false));}
    @Test public void failedDownloadOffersRetry(){assertEquals(Action.RETRY,primary(Phase.FAILED,true,false,false,true,false));}
    @Test public void cancelledDownloadCanRetry(){assertEquals(Action.RETRY,primary(Phase.READY,true,false,false,true,false));}
    @Test public void revokedConnectionDoesNotOfferDownload(){assertEquals(Action.CONNECT,primary(Phase.FAILED,false,false,false,true,false));}
    @Test public void completedDoesNotOfferAnotherInstall(){assertEquals(Action.NONE,primary(Phase.COMPLETED,true,false,false,true,false));}
    @Test public void foldedStaysSingleColumn(){assertFalse(twoColumns(360,1));}
    @Test public void unfoldedUsesTwoColumns(){assertTrue(twoColumns(1000,1));}
    @Test public void foldInnerDisplayUsesTwoColumns(){assertTrue(twoColumns(790,1));}
    @Test public void largeFontsPreferReadableColumn(){assertFalse(twoColumns(1000,2));}
    @Test public void narrowLandscapeKeepsReadableColumn(){assertFalse(twoColumns(700,1));}
    @Test public void progressHasNoInventedIntermediateValues(){assertEquals(-1,percent(-1));assertEquals(37,percent(37));assertEquals(100,percent(130));}
}
