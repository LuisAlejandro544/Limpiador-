package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IPackageManager extends IInterface {
    abstract class Stub extends Binder implements IPackageManager {
        public static IPackageManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IPackageManager) {
                return (IPackageManager) iin;
            }
            return new Proxy(obj);
        }

        private static final String DESCRIPTOR = "android.content.pm.IPackageManager";

        private static class Proxy implements IPackageManager {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public void grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException {
                // Invocamos via reflexión en el Stub o via Shizuku binder transact
                try {
                    Class<?> stubClass = Class.forName("android.content.pm.IPackageManager$Stub");
                    java.lang.reflect.Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
                    Object realInterface = asInterfaceMethod.invoke(null, mRemote);
                    if (realInterface != null) {
                        java.lang.reflect.Method grantMethod = realInterface.getClass().getMethod(
                            "grantRuntimePermission",
                            String.class,
                            String.class,
                            int.class
                        );
                        grantMethod.invoke(realInterface, packageName, permissionName, userId);
                        return;
                    }
                } catch (Exception e) {
                    throw new RemoteException("Error delegating to IPackageManager: " + e.getMessage());
                }
            }

            @Override
            public void revokeRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException {
                try {
                    Class<?> stubClass = Class.forName("android.content.pm.IPackageManager$Stub");
                    java.lang.reflect.Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
                    Object realInterface = asInterfaceMethod.invoke(null, mRemote);
                    if (realInterface != null) {
                        java.lang.reflect.Method revokeMethod = realInterface.getClass().getMethod(
                            "revokeRuntimePermission",
                            String.class,
                            String.class,
                            int.class
                        );
                        revokeMethod.invoke(realInterface, packageName, permissionName, userId);
                    }
                } catch (Exception e) {
                    throw new RemoteException("Error delegating to IPackageManager: " + e.getMessage());
                }
            }
        }
    }

    void grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException;
    void revokeRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException;
}
