package app.expressiveclock.installer;

/** Presentation only. No UI state can authorise an installation or bypass Work/Policy. */
public final class Presentation {
    public enum Phase { READY, CONNECTING, DOWNLOADING, VERIFYING, READY_TO_INSTALL, INSTALLING, COMPLETED, FAILED }
    public enum Action { NONE, CONNECT, AUTHORIZE, RETRY, INSTALL }
    public static Action primary(Phase phase,boolean connected,boolean busy,boolean ready,boolean request,boolean code) {
        if(phase==Phase.CONNECTING && code)return Action.AUTHORIZE;
        if(busy)return Action.NONE;
        if((phase==Phase.READY_TO_INSTALL || phase==Phase.FAILED) && ready)return Action.INSTALL;
        if(!connected)return Action.CONNECT;
        if((phase==Phase.FAILED || phase==Phase.READY) && request)return Action.RETRY;
        return Action.NONE;
    }
    public static boolean twoColumns(float widthDp,float fontScale){return widthDp>=760 && fontScale<=1.3f;}
    public static int percent(int actual){return Math.max(-1,Math.min(100,actual));}
}
