package com.sony.sel.tvapp.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sony.sel.util.ObserverSet;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLAttribute;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.VideoBroadcast;
import org.fourthline.cling.support.model.item.VideoItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of DLNA helper based on cling DLNA libraries.
 */
public class ClingDlnaHelper extends BaseDlnaHelper {

  private static final String TAG = ClingDlnaHelper.class.getSimpleName();

  private ObserverSet<DlnaServiceObserver> serviceObservers = new ObserverSet<>(DlnaServiceObserver.class);
  private ContentObserver deviceObserver;
  private Map<String, List<DlnaObjects.DlnaObject>> dlnaCache = new HashMap<>();

  /**
   * Listener for the service connection.
   */
  private ServiceConnection serviceConnection = new ServiceConnection() {

    public void onServiceConnected(ComponentName className, IBinder service) {
      upnpService = (AndroidUpnpService) service;

      // Get ready for future device advertisements
      upnpService.getRegistry().addListener(registryListener);

      // Now add all devices to the list we already know about
      for (Device device : upnpService.getRegistry().getDevices()) {
        addDevice(device);
      }

      // Search asynchronously for all devices, they will respond soon
      upnpService.getControlPoint().search();

      // notify observers service is connected
      serviceObservers.proxy().onServiceConnected();
    }

    public void onServiceDisconnected(ComponentName className) {
      upnpService = null;

      // notify observers
      serviceObservers.proxy().onServiceDisconnected();
    }
  };

