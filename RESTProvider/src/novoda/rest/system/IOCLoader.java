
package novoda.rest.system;

import novoda.rest.configuration.ProviderMetaData;
import novoda.rest.database.ModularSQLiteOpenHelper;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;

import java.io.IOException;
import java.util.List;

public class IOCLoader {

    private volatile static IOCLoader instance;

    private static final String METADATA_NAME = "novoda.rest";

    private Context context;

    private PackageManager pm;

    /* package */ProviderMetaData metaData;

    /* package */ProviderInfo providerInfo;

    /* package */IOCLoader(final Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
        providerInfo = getProviderInfo();
        metaData = getMetaData();
    }

    public static IOCLoader getInstance(final Context context) {
        if (instance == null) {
            synchronized (IOCLoader.class) {
                if (instance == null)
                    instance = new IOCLoader(context);
            }
        }
        return instance;
    }

    /* package */ProviderMetaData getMetaData() {
        try {
            final XmlResourceParser xml = getProviderInfo().loadXmlMetaData(pm, METADATA_NAME);
            return ProviderMetaData.loadFromXML(xml);
        } catch (XmlPullParserException e) {
            throw new LoaderException("XML not formed correctly", e);
        } catch (IOException e) {
            throw new LoaderException("IOException, can not open metadata file", e);
        }
    }

    /* package */ProviderInfo getProviderInfo() {
        // Get all providers associated with this process id. This might not
        // work with multi-process calls.
        List<ProviderInfo> providerInfo = pm.queryContentProviders(pm
                .getNameForUid(android.os.Process.myUid()), android.os.Process.myUid(), 0);
        for (ProviderInfo info : providerInfo) {
            if (getClass().getName().equals(info.name)) {
                return pm.resolveContentProvider(info.authority, PackageManager.GET_META_DATA);
            }
        }
        throw new LoaderException("can not load provider, provider not found for this process");
    }

    public ServiceInfo getServiceInfo() {
        return getServiceInfo(metaData.serviceClassName);
    }

    /**
     * @param service , the service name as declared in the manifest. With or
     *            without package nameF
     * @return The service info as described in the manifest with meta-data
     *         attached
     */
    public ServiceInfo getServiceInfo(final String service) {
        try {
            ComponentName component = new ComponentName(context, getService(service).getClass());
            return pm.getServiceInfo(component, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            throw new LoaderException(e);
        }
    }

    /* package */Service getService(final String name) {
        try {
            return (Service) Class.forName(appendPackageToName(name)).newInstance();
        } catch (Exception e) {
            throw new LoaderException(e);
        }
    }

    private String appendPackageToName(final String name) {
        if (name.toCharArray()[0] == '.') {
            return new StringBuilder(context.getPackageName()).append(name).toString();
        }
        return name;
    }

    public ModularSQLiteOpenHelper getSQLiteHelper() {
        return null;
    }
}
