package ch.hearc.android.virtuslib;

/**
 * Created by drksnw on 10/19/16.
 */
public abstract class VirtusphereEvent {
    public abstract void moved(int x, int y);
    public abstract void disconnected();
}