  /**
   * Listener for device registry changes
   */
  private RegistryListener registryListener = new DefaultRegistryListener() {

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
      addDevice(device);
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
      removeDevice(device);
    }

  };

  private AndroidUpnpService upnpService;
  private List<Device> deviceList = new ArrayList<>();

  public ClingDlnaHelper(Context context) {
    super(context);
  }

  @Override
  public void startDlnaService(DlnaServiceObserver observer) {
    if (observer != null) {
      serviceObservers.add(observer);
    }
    // This will start the UPnP service if it wasn't already started
    getContext().bindService(
        new Intent(getContext(), AndroidUpnpServiceImpl.class),
        serviceConnection,
        Context.BIND_AUTO_CREATE
    );
  }

  @Override
  public boolean isDlnaServiceStarted() {
    return upnpService != null;
  }

  @Override
  public void stopDlnaService() {
    getContext().unbindService(serviceConnection);
  }

  @Override
  public void unregisterContentObserver(ContentObserver contentObserver) {

  }

  @NonNull
  @Override
  public List<DlnaObjects.UpnpDevice> getDeviceList(@Nullable ContentObserver observer, boolean showAllDevices) {
    List<DlnaObjects.UpnpDevice> devices = new ArrayList<>();
    deviceObserver = observer;
    synchronized (deviceList) {
      for (Device device : deviceList) {
        if (showAllDevices || device.findService(new UDAServiceType("ContentDirectory")) != null) {
          DlnaObjects.UpnpDevice upnpDevice = new DlnaObjects.UpnpDevice();
          upnpDevice.setUdn(device.getIdentity().getUdn().toString());
          upnpDevice.setFriendlyName(device.getDetails().getFriendlyName());
          upnpDevice.setDeviceType(device.getType().getDisplayString());
          upnpDevice.setManufacturer(device.getDetails().getManufacturerDetails().getManufacturer());
          upnpDevice.setModelName(device.getDetails().getModelDetails().getModelName());
          upnpDevice.setModelNumber(device.getDetails().getModelDetails().getModelNumber());
          Icon icon = findBestIcon(device.getIcons());
          RemoteDeviceIdentity identity = (RemoteDeviceIdentity) device.getIdentity();
          if (icon != null && identity.getDescriptorURL() != null) {
            try {
              URI iconUri = identity.getDescriptorURL().toURI().resolve(icon.getUri());
              upnpDevice.setIcon(iconUri.toString());
            } catch (URISyntaxException e) {
              e.printStackTrace();
            }
          }
          devices.add(upnpDevice);
        }
      }
    }
    return devices;
  }

  Icon findBestIcon(Icon[] icons) {
    if (icons == null || icons.length == 0) {
      return null;
    }
    List<String> formats = Arrays.asList(
        "image/bmp",
        "image/gif",
        "image/jpeg",
        "image/png"
    );
    Icon bestIcon = icons[0];
    for (Icon icon : icons) {
      if (formats.indexOf(icon.getMimeType().getType()) > formats.indexOf(bestIcon.getMimeType().getType())) {
        bestIcon = icon;
      } else if (icon.getWidth() > bestIcon.getWidth()) {
        bestIcon = icon;
      }
    }
    return bestIcon;
  }

  @NonNull
  @Override
  public <T extends DlnaObjects.DlnaObject> List<T> getChildren(String udn, String parentId, final Class<T> childClass, @Nullable ContentObserver contentObserver, boolean useCache) {

    // check for cached data
    List<DlnaObjects.DlnaObject> cachedChildren = dlnaCache.get(udn + "/" + parentId);
    if (cachedChildren != null) {
      return (List<T>) cachedChildren;
    }

    Log.d(TAG, "Get children: udn =  " + udn + ", parentId = " + parentId + ".");
    final List<DlnaObjects.DlnaObject> children = new ArrayList<>();
    Device device = upnpService.getRegistry().getDevice(UDN.valueOf(udn), true);
    if (device == null) {
      // device not found
      return (List<T>) children;
    }
    Service service = device.findService(new UDAServiceType("ContentDirectory"));
    Browse browse = new Browse(service, parentId, BrowseFlag.DIRECT_CHILDREN) {

      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        Log.e(TAG, "Browse failure: " + defaultMsg);
        synchronized (children) {
          children.notifyAll();
        }
      }

      @Override
      public void received(ActionInvocation actionInvocation, DIDLContent didl) {
        Log.d(TAG, "Browse received.");
        synchronized (children) {
          for (Container item : didl.getContainers()) {
            DlnaObjects.DlnaObject object = parseDidlItem(item);
            if (object != null) {
              children.add(object);
            }
          }
          for (Item item : didl.getItems()) {
            DlnaObjects.DlnaObject object = parseDidlItem(item);
            if (object != null) {
              children.add(object);
            }
          }
          children.notifyAll();
        }
      }

      @Override
      public void updateStatus(Status status) {
        Log.d(TAG, "Browse status: " + status.getDefaultMessage());
      }
    };
    upnpService.getControlPoint().execute(browse);
    synchronized (children) {
      try {
        children.wait();
      } catch (InterruptedException e) {
        Log.e(TAG, e.getMessage());
      }
    }
    if (children.size() > 0) {
      dlnaCache.put(udn + "/" + parentId, children);
    }
    return (List<T>) children;
  }

  private DlnaObjects.DlnaObject parseDidlItem(DIDLObject object) {
    String clazz = object.getClazz().getValue();
    try {
      DlnaObjects.DlnaObject dlnaObject = DlnaObjects.DlnaClass.newInstance(clazz);
      dlnaObject.setTitle(object.getTitle());
      dlnaObject.setId(object.getId());
      dlnaObject.setUpnpClass(clazz);
      if (object.getResources() != null && object.getResources().size() > 0) {
        Res res = object.getResources().get(0);
        dlnaObject.setRes(res.getValue());
        dlnaObject.setProtocolInfo(res.getProtocolInfo().toString());
      }
      if (dlnaObject instanceof DlnaObjects.VideoItem) {
        VideoItem source = (VideoItem) object;
        DlnaObjects.VideoItem dest = (DlnaObjects.VideoItem) dlnaObject;
        dest.setDescription(source.getDescription());
        dest.setLongDescription(source.getLongDescription());
      }
      if (dlnaObject instanceof DlnaObjects.VideoBroadcast) {
        VideoBroadcast source = (VideoBroadcast) object;
        DlnaObjects.VideoBroadcast dest = (DlnaObjects.VideoBroadcast) dlnaObject;
        if (source.getIcon() != null) {
          dest.setIcon(source.getIcon().toString());
        }
        dest.setChannelNumber(source.getChannelNr().toString());
        dest.setCallSign(source.getTitle().split(" ")[1]);
      }
      if (dlnaObject instanceof DlnaObjects.VideoProgram) {
        Item source = (Item) object;
        DlnaObjects.VideoProgram dest = (DlnaObjects.VideoProgram) dlnaObject;
        for (DIDLObject.Property property : source.getProperties()) {
          if (property.getDescriptorName().equals("icon")) {
            dest.setIcon(property.getValue().toString());
          } else if (property.getDescriptorName().equals("rating")) {
            dest.setRating(property.getValue().toString());
          } else if (property.getDescriptorName().equals("genre")) {
            dest.setGenre(property.getValue().toString());
          } else if (property.getDescriptorName().equals("scheduledStartTime")) {
            dest.setScheduledStartTime(property.getValue().toString());
          } else if (property.getDescriptorName().equals("scheduledEndTime")) {
            dest.setScheduledEndTime(property.getValue().toString());
          } else if (property.getDescriptorName().equals("longDescription")) {
            dest.setLongDescription(property.getValue().toString());
          } else if (property.getDescriptorName().equals("programTitle")) {
            dest.setProgramTitle(property.getValue().toString());
          }
        }
        dest.setChannelId(source.getParentID().split("/")[2]);
        dest.setProgramTitle(source.getTitle());
      }
      return dlnaObject;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void addDevice(Device device) {
    Log.d(TAG, "Device added: " + device.getDisplayString());
    synchronized (deviceList) {
      deviceList.add(device);
    }
    if (deviceObserver != null) {
      deviceObserver.dispatchChange(false, null);
    }
  }

  private void removeDevice(Device device) {
    Log.d(TAG, "Device removed: " + device.getDisplayString());
    synchronized (deviceList) {
      deviceList.remove(device);
    }
    if (deviceObserver != null) {
      deviceObserver.dispatchChange(false, null);
    }
  }
}
